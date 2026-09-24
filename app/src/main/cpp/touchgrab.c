/*
 * touchgrab — privileged touchscreen event grabber for the SRT X sensitivity engine.
 *
 * Runs as uid 2000 (shell) via a Shizuku user-service. It enumerates /dev/input,
 * picks the direct multi-touch touchscreen, ioctl(EVIOCGRAB)s it exclusively, and
 * streams raw ABS_MT events to stdout as text. The controlling Java process reads
 * those events, applies a gain/curve/smoothing transform, and re-injects transformed
 * MotionEvents via InputManager — so the game sees amplified touch displacement.
 *
 * Safety is paramount: an exclusive grab that is never released makes the whole
 * touchscreen unresponsive. Three independent guards release the grab no matter what:
 *   1. prctl(PR_SET_PDEATHSIG, SIGKILL) — kernel kills us if the parent dies.
 *   2. A stdin heartbeat — the controller writes a byte periodically; if it stops
 *      for HEARTBEAT_TIMEOUT_MS we assume the controller is wedged and bail out.
 *   3. A signal handler on every terminating signal that always ungrabs + closes.
 *
 * Protocol (stdout, one message per line):
 *   TOUCHGRAB_READY
 *   TOUCHGRAB_CANDIDATE <path> score=<n> name=<name> x=<min>:<max> y=<min>:<max>
 *   TOUCHGRAB_DEVICE <path>
 *   TOUCHGRAB_NAME <name>
 *   TOUCHGRAB_RANGE <x_min> <x_max> <y_min> <y_max>
 *   TOUCHGRAB_STATUS grabbed <path>
 *   TOUCHGRAB_STATUS detect_complete_no_grab
 *   TOUCHGRAB_STATUS heartbeat_test_no_grab
 *   TOUCHGRAB_STATUS heartbeat_timeout
 *   TOUCHGRAB_ERROR touchscreen_not_found
 *   TOUCHGRAB_ERROR grab_failed errno=<e> message=<m>
 *   TOUCHGRAB_ERROR open_failed path=<p> errno=<e> message=<m>
 *   TOUCHGRAB_ERROR input_dir_open_failed errno=<e> message=<m>
 *   TOUCHGRAB_ERROR device_not_direct_touch <path>
 *   TOUCHGRAB_ERROR device_missing_mt_xy <path>
 *   TOUCHGRAB_ERROR usage: ...
 *   EV_ABS ABS_MT_SLOT <hex>
 *   EV_ABS ABS_MT_TRACKING_ID <hex>
 *   EV_ABS ABS_MT_POSITION_X <hex>
 *   EV_ABS ABS_MT_POSITION_Y <hex>
 *   EV_SYN SYN_REPORT <hex>
 *
 * stdin: any byte is a heartbeat. The controller sends '.' every ~500ms.
 */

#include <dirent.h>
#include <errno.h>
#include <fcntl.h>
#include <poll.h>
#include <signal.h>
#include <stdarg.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>
#include <unistd.h>

#include <sys/ioctl.h>
#include <sys/prctl.h>
#include <sys/types.h>

#include <linux/input.h>

/* Some NDK/kernel header combos do not expose input-event-codes directly. */
#ifndef INPUT_PROP_DIRECT
#define INPUT_PROP_DIRECT 0x01
#endif
#ifndef ABS_MT_SLOT
#define ABS_MT_SLOT 0x2f
#endif
#ifndef ABS_MT_POSITION_X
#define ABS_MT_POSITION_X 0x35
#endif
#ifndef ABS_MT_POSITION_Y
#define ABS_MT_POSITION_Y 0x36
#endif
#ifndef ABS_MT_TRACKING_ID
#define ABS_MT_TRACKING_ID 0x39
#endif
#ifndef EVIOCGRAB
#define EVIOCGRAB _IOW('E', 0x90, int)
#endif

#define HEARTBEAT_TIMEOUT_MS 2000
#define INPUT_DIR "/dev/input"

#define BITS_PER_LONG (sizeof(long) * 8)
#define NBITS(x) ((((x) - 1) / BITS_PER_LONG) + 1)
#define OFF(x) ((x) % BITS_PER_LONG)
#define LONG(x) ((x) / BITS_PER_LONG)
#define TEST_BIT(bit, array) ((array[LONG(bit)] >> OFF(bit)) & 1)

