package app;

/*
 * Main.java
 * ---------
 * Entry point of the Scheduler Application.
 *
 * Role in MVC:
 * - Acts as the Controller
 * - Handles user input via console
 * - Routes actions to services, utilities, and views
 *
 * IMPORTANT:
 * - This file contains ONLY comments added.
 * - No executable code has been changed.
 */


import app.model.Event;
import app.model.RecurringEvent;
import app.model.Reminder;
import app.service.ReminderService;
import app.util.BackupManager;
import app.util.AdditionalFileHandler;
import app.util.EventFileHandler;
import app.util.RecurringFileHandler;
import app.util.ReminderFileHandler;
import app.view.CalendarView;
import app.model.AdditionalEventFields;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Scanner;

public class Main {

    private static final Scanner sc = new Scanner(System.in);

    public static void main(String[] args) {

        // Launch-time reminder notification
        showNextReminderAtLaunch();


        while (true) {
            showMenu();
            String input = sc.nextLine();

            if (input.isEmpty()) continue;

            int choice;
            try {
                choice = Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Invalid choice.");
                continue;
            }

            switch (choice) {
                case 1 -> eventMenu();
                case 2 -> viewMenu();
                case 3 -> backupMenu();
                case 0 -> {
                    renumberAllData();
                    System.out.println("Have a nice day!");
                    return;
                }
                default -> System.out.println("Invalid choice.");
            }

            // Spacer between user responses
            System.out.println();
        }
    }

    private static void showMenu() {
        System.out.println("""
        \n==== Calendar App ====
        1. Event
        2. View
        3. Backup
        0. Exit
                """);
        System.out.print("Choose: ");
    }

    private static void eventMenu() {
        System.out.println("""
                Event menu:
                1. Add event
                2. Update event
                3. Delete event
                4. Search events
                5. Reminders
                """);
        int choice;
        try {
            System.out.print("Choose: ");
            choice = Integer.parseInt(sc.nextLine());
        } catch (NumberFormatException e) {
            System.out.println("Invalid choice.");
            return;
        }

        switch (choice) {
            case 1 -> addEvent();
            case 2 -> updateEvent();
            case 3 -> deleteEvent();
            case 4 -> searchEvents();
            case 5 -> reminderMenu();
            default -> System.out.println("Invalid choice.");
        }
    }

    private static void viewMenu() {
        System.out.println("""
                View menu:
                1. Day calendar
                2. Week calendar
                3. Month calendar
                """);
        int choice;
        try {
            System.out.print("Choose: ");
            choice = Integer.parseInt(sc.nextLine());
        } catch (NumberFormatException e) {
            System.out.println("Invalid choice.");
            return;
        }

        switch (choice) {
            case 1 -> viewDay();
            case 2 -> viewWeek();
            case 3 -> viewMonth();
            default -> System.out.println("Invalid choice.");
        }
    }

    private static void viewDay() {
        LocalDate date;
        try {
            System.out.print("Enter date (yyyy-MM-dd): "); // e.g., 2026-01-11
            date = LocalDate.parse(sc.nextLine());
        } catch (Exception e) {
            System.out.println("Invalid date format. Please use yyyy-MM-dd.");
            return;
        }

        List<Event> events = EventFileHandler.readEvents();
        CalendarView.showDayView(events, date);
    }

    private static void backupMenu() {
        System.out.println("""
                Backup menu:
                1. Backup
                2. Restore
                """);
        int choice;
        try {
            System.out.print("Choose: ");
            choice = Integer.parseInt(sc.nextLine());
        } catch (NumberFormatException e) {
            System.out.println("Invalid choice.");
            return;
        }

        switch (choice) {
            case 1 -> BackupManager.backup("backup.txt");
            case 2 -> BackupManager.restore("backup.txt");
            default -> System.out.println("Invalid choice.");
        }
    }

    private static void showNextReminderAtLaunch() {
        List<Event> events = EventFileHandler.readEvents();
        List<RecurringEvent> recurringRules = RecurringFileHandler.readRecurringEvents();
        List<Reminder> reminders = ReminderFileHandler.readReminders();

        ReminderService.getNextUpcomingReminder(events, recurringRules, reminders, LocalDateTime.now())
                .ifPresent(info -> {
                    String human = ReminderService.formatDuration(info.timeUntilNotify);
                    System.out.println("Your next event is coming soon in " + human + ": " + info.event.getTitle());
                });
    }

