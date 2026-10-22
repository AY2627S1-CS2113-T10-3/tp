# Developer Guide

## Acknowledgements

{list here sources of all reused/adapted ideas, code, documentation, and third-party libraries -- include links to the original source as well}

## Design & implementation

{Describe the design and implementation of the product. Use UML diagrams and short code snippets where applicable.}

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


## Product scope
### Target user profile

{Describe the target user profile}

### Value proposition

{Describe the value proposition: what problem does it solve?}

## User Stories

|Version| As a ... | I want to ... | So that I can ...|
|--------|----------|---------------|------------------|
|v1.0|new user|see usage instructions|refer to them when I forget how to use the application|
|v2.0|user|find a to-do item by name|locate a to-do without having to go through the entire list|
|v2.0|freelancer responding to changes in client requirements|edit an existing task's description and date without changing its ID or project|keep the work plan accurate|
|v2.0|freelance consultant arranging client meetings|edit an existing event's description and date without changing its ID or project|update a rescheduled meeting without deleting and recreating it|

## Non-Functional Requirements

{Give non-functional requirements}

## Glossary

* *glossary item* - Definition

## Instructions for manual testing

{Give instructions on how to do a manual product testing e.g., how to load sample data to be used for testing}
