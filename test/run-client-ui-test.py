"""Compare a fresh packaged CLI run against fixed inputs and complete output."""

import argparse
import os
from pathlib import Path
import re
import subprocess
import sys
import tempfile
from datetime import datetime, timedelta, timezone


def normalize_lines(text):
    """Normalize only CRLF, preserving all other whitespace."""
    return text.replace("\r\n", "\n")


def main():
    root = Path(__file__).resolve().parent.parent
    parser = argparse.ArgumentParser(description=__doc__)
    default_java = "java"
    if os.environ.get("JAVA_HOME"):
        executable = "java.exe" if os.name == "nt" else "java"
        default_java = str(Path(os.environ["JAVA_HOME"]) / "bin" / executable)
    parser.add_argument("--java", default=default_java, help="Java 25 executable")
    args = parser.parse_args()

    inputs = (root / "test/client/INPUT.TXT").read_text(encoding="utf-8")
    expected = (root / "test/client/EXPECTED.TXT").read_text(encoding="utf-8")
    report_path = root / "build/reports/client-ui/session.md"
    timestamp = datetime.now(timezone(timedelta(hours=8))).isoformat(timespec="seconds")
    stdout = ""
    stderr = ""
    exit_code = "NOT LAUNCHED"
    try:
        version = subprocess.run([args.java, "-version"], capture_output=True,
                                 encoding="utf-8", timeout=20)
        if version.returncode != 0 or not re.search(r'version "25(?:[.\"]|$)', version.stderr):
            raise RuntimeError("Java 25 is required.\n" + version.stdout + version.stderr)
        # Keep saved data isolated from the user's working directory and other runs.
        with tempfile.TemporaryDirectory(prefix="busynotfree-client-ui-") as working_directory:
            process = subprocess.run([args.java, "-jar", str(root / "build/libs/BusyNotFree.jar")],
                                     input=inputs.encode("utf-8"), capture_output=True,
                                     cwd=working_directory, timeout=20)
        # Decode explicitly so Python does not normalize lone carriage returns.
        stdout = process.stdout.decode("utf-8")
        stderr = process.stderr.decode("utf-8")
        exit_code = process.returncode
    except subprocess.TimeoutExpired as error:
        stdout = (error.stdout or b"").decode("utf-8", errors="replace")
        stderr = (error.stderr or b"").decode("utf-8", errors="replace")
        exit_code = "TIMEOUT"
    except (OSError, RuntimeError, UnicodeError) as error:
        stderr = str(error)

    passed = exit_code == 0 and not stderr and normalize_lines(stdout) == normalize_lines(expected)
    status = "PASS" if passed else "FAIL"
    transcript = f"""# Client console UI test session

Timestamp: {timestamp} (Asia/Singapore)

CLIENT-UI-001: {status}

Console input:

```text
{inputs}```

Actual stdout:

```text
{stdout}```

Actual stderr:

```text
{stderr}```

Exit code: {exit_code}

Result: {status}. CLIENT-UI-001: {status}. Remaining cases: none.
"""
    report_path.parent.mkdir(parents=True, exist_ok=True)
    report_path.write_text(transcript, encoding="utf-8", newline="\n")
    print(f"CLIENT-UI-001: {status}. Session log: {report_path}")
    if not passed:
        print(transcript)
        print("Expected stdout:\n" + expected)
    return 0 if passed else 1


if __name__ == "__main__":
    sys.exit(main())
