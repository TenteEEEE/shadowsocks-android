from __future__ import absolute_import, print_function, unicode_literals

import os
import shlex
import shutil
import subprocess
import sys

try:
    rustcc = os.environ["RUST_ANDROID_GRADLE_CC"]
except KeyError:
    print("ERROR: RUST_ANDROID_GRADLE_CC environment variable is not set")
    sys.exit(1)

if sys.platform == "msys" or sys.platform == "cygwin":
    import ctypes

    cygdll = "cygwin1.dll" if sys.platform == "cygwin" else "msys-2.0.dll"
    cygwin = ctypes.cdll.LoadLibrary(cygdll)

    def win2posix(path):
        CCP_WIN_W_TO_POSIX = 3
        size = cygwin.cygwin_conv_path(CCP_WIN_W_TO_POSIX, path, 0, 0)
        retval = ctypes.create_string_buffer(size)
        cygwin.cygwin_conv_path(CCP_WIN_W_TO_POSIX, path, retval, size)
        return retval.value.decode()

    rustcc = win2posix(rustcc)

try:
    link_arg = os.environ["RUST_ANDROID_GRADLE_CC_LINK_ARG"]
except KeyError:
    print("ERROR: RUST_ANDROID_GRADLE_CC_LINK_ARG environment variable is not set")
    sys.exit(1)

args = [rustcc, link_arg] + sys.argv[1:]


def update_in_place(arglist):
    # The `gcc` library is not included starting from NDK version 23.
    # Work around by using `unwind` replacement.
    try:
        ndk_major_version = os.environ["CARGO_NDK_MAJOR_VERSION"]
    except KeyError:
        print("WARNING: CARGO_NDK_MAJOR_VERSION environment variable is not set, skipping GCC workaround")
        return
    if ndk_major_version.isdigit():
        if 23 <= int(ndk_major_version):
            for i, arg in enumerate(arglist):
                if arg.startswith("-lgcc"):
                    # This is one way to preserve line endings.
                    arglist[i] = "-lunwind" + arg[len("-lgcc") :]


update_in_place(args)

for arg in args:
    if arg.startswith("@"):
        fileargs = open(arg[1:], "r").read().splitlines(keepends=True)
        update_in_place(fileargs)
        open(arg[1:], "w").write("".join(fileargs))

linkargfileName = ""
if (sys.platform == "msys" or sys.platform == "cygwin") and len("".join(args)) > 8191:
    import codecs
    import tempfile

    def posix2win(path):
        CCP_POSIX_TO_WIN_W = 1
        size = cygwin.cygwin_conv_path(CCP_POSIX_TO_WIN_W, str(path).encode(), 0, 0)
        retval = ctypes.create_unicode_buffer(size)
        cygwin.cygwin_conv_path(CCP_POSIX_TO_WIN_W, str(path).encode(), retval, size)
        return retval.value

    # response file should be use UTF-16 with BOM
    linkargfile = tempfile.NamedTemporaryFile(delete=False)
    linkargfile.write(codecs.BOM_UTF16_LE)
    linkargfile.write("\n".join(sys.argv[1:]).encode("utf-16-le"))
    linkargfile.close()
    linkargfileName = linkargfile.name
    linkargfileNameW = posix2win(linkargfileName)
    args = [rustcc, os.environ["RUST_ANDROID_GRADLE_CC_LINK_ARG"], "@" + linkargfileNameW]


# デバッグ情報を出力
print(f"DEBUG: Working directory: {os.getcwd()}")
print(f"DEBUG: Python executable: {sys.executable}")
print(f"DEBUG: Python version: {sys.version}")

# This only appears when the subprocess call fails, but it's helpful then.
printable_cmd = " ".join(shlex.quote(arg) for arg in args)
print(f"DEBUG: Executing command: {printable_cmd}")

code = subprocess.call(args)
print(f"DEBUG: Command exit code: {code}")
if code == 0:
    sys_argv = sys.argv
    if sys.platform == "msys" or sys.platform == "cygwin" or sys.platform == "win32":
        linkargs = list(filter(lambda s: s.startswith("@") and s.find("linker-arguments") != -1, sys.argv[1:]))
        if linkargs != []:
            with open(linkargs[0][1:]) as f:
                sys_argv = f.read().splitlines()
    try:
        target_path = os.environ["RUST_ANDROID_GRADLE_TARGET"]
        shutil.copyfile(sys_argv[sys_argv.index("-o") + 1], target_path)
    except KeyError:
        print("WARNING: RUST_ANDROID_GRADLE_TARGET environment variable is not set, skipping copy")
    except ValueError:
        print("WARNING: Could not find output file in arguments")
    except Exception as e:
        print(f"WARNING: Failed to copy output file: {e}")
if linkargfileName != "":
    os.unlink(linkargfileName)
sys.exit(code)