    private static void reminderMenu() {
        System.out.println("""
                Reminder options:
                1. Add reminder
                2. Update reminder
                3. Delete reminder
                """);

        int choice;
        try {
            System.out.print("Choose: ");
            choice = Integer.parseInt(sc.nextLine());
        } catch (NumberFormatException e) {
            System.out.println("Invalid choice.");
            return;
        }

        switch (choice) {
            case 1 -> addReminder(false);
            case 2 -> addReminder(true);
            case 3 -> deleteReminder();
            default -> System.out.println("Invalid choice.");
        }
    }

    private static void addReminder(boolean isUpdate) {
        List<Event> events = EventFileHandler.readEvents();
        Integer eventId = resolveEventId(events, isUpdate ? "update a reminder for" : "add a reminder for");
        if (eventId == null) {
            return;
        }

        List<Reminder> reminders = ReminderFileHandler.readReminders();
        Reminder existing = reminders.stream().filter(r -> r.getEventId() == eventId).findFirst().orElse(null);

        if (!isUpdate && existing != null) {
            System.out.println("A reminder already exists for this event. Choose update instead.");
            return;
        }

        if (isUpdate && existing == null) {
            System.out.println("No reminder exists for this event. Choose add instead.");
            return;
        }

        Integer minutes = promptReminderMinutes();
        if (minutes == null) {
            return;
        }

        reminders.removeIf(r -> r.getEventId() == eventId);
        reminders.add(new Reminder(eventId, minutes));
        ReminderFileHandler.writeReminders(reminders);

        System.out.println("Reminder " + (isUpdate ? "updated" : "added") + " (" + minutes + " minutes before the event)");
    }

    private static void deleteReminder() {
        List<Event> events = EventFileHandler.readEvents();
        Integer eventId = resolveEventId(events, "delete a reminder for");
        if (eventId == null) {
            return;
        }

        List<Reminder> reminders = ReminderFileHandler.readReminders();
        boolean removed = reminders.removeIf(r -> r.getEventId() == eventId);
        if (removed) {
            ReminderFileHandler.writeReminders(reminders);
            System.out.println("Reminder deleted.");
        } else {
            System.out.println("No reminder found for that event.");
        }
    }

    private static Integer promptReminderMinutes() {
        try {
            System.out.print("Remind how many minutes before the event? (example: 30, 1440 for 1 day): ");
            int minutes = Integer.parseInt(sc.nextLine());
            if (minutes < 0) {
                System.out.println("Minutes cannot be negative.");
                return null;
            }
            return minutes;
        } catch (NumberFormatException e) {
            System.out.println("Invalid number.");
            return null;
        }
    }

    // ================= RENORMALIZE IDS ON EXIT =================
    private static void renumberAllData() {
        List<Event> events = EventFileHandler.readEvents();
        if (events.isEmpty()) {
            return;
        }

        // Preserve current ordering from file read; assign new sequential IDs starting at 1.
        List<Event> newEvents = new java.util.ArrayList<>();
        java.util.Map<Integer, Integer> idMap = new java.util.HashMap<>();

        int nextId = 1;
        for (Event e : events) {
            idMap.put(e.getEventId(), nextId);
            newEvents.add(new Event(nextId, e.getTitle(), e.getDescription(), e.getStartDateTime(), e.getEndDateTime()));
            nextId++;
        }
        EventFileHandler.writeEvents(newEvents);

        // Update recurring rules
        List<RecurringEvent> recurring = RecurringFileHandler.readRecurringEvents();
        List<RecurringEvent> newRecurring = new java.util.ArrayList<>();
        for (RecurringEvent r : recurring) {
            Integer newId = idMap.get(r.getEventId());
            if (newId != null) {
                newRecurring.add(new RecurringEvent(newId, r.getInterval(), r.getRecurrentTimes(), r.getRecurrentEndDate()));
            }
        }
        RecurringFileHandler.writeRecurringEvents(newRecurring);

        // Update additional fields
        List<AdditionalEventFields> additional = AdditionalFileHandler.readAdditional();
        List<AdditionalEventFields> newAdditional = new java.util.ArrayList<>();
        for (AdditionalEventFields a : additional) {
            Integer newId = idMap.get(a.getEventId());
            if (newId != null) {
                newAdditional.add(new AdditionalEventFields(newId, a.getLocation(), a.getCategory()));
            }
        }
        AdditionalFileHandler.writeAdditional(newAdditional);

        // Update reminders
        List<Reminder> reminders = ReminderFileHandler.readReminders();
        List<Reminder> newReminders = new java.util.ArrayList<>();
        for (Reminder r : reminders) {
            Integer newId = idMap.get(r.getEventId());
            if (newId != null) {
                newReminders.add(new Reminder(newId, r.getMinutesBefore()));
            }
        }
        ReminderFileHandler.writeReminders(newReminders);
    }

