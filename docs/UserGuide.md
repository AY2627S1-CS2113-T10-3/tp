# BusyNotFree User Guide

## Introduction

BusyNotFree is a command-line app for managing clients and their projects. A project can have dated tasks and events. You can view a project's details, update its status, and save your work during a session or when you exit.

## Quick start

1. Install Java 25.
2. Download `BusyNotFree.jar` from the [latest release](https://github.com/AY2627S1-CS2113-T10-3/tp/releases/latest) and place it in a folder where you have write permission.
3. Open a terminal in that folder and run `java -jar BusyNotFree.jar`. Use the terminal so you can enter commands and read the output.
4. Type a command and press Enter. Try the commands below in order:

   ```text
   client.add name/Acme Studio
   project.add client/C001 name/Website redesign due/2026-11-30
   task.add project/P001 name/Create homepage wireframe date/2026-10-15
   event.add project/P001 name/Stakeholder meeting date/2026-10-20
   project.view P001
   save
   exit
   ```

The app prints the assigned ID after each addition. The example IDs assume no existing data; use the IDs shown on your screen if you already have saved data.

If you are building from source, run `.\gradlew.bat shadowJar` on Windows or `./gradlew shadowJar` on macOS or Linux from the repository root. Then run `java -jar build/libs/BusyNotFree.jar` from that directory.

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
Note 1: Prefers weekly updates
```

### List clients: `client.list`

Format: `client.list`

Lists client IDs and names in creation order. If there are none, the app shows `No clients found.` Extra words after this command are ignored.

### Find clients by name: `client.find`

Format: `client.find NAME`

Finds clients whose names contain `NAME`, ignoring case. For example,
`client.find acme` matches `Acme Studio`. Multiword searches are allowed.
A summary such as `Found 2 matching clients.` appears before the matching client
IDs and names in creation order, using the same format as `client.list`.
A single match displays `Found 1 matching client.` No matches displays
`Found 0 matching clients.` followed by `No clients found.` A search must not be blank.

### Edit a client's name: `client.edit`

Format: `client.edit CLIENT_ID name/NEW_NAME`

Renames an existing client, preserving its ID, notes and project links. For
example, `client.edit C001 name/Acme Design` displays
`Updated client C001 from "Acme Studio" to "Acme Design".`
The new name is saved when you enter `save` or `exit`.
Missing or unknown client IDs and blank names are rejected.

### Delete a client: `client.delete`

Format: `client.delete CLIENT_ID`

Deletes a client and all its notes. The confirmation prompt shows the client's ID,
name and note count. Type `yes` to delete or `no` to cancel. Other input repeats
the prompt; reaching the end of input cancels deletion.

The client must have no associated projects, including completed or cancelled
projects. Delete those projects first. Missing, unknown or multiple IDs are
rejected; IDs are case-sensitive. Changes are saved on `save` or `exit`, and
deleted client IDs are never reused.

### Delete a client note: `client.delete_note`

Format: `client.delete_note CLIENT_ID index/NOTE_INDEX`

Use `client.view CLIENT_ID` to see numbered notes. Indexes start at 1.
For example, `client.delete_note C001 index/2` requests deletion of the second note.
The prompt shows the selected note. Type `yes` to delete it or `no` to cancel.
Other input repeats the prompt; reaching the end of input cancels deletion.

Missing or invalid client IDs and missing, nonpositive, nonnumeric or out-of-range
indexes are rejected. Remaining notes are renumbered after deletion. The client
and its projects remain available; having projects does not block note deletion.
Changes are saved on `save` or `exit`.

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

Shows each project's ID, name, status, and deadline in creation order without
changing project data. If there are no projects, the app shows
`No projects found.` Extra words after this command are ignored.

```text
project.list
1. P001|Website redesign|ACTIVE|Due 2026-11-30
```

### Filter projects by status: `project.filter`

Format: `project.filter STATUS`

Displays only projects with the selected status without changing their saved
details. The status must be one of `active`, `paused`, `completed`, or
`cancelled`, and is not case-sensitive. If no projects match, the app shows
`No projects found.`

```text
project.filter active
1. P001|Website redesign|ACTIVE|Due 2026-11-30
```

### List projects by deadline: `project.deadlines`

Format: `project.deadlines`

Displays projects from the earliest deadline to the latest deadline without
changing their saved details. Projects with the same deadline remain in their
creation order. If there are no projects, the app shows `No projects found.`

```text
project.deadlines
1. P001|Website redesign|ACTIVE|Due 2026-11-30
2. P002|Logo design|ACTIVE|Due 2026-12-15
```

### List overdue active projects: `project.overdue`

Format: `project.overdue`

Displays active projects whose deadlines are earlier than today's date without
changing their saved details. Projects that are paused, completed, or cancelled
are not included. If there are no overdue active projects, the app shows
`No overdue projects found.`

```text
project.overdue
1. P001|Website redesign|ACTIVE|Due 2026-10-01
```

`project.list`, `project.filter`, `project.deadlines`, and `project.overdue`
only display information; they do not modify or save project data.
`project.status` changes only the selected project's status.

### View a project summary: `project.view`

Format: `project.view [PROJECT_ID|all]`

Use a project ID to view one project. Use `project.view all` or just `project.view` to view every project, separated by a blank line. Supply at most one project ID, and use lowercase `all` to request every project. A summary shows the client, status, deadline, tasks, and events in separate sections, plus a completed-task count.

```text
project.view P001
Project: P001 - Website redesign
Client: C001 - Acme Studio
Status: ACTIVE
Deadline: 2026-11-30
Tasks:
[T001] Create homepage wireframe - 2026-10-15 [In Progress]
Progress: 0 of 1 task completed.
Events:
[E001] Stakeholder meeting - 2026-10-20
```

An empty section says `No tasks found.` or `No events found.` A project without tasks shows `Progress: 0 of 0 tasks completed.` If there are no projects, viewing all says `No projects found.` For example, if `P999` does not exist, `project.view P999` displays `Error: Project P999 was not found.` and the command format. You can then enter another command.

Each task shows `[In Progress]` after its date until it is marked complete, when it shows `[COMPLETED]`.
The progress count includes only that project's tasks; events do not contribute to it.
Marking every task complete does not change the project's status automatically. Use
`project.status` to change that status. Viewing a summary does not change or save data.
Projects in the trashcan are excluded from `project.view`; use `trashcan.list` to see them.

### Change a project's status: `project.status`

Format: `project.status PROJECT_ID STATUS`

Changes the status of an existing project. The status must be one of `active`,
`paused`, `completed`, or `cancelled`, and is not case-sensitive. The project
ID must belong to an existing project. Only the project's status is changed;
its name, deadline and activities remain unchanged.

```text
project.status P001 paused
Project P001 status changed to PAUSED.
```

### Delete a project: `project.delete`

Format: `project.delete PROJECT_ID`

Moves an existing project and its tasks and events to the trashcan after confirmation.
It does not delete the client. Deleted IDs are not reused. Use `trashcan.restore PROJECT_ID`
to recover the project while its client is still active.

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

Marks an existing task complete. Find its ID using `project.view PROJECT_ID` or the confirmation from `task.add`. Task IDs are unique across all projects, so no project ID is needed. The task keeps its project, description, and date. `project.view` then shows `[COMPLETED]` beside it and updates the progress count.

After a successful `save` or `exit`, the task's completion state is restored the next time BusyNotFree starts. Tasks from older data files without a saved completion state start as `[In Progress]`.

```text
task.mark T001
Task T001 marked as completed.
```

Supply exactly one existing task ID. A missing, unknown or multiple ID input displays an error
without changing any task. Marking an already completed task leaves it completed and displays
the same confirmation.

### Reopen a task: `task.unmark`

Format: `task.unmark TASK_ID`

Marks an existing task incomplete again, for example when a client requests more work. The task keeps its ID, project, description, and date. In `project.view`, its `[COMPLETED]` label changes to `[In Progress]` and the completed-task count decreases by one. Save the change with `save` or `exit` to retain it after restarting.

```text
task.unmark T001
Task T001 reopened. Status: [In Progress]
```

Supply exactly one existing task ID. Invalid input leaves all tasks unchanged. Reopening a task
that is already in progress leaves it in progress and displays the same confirmation; the
completed-task count does not decrease again.

### Edit a task: `task.edit`

Format: `task.edit TASK_ID name/DESCRIPTION date/YYYY-MM-DD`

Replaces an existing task's description and date, for example when a client changes their requirements. Both `name/` and `date/` are required, in any order. The task keeps its ID, project, and completion status. To move work to another project, add a new task there instead; `project/` is not accepted.

```text
task.edit T001 name/Create mobile homepage wireframe date/2026-10-22
Updated task in project P001 "Website redesign": [T001] Create mobile homepage wireframe - 2026-10-22 [In Progress]
```

If the task ID is unknown, a value is blank, or the date is invalid, the app shows an error and the format, and the task is not changed.

### Delete a task: `task.delete`

Format: `task.delete TASK_ID`

Deletes a task after confirmation. The prompt identifies the task and its
project. Type `yes` to delete or `no` to cancel. Other input repeats the
prompt; reaching the end of input cancels deletion.

Task IDs are case-sensitive. Missing, unknown or multiple IDs are rejected.
Completed tasks can also be deleted. The project, its events and other tasks
remain available. Changes are saved on `save` or `exit`, and deleted task IDs
are never reused.

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

### Delete an event: `event.delete`

Format: `event.delete EVENT_ID`

Deletes an event after confirmation. The prompt identifies the event and its
project. Type `yes` to delete or `no` to cancel. Other input repeats the
prompt; reaching the end of input cancels deletion.

Event IDs are case-sensitive. Missing, unknown or multiple IDs are rejected.
The project, its tasks and other events remain available. Changes are saved
on `save` or `exit`, and deleted event IDs are never reused.

## Trashcan

### List deleted items: `trashcan.list`

Format: `trashcan.list`

Shows deleted projects with their tasks and events, including dates, project status
and task completion state. Also shows deleted clients with all their notes.
Entries are grouped by type and displayed in ID order. Active items are not listed.
An empty trashcan displays `Trashcan is empty.` Extra arguments are rejected.

Deleting a project or client retains a snapshot in the trashcan. There is currently
no expiry or automatic cleanup. Entries are saved on `save` or `exit` and remain
available after restarting. Individually deleted tasks, events and notes are not
retained; project snapshots contain only activities present when the project was deleted.
Items deleted before this feature was installed cannot be recovered.

### Restore a deleted project: `trashcan.restore`

Format: `trashcan.restore PROJECT_ID`

Restores a project immediately, including its tasks and events with their original
IDs, descriptions, dates, status and task completion states. The restored project
is removed from the trashcan and appended to the active project list.
No new IDs are allocated. Changes are saved on `save` or `exit`.

```text
trashcan.restore P001
Restored project P001 "Website" with 2 tasks and 1 event.
```

Missing, unknown, active, lowercase or multiple IDs are rejected. Only projects
can be restored. The owning client must still be active; a client with the same
name and a different ID does not satisfy this requirement. If a client was deleted
after its projects, those projects remain visible in the trashcan but cannot yet
be restored. Restoration also rejects conflicting IDs without changing any data.

## Help and saving

### Show help: `help`

Format: `help`

Shows a list of available commands and short descriptions.

### Save without exiting: `save`

Format: `save`

Writes current clients and their notes, projects, tasks and their completion states, events,
and trashcan entries to the same data file used by `exit`. The app then accepts another command:

```text
save
Data saved.
```

`save` does not accept arguments. For example, `save now` displays
`Error: Use save without arguments.` and does not save. If writing fails, the app displays
`Error: Critical ERROR!!! Unable to save data: ...`, followed by the reason. It does not show
`Data saved.` and lets you try again or continue working. Changes made after a successful
`save` need another `save` or `exit` to reach the file.

### Save and exit: `exit`

Format: `exit`

Saves the same data as `save`, including trashcan entries, then closes the app:

```text
exit
Data saved. Goodbye!
```

Extra words after `exit` are ignored, so `exit now` also saves and exits. If saving fails,
the app displays the same save error described above and keeps accepting commands. It shows
`Data saved. Goodbye!` only after a successful save. Closing the terminal or stopping the
process from IntelliJ may lose changes made since the last successful `save`.

Finish a pending deletion confirmation with `yes` or `no` before entering `save` or `exit`.
While confirmation is pending, those words are treated as answers to the confirmation prompt.

### Data location and recovery

The save file is `data/busynotfree.properties` relative to the directory from which you start
the app, which may differ from the folder containing the JAR. For example, running the JAR
from a terminal in `C:\Work` uses `C:\Work\data\busynotfree.properties`.
The app creates the data directory and file on a successful save and loads the file automatically
when launched from the same directory. Without a save file, it starts with no records.

To back up or move your data, exit successfully, then copy this file. On another computer,
place it in a `data` folder relative to the directory from which you will launch BusyNotFree.
Keep a backup before replacing a file. The data is stored as readable text; avoid editing it
manually because invalid values can prevent the app from loading.

If loading fails, the app displays `Error: Unable to load saved data: ...` and stops before
accepting commands. It does not overwrite that file. Keep a copy of the file and restore a
known working backup before restarting.

When terminal input ends normally, the app attempts one save and then returns. If that save
fails, it displays the error but cannot accept another command because input has ended.

## Command summary

| Command | Format |
| --- | --- |
| Add client | `client.add name/CLIENT_NAME` |
| Add client note | `client.note CLIENT_ID text/NOTE` |
| View client | `client.view CLIENT_ID` |
| List clients | `client.list` |
| Find clients | `client.find NAME` |
| Edit client name | `client.edit CLIENT_ID name/NEW_NAME` |
| Delete client | `client.delete CLIENT_ID` |
| Delete client note | `client.delete_note CLIENT_ID index/NOTE_INDEX` |
| Add project | `project.add client/CLIENT_ID name/PROJECT_NAME due/YYYY-MM-DD` |
| List projects | `project.list` |
| Filter projects by status | `project.filter STATUS` |
| List projects by deadline | `project.deadlines` |
| List overdue active projects | `project.overdue` |
| View project(s) | `project.view [PROJECT_ID\|all]` |
| Change project status | `project.status PROJECT_ID STATUS` |
| Delete project | `project.delete PROJECT_ID` |
| Add task | `task.add project/PROJECT_ID name/DESCRIPTION date/YYYY-MM-DD` |
| Mark task complete | `task.mark TASK_ID` |
| Reopen task | `task.unmark TASK_ID` |
| Edit task | `task.edit TASK_ID name/DESCRIPTION date/YYYY-MM-DD` |
| Delete task | `task.delete TASK_ID` |
| Delete event | `event.delete EVENT_ID` |
| Add event | `event.add project/PROJECT_ID name/DESCRIPTION date/YYYY-MM-DD` |
| Edit event | `event.edit EVENT_ID name/DESCRIPTION date/YYYY-MM-DD` |
| Help | `help` |
| List deleted items | `trashcan.list` |
| Restore project | `trashcan.restore PROJECT_ID` |
| Save and continue | `save` |
| Save and exit | `exit` |
