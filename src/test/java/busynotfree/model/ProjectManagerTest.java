package busynotfree.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import busynotfree.exception.EventException;
import busynotfree.exception.ProjectException;
import busynotfree.exception.TaskException;

/**
 * Tests in-memory ownership, independent IDs and the replaceable client lookup.
 */
class ProjectManagerTest {
    private final ProjectManager manager = new ProjectManager(new PlaceholderClientLookup());
    private final LocalDate date = LocalDate.of(2026, 11, 30);

    @Test
    public void addProject_validData_assignsSequentialIds() {
        Project first = manager.addProject("C001", "Website", date);
        Project second = manager.addProject("C002", "App", date);

        assertEquals("P001", first.getProjectId());
        assertEquals("P002", second.getProjectId());
        assertEquals(List.of(first, second), manager.getProjects());
        assertSame(first, manager.getProject(" P001 "));
    }

    @Test
    public void addProject_placeholderClient_doesNotRequireClientImplementation() {
        Project project = manager.addProject(" C999 ", "Website", date);

        assertEquals("C999", project.getClientId());
        assertEquals("P001", project.getProjectId());
    }

    @Test
    public void addProject_injectedClientLookup_rejectsUnknownClient() {
        ProjectManager checkedManager = new ProjectManager(clientId -> clientId.equals("C001"));

        assertThrows(ProjectException.class, () -> checkedManager.addProject("C999", "Website", date));
        assertTrue(checkedManager.getProjects().isEmpty());
        assertEquals("P001", checkedManager.addProject("C001", "Website", date).getProjectId());
    }

    @Test
    public void addProject_invalidData_doesNotConsumeId() {
        assertThrows(ProjectException.class, () -> manager.addProject("", "Website", date));
        assertThrows(ProjectException.class, () -> manager.addProject("C001", " ", date));
        assertThrows(ProjectException.class, () -> manager.addProject("C001", "Website", null));

        assertTrue(manager.getProjects().isEmpty());
        assertEquals("P001", manager.addProject("C001", "Website", date).getProjectId());
    }

    @Test
    public void addItems_acrossProjects_assignsIndependentGlobalIds() {
        Project first = manager.addProject("C001", "Website", date);
        Project second = manager.addProject("C001", "App", date);
        Task firstTask = manager.addTask("P001", "Wireframe", date);
        Event firstEvent = manager.addEvent("P002", "Meeting", date);
        Task secondTask = manager.addTask("P002", "Prototype", date);
        Event secondEvent = manager.addEvent("P001", "Review", date);

        assertEquals("T001", firstTask.getId());
        assertEquals("T002", secondTask.getId());
        assertEquals("E001", firstEvent.getId());
        assertEquals("E002", secondEvent.getId());
        assertSame(first, firstTask.getProject());
        assertSame(second, firstEvent.getProject());
        assertEquals(List.of(firstTask), first.getTasks());
        assertEquals(List.of(secondTask), second.getTasks());
        assertEquals(List.of(firstEvent), second.getEvents());
        assertEquals(List.of(secondEvent), first.getEvents());
    }

    @Test
    public void addItems_missingProject_doesNotCreateParentOrConsumeIds() {
        assertThrows(ProjectException.class, () -> manager.addTask("P999", "Work", date));
        assertThrows(ProjectException.class, () -> manager.addEvent("P999", "Meeting", date));
        assertTrue(manager.getProjects().isEmpty());

        manager.addProject("C001", "Website", date);
        assertEquals("T001", manager.addTask("P001", "Work", date).getId());
        assertEquals("E001", manager.addEvent("P001", "Meeting", date).getId());
    }

    @Test
    public void addItems_invalidData_doesNotMutateProjectOrConsumeIds() {
        Project project = manager.addProject("C001", "Website", date);

        assertThrows(TaskException.class, () -> manager.addTask("P001", " ", date));
        assertThrows(TaskException.class, () -> manager.addTask("P001", "Work", null));
        assertThrows(EventException.class, () -> manager.addEvent("P001", " ", date));
        assertThrows(EventException.class, () -> manager.addEvent("P001", "Meeting", null));
        assertEquals(0, project.getItemCount());
        assertEquals("T001", manager.addTask("P001", "Work", date).getId());
        assertEquals("E001", manager.addEvent("P001", "Meeting", date).getId());
    }

    @Test
    public void getProject_blankOrUnknownId_throwsException() {
        assertThrows(ProjectException.class, () -> manager.getProject(null));
        assertThrows(ProjectException.class, () -> manager.getProject(" "));
        assertThrows(ProjectException.class, () -> manager.getProject("P999"));
    }