    // ================= ADD EVENT =================
    private static void addEvent() {

        List<Event> events = EventFileHandler.readEvents();
        int id = EventFileHandler.getNextEventId(events);

        System.out.print("Title: ");
        String title = sc.nextLine();

    System.out.print("Description (optional, press Enter to skip): ");
    String descInput = sc.nextLine();
    String desc = descInput.isBlank() ? "" : descInput;

        LocalDateTime start;
        LocalDateTime end;
        try {
            System.out.print("Start (yyyy-MM-ddTHH:mm): ");
            start = LocalDateTime.parse(sc.nextLine());

            System.out.print("End (yyyy-MM-ddTHH:mm): ");
            end = LocalDateTime.parse(sc.nextLine());
        } catch (Exception parseEx) {
            System.out.println("Invalid date/time format. Please use yyyy-MM-ddTHH:mm (example: 2026-01-06T10:30)");
            return;
        }

        if (!start.isBefore(end)) {
            System.out.println("Start time must be before end time.");
            return;
        }

        Event e = new Event(id, title, desc, start, end);

        if (EventFileHandler.hasConflict(e)) {
            System.out.println("Time conflict detected. Event not added.");
            return;
        }

        events.add(e);
        EventFileHandler.writeEvents(events);

        // Additional fields (stored separately for marking purposes)
        System.out.print("Location (optional): ");
        String location = sc.nextLine();

        System.out.print("Category (optional): ");
        String category = sc.nextLine();

        AdditionalFileHandler.upsert(new AdditionalEventFields(id, location, category));

        // Optional: add reminder immediately after creating the event
        maybeAddReminderForEvent(id);

        // Optional: add recurring settings immediately after creating the event
    System.out.print("Is this event recurring? (y/n): ");
    String recurringAnswer = sc.nextLine().trim().toLowerCase();
        if (recurringAnswer.equals("y") || recurringAnswer.equals("yes")) {

            System.out.print("Interval (1d / 1w / 1m / 1y): ");
            String interval = sc.nextLine().trim();

            System.out.println("""
                    1. Repeat until end date
                    2. Repeat for number of times
                    """);
            int choice;
            try {
                System.out.print("Choose: ");
                choice = Integer.parseInt(sc.nextLine());
            } catch (NumberFormatException nfeChoice) {
                System.out.println("Invalid choice. Event was added as non-recurring.");
                System.out.println("Event added with ID " + id);
                return;
            }

            int times = 0;
            LocalDate endDate = null;

            if (choice == 1) {
                try {
                    System.out.print("End date (yyyy-MM-dd): ");
                    endDate = LocalDate.parse(sc.nextLine());
                } catch (Exception parseEndDateEx) {
                    System.out.println("Invalid date format. Event was added as non-recurring.");
                    System.out.println("Event added with ID " + id);
                    return;
                }
            } else if (choice == 2) {
                try {
                    System.out.print("Number of times: ");
                    times = Integer.parseInt(sc.nextLine());
                } catch (NumberFormatException nfeTimes) {
                    System.out.println("Invalid number. Event was added as non-recurring.");
                    System.out.println("Event added with ID " + id);
                    return;
                }
            } else {
                System.out.println("Invalid choice. Event was added as non-recurring.");
                System.out.println("Event added with ID " + id);
                return;
            }

            List<RecurringEvent> list = RecurringFileHandler.readRecurringEvents();
            // Ensure we don't end up with duplicate recurring entries for the same event.
            list.removeIf(r -> r.getEventId() == id);
            list.add(new RecurringEvent(id, interval, times, endDate));
            RecurringFileHandler.writeRecurringEvents(list);

            System.out.println("Event added with ID " + id + " (recurring)");
            return;
        }

        System.out.println("Event added with ID " + id);
    }

