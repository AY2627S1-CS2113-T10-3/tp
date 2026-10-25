# Developer Guide

## Acknowledgements

{list here sources of all reused/adapted ideas, code, documentation, and third-party libraries -- include links to the original source as well}

## Design & implementation

{Describe the design and implementation of the product. Use UML diagrams and short code snippets where applicable.}

### Creating projects, tasks and events: `project.add`, `task.add` and `event.add`

These commands create the core records of BusyNotFree:

* `project.add client/CLIENT_ID name/PROJECT_NAME due/YYYY-MM-DD` creates a project for an existing client.
* `task.add project/PROJECT_ID name/DESCRIPTION date/YYYY-MM-DD` adds a dated internal task to an existing project.
* `event.add project/PROJECT_ID name/DESCRIPTION date/YYYY-MM-DD` adds a dated event, such as a meeting or review, to an existing project.

#### Model

The class diagram below shows the model classes involved.

![Class diagram of projects, tasks and events](images/CreationModelClassDiagram.png)

* `ProjectManager` keeps every project in creation order and assigns IDs. It has a separate counter for each ID type: `P001` for projects, `T001` for tasks and `E001` for events. Task and event IDs are unique across the whole application, not just within one project, so later commands such as `task.mark T001` do not need a project ID.
* `Project` owns two separate lists, one of `Task` objects and one of `Event` objects. `getTasks()` and `getEvents()` return unmodifiable copies, so other classes cannot change a project's items without going through `Project`.
* `Task` and `Event` extend the abstract class `ScheduledItem`, which holds the fields they share: ID, owning project, description and date.
* `ProjectManager` checks that a client exists through the `ClientLookup` interface, which `ClientManager` implements.

#### Parsing the command

All three commands are parsed the same way before the model is changed:

1. `CommandParser` splits the input into a target (`project`, `task` or `event`), an action (`add`) and the remaining argument text.
2. `CommandHandler#routeCommand` sends the command to `handleProjectAdd`, `handleTaskAdd` or `handleEventAdd`.
3. The handler calls `ArgumentParser#parse` with the prefixes the command requires, for example `"project", "name", "date"` for `task.add`. It returns a map from each prefix to its trimmed value. The prefixes may appear in any order.
4. The handler calls `ArgumentParser#parseDate` to turn the date text into a `LocalDate`.

`ArgumentParser#parse` rejects a missing, blank, repeated or unexpected prefix with a `CommandException`. A prefix is recognised only at the start of the arguments or after a space. Slashes that are not recognised prefixes stay part of the value, so names such as `Create UI/UX mockup` or `Review src/main changes` are accepted as typed.

`ArgumentParser#parseDate` first checks that the text matches `YYYY-MM-DD` exactly, then parses it with `LocalDate#parse`, which rejects dates that do not exist, such as `2026-02-30`.

#### Creating a project

The sequence diagram below shows how `project.add client/C001 name/Website due/2026-11-30` is executed.

![Sequence diagram of project.add](images/AddProjectSequenceDiagram.png)

`ProjectManager#addProject` builds the `Project` using the next project ID. The `Project` constructor rejects a blank client ID or name and a missing deadline. The manager then asks its `ClientLookup` whether the client exists. Only after both checks pass does it store the project and advance the counter:

```java
public Project addProject(String clientId, String name, LocalDate deadline) {
    Project project = new Project(formatId("P", nextProjectNumber), clientId, name, deadline);
    if (!clientLookup.exists(project.getClientId())) {
        throw new ProjectException("Client " + project.getClientId()
                + " does not exist. Use client.list to see client IDs.");
    }
    projects.put(project.getProjectId(), project);
    nextProjectNumber++;
    return project; // logging omitted from this excerpt
}
```

#### Adding a task or an event

The sequence diagram below shows how `task.add project/P001 name/Wireframe date/2026-10-15` is executed. `event.add` follows the same steps, using `addEvent`, `Event`, `Project#addEvent` and the `E` ID prefix.

![Sequence diagram of task.add](images/AddTaskSequenceDiagram.png)

1. `ProjectManager#addTask` finds the project with `getProject`, which throws a `ProjectException` if the project does not exist. Missing projects are never created automatically.
2. It creates the `Task` with the next task ID. The `ScheduledItem` constructor rejects a blank description or a missing date.
3. `Project#addTask` checks that the task belongs to this project and that its ID is not already used, then adds it to the task list.
4. Only then does the manager advance the task counter. `CommandHandler` passes the new task to `Ui#showTaskAdded`.

#### Handling errors

Each layer throws its own subclass of the unchecked `BusyNotFreeException`: `CommandException` for malformed input, `ProjectException`, `TaskException` and `EventException` for invalid model data. `CommandHandler#execute` catches `BusyNotFreeException`, displays its message, and then shows the format of the failed command through `Ui#showAddCommandUsage`. The application keeps running, so the user can enter a corrected command. Unrelated programming errors are not caught, so they are not hidden.

