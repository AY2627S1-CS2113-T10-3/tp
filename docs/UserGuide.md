# BusyNotFree User Guide

## Introduction

BusyNotFree is a command-line app for managing clients and their projects. A project can have dated tasks and events. You can view a project's details, update its status, and save your work when you exit.

## Quick start

1. Install Java 25.
2. From the repository root, run `.\gradlew.bat shadowJar` on Windows or `./gradlew shadowJar` on macOS or Linux.
3. Run `java -jar build/libs/BusyNotFree.jar` from the repository root.
4. Type a command and press Enter. Try the commands below in order:

   ```text
   client.add name/Acme Studio
   project.add client/C001 name/Website redesign due/2026-11-30
   task.add project/P001 name/Create homepage wireframe date/2026-10-15
   event.add project/P001 name/Stakeholder meeting date/2026-10-20
   project.view P001
   exit
   ```

The app prints the assigned ID after each addition. The example IDs assume an empty data file; use the IDs shown on your screen if you already have saved data.

The commands and IDs are case-sensitive. Enter dates as real calendar dates in `YYYY-MM-DD` format, such as `2026-11-30`. Names, descriptions, and notes may contain spaces. Prefixes such as `name/` can be supplied in any order, but each required prefix must appear once with a non-blank value.

## Clients

### Add a client: `client.add`

Format: `client.add name/CLIENT_NAME`

Adds a client and assigns an ID such as `C001`. Client names may be repeated; IDs distinguish them.

```text
client.add name/Acme Studio
Added client C001: Acme Studio.
```

### Add a client note: `client.note`

Format: `client.note CLIENT_ID text/NOTE`

Appends a note to an existing client. Previous notes are kept.

```text
client.note C001 text/Prefers weekly updates
Note added to client C001.
```

### View a client: `client.view`

Format: `client.view CLIENT_ID`

Displays one client's name and notes. If the client has no notes, the app shows `Notes: No notes found.`

```text
client.view C001
C001 | Acme Studio
Notes: Prefers weekly updates
```

### List clients: `client.list`

Format: `client.list`

Lists client IDs and names in creation order. If there are none, the app shows `No clients found.` Extra words after this command are ignored.

## Projects

### Add a project: `project.add`

Format: `project.add client/CLIENT_ID name/PROJECT_NAME due/YYYY-MM-DD`

The client must already exist; use `client.list` to look up client IDs. A new project receives an ID such as `P001` and starts with status `ACTIVE`. The confirmation shows the project and client names, so you can check that the right client was used.

```text
project.add client/C001 name/Website redesign due/2026-11-30
Created project P001 "Website redesign" for client C001 "Acme Studio".
```

### List projects: `project.list`

Format: `project.list`

Shows each project's ID, name, status, and deadline in creation order. If there are no projects, the app shows `No projects found.` Extra words after this command are ignored.

```text
project.list
1. P001|Website redesign|ACTIVE|Due 2026-11-30
```

### Filter projects by status: `project.filter`

Format: `project.filter STATUS`

Displays only projects with the selected status. The status must be one of
`active`, `paused`, `completed`, or `cancelled`, and is not case-sensitive.
If no projects match, the app shows `No projects found.`

```text
project.filter active
1. P001|Website redesign|ACTIVE|Due 2026-11-30
```

### List projects by deadline: `project.deadlines`

Format: `project.deadlines`

Displays projects from the earliest deadline to the latest deadline. Projects
with the same deadline remain in their creation order. If there are no projects,
the app shows `No projects found.`

```text
project.deadlines
1. P001|Website redesign|ACTIVE|Due 2026-11-30
2. P002|Logo design|ACTIVE|Due 2026-12-15
```

### List overdue active projects: `project.overdue`

Format: `project.overdue`

Displays active projects whose deadlines are earlier than today's date. Projects
that are paused, completed, or cancelled are not included. If there are no
overdue active projects, the app shows `No overdue projects found.`

```text
project.overdue
1. P001|Website redesign|ACTIVE|Due 2026-10-01
```

### View a project summary: `project.view`

Format: `project.view [PROJECT_ID|all]`

Use a project ID to view one project. Use `project.view all` or just `project.view` to view every project, separated by a blank line. A summary shows the client, status, deadline, tasks, and events in separate sections.

```text
project.view P001
Project: P001 - Website redesign
Client: C001 - Acme Studio
Status: ACTIVE
Deadline: 2026-11-30
Tasks:
[T001] Create homepage wireframe - 2026-10-15 [In Progress]
Events:
[E001] Stakeholder meeting - 2026-10-20
```

An empty section says `No tasks found.` or `No events found.` If there are no projects, viewing all says `No projects found.` A missing ID produces `Project PROJECT_ID was not found.` with the requested ID in place of `PROJECT_ID`.

Each task shows `[In Progress]` after its date until it is marked complete, when it shows `[COMPLETED]`.

### Change a project's status: `project.status`

Format: `project.status PROJECT_ID STATUS`

The status must be one of `active`, `paused`, `completed`, or `cancelled` in lowercase. The project must exist.