    private static void maybeAddReminderForEvent(int eventId) {
        System.out.print("Add a reminder for this event? (y/n): ");
        String ans = sc.nextLine().trim().toLowerCase();
        if (!(ans.equals("y") || ans.equals("yes"))) {
            return;
        }

        Integer minutes = promptReminderMinutes();
        if (minutes == null) {
            return;
        }

        List<Reminder> reminders = ReminderFileHandler.readReminders();
        reminders.removeIf(r -> r.getEventId() == eventId);
        reminders.add(new Reminder(eventId, minutes));
        ReminderFileHandler.writeReminders(reminders);

        System.out.println("Reminder added (" + minutes + " minutes before the event)");
    }

    // ================= UPDATE EVENT =================
    private static void updateEvent() {

        List<Event> events = EventFileHandler.readEvents();
        Integer id = resolveEventId(events, "update");
        if (id == null) {
            return;
        }

        Event current = events.stream().filter(e -> e.getEventId() == id).findFirst().orElse(null);
        if (current == null) {
            System.out.println("Event not found.");
            return;
        }

        // Allow per-field opt-out by leaving input blank.
        System.out.print("New Title (leave blank to keep current: '" + current.getTitle() + "'): ");
        String titleInput = sc.nextLine();
        String title = titleInput.isBlank() ? current.getTitle() : titleInput;

        System.out.print("New Description (leave blank to keep current): ");
        String descInput = sc.nextLine();
        String desc = descInput.isBlank() ? current.getDescription() : descInput;

        LocalDateTime start = current.getStartDateTime();
        LocalDateTime end = current.getEndDateTime();
        try {
            System.out.print("New Start (yyyy-MM-ddTHH:mm, leave blank to keep current): ");
            String startInput = sc.nextLine();
            if (!startInput.isBlank()) {
                start = LocalDateTime.parse(startInput);
            }

            System.out.print("New End (yyyy-MM-ddTHH:mm, leave blank to keep current): ");
            String endInput = sc.nextLine();
            if (!endInput.isBlank()) {
                end = LocalDateTime.parse(endInput);
            }
        } catch (Exception e) {
            System.out.println("Invalid date/time format. Please use yyyy-MM-ddTHH:mm (example: 2026-01-06T10:30)");
            return;
        }

        if (!start.isBefore(end)) {
            System.out.println("Start time must be before end time.");
            return;
        }

        Event updated = new Event(id, title, desc, start, end);

        if (EventFileHandler.hasConflict(updated)) {
            System.out.println("Time conflict detected. Update cancelled.");
            return;
        }

        boolean ok = EventFileHandler.updateEvent(updated);

        if (ok) {
            var additionalMap = AdditionalFileHandler.readAdditionalMap();
            AdditionalEventFields existingAdd = additionalMap.get(id);

            System.out.print("New Location (leave blank to keep current" + (existingAdd != null && existingAdd.getLocation() != null ? ": '" + existingAdd.getLocation() + "'" : "") + "): ");
            String locationInput = sc.nextLine();

            System.out.print("New Category (leave blank to keep current" + (existingAdd != null && existingAdd.getCategory() != null ? ": '" + existingAdd.getCategory() + "'" : "") + "): ");
            String categoryInput = sc.nextLine();

            String location = locationInput.isBlank() ? (existingAdd != null ? existingAdd.getLocation() : null) : locationInput;
            String category = categoryInput.isBlank() ? (existingAdd != null ? existingAdd.getCategory() : null) : categoryInput;

            // Only write if there was an existing record or the user provided something.
            if (existingAdd != null || !locationInput.isBlank() || !categoryInput.isBlank()) {
                AdditionalFileHandler.upsert(new AdditionalEventFields(id, location, category));
            }

            // Recurrence: allow optional change/remove/keep
            RecurringEvent existingRec = RecurringFileHandler.findByEventId(id);
            handleRecurringUpdate(id, existingRec);

            // Reminder: optional keep/change/remove
            handleReminderUpdate(id);
        }
        System.out.println(ok ? "Event updated." : "Event not found.");
    }

