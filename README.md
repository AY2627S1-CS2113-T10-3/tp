# BusyNotFree

BusyNotFree is a command-line app for keeping track of clients, their projects, and each project's dated tasks and events. It was built for the CS2113 team project.

## Run the app

You need Java 25. From the repository root, build the runnable JAR:

```powershell
.\gradlew.bat shadowJar
java -jar build/libs/BusyNotFree.jar
```

On macOS or Linux, use `./gradlew shadowJar` for the build step. Alternatively, import the repository as a Gradle project in IntelliJ IDEA and run `busynotfree.BusyNotFree.main()`.

Enter one command per line. For example:

```text
client.add name/Acme Studio
project.add client/C001 name/Website redesign due/2026-11-30
task.add project/P001 name/Create homepage wireframe date/2026-10-15
project.view P001
exit
```

BusyNotFree assigns IDs such as `C001`, `P001`, `T001`, and `E001`. Use the IDs printed by the app in later commands. Dates use `YYYY-MM-DD`.

`exit` saves clients, notes, projects, tasks, and events to `data/busynotfree.properties` relative to the directory where you run the app. The next run from that directory loads the file. Keep that file if you want to keep your data; do not edit it by hand.

See the [User Guide](docs/UserGuide.md) for every command and more examples.

## Develop and test

Use JDK 25 when importing the Gradle project or running these commands:

```powershell
.\gradlew.bat test checkstyleMain checkstyleTest shadowJar
```

On macOS or Linux, replace `.\gradlew.bat` with `./gradlew`. The runnable JAR is created at `build/libs/BusyNotFree.jar`.

Project documentation: [User Guide](docs/UserGuide.md) · [Developer Guide](docs/DeveloperGuide.md) · [About Us](docs/AboutUs.md).