/* --- globals (mirror the recovered design) --- */
static int g_touch_fd = -1;
static char g_device_path[256];
static char g_device_name[256];
static int g_abs_x_min = 0, g_abs_x_max = 0;
static int g_abs_y_min = 0, g_abs_y_max = 0;
static pid_t g_parent_pid = 0;
static volatile sig_atomic_t g_stop_requested = 0;
static int g_grabbed = 0;
static long g_last_heartbeat_ms = 0;

/* --- async-signal-safe-ish line writer straight to fd 1 --- */
static void safe_printf(const char *fmt, ...) {
    char buf[1024];
    va_list ap;
    va_start(ap, fmt);
    int n = vsnprintf(buf, sizeof(buf), fmt, ap);
    va_end(ap);
    if (n < 0) return;
    if (n > (int)sizeof(buf)) n = (int)sizeof(buf);
    ssize_t ignored = write(1, buf, (size_t)n);
    (void)ignored;
}

static long now_ms(void) {
    struct timespec ts;
    clock_gettime(CLOCK_MONOTONIC, &ts);
    return (long)ts.tv_sec * 1000L + ts.tv_nsec / 1000000L;
}

static void cleanup(void) {
    if (g_touch_fd >= 0) {
        if (g_grabbed) {
            ioctl(g_touch_fd, EVIOCGRAB, 0);
            g_grabbed = 0;
        }
        close(g_touch_fd);
        g_touch_fd = -1;
    }
}

static void signal_handler(int signum) {
    (void)signum;
    g_stop_requested = 1;
    cleanup();
    _exit(0);
}

static void install_signal_handlers(void) {
    struct sigaction sa;
    memset(&sa, 0, sizeof(sa));
    sigemptyset(&sa.sa_mask);
    sa.sa_handler = signal_handler;
    sigaction(SIGTERM, &sa, NULL);
    sigaction(SIGINT, &sa, NULL);
    sigaction(SIGHUP, &sa, NULL);
    sigaction(SIGPIPE, &sa, NULL);
}

/*
 * Inspect one device path. Fills name + abs ranges. Returns a score:
 *   -1  cannot open / not an eligible device
 *    0+ eligible touchscreen, higher is better.
 * When quiet==0 a TOUCHGRAB_CANDIDATE / diagnostic line is emitted.
 */