    @Test
    public void markTaskComplete_findsTaskAcrossProjectsWithoutChangingOtherTasks() {
        manager.addProject("C001", "Website", date);
        manager.addProject("C001", "App", date);
        Task first = manager.addTask("P001", "Wireframe", date);
        Task second = manager.addTask("P002", "Prototype", date);

        assertSame(second, manager.markTaskComplete(" T002 "));

        assertTrue(second.isCompleted());
        assertFalse(first.isCompleted());
        assertSame(second, manager.getTask("T002"));
        assertThrows(TaskException.class, () -> manager.markTaskComplete("T999"));
        assertThrows(TaskException.class, () -> manager.markTaskComplete("E001"));
        assertThrows(TaskException.class, () -> manager.markTaskComplete(" "));
        assertTrue(second.isCompleted());
    }

    @Test
    public void markTaskIncomplete_reopensOnlyMatchingTask() {
        manager.addProject("C001", "Website", date);
        manager.addProject("C001", "App", date);
        Task first = manager.addTask("P001", "Wireframe", date);
        Task second = manager.addTask("P002", "Prototype", date);
        manager.markTaskComplete("T001");
        manager.markTaskComplete("T002");

        assertSame(second, manager.markTaskIncomplete(" T002 "));

        assertTrue(first.isCompleted());
        assertFalse(second.isCompleted());
        assertThrows(TaskException.class, () -> manager.markTaskIncomplete("T999"));
        assertThrows(TaskException.class, () -> manager.markTaskIncomplete("E001"));
        assertThrows(TaskException.class, () -> manager.markTaskIncomplete(" "));
        assertTrue(first.isCompleted());
        assertFalse(second.isCompleted());
    }

    @Test
    public void getProjects_returnsUnmodifiableSnapshot() {
        Project first = manager.addProject("C001", "Website", date);
        List<Project> snapshot = manager.getProjects();
        manager.addProject("C001", "App", date);

        assertEquals(List.of(first), snapshot);
        assertThrows(UnsupportedOperationException.class, snapshot::clear);
    }

    @Test
    public void getProjectsByStatus_returnsMatchingProjectsInCreationOrder() {
        Project first = manager.addProject("C001", "Website", date);
        Project second = manager.addProject("C001", "App", date);
        Project third = manager.addProject("C001", "Logo", date);
        manager.changeProjectStatus(first.getProjectId(), ProjectStatus.PAUSED);
        manager.changeProjectStatus(third.getProjectId(), ProjectStatus.PAUSED);

        assertEquals(List.of(first, third), manager.getProjectsByStatus(ProjectStatus.PAUSED));
        assertEquals(List.of(second), manager.getProjectsByStatus(ProjectStatus.ACTIVE));
        assertThrows(ProjectException.class, () -> manager.getProjectsByStatus(null));
    }

    @Test
    public void getProjectsByDeadline_sortsEarliestFirstAndKeepsTiesInCreationOrder() {
        Project latest = manager.addProject("C001", "Latest", LocalDate.of(2026, 12, 15));
        Project earliest = manager.addProject("C001", "Earliest", LocalDate.of(2026, 10, 15));
        Project tied = manager.addProject("C001", "Tied", LocalDate.of(2026, 10, 15));

        assertEquals(List.of(earliest, tied, latest), manager.getProjectsByDeadline());
    }

    @Test
    public void getOverdueActiveProjects_returnsOnlyActiveProjectsBeforeToday() {
        LocalDate today = LocalDate.of(2026, 10, 25);
        Project overdue = manager.addProject("C001", "Overdue", today.minusDays(1));
        Project dueToday = manager.addProject("C001", "Due today", today);
        Project upcoming = manager.addProject("C001", "Upcoming", today.plusDays(1));
        Project pausedOverdue = manager.addProject("C001", "Paused overdue", today.minusDays(2));
        manager.changeProjectStatus(pausedOverdue.getProjectId(), ProjectStatus.PAUSED);

        assertEquals(List.of(overdue), manager.getOverdueActiveProjects(today));
        assertThrows(ProjectException.class, () -> manager.getOverdueActiveProjects(null));
        assertEquals(ProjectStatus.ACTIVE, dueToday.getStatus());
        assertEquals(ProjectStatus.ACTIVE, upcoming.getStatus());
    }

    @Test
    public void addProject_after999Ids_doesNotWrapOrReuseId() {
        for (int number = 1; number <= 999; number++) {
            manager.addProject("C001", "Project " + number, date);
        }

        assertEquals("P1000", manager.addProject("C001", "Another project", date).getProjectId());
        assertEquals(1000, manager.getProjects().size());
    }

