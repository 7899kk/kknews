"""Capture the installed app on a temporary Android emulator using real UI nodes."""
import pathlib
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET

out = pathlib.Path(sys.argv[1])
out.mkdir(parents=True, exist_ok=True)

def adb(*args):
    return subprocess.check_output(["adb", *args])

def nodes():
    adb("shell", "uiautomator", "dump", "--compressed", "/sdcard/radar-window.xml")
    return ET.fromstring(adb("exec-out", "cat", "/sdcard/radar-window.xml")).iter("node")

def click(label):
    for _ in range(15):
        for node in nodes():
            if node.attrib.get("text") == label or node.attrib.get("content-desc") == label:
                coords = list(map(int, re.findall(r"\d+", node.attrib["bounds"])))
                adb("shell", "input", "tap", str((coords[0] + coords[2]) // 2), str((coords[1] + coords[3]) // 2))
                time.sleep(1)
                return
        time.sleep(1)
    raise AssertionError("Visible UI control missing: " + label)

def screenshot(name):
    (out / (name + ".png")).write_bytes(adb("exec-out", "screencap", "-p"))
    adb("shell", "uiautomator", "dump", "--compressed", "/sdcard/radar-window.xml")
    (out / (name + ".xml")).write_bytes(adb("exec-out", "cat", "/sdcard/radar-window.xml"))

adb("logcat", "-c")
adb("shell", "svc", "wifi", "disable")
adb("shell", "svc", "data", "disable")
adb("shell", "am", "start", "-n", "com.karthikkilari.expiryradar/.MainActivity")
time.sleep(3)
screenshot("onboarding")
for label in ["Get Started", "Continue", "Start tracking"]:
    click(label)
for label, heading in [("Home", "Nothing to track yet."), ("Items", "Your items"), ("Calendar", "Expiry calendar"), ("Insights", "Your insights"), ("Settings", "Offline, private, and yours.")]:
    click(label)
    screenshot(label.lower())
    text = [n.attrib.get("text", "") for n in nodes()]
    assert any(heading in value for value in text), (label, text)
click("Home")
click("Add item")
screenshot("add-item")
log = adb("logcat", "-d", "-s", "AndroidRuntime:E").decode()
(out / "crash-log.txt").write_text(log)
assert "FATAL EXCEPTION" not in log, log
print("Actual MainActivity onboarding, five destinations, Add Item, and crash-log checks passed.")
