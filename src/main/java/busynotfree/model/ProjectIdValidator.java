package busynotfree.model;

import busynotfree.exception.ProjectException;

/**
 * Checks project and activity IDs against another collection of projects.
 * Shared by active-project registration and trashcan storage.
 */
final class ProjectIdValidator {
    /** Prevents instantiation of this validation utility. */
    private ProjectIdValidator() {
    }

    /** Rejects conflicting project, task or event IDs before any data changes. */
    static void validate(Project candidate, Iterable<Project> existingProjects) {
        for (Project existing : existingProjects) {
            boolean hasDuplicateTask = candidate.getTasks().stream()
                    .anyMatch(task -> existing.getTasks().stream()
                            .anyMatch(other -> other.getId().equals(task.getId())));
            boolean hasDuplicateEvent = candidate.getEvents().stream()
                    .anyMatch(event -> existing.getEvents().stream()
                            .anyMatch(other -> other.getId().equals(event.getId())));

            if (existing.getProjectId().equals(candidate.getProjectId())
                    || hasDuplicateTask || hasDuplicateEvent) {
                throw new ProjectException("Cannot restore or load project "
                        + candidate.getProjectId() + ": an ID is already in use.");
            }
        }
    }
}