    @Test
    public void addProject_paddedClientId_passesTrimmedIdToLookup() {
        List<String> checkedIds = new ArrayList<>();
        ProjectManager checkedManager = new ProjectManager(clientId -> {
            checkedIds.add(clientId);
            return true;
        });

        Project project = checkedManager.addProject(" \tC007 \t", " Website ", date);

        assertEquals(List.of("C007"), checkedIds);
        assertEquals("C007", project.getClientId());
        assertEquals("Website", project.getName());
    }

    @Test
    public void addProject_nullRequiredValues_doesNotMutateOrConsumeId() {
        assertThrows(ProjectException.class, () -> manager.addProject(null, "Website", date));
        assertThrows(ProjectException.class, () -> manager.addProject("C001", null, date));
        assertThrows(ProjectException.class, () -> manager.addProject("C001", "Website", null));

        assertTrue(manager.getProjects().isEmpty());
        assertEquals("P001", manager.addProject("C001", "Website", date).getProjectId());
    }

    @Test
    public void addItems_nullRequiredValues_doesNotMutateOrConsumeIds() {
        Project project = manager.addProject("C001", "Website", date);
        assertThrows(ProjectException.class, () -> manager.addTask(null, "Work", date));
        assertThrows(ProjectException.class, () -> manager.addEvent(null, "Meeting", date));
        assertThrows(TaskException.class, () -> manager.addTask("P001", null, date));
        assertThrows(EventException.class, () -> manager.addEvent("P001", null, date));
        assertThrows(TaskException.class, () -> manager.addTask("P001", "Work", null));
        assertThrows(EventException.class, () -> manager.addEvent("P001", "Meeting", null));

        assertEquals(0, project.getItemCount());
        assertEquals("T001", manager.addTask("P001", "Work", date).getId());
        assertEquals("E001", manager.addEvent("P001", "Meeting", date).getId());
    }

    @Test
    public void addItems_paddedProjectIdAndDescription_preservesOwnerAndTrimmedText() {
        Project project = manager.addProject("C001", "Website", date);

        Task task = manager.addTask(" \tP001 \t", " \tCreate UI/UX mockup \t", date);
        Event event = manager.addEvent(" \tP001 \t", " \tReview src/main changes \t", date);

        assertSame(project, task.getProject());
        assertSame(project, event.getProject());
        assertEquals("Create UI/UX mockup", task.getDescription());
        assertEquals("Review src/main changes", event.getDescription());
        assertEquals(List.of(task), project.getTasks());
        assertEquals(List.of(event), project.getEvents());
    }

    @Test
    public void addItems_after999Ids_keepsIndependentUniqueIds() {
        Project project = manager.addProject("C001", "Website", date);
        Set<String> taskIds = new HashSet<>();
        Set<String> eventIds = new HashSet<>();
        for (int number = 1; number <= 1000; number++) {
            assertTrue(taskIds.add(manager.addTask("P001", "Task " + number, date).getId()));
            assertTrue(eventIds.add(manager.addEvent("P001", "Event " + number, date).getId()));
        }

        assertEquals("T999", project.getTasks().get(998).getId());
        assertEquals("T1000", project.getTasks().getLast().getId());
        assertEquals("E999", project.getEvents().get(998).getId());
        assertEquals("E1000", project.getEvents().getLast().getId());
        assertEquals(1000, taskIds.size());
        assertEquals(1000, eventIds.size());
        assertEquals(2000, project.getItemCount());
    }

    @Test
    public void addItems_failureAfterSuccess_preservesExistingDataAndNextIds() {
        Project project = manager.addProject("C001", "Website", date);
        Task firstTask = manager.addTask("P001", "First task", date);
        Event firstEvent = manager.addEvent("P001", "First event", date);

        assertThrows(ProjectException.class, () -> manager.addProject("C001", " ", date));
        assertThrows(TaskException.class, () -> manager.addTask("P001", " ", date));
        assertThrows(EventException.class, () -> manager.addEvent("P001", " ", date));
        assertThrows(ProjectException.class, () -> manager.addTask("P999", "Work", date));
        assertThrows(ProjectException.class, () -> manager.addEvent("P999", "Meeting", date));

        assertEquals(List.of(project), manager.getProjects());
        assertEquals(List.of(firstTask), project.getTasks());
        assertEquals(List.of(firstEvent), project.getEvents());
        assertEquals("P002", manager.addProject("C001", "Another project", date).getProjectId());
        assertEquals("T002", manager.addTask("P001", "Second task", date).getId());
        assertEquals("E002", manager.addEvent("P001", "Second event", date).getId());
    }

    @Test
    public void changeProjectStatus_existingProject_updatesAndReturnsProject() {
        Project project = manager.addProject("C001", "Website", date);

        Project updatedProject = manager.changeProjectStatus("P001", ProjectStatus.PAUSED);

        assertSame(project, updatedProject);
        assertEquals(ProjectStatus.PAUSED, project.getStatus());
    }

