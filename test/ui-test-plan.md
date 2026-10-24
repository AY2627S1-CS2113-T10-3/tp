# Client console UI test plan

## CLIENT-UI-001

Verify client addition, note creation, viewing, listing and saving on exit through
the packaged application. The fixed input is in [client/INPUT.TXT](client/INPUT.TXT)
and the complete expected stdout is in [client/EXPECTED.TXT](client/EXPECTED.TXT).

## Run the test

Use Java 25 and Python 3. Build and check the application from the repository root:

```powershell
.\gradlew.bat check shadowJar
python test/run-client-ui-test.py
```

On macOS or Linux, use `./gradlew check shadowJar` for the build step.
The runner uses `JAVA_HOME` when set, otherwise `java` from `PATH`.
To choose a Java 25 executable explicitly, pass `--java PATH_TO_JAVA`.

The runner launches `build/libs/BusyNotFree.jar` in a fresh temporary directory.
Saved data is created there and removed after the process ends, so existing user
data cannot affect the result and repeated runs start with no clients or projects.

## Pass criteria and reports

- Complete stdout must match the expected fixture, normalizing only CRLF to LF.
- Stderr must be empty and the exit code must be zero.
- A timeout after 20 seconds or a runtime other than Java 25 fails the test.

Each run writes its timestamp, input, actual output, exit code and PASS/FAIL result
to `build/reports/client-ui/session.md`, which is ignored by Git through `/build/`.
The runner does not rewrite this plan or either fixture. On failure it also prints
the transcript and expected output to the console for diagnosis.

CI builds the JAR and runs this test on Windows, macOS and Linux. When commands
or intentional console messages change, update `client/EXPECTED.TXT` and review
the complete output by hand before committing. Do not accept unexpected output
changes simply to make the test pass.
