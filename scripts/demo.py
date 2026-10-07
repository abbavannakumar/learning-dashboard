"""Capture the assignment demo on an emulator (Python standard library + adb).

Install the APK first. Usage: python scripts/demo.py --serial emulator-5554
Starts with existing data; use a fresh app install for the 65 -> 70% example.
Network settings are restored even if a check fails. Outputs go to artifacts/.
"""
import argparse
import pathlib
import re
import subprocess
import time
import xml.etree.ElementTree as ET

parser = argparse.ArgumentParser()
parser.add_argument("--serial", required=True)
args = parser.parse_args()
ADB = ["adb", "-s", args.serial]
OUT = pathlib.Path(__file__).resolve().parents[1] / "artifacts"
OUT.mkdir(exist_ok=True)


def adb(*args):
    return subprocess.check_output(ADB + list(args), text=True, encoding="utf-8").strip()


def nodes():
    try:
        output = adb("shell", "uiautomator", "dump", "/sdcard/learning-ui.xml")
        if "dumped" not in output:
            return []
        return list(ET.fromstring(adb("shell", "cat", "/sdcard/learning-ui.xml")).iter("node"))
    except (subprocess.CalledProcessError, ET.ParseError):
        return []  # The emulator may expose no root during an activity transition.


def find(text, timeout=20):
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        for node in nodes():
            if node.get("text") == text:
                return node
        time.sleep(0.5)
    raise AssertionError(f"UI text not found: {text}")


def tap(text):
    node = find(text)
    x1, y1, x2, y2 = map(int, re.findall(r"\d+", node.attrib["bounds"]))
    adb("shell", "input", "tap", str((x1 + x2) // 2), str((y1 + y2) // 2))
    time.sleep(0.5)


def screenshot(name):
    adb("shell", "screencap", "-p", "/sdcard/learning-screen.png")
    adb("pull", "/sdcard/learning-screen.png", str(OUT / f"{name}.png"))
    print(f"Captured {name}", flush=True)


def login():
    find("Welcome back")
    tap("Email")
    adb("shell", "input", "text", "demo@example.com")
    tap("Password")
    adb("shell", "input", "text", "password123")
    adb("shell", "input", "keyevent", "4")
    tap("Log in")
    find("Python Programming")


airplane = adb("shell", "settings", "get", "global", "airplane_mode_on")
wifi = adb("shell", "settings", "get", "global", "wifi_on")
recording = None
try:
    adb("shell", "cmd", "connectivity", "airplane-mode", "disable")
    adb("shell", "svc", "wifi", "enable")
    adb("shell", "am", "force-stop", "com.example.learning")
    adb("shell", "am", "start", "-n", "com.example.learning/.MainActivity")
    find("Welcome back")
    recording = subprocess.Popen(ADB + ["shell", "screenrecord", "--size", "720x1280", "--bit-rate", "2000000", "--time-limit", "120", "/sdcard/learning-demo.mp4"])
    screenshot("01-login")
    login()
    screenshot("02-dashboard")
    time.sleep(2)
    tap("Continue")
    find("Course details")
    screenshot("03-details")
    # Scroll to the first pending lesson; completed lessons are at the top.
    for _ in range(10):
        pending = [node for node in nodes() if node.get("text") == "Mark completed"]
        if pending:
            break
        adb("shell", "input", "swipe", "540", "1550", "540", "600", "350")
    else:
        raise AssertionError("No pending lesson to complete")
    tap("Mark completed")
    screenshot("04-completed")
    tap("Back")
    find("70%")
    tap("Refresh")
    time.sleep(2)
    find("70%")
    screenshot("05-updated-progress")
    print("Verified completion and refresh preserve 70% progress", flush=True)
    # Show airplane-mode status in the system shade for the recording.
    adb("shell", "cmd", "connectivity", "airplane-mode", "enable")
    adb("shell", "svc", "wifi", "disable")
    adb("shell", "cmd", "statusbar", "expand-settings")
    time.sleep(3)
    adb("shell", "cmd", "statusbar", "collapse")
    tap("Refresh")
    time.sleep(2)
    find("70%")
    warning = "You're offline. Connect to refresh your courses.\nShowing saved courses."
    find(warning)
    screenshot("06-offline-cache")
    adb("shell", "am", "force-stop", "com.example.learning")
    adb("shell", "am", "start", "-n", "com.example.learning/.MainActivity")
    login()
    find("70%")
    find(warning)
    screenshot("07-offline-restart")
    tap("Continue")
    find("70%")
    screenshot("08-offline-details")
    print("Verified saved courses and completion after offline restart", flush=True)
    time.sleep(3)
finally:
    try:
        if recording:
            result = subprocess.run(ADB + ["shell", "pidof", "screenrecord"], capture_output=True, text=True)
            pid = result.stdout.strip()
            if pid:
                adb("shell", "kill", "-2", pid)
            recording.wait(timeout=10)
            adb("pull", "/sdcard/learning-demo.mp4", str(OUT / "learning-demo.mp4"))
    finally:
        adb("shell", "cmd", "connectivity", "airplane-mode", "enable" if airplane == "1" else "disable")
        adb("shell", "svc", "wifi", "enable" if wifi in ("1", "2") else "disable")
