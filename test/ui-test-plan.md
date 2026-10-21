# Client console UI test plan

Working directory: `E:\NUS\Academics\Year 2\CS2113\Team_Project\tp`

Required runtime: Java 25. Build once with:

```powershell
.\gradlew.bat test checkstyleMain checkstyleTest shadowJar
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
client.add name/Acme Studio
client.note C001 text/Prefers weekly updates
client.view C001
client.list
exit
```

Expected complete stdout:

```text
------------------------------------------------------------
Welcome to BusyNotFree!
Commands: client.add, client.note, client.view, client.list, project.add, task.add, event.add, exit
Data is kept in memory only and is not saved on exit.
------------------------------------------------------------
Enter a command:
------------------------------------------------------------
Added client C001: Acme Studio.
------------------------------------------------------------
Enter a command:
------------------------------------------------------------
Note added to client C001.
------------------------------------------------------------
Enter a command:
------------------------------------------------------------
C001 | Acme Studio
Notes: Prefers weekly updates
------------------------------------------------------------
Enter a command:
------------------------------------------------------------
1. C001 | Acme Studio
------------------------------------------------------------
Enter a command:
------------------------------------------------------------
------------------------------------------------------------
Goodbye!
```

Expected stderr: empty. Expected exit code: 0.

## Latest test session

Timestamp: 2026-10-13T00:57:56+08:00 (Asia/Singapore)

CLIENT-UI-001: PASS

Console input:

```text
client.add name/Acme Studio
client.note C001 text/Prefers weekly updates
client.view C001
client.list
exit
```

Actual stdout:

```text
------------------------------------------------------------
Welcome to BusyNotFree!
Commands: client.add, client.note, client.view, client.list, project.add, task.add, event.add, exit
Data is kept in memory only and is not saved on exit.
------------------------------------------------------------
Enter a command:
------------------------------------------------------------
Added client C001: Acme Studio.
------------------------------------------------------------
Enter a command:
------------------------------------------------------------
Note added to client C001.
------------------------------------------------------------
Enter a command:
------------------------------------------------------------
C001 | Acme Studio
Notes: Prefers weekly updates
------------------------------------------------------------
Enter a command:
------------------------------------------------------------
1. C001 | Acme Studio
------------------------------------------------------------
Enter a command:
------------------------------------------------------------
------------------------------------------------------------
Goodbye!
```

Actual stderr:

```text
```

Exit code: 0

Result: PASS. CLIENT-UI-001: PASS. Remaining cases: none.