#### Design considerations

* **IDs are only used up by successful commands.** Every check runs before a record is stored and its counter is advanced. A rejected command therefore leaves no gap in the ID sequence, and deleted IDs are never reused because the counters are saved by `DataStorage`.
* **`ProjectManager` depends on the `ClientLookup` interface, not on `ClientManager`.** Project creation could therefore be built and tested before client management was ready, using `PlaceholderClientLookup`. Tests can also supply a lambda such as `clientId -> false` to simulate a missing client.
* **A shared `ScheduledItem` superclass for tasks and events.** The common fields and checks live in one place. Each subclass passes its own exception type into the constructor as a function, for example `TaskException::new`, so a failed check throws the right exception without duplicating the checks.
  * Alternative considered: a single `Activity` class with a type field. This was rejected because tasks and events behave differently; for example, only tasks can be marked complete. Separate classes also keep the task and event sections of `project.view` simple.
* **Prefixed arguments in any order.** The trade-off is that recognised prefixes such as `name/` cannot appear after a space inside a value. Alternative considered: quoting or escaping values. This was rejected as harder to type, because descriptions that need a reserved prefix are rare.
* **Strict dates.** Impossible or loosely formatted dates are rejected instead of being corrected, so a typo cannot silently schedule work on the wrong day.
* **Logging without private text.** `ProjectManager` logs creation at the `FINE` level and includes only IDs, never client names or descriptions.

### Task completion, project progress and persistence

`task.mark TASK_ID` and `task.unmark TASK_ID` find a task by its application-wide ID through
`ProjectManager`. They change the task's `completed` flag without changing its ID, project,
description or date. `Task#toString` includes `[COMPLETED]` or `[In Progress]` so each task's
state is visible in `project.view`.

`Ui#showProjectSummary` counts completed tasks while it prints the project's task list, then
shows the completed and total task counts. Events are printed in a separate section and do not
affect this count. A project with no tasks shows `No tasks found.` and
`Progress: 0 of 0 tasks completed.`.

`DataStorage` saves each task's flag as `task.N.completed=true` or `false` in the existing
version 1 properties file. On load, a missing flag defaults to `false`, allowing files saved
before task completion was added to load with their tasks in progress. An invalid flag is
rejected as invalid saved data. The existing exit path saves the file and the next startup
loads it; marking a task alone does not write to disk.

[`DataStorageTest`](../src/test/java/busynotfree/storage/DataStorageTest.java) checks saved and
restored completion state and the older-file default. [`ProjectViewTest`](../src/test/java/busynotfree/command/ProjectViewTest.java)
checks empty, partial and complete progress counts, including `project.view all`.

### Editing tasks and events: `task.edit` and `event.edit`

`task.edit TASK_ID name/DESCRIPTION date/YYYY-MM-DD` and `event.edit EVENT_ID name/DESCRIPTION date/YYYY-MM-DD` replace the description and date of an existing task or event. Both commands share one implementation path:

1. `CommandHandler#routeCommand` sends `task.edit` to `handleTaskEdit` and `event.edit` to `handleEventEdit`.
2. Both handlers call `parseEditArguments`. It takes the first word as the item ID, then uses `ArgumentParser` to read the required `name/` and `date/` arguments, and `ArgumentParser#parseDate` to check the date. The result is held in a private `EditArguments` record.
3. The handler calls `ProjectManager#editTask` or `ProjectManager#editEvent`. The manager finds the item by its application-wide ID with `getTask` or `getEvent`, searching every project.
4. The manager calls the package-private `Task#edit` or `Event#edit`. Both delegate to `ScheduledItem#updateDetails`, passing in their own exception type.
5. `updateDetails` checks the new description and date with the same `validateDetails` helper used by the constructor, and only then assigns both fields.
6. `Ui#showTaskEdited` or `Ui#showEventEdited` displays the updated item using its `toString()`, so a task also shows its completion state.

```java
protected final void updateDetails(String description, LocalDate date,
        Function<String, ? extends BusyNotFreeException> exceptionFactory) {
    validateDetails(description, date, exceptionFactory);
    this.description = description.trim();
    this.date = date;
}
```

**Design considerations**