static int inspect_device(const char *path, int quiet, char *out_name,
                          int *x_min, int *x_max, int *y_min, int *y_max) {
    int fd = open(path, O_RDONLY | O_NONBLOCK);
    if (fd < 0) {
        if (!quiet) {
            safe_printf("TOUCHGRAB_ERROR open_failed path=%s errno=%d message=%s\n",
                        path, errno, strerror(errno));
        }
        return -1;
    }

    unsigned long ev_bits[NBITS(EV_MAX)];
    unsigned long abs_bits[NBITS(ABS_MAX)];
    unsigned long prop_bits[NBITS(INPUT_PROP_MAX)];
    memset(ev_bits, 0, sizeof(ev_bits));
    memset(abs_bits, 0, sizeof(abs_bits));
    memset(prop_bits, 0, sizeof(prop_bits));

    char name[256];
    memset(name, 0, sizeof(name));
    if (ioctl(fd, EVIOCGNAME(sizeof(name) - 1), name) < 0) {
        strncpy(name, "(unknown)", sizeof(name) - 1);
    }

    ioctl(fd, EVIOCGBIT(0, sizeof(ev_bits)), ev_bits);

    /* Must support absolute axes at all. */
    if (!TEST_BIT(EV_ABS, ev_bits)) {
        close(fd);
        return -1;
    }

    ioctl(fd, EVIOCGBIT(EV_ABS, sizeof(abs_bits)), abs_bits);

    /* Must report multi-touch X and Y — otherwise it is not a touchscreen. */
    if (!TEST_BIT(ABS_MT_POSITION_X, abs_bits) || !TEST_BIT(ABS_MT_POSITION_Y, abs_bits)) {
        if (!quiet) {
            safe_printf("TOUCHGRAB_ERROR device_missing_mt_xy %s\n", path);
        }
        close(fd);
        return -1;
    }

    int has_direct = 0;
    if (ioctl(fd, EVIOCGPROP(sizeof(prop_bits)), prop_bits) >= 0) {
        has_direct = TEST_BIT(INPUT_PROP_DIRECT, prop_bits) ? 1 : 0;
    }
    if (!has_direct) {
        /* A digitizer without DIRECT is a trackpad/pen tablet: sensitivity does not apply. */
        if (!quiet) {
            safe_printf("TOUCHGRAB_ERROR device_not_direct_touch %s\n", path);
        }
        close(fd);
        return -1;
    }

    struct input_absinfo ax, ay;
    memset(&ax, 0, sizeof(ax));
    memset(&ay, 0, sizeof(ay));
    ioctl(fd, EVIOCGABS(ABS_MT_POSITION_X), &ax);
    ioctl(fd, EVIOCGABS(ABS_MT_POSITION_Y), &ay);

    int score = 10;
    if (TEST_BIT(ABS_MT_SLOT, abs_bits)) score += 5;        /* modern protocol B */
    if (TEST_BIT(ABS_MT_TRACKING_ID, abs_bits)) score += 5;
    if (strcasestr(name, "touch") != NULL) score += 10;      /* prefer the panel */
    if (strcasestr(name, "ts") != NULL) score += 2;
    if ((ax.maximum - ax.minimum) > 200 && (ay.maximum - ay.minimum) > 200) score += 5;

    if (out_name) {
        strncpy(out_name, name, 255);
        out_name[255] = '\0';
    }
    if (x_min) *x_min = ax.minimum;
    if (x_max) *x_max = ax.maximum;
    if (y_min) *y_min = ay.minimum;
    if (y_max) *y_max = ay.maximum;

    if (!quiet) {
        safe_printf("TOUCHGRAB_CANDIDATE %s score=%d name=%s x=%d:%d y=%d:%d\n",
                    path, score, name, ax.minimum, ax.maximum, ay.minimum, ay.maximum);
    }

    close(fd);
    return score;
}

/*
 * Enumerate /dev/input and select the best-scoring touchscreen into the globals.
 * Returns 0 on success, -1 if none found. Emits candidate lines unless quiet.
 */
static int find_touchscreen_auto(int quiet) {
    DIR *dir = opendir(INPUT_DIR);
    if (!dir) {
        safe_printf("TOUCHGRAB_ERROR input_dir_open_failed errno=%d message=%s\n",
                    errno, strerror(errno));
        return -1;
    }

    int best_score = -1;
    char best_path[256];
    char best_name[256];
    int bx0 = 0, bx1 = 0, by0 = 0, by1 = 0;
    best_path[0] = '\0';
    best_name[0] = '\0';

    struct dirent *entry;
    while ((entry = readdir(dir)) != NULL) {
        if (strncmp(entry->d_name, "event", 5) != 0) continue;
        char path[256];
        snprintf(path, sizeof(path), "%s/%s", INPUT_DIR, entry->d_name);

        char name[256];
        int x0, x1, y0, y1;
        int score = inspect_device(path, quiet, name, &x0, &x1, &y0, &y1);
        if (score > best_score) {
            best_score = score;
            strncpy(best_path, path, sizeof(best_path) - 1);
            best_path[sizeof(best_path) - 1] = '\0';
            strncpy(best_name, name, sizeof(best_name) - 1);
            best_name[sizeof(best_name) - 1] = '\0';
            bx0 = x0; bx1 = x1; by0 = y0; by1 = y1;
        }
    }
    closedir(dir);

    if (best_score < 0 || best_path[0] == '\0') {
        safe_printf("TOUCHGRAB_ERROR touchscreen_not_found\n");
        return -1;
    }

    strncpy(g_device_path, best_path, sizeof(g_device_path) - 1);
    g_device_path[sizeof(g_device_path) - 1] = '\0';
    strncpy(g_device_name, best_name, sizeof(g_device_name) - 1);
    g_device_name[sizeof(g_device_name) - 1] = '\0';
    g_abs_x_min = bx0; g_abs_x_max = bx1;
    g_abs_y_min = by0; g_abs_y_max = by1;
    return 0;
}

