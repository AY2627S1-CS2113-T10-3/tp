# Client commands

Member 1's client commands use the existing `busynotfree` CLI. Names and notes
may contain spaces, punctuation, URLs and slash-containing text such as `UI/UX`.
Leading and trailing whitespace is removed; internal spacing is preserved.
Commands and client IDs are case-sensitive.

## Add a client

Format: `client.add name/CLIENT_NAME`

The name must not be blank. Each successful addition receives a unique ID in
creation order (`C001`, `C002`, ..., `C1000`). Duplicate names are allowed because
IDs identify clients. Rejected additions do not consume IDs.

```text
client.add name/Acme Studio
Added client C001: Acme Studio.
```

## Add a note

Format: `client.note CLIENT_ID text/NOTE`

The client must already exist and the note must not be blank. New notes are
appended, so previous notes remain available.

```text
client.note C001 text/Prefers weekly updates
Note added to client C001.
```

Prefix arguments follow the existing parser conventions: `client/`, `project/`,
`name/`, `due/` and `date/` are reserved when they start at a whitespace boundary.
`text/` is also reserved when parsing a note. Repeated or inappropriate reserved
prefixes are rejected. Other slash-containing text remains part of the value.

## View a client

Format: `client.view CLIENT_ID`

Supply exactly one existing client ID. Every recorded note is shown in the order
it was added. A client without notes displays `Notes: No notes found.`

```text
client.view C001
C001 | Acme Studio
Notes: Prefers weekly updates
```

## List clients

Format: `client.list`

The list shows each client's ID and name in creation order. As specified by the
draft's general command rules, extra arguments are ignored for this command.

```text
client.list
1. C001 | Acme Studio
2. C002 | Bright Ideas
```

An empty list displays `No clients found.` Invalid client commands display an
error and the expected command format, then allow the user to try again.

## Find clients by name

Format: `client.find NAME`

Searches for a case-insensitive substring anywhere in a client's name. Multiword
searches are allowed. Matches show numbered IDs and names in creation order,
using the same format as `client.list`. No matches displays `No clients found.`
Missing or whitespace-only search text is rejected.

## Edit a client's name

Format: `client.edit CLIENT_ID name/NEW_NAME`

Renames an existing client and displays `Updated client C001: NEW_NAME.` for
client `C001`. The ID, notes and project links are preserved, and the updated
name is saved on exit. The new name must not be blank; missing or unknown IDs
are rejected. Name arguments follow the prefix rules described above.

## Integration and testing

`ClientManager` implements `ClientLookup`. Application startup supplies the same
manager to `ProjectManager` and `CommandHandler`, so `project.add` now requires a
registered client. Project, task and event models retain their existing APIs.
The three-argument handler constructor remains available for existing callers;
it creates a separate client manager. Callers that need real client validation
should use the four-argument constructor with a shared manager, as in
`BusyNotFree.main`.

Clients and notes are kept in memory while the app runs. On `exit`, the app saves
them with the project data to `data/busynotfree.properties` and loads them on
the next run from the same working directory.

Use Java 25 to run the complete JUnit and Checkstyle checks:

```powershell
.\gradlew.bat test checkstyleMain checkstyleTest shadowJar
```

The repeatable console test and its captured output are recorded in
[`test/ui-test-plan.md`](../test/ui-test-plan.md).