* **An invalid edit leaves the item unchanged.** Every check happens before any field is assigned: the command format and date are checked in `CommandHandler`, the ID in `ProjectManager`, and blank values in `ScheduledItem`. Description and date are assigned together, so an edit never applies half of its changes.
* **ID and project cannot change.** `ScheduledItem` keeps `id` and `project` as `final` fields and makes only `description` and `date` editable. `parseEditArguments` accepts only `name/` and `date/`, so `project/` is rejected as an unexpected prefix. Editing does not create an item, so ID counters are not advanced.
* **Completion status is preserved.** `Task#edit` changes only the fields inherited from `ScheduledItem`; it never touches `completed`.
* **Edits go through `ProjectManager`.** `edit` is package-private, like `Project#removeAllActivities`, so other packages cannot change an item without the manager's ID lookup.
* **Alternative considered: replacing the item.** A new `Task` or `Event` could replace the old one in its project's list. This would need extra code to copy the completion state and keep the item's position in the list, so editing the existing object was chosen as the simpler option.
* **Storage needs no change.** `DataStorage` already saves each item's current description and date, so edited values survive a restart.


### Managing clients: `client.add`, `client.note`, `client.view` and `client.list`

Client management lets freelancers register clients and keep notes about their preferences or requirements. The four commands are:

* `client.add name/CLIENT_NAME` registers a client and displays the assigned ID.
* `client.note CLIENT_ID text/NOTE` appends a note to an existing client.
* `client.view CLIENT_ID` displays a client's ID, name and all recorded notes.
* `client.list` displays every client's ID and name in creation order.

For example, `client.add name/Acme Studio` can create `C001`, after which `client.note C001 text/Prefers weekly updates` records a preference for that client. IDs are case-sensitive; two clients may have the same name but receive different IDs.

#### Responsibilities and integration

* `CommandHandler` parses the command through `CommandParser` and delegates commands whose entity type is `client` to `ClientCommandHandler`. It also handles errors through the shared `Ui`.
* `ClientCommandHandler` selects the client operation, validates its argument structure and calls `ClientManager`. It reuses `ArgumentParser` for the `name/` and `text/` arguments.
* `ClientManager` owns a `LinkedHashMap<String, Client>` and the next client number. It creates clients, looks them up by ID and directs notes to their owner. The map preserves creation order while supporting ID lookup without scanning every client.
* `Client` holds an immutable ID and name, and an append-only list of notes. Its constructor and `addNote` method reject null or blank values and trim surrounding whitespace.
* `ClientUi` formats client responses and sends them through the shared `Ui#showMessage`, keeping output formatting separate from command parsing and model changes.

`ClientManager` implements `ClientLookup`, allowing `ProjectManager` to check whether a client exists without depending on client command handling. The application uses the same client manager for command handling and project lookup, so a newly registered client can immediately be used by `project.add`. Callers that need this integration should use the four-argument `CommandHandler` constructor with the shared manager; the three-argument constructor creates an independent client manager.

#### Registering a client

The sequence diagram below shows the successful execution of `client.add name/Acme Studio`, assuming the next available client ID is `C001`. It starts after `CommandParser` has parsed the input; separator output and read-only getter calls used for formatting are omitted.

![Sequence diagram of client.add](images/ClientAddSequenceDiagram.png)

1. `CommandHandler#routeCommand` delegates the parsed command to `ClientCommandHandler#execute`, which selects `handleAdd`.
2. `ArgumentParser#parse(arguments, "name")` requires exactly one non-blank `name/` value. Parsing finishes before the manager is called, so a malformed command cannot allocate an ID.
3. `ClientManager#addClient` formats the next number as an ID with the `C` prefix and at least three digits, then constructs a `Client` with that ID and the supplied name.
4. Only after construction succeeds does the manager put the client in its map and increment `nextClientNumber`. A rejected name therefore leaves both the collection and counter unchanged.
5. `ClientUi#showClientAdded` displays the assigned ID and name, for example `Added client C001: Acme Studio.`.

The three-digit padding is a minimum width, so the sequence continues from `C999` to `C1000`. Client names are not map keys, allowing separate clients with identical names.

#### Recording a client note

The sequence diagram below shows `client.note C001 text/Prefers weekly updates`, assuming `C001` already exists. As above, command parsing, separators and formatting getters are omitted.

![Sequence diagram of client.note](images/ClientNoteSequenceDiagram.png)

1. `ClientCommandHandler#handleNote` separates the first word, the positional client ID, from the remaining prefixed arguments. A missing ID, or a prefix supplied in its place, is rejected.
2. `ArgumentParser#parse(noteArguments, "text")` checks that a non-blank note is supplied exactly once.
3. `ClientManager#addNote` calls `getClient` before attempting the update. A missing client raises `ClientException`; it never creates a client automatically.
4. `Client#addNote` validates and trims the text, then appends it to the existing notes. Earlier notes and other clients are unchanged.
5. `ClientUi#showNoteAdded` confirms the update with `Note added to client C001.`.

Names and notes preserve internal spaces and ordinary slash-containing text such as `UI/UX` or URLs. Recognised argument prefixes at whitespace boundaries remain reserved: for example, `name/` inside a note is treated as an unexpected argument, and a second `text/` is rejected as a repeated prefix. Quoting or escaping reserved prefixes is not supported.