/* Open a caller-specified /dev/input/eventX, validating it is a touchscreen. */
static int open_explicit_device(const char *path) {
    char name[256];
    int x0, x1, y0, y1;
    int score = inspect_device(path, 1, name, &x0, &x1, &y0, &y1);
    if (score < 0) {
        return -1;
    }
    strncpy(g_device_path, path, sizeof(g_device_path) - 1);
    g_device_path[sizeof(g_device_path) - 1] = '\0';
    strncpy(g_device_name, name, sizeof(g_device_name) - 1);
    g_device_name[sizeof(g_device_name) - 1] = '\0';
    g_abs_x_min = x0; g_abs_x_max = x1;
    g_abs_y_min = y0; g_abs_y_max = y1;
    return 0;
}

/* Open g_device_path and grab it exclusively. Returns 0 on success. */
static int grab_device(void) {
    /* Non-blocking: the active loop is poll-driven and drains until EAGAIN, so a
     * read must never block between events (that would stall the heartbeat and
     * parent-death checks the whole safety model depends on). */
    g_touch_fd = open(g_device_path, O_RDONLY | O_NONBLOCK);
    if (g_touch_fd < 0) {
        safe_printf("TOUCHGRAB_ERROR open_failed path=%s errno=%d message=%s\n",
                    g_device_path, errno, strerror(errno));
        return -1;
    }
    if (ioctl(g_touch_fd, EVIOCGRAB, 1) < 0) {
        safe_printf("TOUCHGRAB_ERROR grab_failed errno=%d message=%s\n",
                    errno, strerror(errno));
        close(g_touch_fd);
        g_touch_fd = -1;
        return -1;
    }
    g_grabbed = 1;
    return 0;
}

static void emit_device_info(void) {
    safe_printf("TOUCHGRAB_DEVICE %s\n", g_device_path);
    safe_printf("TOUCHGRAB_NAME %s\n", g_device_name);
    safe_printf("TOUCHGRAB_RANGE %d %d %d %d\n",
                g_abs_x_min, g_abs_x_max, g_abs_y_min, g_abs_y_max);
}

/* Drain stdin heartbeat bytes; update g_last_heartbeat_ms. Returns 1 if EOF. */
static int drain_stdin_heartbeat(void) {
    char buf[64];
    ssize_t r;
    int eof = 0;
    while ((r = read(0, buf, sizeof(buf))) > 0) {
        g_last_heartbeat_ms = now_ms();
    }
    if (r == 0) eof = 1; /* controller closed the pipe */
    return eof;
}

/*
 * Grab the device and stream events until: heartbeat timeout, parent death,
 * stdin EOF, or a terminating signal. Always releases the grab on exit.
 */
static int run_active_loop(void) {
    if (grab_device() != 0) {
        return 1;
    }
    emit_device_info();
    safe_printf("TOUCHGRAB_STATUS grabbed %s\n", g_device_path);
    safe_printf("TOUCHGRAB_READY\n");

    g_last_heartbeat_ms = now_ms();

    struct pollfd fds[2];
    fds[0].fd = g_touch_fd;
    fds[0].events = POLLIN;
    fds[1].fd = 0; /* stdin */
    fds[1].events = POLLIN;

    struct input_event ev;

    while (!g_stop_requested) {
        int pr = poll(fds, 2, 200);
        if (pr < 0) {
            if (errno == EINTR) continue;
            break;
        }

        /* Parent gone? Bail immediately (belt-and-braces with PDEATHSIG). */
        if (g_parent_pid > 0 && getppid() != g_parent_pid) {
            break;
        }

        if (pr > 0) {
            if (fds[1].revents & (POLLIN | POLLHUP)) {
                if (drain_stdin_heartbeat()) {
                    break; /* controller closed stdin */
                }
            }
            if (fds[0].revents & POLLIN) {
                ssize_t n = read(g_touch_fd, &ev, sizeof(ev));
                while (n == sizeof(ev)) {
                    if (ev.type == EV_ABS) {
                        switch (ev.code) {
                            case ABS_MT_SLOT:
                                safe_printf("EV_ABS ABS_MT_SLOT %08x\n", ev.value);
                                break;
                            case ABS_MT_TRACKING_ID:
                                safe_printf("EV_ABS ABS_MT_TRACKING_ID %08x\n", ev.value);
                                break;
                            case ABS_MT_POSITION_X:
                                safe_printf("EV_ABS ABS_MT_POSITION_X %08x\n", ev.value);
                                break;
                            case ABS_MT_POSITION_Y:
                                safe_printf("EV_ABS ABS_MT_POSITION_Y %08x\n", ev.value);
                                break;
                            default:
                                break;
                        }
                    } else if (ev.type == EV_SYN && ev.code == SYN_REPORT) {
                        safe_printf("EV_SYN SYN_REPORT %08x\n", ev.value);
                    }
                    n = read(g_touch_fd, &ev, sizeof(ev));
                }
            }
        }

        /* Heartbeat watchdog. */
        if (now_ms() - g_last_heartbeat_ms > HEARTBEAT_TIMEOUT_MS) {
            safe_printf("TOUCHGRAB_STATUS heartbeat_timeout\n");
            break;
        }
    }

    cleanup();
    return 0;
}

