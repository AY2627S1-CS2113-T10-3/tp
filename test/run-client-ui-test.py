"""Compare a fresh packaged CLI run against fixed inputs and complete output."""

import argparse
import os
from pathlib import Path
import re
import subprocess
import sys
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
    plan_path = root / "test/ui-test-plan.md"
    timestamp = datetime.now(timezone(timedelta(hours=8))).isoformat(timespec="seconds")
    plan = f"""# Client console UI test plan

Working directory: `{root}`

Required runtime: Java 25. Build once with:

```powershell
.\\gradlew.bat test checkstyleMain checkstyleTest shadowJar
```

Launch a fresh process with:

```text
java -jar build/libs/duke.jar
```

Repeat this session with Python 3 and Java 25:

```text
python test/run-client-ui-test.py
```

Preconditions: The JAR has been built from the branch under test. Each process
starts with no clients or projects. Client data is kept in memory.

Comparison policy: Compare complete stdout; normalize CRLF to LF only.
Any other mismatch, nonempty stderr, nonzero exit code or 20-second timeout
fails the session. Stop on the first failure. Do not alter the expected output.

## CLIENT-UI-001

Aim: Verify all four client commands through a fresh packaged application run.

Inputs:

```text
{inputs}```

Expected complete stdout:

```text
{expected}```

Expected stderr: empty. Expected exit code: 0.

## Latest test session

Timestamp: {timestamp} (Asia/Singapore)

CLIENT-UI-001: PENDING
"""
    # Save the fixed expectations before launching any process in the session.
    plan_path.write_text(plan, encoding="utf-8")
    stdout = ""
    stderr = ""
    exit_code = "NOT LAUNCHED"
    try:
        version = subprocess.run([args.java, "-version"], capture_output=True,
                                 encoding="utf-8", timeout=20)
        if version.returncode != 0 or not re.search(r'version "25(?:[.\"]|$)', version.stderr):
            raise RuntimeError("Java 25 is required.\n" + version.stdout + version.stderr)
        process = subprocess.run([args.java, "-jar", str(root / "build/libs/duke.jar")],
                                 input=inputs, capture_output=True, encoding="utf-8",
                                 cwd=root, timeout=20)
        stdout, stderr, exit_code = process.stdout, process.stderr, process.returncode
    except subprocess.TimeoutExpired as error:
        stdout = (error.stdout or b"").decode("utf-8", errors="replace")
        stderr = (error.stderr or b"").decode("utf-8", errors="replace")
        exit_code = "TIMEOUT"
    except (OSError, RuntimeError) as error:
        stderr = str(error)

    passed = exit_code == 0 and not stderr and normalize_lines(stdout) == normalize_lines(expected)
    status = "PASS" if passed else "FAIL"
    transcript = f"""Console input:

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
    plan_path.write_text(plan.replace("CLIENT-UI-001: PENDING", f"CLIENT-UI-001: {status}")
                         + "\n" + transcript, encoding="utf-8")
    print(transcript)
    if not passed:
        print("Expected stdout:\n" + expected)
    return 0 if passed else 1


if __name__ == "__main__":
    sys.exit(main())