    private static void handleRecurringUpdate(int eventId, RecurringEvent existingRec) {
        String existingLabel = existingRec == null ? "none" : (existingRec.getInterval() + (existingRec.getRecurrentEndDate() != null ? ", until " + existingRec.getRecurrentEndDate() : ", times=" + existingRec.getRecurrentTimes()));
        System.out.print("Recurring settings (current: " + existingLabel + ") — press Enter to keep, type 'change' to edit, or 'remove' to delete: ");
        String ans = sc.nextLine().trim().toLowerCase();

        if (ans.isEmpty() || ans.equals("keep")) {
            return; // no change
        }

        List<RecurringEvent> list = RecurringFileHandler.readRecurringEvents();
        list.removeIf(r -> r.getEventId() == eventId);

        if (ans.equals("remove")) {
            RecurringFileHandler.writeRecurringEvents(list);
            System.out.println("Recurring settings removed.");
            return;
        }

        if (ans.equals("change") || ans.equals("y") || ans.equals("yes")) {
            RecurringEvent updated = promptRecurringSettings(eventId);
            if (updated != null) {
                list.add(updated);
                RecurringFileHandler.writeRecurringEvents(list);
                System.out.println("Recurring settings updated.");
            } else {
                // If prompt failed, keep original (already removed; restore if existed)
                if (existingRec != null) {
                    list.add(existingRec);
                    RecurringFileHandler.writeRecurringEvents(list);
                }
            }
            return;
        }

        // Unrecognized answer: restore prior state
        if (existingRec != null) {
            list.add(existingRec);
        }
        RecurringFileHandler.writeRecurringEvents(list);
        System.out.println("Recurring unchanged (unrecognized input).");
    }

    private static RecurringEvent promptRecurringSettings(int eventId) {
        try {
            System.out.print("Interval (1d / 1w / 1m / 1y): ");
            String interval = sc.nextLine().trim();

            System.out.println("""
                    1. Repeat until end date
                    2. Repeat for number of times
                    """);
            System.out.print("Choose: ");
            int choice = Integer.parseInt(sc.nextLine());

            int times = 0;
            LocalDate endDate = null;

            if (choice == 1) {
                System.out.print("End date (yyyy-MM-dd): ");
                endDate = LocalDate.parse(sc.nextLine());
            } else if (choice == 2) {
                System.out.print("Number of times: ");
                times = Integer.parseInt(sc.nextLine());
            } else {
                System.out.println("Invalid choice. Recurrence not changed.");
                return null;
            }

            return new RecurringEvent(eventId, interval, times, endDate);
        } catch (Exception e) {
            System.out.println("Invalid recurrence input. Recurrence not changed.");
            return null;
        }
    }

    private static void handleReminderUpdate(int eventId) {
        List<Reminder> reminders = ReminderFileHandler.readReminders();
        Reminder existing = reminders.stream().filter(r -> r.getEventId() == eventId).findFirst().orElse(null);

        String currentLabel = existing == null ? "none" : (existing.getMinutesBefore() + " min before");
        System.out.print("Reminder (current: " + currentLabel + ") — press Enter to keep, type 'change' to set minutes, or 'remove' to delete: ");
        String ans = sc.nextLine().trim().toLowerCase();

        if (ans.isEmpty() || ans.equals("keep")) {
            return; // no change
        }

        if (ans.equals("remove")) {
            boolean removed = reminders.removeIf(r -> r.getEventId() == eventId);
            if (removed) {
                ReminderFileHandler.writeReminders(reminders);
                System.out.println("Reminder removed.");
            } else {
                System.out.println("No reminder to remove.");
            }
            return;
        }

        if (ans.equals("change") || ans.equals("add") || ans.equals("y") || ans.equals("yes")) {
            Integer minutes = promptReminderMinutes();
            if (minutes == null) return;
            reminders.removeIf(r -> r.getEventId() == eventId);
            reminders.add(new Reminder(eventId, minutes));
            ReminderFileHandler.writeReminders(reminders);
            System.out.println("Reminder updated (" + minutes + " minutes before the event)");
            return;
        }

        System.out.println("Reminder unchanged (unrecognized input).");
    }

