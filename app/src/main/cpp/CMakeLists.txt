# CMakeLists for the touchgrab privileged touchscreen grabber.
#
# The build target is a PIE *executable*, but it must be emitted as
# "libtouchgrab.so" so that AGP packages it into lib/<abi>/. AGP's native
# packaging only merges files matching lib*.so from the CMake output dir into
# the APK, and — since API 29 — an app may not exec() from its writable data
# dir while uid 2000 (shell) cannot read /data/data/<pkg>/. Files under
# /data/app/.../lib/<abi>/ are 0755 and world-readable, so uid 2000 can exec
# libtouchgrab.so directly. It is a normal executable that merely carries the
# .so name; nothing ever dlopen()s it.
#
# CMake does NOT auto-prefix executables (unlike SHARED libraries), so we set
# PREFIX/SUFFIX explicitly to produce "libtouchgrab.so" rather than "touchgrab".

cmake_minimum_required(VERSION 3.22.1)

project(touchgrab C)

add_executable(touchgrab touchgrab.c)

# Emit as libtouchgrab.so so AGP's lib*.so glob packages it into lib/<abi>/.
set_target_properties(touchgrab PROPERTIES
    PREFIX "lib"
    OUTPUT_NAME "touchgrab"
    SUFFIX ".so"
)

# Build as a position-independent executable (Android requires PIE) and keep the
# binary small. -fPIE/-pie make it a runnable executable rather than a library.
target_compile_options(touchgrab PRIVATE
    -Wall
    -Wextra
    -O2
    -fPIE
    -D_GNU_SOURCE          # strcasestr lives behind _GNU_SOURCE on bionic
)

target_link_options(touchgrab PRIVATE
    -pie
)