    @Test
    public void changeProjectStatus_missingProject_rejectsWithoutMutation() {
        assertThrows(ProjectException.class, () -> manager.changeProjectStatus("P999", ProjectStatus.PAUSED));

        assertTrue(manager.getProjects().isEmpty());
    }

    @Test
    public void changeProjectStatus_nullStatus_preservesExistingStatus() {
        Project project = manager.addProject("C001", "Website", date);

        assertThrows(ProjectException.class, () -> manager.changeProjectStatus("P001", null));

        assertEquals(ProjectStatus.ACTIVE, project.getStatus());
    }

    @Test
    public void editTask_taskInOtherProject_updatesOnlyThatTaskWithoutConsumingIds() {
        Project first = manager.addProject("C001", "Website", date);
        Project second = manager.addProject("C001", "App", date);
        Task untouched = manager.addTask("P001", "Wireframe", date);
        Task target = manager.addTask("P002", "Prototype", date);
        manager.markTaskComplete("T002");
        LocalDate newDate = date.plusDays(7);

        assertSame(target, manager.editTask(" T002 ", "Revised prototype", newDate));

        assertEquals("Revised prototype", target.getDescription());
        assertEquals(newDate, target.getDate());
        assertEquals("T002", target.getId());
        assertSame(second, target.getProject());
        assertTrue(target.isCompleted());
        assertEquals(List.of(target), second.getTasks());
        assertEquals(List.of(untouched), first.getTasks());
        assertEquals("Wireframe", untouched.getDescription());
        assertEquals("T003", manager.addTask("P001", "Next task", date).getId());
    }

    @Test
    public void editEvent_eventInOtherProject_updatesOnlyThatEventWithoutConsumingIds() {
        Project first = manager.addProject("C001", "Website", date);
        Project second = manager.addProject("C001", "App", date);
        Event untouched = manager.addEvent("P001", "Kickoff", date);
        Event target = manager.addEvent("P002", "Review", date);
        LocalDate newDate = date.plusDays(7);

        assertSame(target, manager.editEvent(" E002 ", "Rescheduled review", newDate));

        assertEquals("Rescheduled review", target.getDescription());
        assertEquals(newDate, target.getDate());
        assertEquals("E002", target.getId());
        assertSame(second, target.getProject());
        assertEquals(List.of(target), second.getEvents());
        assertEquals(List.of(untouched), first.getEvents());
        assertEquals("Kickoff", untouched.getDescription());
        assertEquals("E003", manager.addEvent("P001", "Next event", date).getId());
    }

    @Test
    public void getEvent_blankOrUnknownId_throwsException() {
        manager.addProject("C001", "Website", date);
        manager.addTask("P001", "Wireframe", date);

        assertThrows(EventException.class, () -> manager.getEvent(null));
        assertThrows(EventException.class, () -> manager.getEvent(" "));
        EventException exception = assertThrows(EventException.class, () -> manager.getEvent("E999"));
        assertEquals("Event E999 was not found. Use project.view to see event IDs.", exception.getMessage());
        assertThrows(EventException.class, () -> manager.getEvent("T001"));
    }

    @Test
    public void editItems_missingIdsOrInvalidDetails_throwWithoutMutation() {
        manager.addProject("C001", "Website", date);
        Task task = manager.addTask("P001", "Wireframe", date);
        Event event = manager.addEvent("P001", "Meeting", date);
        LocalDate newDate = date.plusDays(7);

        assertThrows(TaskException.class, () -> manager.editTask(null, "Work", newDate));
        assertThrows(TaskException.class, () -> manager.editTask(" ", "Work", newDate));
        assertThrows(TaskException.class, () -> manager.editTask("T999", "Work", newDate));
        assertThrows(TaskException.class, () -> manager.editTask("E001", "Work", newDate));
        assertThrows(TaskException.class, () -> manager.editTask("T001", " ", newDate));
        assertThrows(TaskException.class, () -> manager.editTask("T001", "Work", null));
        assertThrows(EventException.class, () -> manager.editEvent(null, "Meeting", newDate));
        assertThrows(EventException.class, () -> manager.editEvent(" ", "Meeting", newDate));
        assertThrows(EventException.class, () -> manager.editEvent("E999", "Meeting", newDate));
        assertThrows(EventException.class, () -> manager.editEvent("T001", "Meeting", newDate));
        assertThrows(EventException.class, () -> manager.editEvent("E001", " ", newDate));
        assertThrows(EventException.class, () -> manager.editEvent("E001", "Meeting", null));

        assertEquals("Wireframe", task.getDescription());
        assertEquals(date, task.getDate());
        assertEquals("Meeting", event.getDescription());
        assertEquals(date, event.getDate());
    }
}