    // ================= DELETE EVENT =================
    private static void deleteEvent() {

        List<Event> events = EventFileHandler.readEvents();
        Integer id = resolveEventId(events, "delete");
        if (id == null) {
            return;
        }

        boolean ok = EventFileHandler.deleteEvent(id);
        if (ok) {
            // Remove additional-field row for this event as well.
            AdditionalFileHandler.deleteByEventId(id);

            // Remove reminders tied to this event.
            List<Reminder> reminders = ReminderFileHandler.readReminders();
            boolean removedReminder = reminders.removeIf(r -> r.getEventId() == id);
            if (removedReminder) {
                ReminderFileHandler.writeReminders(reminders);
            }

            // Also remove any recurring settings tied to this event.
            List<RecurringEvent> recurring = RecurringFileHandler.readRecurringEvents();
            boolean removedRecurring = recurring.removeIf(r -> r.getEventId() == id);
            if (removedRecurring) {
                RecurringFileHandler.writeRecurringEvents(recurring);
            }

            System.out.println("Event deleted." + (removedRecurring ? " (Recurring entry removed.)" : ""));
        } else {
            System.out.println("Event not found.");
        }
    }

    // ================= VIEW MONTH =================
    private static void viewMonth() {

        int year;
        int month;
        try {
            System.out.print("Year: ");
            year = Integer.parseInt(sc.nextLine());

            System.out.print("Month (1-12): ");
            month = Integer.parseInt(sc.nextLine());
        } catch (NumberFormatException e) {
            System.out.println("Invalid year/month.");
            return;
        }

        List<Event> events = EventFileHandler.readEvents();
        CalendarView.showMonthView(events, year, month);
    }

    // ================= VIEW WEEK =================
    private static void viewWeek() {

        LocalDate start;
        try {
            System.out.print("Week start date (yyyy-MM-dd): ");
            start = LocalDate.parse(sc.nextLine());
        } catch (Exception e) {
            System.out.println("Invalid date format. Please use yyyy-MM-dd (example: 2026-01-06)");
            return;
        }

        List<Event> events = EventFileHandler.readEvents();
        CalendarView.showWeekView(events, start);
    }

    // ================= SEARCH =================
    private static void searchEvents() {

        List<Event> events = EventFileHandler.readEvents();

        System.out.println("""
                Search events by:
                1. Title
                2. Date
                3. Date range
                4. Location
                5. Category
                """);

        int choice;
        try {
            System.out.print("Choose: ");
            choice = Integer.parseInt(sc.nextLine());
        } catch (NumberFormatException e) {
            System.out.println("Invalid choice.");
            return;
        }

        if (choice == 1) {
            System.out.print("Title keyword: ");
            String q = sc.nextLine().trim().toLowerCase();
            if (q.isEmpty()) {
                System.out.println("Title cannot be empty.");
                return;
            }

            List<Event> matches = events.stream()
                    .filter(e -> e.getTitle() != null && e.getTitle().toLowerCase().contains(q))
                    .toList();
            displayResults(matches);
            return;
        }

        if (choice == 2) {
            try {
                System.out.print("Date (yyyy-MM-dd): ");
                LocalDate date = LocalDate.parse(sc.nextLine());
                displayResults(EventFileHandler.searchByDate(date));
            } catch (Exception e) {
                System.out.println("Invalid date format. Please use yyyy-MM-dd (example: 2026-01-06)");
            }
            return;
        }

        if (choice == 3) {
            try {
                System.out.print("Start date (yyyy-MM-dd): ");
                LocalDate start = LocalDate.parse(sc.nextLine());

                System.out.print("End date (yyyy-MM-dd): ");
                LocalDate end = LocalDate.parse(sc.nextLine());

                displayResults(EventFileHandler.searchByDateRange(start, end));
            } catch (Exception e) {
                System.out.println("Invalid date format. Please use yyyy-MM-dd (example: 2026-01-06)");
            }
            return;
        }

        if (choice == 4) {
            System.out.print("Location keyword: ");
            String q = sc.nextLine().trim().toLowerCase();

            var additionalMap = AdditionalFileHandler.readAdditionalMap();
            List<Event> matches = events.stream()
                    .filter(e -> {
                        var a = additionalMap.get(e.getEventId());
                        if (a == null || a.getLocation() == null) return false;
                        return a.getLocation().toLowerCase().contains(q);
                    })
                    .toList();

            displayResults(matches);
            return;
        }

        if (choice == 5) {
            System.out.print("Category keyword: ");
            String q = sc.nextLine().trim().toLowerCase();

            var additionalMap = AdditionalFileHandler.readAdditionalMap();
            List<Event> matches = events.stream()
                    .filter(e -> {
                        var a = additionalMap.get(e.getEventId());
                        if (a == null || a.getCategory() == null) return false;
                        return a.getCategory().toLowerCase().contains(q);
                    })
                    .toList();

            displayResults(matches);
            return;
        }

        System.out.println("Invalid choice.");
    }