```text
project.status P001 paused
Project P001 status changed to PAUSED.
```

### Delete a project: `project.delete`

Format: `project.delete PROJECT_ID`

Deletes an existing project and its tasks and events. It does not delete the client. Deleted IDs are not reused.

```text
project.delete P001
Website redesign has been deleted
2 associated activities removed.
```

## Tasks and events

### Add a task: `task.add`

Format: `task.add project/PROJECT_ID name/DESCRIPTION date/YYYY-MM-DD`

Adds a dated task to an existing project; use `project.list` to look up project IDs. Task IDs use `T`, for example `T001`.

```text
task.add project/P001 name/Create homepage wireframe date/2026-10-15
Added task T001 "Create homepage wireframe" to project P001 "Website redesign".
```

### Mark a task complete: `task.mark`

Format: `task.mark TASK_ID`

Marks an existing task complete. Use the task ID shown by `task.add`; the task keeps its project, description, and date. `project.view` then shows `[COMPLETED]` beside it. An unknown task ID produces an error.

```text
task.mark T001
Task T001 marked as completed.
```

### Reopen a task: `task.unmark`

Format: `task.unmark TASK_ID`

Marks an existing task incomplete again, for example when a client requests more work. The task keeps its ID, project, description, and date. In `project.view`, its `[COMPLETED]` label changes to `[In Progress]`. An unknown task ID produces an error.

```text
task.unmark T001
Task T001 reopened. Status: [In Progress]
```

### Edit a task: `task.edit`

Format: `task.edit TASK_ID name/DESCRIPTION date/YYYY-MM-DD`

Replaces an existing task's description and date, for example when a client changes their requirements. Both `name/` and `date/` are required, in any order. The task keeps its ID, project, and completion status. To move work to another project, add a new task there instead; `project/` is not accepted.

```text
task.edit T001 name/Create mobile homepage wireframe date/2026-10-22
Updated task in project P001 "Website redesign": [T001] Create mobile homepage wireframe - 2026-10-22 [In Progress]
```

If the task ID is unknown, a value is blank, or the date is invalid, the app shows an error and the format, and the task is not changed.

### Add an event: `event.add`

Format: `event.add project/PROJECT_ID name/DESCRIPTION date/YYYY-MM-DD`

Adds a dated event to an existing project; use `project.list` to look up project IDs. Event IDs use `E`, for example `E001`.

```text
event.add project/P001 name/Stakeholder meeting date/2026-10-20
Added event E001 "Stakeholder meeting" to project P001 "Website redesign".
```

### Edit an event: `event.edit`

Format: `event.edit EVENT_ID name/DESCRIPTION date/YYYY-MM-DD`

Replaces an existing event's description and date, for example when a meeting is rescheduled. Both `name/` and `date/` are required, in any order. The event keeps its ID and project.

```text
event.edit E001 name/Stakeholder meeting (rescheduled) date/2026-10-27
Updated event in project P001 "Website redesign": [E001] Stakeholder meeting (rescheduled) - 2026-10-27
```

If the event ID is unknown, a value is blank, or the date is invalid, the app shows an error and the format, and the event is not changed.

## Help and saving

### Show help: `help`

Format: `help`

Shows a list of available commands and short descriptions.

### Save and exit: `exit`

Format: `exit`

Saves clients, notes, projects, tasks, and events, then closes the app. Task completion is not yet included in the save file:

```text
exit
Data saved. Goodbye!
```

Extra words after `exit` are ignored. If saving fails, the app displays an error and lets you try again. Closing the app without exiting normally may lose changes made since the last save.

The save file is `data/busynotfree.properties` relative to the directory from which you start the app. It is created on a successful exit and loaded automatically on the next run from that directory. To move your data to another computer, copy this file and place it in the same `data` directory relative to where you will run BusyNotFree.

## Command summary

| Command | Format |
| --- | --- |
| Add client | `client.add name/CLIENT_NAME` |
| Add client note | `client.note CLIENT_ID text/NOTE` |
| View client | `client.view CLIENT_ID` |
| List clients | `client.list` |
| Add project | `project.add client/CLIENT_ID name/PROJECT_NAME due/YYYY-MM-DD` |
| List projects | `project.list` |
| Filter projects by status | `project.filter STATUS` |
| List projects by deadline | `project.deadlines` |
| List overdue active projects | `project.overdue` |
| View project(s) | `project.view [PROJECT_ID|all]` |
| Change project status | `project.status PROJECT_ID STATUS` |
| Delete project | `project.delete PROJECT_ID` |
| Add task | `task.add project/PROJECT_ID name/DESCRIPTION date/YYYY-MM-DD` |
| Mark task complete | `task.mark TASK_ID` |
| Reopen task | `task.unmark TASK_ID` |
| Edit task | `task.edit TASK_ID name/DESCRIPTION date/YYYY-MM-DD` |
| Add event | `event.add project/PROJECT_ID name/DESCRIPTION date/YYYY-MM-DD` |
| Edit event | `event.edit EVENT_ID name/DESCRIPTION date/YYYY-MM-DD` |
| Help | `help` |
| Save and exit | `exit` |