/* --detect: enumerate + report, never grab. */
static int run_detect(void) {
    int rc = find_touchscreen_auto(0);
    if (rc == 0) {
        emit_device_info();
    }
    safe_printf("TOUCHGRAB_STATUS detect_complete_no_grab\n");
    return 0;
}

/* --heartbeat-test: verify the stdin heartbeat plumbing without grabbing. */
static int run_heartbeat_test(void) {
    safe_printf("TOUCHGRAB_READY\n");
    g_last_heartbeat_ms = now_ms();
    struct pollfd fds[1];
    fds[0].fd = 0;
    fds[0].events = POLLIN;
    while (!g_stop_requested) {
        int pr = poll(fds, 1, 200);
        if (pr < 0 && errno == EINTR) continue;
        if (pr > 0 && (fds[0].revents & (POLLIN | POLLHUP))) {
            if (drain_stdin_heartbeat()) break;
        }
        if (now_ms() - g_last_heartbeat_ms > HEARTBEAT_TIMEOUT_MS) {
            safe_printf("TOUCHGRAB_STATUS heartbeat_timeout\n");
            break;
        }
    }
    safe_printf("TOUCHGRAB_STATUS heartbeat_test_no_grab\n");
    return 0;
}

int main(int argc, char **argv) {
    /* Line-buffered / unbuffered stdout so the controller sees events promptly. */
    setvbuf(stdout, NULL, _IONBF, 0);

    g_parent_pid = getppid();
    prctl(PR_SET_PDEATHSIG, SIGKILL, 0, 0, 0);
    install_signal_handlers();

    /* Non-blocking stdin: drain_stdin_heartbeat() must return promptly on EAGAIN
     * instead of blocking until the next heartbeat byte, which would stall the
     * event loop for up to a full heartbeat interval. */
    int stdin_fl = fcntl(0, F_GETFL, 0);
    if (stdin_fl >= 0) {
        fcntl(0, F_SETFL, stdin_fl | O_NONBLOCK);
    }

    memset(g_device_path, 0, sizeof(g_device_path));
    memset(g_device_name, 0, sizeof(g_device_name));

    if (argc < 2) {
        safe_printf("TOUCHGRAB_ERROR usage: %s --heartbeat-test | %s --detect | %s --auto | %s /dev/input/eventX\n",
                    argv[0], argv[0], argv[0], argv[0]);
        return 2;
    }

    if (strcmp(argv[1], "--heartbeat-test") == 0) {
        return run_heartbeat_test();
    }
    if (strcmp(argv[1], "--detect") == 0) {
        return run_detect();
    }
    if (strcmp(argv[1], "--auto") == 0) {
        if (find_touchscreen_auto(1) != 0) {
            return 1;
        }
        return run_active_loop();
    }

    /* Explicit device path. */
    if (open_explicit_device(argv[1]) != 0) {
        safe_printf("TOUCHGRAB_ERROR open_failed path=%s errno=%d message=%s\n",
                    argv[1], errno, strerror(errno));
        return 1;
    }
    return run_active_loop();
}
