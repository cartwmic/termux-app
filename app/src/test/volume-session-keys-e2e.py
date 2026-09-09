#!/usr/bin/env python3
"""Run against a disposable, bootstrapped Android emulator with the APK installed.

Usage: python3 app/src/test/volume-session-keys-e2e.py emulator-5554
Requires a rootable ARM64 Android 14 emulator, adb, and ANDROID_HOME with
NDK 29.0.14206865 on macOS. Restarts Termux, changes its config and opens sessions.
Drives Android key events and checks which real shell receives a command; no SSH
or external backend is involved. Never run against a personal phone.
"""
import os
from pathlib import Path
import re
import shlex
import subprocess
import sys
import time
import tempfile
import uuid
import xml.etree.ElementTree as ET

serial = sys.argv[1]
if not serial.startswith("emulator-"):
    raise SystemExit("Only disposable emulator serials are accepted")


def adb(*args):
    return subprocess.check_output(["adb", "-s", serial, *args], text=True, timeout=30)


def shell(*args):
    return adb("shell", shlex.join(args))


def wait_for(check, label):
    deadline = time.monotonic() + 10
    while time.monotonic() < deadline:
        if check():
            return
        time.sleep(0.1)
    raise AssertionError(label)


def focus_terminal():
    shell("uiautomator", "dump", "/sdcard/volume-session-test.xml")
    root = ET.fromstring(shell("cat", "/sdcard/volume-session-test.xml"))
    node = next(n for n in root.iter("node")
                if n.get("resource-id") == "com.termux:id/terminal_view")
    x1, y1, x2, y2 = map(int, re.findall(r"\d+", node.get("bounds")))
    shell("input", "tap", str((x1 + x2) // 2), str((y1 + y2) // 2))


def command(text):
    shell("input", "text", text.replace(" ", "%s"))
    shell("input", "keyevent", "ENTER")


home = "/data/data/com.termux/files/home"
config = home + "/.termux/termux.properties"
probe = home + "/.volume-session-test"


def configure(mode):
    content = (f"volume-keys = {mode}\nshortcut.previous-session = ctrl + 1\n"
               "shortcut.next-session = ctrl + 2\n")
    shell("sh", "-c", f"mkdir -p {home}/.termux; printf %s {shlex.quote(content)} > {config}; chmod 644 {config}")
    shell("am", "broadcast", "-a", "com.termux.app.reload_style", "-p", "com.termux")
    focus_terminal()


def assert_slot(expected):
    token = uuid.uuid4().hex
    command(f"echo {token}:$VOLUME_TEST_SLOT > {probe}")
    wait_for(lambda: shell("cat", probe).strip() == f"{token}:{expected}",
             f"active shell should be {expected}")


def volume(key, long=False):
    # Android's `input keyevent` substitutes device -1 (alphabetic). Use a
    # kernel input device instead, matching dedicated phone volume buttons.
    args = ["/data/local/tmp/volume-key-input", key]
    if long:
        args.append("repeat")
    shell(*args)


adb("root")
adb("wait-for-device")
sdk = Path(os.environ["ANDROID_HOME"])
with tempfile.TemporaryDirectory(prefix="volume-key-input-") as tmp:
    compiler = sdk / "ndk/29.0.14206865/toolchains/llvm/prebuilt/darwin-x86_64/bin/aarch64-linux-android28-clang"
    binary = str(Path(tmp) / "volume-key-input")
    subprocess.run([str(compiler), "-Wall", "-Wextra", "-Werror", "-o", binary,
                    str(Path(__file__).with_name("volume-key-input.c"))], check=True)
    adb("push", binary, "/data/local/tmp/volume-key-input")
    shell("chmod", "755", "/data/local/tmp/volume-key-input")
shell("am", "force-stop", "com.termux")
shell("am", "start", "-n", "com.termux/.app.TermuxActivity")
configure("sessions")
command("VOLUME_TEST_SLOT=first")
assert_slot("first")
volume("VOLUME_UP")
assert_slot("first")
volume("VOLUME_DOWN")
assert_slot("first")
print("PASS: single session remains selected")
for name in ("second", "third"):
    shell("input", "keycombination", "CTRL_LEFT", "ALT_LEFT", "C")
    focus_terminal()
    command(f"VOLUME_TEST_SLOT={name}")
    assert_slot(name)

volume("VOLUME_UP")
assert_slot("second")
volume("VOLUME_UP")
assert_slot("first")
volume("VOLUME_UP")
assert_slot("third")
volume("VOLUME_DOWN")
assert_slot("first")
volume("VOLUME_DOWN", long=True)
assert_slot("second")
print("PASS: previous, next, wraparound, one switch per long press")

shell("input", "keycombination", "CTRL_LEFT", "KEYCODE_1")
assert_slot("first")
shell("input", "keycombination", "CTRL_LEFT", "KEYCODE_2")
assert_slot("second")
print("PASS: Ctrl+1/2 retained")

configure("virtual")
volume("VOLUME_UP")
volume("VOLUME_DOWN")
assert_slot("second")
configure("volume")
volume("VOLUME_UP")
volume("VOLUME_DOWN")
assert_slot("second")
configure("sessions")
volume("VOLUME_DOWN")
assert_slot("third")
print("PASS: mode reload disables and re-enables session switching")
