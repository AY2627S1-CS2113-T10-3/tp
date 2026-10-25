# Balakrishnan Shrikkanth - Project Portfolio Page

## Overview

BusyNotFree is a command-line application that helps freelancers manage clients,
projects, tasks and events. My contributions focus on client management.

### Summary of Contributions

- **Client search:** Implemented `client.find NAME` to find clients using
  case-insensitive partial-name matching and display matching IDs and names.
- **Search summaries:** Added match counts for zero, one and multiple results.
- **Client name editing:** Implemented `client.edit CLIENT_ID name/NEW_NAME`,
  preserving the client's ID, notes and project links while validating input.
- **Edit confirmations:** Displayed both the previous and new client names so
  users can verify the change.
- **Testing:** Added five focused JUnit tests covering search results, invalid
  inputs, client identity, notes, project links and persistence after renaming.
- **User documentation:** Documented command syntax, examples and validation in
  the [User Guide](../UserGuide.md) and [Client Commands guide](../ClientCommands.md).
- **Developer documentation:** Documented the client management design and
  client addition and note sequence diagrams in the [Developer Guide](../DeveloperGuide.md).
