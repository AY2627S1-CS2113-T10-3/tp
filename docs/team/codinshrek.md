# Balakrishnan Shrikkanth - Project Portfolio Page

## Overview

BusyNotFree is a Java command-line application that helps freelancers manage clients,
projects, tasks and events. I developed the client management features that let users
register clients, record notes, find and rename clients, and review their information.

## Summary of Contributions

- **Code contributed.** My contributions are listed on the
  [tP Code Dashboard](https://nus-cs2113-ay2627-s1.github.io/tp-dashboard/?search=CodinShrek&breakdown=true).

- **Client registration and notes.** Implemented `client.add`, `client.note`,
  `client.view` and `client.list`, including the `Client` and `ClientManager` model
  code. Each client receives a unique ID. Notes retain their insertion order, and
  client information is saved between sessions. [PR #37](https://github.com/AY2627S1-CS2113-T10-3/tp/pull/37).

- **Client search and editing.** Implemented `client.find NAME` to search names
  by a case-insensitive substring, including multiword queries. Implemented
  `client.edit CLIENT_ID name/NEW_NAME` so users can correct a name while keeping
  the client's ID, notes and project links. Search results include a match count,
  while edit confirmations show the old and new names. [PR #112](https://github.com/AY2627S1-CS2113-T10-3/tp/pull/112).

- **Output and reliability improvements.** Made client lists and details easier
  to scan by aligning numbered entries and wrapping long names and notes. Added
  validation for blank and malformed Unicode text, improved Unicode name search,
  guarded client ID allocation against invalid or exhausted counters, and
  displayed terminal control characters safely. These changes involved command
  handling, the model, storage and output formatting. [PR #112](https://github.com/AY2627S1-CS2113-T10-3/tp/pull/112).

- **Testing.** Added tests for client commands and model behavior, search and
  editing, saved data, Unicode input, ID boundaries and long console output. I
  also added a repeatable client UI test to the CI matrix. [PR #37](https://github.com/AY2627S1-CS2113-T10-3/tp/pull/37),
  [PR #112](https://github.com/AY2627S1-CS2113-T10-3/tp/pull/112).

- **Contributions to the User Guide.** Documented the client commands, their
  formats, examples and validation in the [User Guide](../UserGuide.md) and
  [Client Commands guide](../ClientCommands.md). Updated the client guide's
  prefix explanation to match the current parser.

- **Contributions to the Developer Guide.** Wrote the
  [Managing clients](../DeveloperGuide.md#managing-clients) section, covering the
  client classes, command flow, listing, search and error handling. Added the
  `client.add` and `client.note` sequence diagrams and corrected the creation
  arrow in the `client.add` diagram. [PR #37](https://github.com/AY2627S1-CS2113-T10-3/tp/pull/37),
  [PR #112](https://github.com/AY2627S1-CS2113-T10-3/tp/pull/112).

- **Contributions to team-based tasks.** Connected client management with the
  shared project workflow through `ClientLookup`, allowing project creation to
  check registered clients. Added the client UI regression test to the CI matrix
  so changes to shared code are checked against client command behavior.
  [PR #37](https://github.com/AY2627S1-CS2113-T10-3/tp/pull/37),
  [PR #112](https://github.com/AY2627S1-CS2113-T10-3/tp/pull/112).