#### Viewing and listing clients

`client.view` requires exactly one positional ID. The handler calls `ClientManager#getClient`, then `ClientUi#showClient` prints the ID and name followed by each note in insertion order. A client without notes displays `Notes: No notes found.`. Leading and trailing whitespace around an ID is trimmed, but its case and digits must match an existing ID.

`client.list` calls `ClientManager#getClients`, which returns `List.copyOf(clients.values())`. `ClientUi#showClients` prints numbered entries in creation order, or `No clients found.` when the collection is empty. Listing omits notes; users can request them through `client.view`. Extra arguments to `client.list` are ignored, matching the application's convention for commands without parameters.

Both `getClients()` and `Client#getNotes()` return unmodifiable snapshots of their collections. Callers cannot add or remove entries through these lists. The client snapshot still contains references to the existing `Client` objects; it is not a deep copy of their state.

#### Validation and error handling

Malformed command arguments raise `CommandException`; invalid client data or an unsuccessful client lookup raises `ClientException`. Both extend `BusyNotFreeException`, which `CommandHandler#execute` catches. It displays the error and, for a supported client action, asks `ClientCommandHandler#showCommandUsage` to display the expected format. The command loop remains available for corrected input. An unsupported client action produces an error without a usage hint.

Assertions check internal assumptions: only client commands reach `ClientCommandHandler`, the next client number is positive, a generated ID cannot replace an existing client, and a successful lookup returns the requested ID. These checks help detect programming errors when assertions are enabled; exceptions still validate user input when assertions are disabled.

#### Design considerations

* **Separate client command handling.** `ClientCommandHandler` and `ClientUi` keep client-specific parsing and formatting together while reusing the shared router, parser and output interface. An alternative would be to place every client action in `CommandHandler`; the separate handler limits the growth of that shared class.
* **IDs identify clients instead of names.** Sequential IDs provide a stable reference for notes and projects even when names are duplicated. Using names as keys would require rejecting duplicates or introducing rules to distinguish them.
* **A map preserves both lookup and display order.** `LinkedHashMap` supports lookup by ID and predictable listing. An `ArrayList` would preserve creation order but require a search for each ID; an ordinary `HashMap` would not guarantee creation order.
* **Notes belong to their client.** An ordered list of strings is sufficient for appending and displaying notes. Separate note objects with their own IDs could support editing or deleting individual notes, but those operations are outside the current client commands.
* **Validate before changing state.** Parsing precedes manager calls, client construction precedes registration and counter advancement, and note validation precedes insertion. Invalid operations therefore leave existing client data unchanged.
* **Persistence is handled separately.** Client commands update the in-memory model. The application's `DataStorage` integration saves clients, notes and the next client number with the project data on exit and restores them at startup. The client handler does not perform file I/O or save after each command.

#### Existing verification

[`ClientTest`](../src/test/java/busynotfree/model/ClientTest.java) and [`ClientManagerTest`](../src/test/java/busynotfree/model/ClientManagerTest.java) cover validation, duplicate names, ID allocation beyond `C999`, note ownership, collection snapshots and real client lookup during project creation. [`ClientCommandHandlerTest`](../src/test/java/busynotfree/command/ClientCommandHandlerTest.java) covers all four commands through the shared router, including malformed input, missing clients, output order and recovery after errors. [`ClientArgumentParserTest`](../src/test/java/busynotfree/command/ClientArgumentParserTest.java) covers free text and reserved prefixes. The repeatable console acceptance scenario is documented in the [UI test plan](../test/ui-test-plan.md).

## Product scope
### Target user profile

{Describe the target user profile}

### Value proposition

{Describe the value proposition: what problem does it solve?}

## User Stories

|Version| As a ... | I want to ... | So that I can ...|
|--------|----------|---------------|------------------|
|v1.0|new user|see usage instructions|refer to them when I forget how to use the application|
|v1.0|freelancer|create a project for an existing client with a deadline|organise and plan the client's work|
|v1.0|freelancer|add a dated internal task to a project|track the work that needs to be completed|
|v1.0|freelancer|add a scheduled event to a project|keep track of meetings, reviews and presentations|
|v2.0|user|find a to-do item by name|locate a to-do without having to go through the entire list|
|v2.0|freelancer responding to changes in client requirements|edit an existing task's description and date without changing its ID or project|keep the work plan accurate|
|v2.0|freelance consultant arranging client meetings|edit an existing event's description and date without changing its ID or project|update a rescheduled meeting without deleting and recreating it|

## Non-Functional Requirements

{Give non-functional requirements}

## Glossary

* *glossary item* - Definition

## Instructions for manual testing

{Give instructions on how to do a manual product testing e.g., how to load sample data to be used for testing}