    private static void displayResults(List<Event> events) {

        if (events.isEmpty()) {
            System.out.println("No events found.");
            return;
        }

        for (Event e : events) {
            System.out.println("[" + e.getEventId() + "] " + e.getTitle());
            System.out.println("    " + e.getStartDateTime() +
                    " → " + e.getEndDateTime());
            System.out.println();
        }
    }

    // ================= EVENT PICKER =================
    /*
    * Resolve an eventId based on user preference: Title or Date.
     * Returns null if the user cancels or no matching event is found.
     */
    private static Integer resolveEventId(List<Event> events, String action) {

        if (events == null || events.isEmpty()) {
            System.out.println("No events available.");
            return null;
        }

    System.out.println("Select event to " + action + " by:");
    System.out.println("1. Title");
    System.out.println("2. Date");

        int choice;
        try {
            System.out.print("Choose: ");
            choice = Integer.parseInt(sc.nextLine());
        } catch (NumberFormatException e) {
            System.out.println("Invalid choice.");
            return null;
        }

        if (choice == 1) {
            System.out.print("Title keyword: ");
            String q = sc.nextLine().trim().toLowerCase();
            if (q.isEmpty()) {
                System.out.println("Title cannot be empty.");
                return null;
            }

            List<Event> matches = events.stream()
                    .filter(e -> e.getTitle() != null && e.getTitle().toLowerCase().contains(q))
                    .toList();

            return resolveFromMatches(matches);
        }

        if (choice == 2) {
            LocalDate date;
            try {
                System.out.print("Date (yyyy-MM-dd): ");
                date = LocalDate.parse(sc.nextLine());
            } catch (Exception e) {
                System.out.println("Invalid date format. Please use yyyy-MM-dd (example: 2026-01-06)");
                return null;
            }

            List<Event> matches = events.stream()
                    .filter(e -> e.getStartDateTime() != null && e.getStartDateTime().toLocalDate().equals(date))
                    .toList();

            return resolveFromMatches(matches);
        }

        System.out.println("Invalid choice.");
        return null;
    }

    private static Integer resolveFromMatches(List<Event> matches) {
        if (matches.isEmpty()) {
            System.out.println("No matching events found.");
            return null;
        }

        if (matches.size() == 1) {
            Event e = matches.get(0);
            System.out.println("Selected: [" + e.getEventId() + "] " + e.getTitle() + " (" + e.getStartDateTime() + ")");
            return e.getEventId();
        }

        System.out.println("Multiple matches found:");
        for (int i = 0; i < matches.size(); i++) {
            Event e = matches.get(i);
            System.out.println((i + 1) + ". " + e.getTitle() + " (" + e.getStartDateTime() + ")");
        }

        int pick;
        try {
            System.out.print("Choose 1-" + matches.size() + ": ");
            pick = Integer.parseInt(sc.nextLine());
        } catch (NumberFormatException e) {
            System.out.println("Invalid choice.");
            return null;
        }

        if (pick < 1 || pick > matches.size()) {
            System.out.println("Choice out of range.");
            return null;
        }

        return matches.get(pick - 1).getEventId();
    }

}
