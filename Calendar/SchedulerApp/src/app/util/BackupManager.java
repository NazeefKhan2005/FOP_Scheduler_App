package app.util;

import app.model.Event;
import app.model.RecurringEvent;
import app.model.Reminder;

import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * BackupManager
 * -------------
 * This utility class handles the backup and restore functionality
 * of the Scheduler application.
 *
 * It allows all event-related data (events, recurrence rules,
 * additional fields, and reminders) to be saved into a single
 * backup file and restored later.
 *
 * This class focuses on data migration and persistence only.
 * It does not contain any user interaction or menu logic.
 */
public class BackupManager {

    // File paths for all data files used in the application
    private static final String EVENT_FILE = "data/event.csv";
    private static final String RECURRENT_FILE = "data/recurrent.csv";
    private static final String ADDITIONAL_FILE = "data/additional.csv";
    private static final String REMINDER_FILE = "data/reminder.csv";

    /*
     * Creates a backup file that contains all application data.
     */
    public static void backup(String backupPath) {

        // Write all data into a single backup file
        try (PrintWriter pw = new PrintWriter(new FileWriter(backupPath))) {

            // Backup event data
            pw.println("#EVENTS");
            copyFile(EVENT_FILE, pw);

            // Backup recurrence rules
            pw.println();
            pw.println("#RECURRENT");
            copyFile(RECURRENT_FILE, pw);

            // Backup additional event fields
            pw.println();
            pw.println("#ADDITIONAL");
            copyFile(ADDITIONAL_FILE, pw);

            // Backup reminder configurations
            pw.println();
            pw.println("#REMINDERS");
            copyFile(REMINDER_FILE, pw);

            System.out.println("Backup completed to " + backupPath);

        } catch (IOException e) {
            // Handle any error during backup creation
            System.out.println("Backup failed.");
        }
    }

    /*
     * Copies the content of a source CSV file into the backup file.
     */
    private static void copyFile(String source, PrintWriter pw) {

        // Read from source file and write each line into backup
        try (BufferedReader br = new BufferedReader(new FileReader(source))) {
            String line;
            while ((line = br.readLine()) != null) {
                pw.println(line);
            }
        } catch (IOException e) {
            // Source file may not exist yet (e.g., first program run)
            pw.println();
        }
    }

    /*
     * Restores data from a backup file into the application.
     *
     * Restore strategy:
     * - Existing events are preserved
     * - Restored events are assigned new IDs to avoid collisions
     * - All linked data (recurrence, reminders, additional fields)
     *   are updated to use the new IDs
     */
    public static void restore(String backupPath) {

        // Read existing events to avoid overwriting them
        List<Event> currentEvents = EventFileHandler.readEvents();
        int nextId = EventFileHandler.getNextEventId(currentEvents);

        // Temporary storage for backup data
        List<Event> backupEvents = new ArrayList<>();
        List<RecurringEvent> backupRecurring = new ArrayList<>();
        List<String[]> backupAdditional = new ArrayList<>();
        List<Reminder> backupReminders = new ArrayList<>();

        // Enum used to track which section of the backup file is being read
        enum Section { NONE, EVENTS, RECURRENT, ADDITIONAL, REMINDERS }
        Section section = Section.NONE;

        // Read and parse the backup file
        try (BufferedReader br = new BufferedReader(new FileReader(backupPath))) {
            String line;

            while ((line = br.readLine()) != null) {

                // Detect section headers
                if (line.equals("#EVENTS")) {
                    section = Section.EVENTS;
                    continue;
                }
                if (line.equals("#RECURRENT")) {
                    section = Section.RECURRENT;
                    continue;
                }
                if (line.equals("#ADDITIONAL")) {
                    section = Section.ADDITIONAL;
                    continue;
                }
                if (line.equals("#REMINDERS")) {
                    section = Section.REMINDERS;
                    continue;
                }

                // Ignore lines outside of valid sections
                if (section == Section.NONE) continue;

                // Skip empty lines
                if (line.trim().isEmpty()) continue;

                // Skip CSV header rows
                if (line.startsWith("eventId,")) continue;

                String[] parts = line.split(",");

                // Parse event records
                if (section == Section.EVENTS) {
                    if (parts.length < 5) continue;

                    int oldId = Integer.parseInt(parts[0]);
                    String title = parts[1];
                    String desc = parts[2];
                    LocalDateTime start = LocalDateTime.parse(parts[3]);
                    LocalDateTime end = LocalDateTime.parse(parts[4]);
                    backupEvents.add(new Event(oldId, title, desc, start, end));
                }

                // Parse recurrence rules
                else if (section == Section.RECURRENT) {
                    if (parts.length < 4) continue;

                    int oldId = Integer.parseInt(parts[0]);
                    String interval = parts[1];
                    int times = Integer.parseInt(parts[2]);
                    LocalDate endDate = parts[3].equals("0")
                            ? null
                            : LocalDate.parse(parts[3]);
                    backupRecurring.add(
                            new RecurringEvent(oldId, interval, times, endDate)
                    );
                }

                // Parse additional event fields
                else if (section == Section.ADDITIONAL) {
                    if (parts.length < 3) continue;
                    backupAdditional.add(parts);
                }

                // Parse reminders
                else if (section == Section.REMINDERS) {
                    if (parts.length < 2) continue;

                    try {
                        int oldEventId = Integer.parseInt(parts[0].trim());
                        int minutesBefore = Integer.parseInt(parts[1].trim());
                        backupReminders.add(new Reminder(oldEventId, minutesBefore));
                    } catch (NumberFormatException ignored) {
                        // Skip malformed reminder rows
                    }
                }
            }

        } catch (IOException | RuntimeException e) {
            // Abort restore if any critical error occurs
            System.out.println("Restore failed.");
            return;
        }

        /*
         * Build mapping from old event IDs (backup)
         * to newly assigned event IDs (current system).
         */
        Map<Integer, Integer> idMap = new HashMap<>();
        List<Event> mergedEvents = new ArrayList<>(currentEvents);

        for (Event be : backupEvents) {

            // Skip restoring events that conflict with existing events
            Event candidate = new Event(
                    -1,
                    be.getTitle(),
                    be.getDescription(),
                    be.getStartDateTime(),
                    be.getEndDateTime()
            );

            if (hasConflict(candidate, mergedEvents)) {
                continue;
            }

            // Assign a new unique ID
            int newId = nextId++;
            idMap.put(be.getEventId(), newId);

            mergedEvents.add(
                    new Event(newId, be.getTitle(),
                              be.getDescription(),
                              be.getStartDateTime(),
                              be.getEndDateTime())
            );
        }

        /*
         * Restore recurring rules with updated event IDs.
         */
        List<RecurringEvent> mergedRecurring =
                new ArrayList<>(RecurringFileHandler.readRecurringEvents());

        for (RecurringEvent brc : backupRecurring) {
            Integer newEventId = idMap.get(brc.getEventId());
            if (newEventId == null) continue;

            // Ensure no duplicate recurrence rule exists
            mergedRecurring.removeIf(r -> r.getEventId() == newEventId);
            mergedRecurring.add(
                    new RecurringEvent(
                            newEventId,
                            brc.getInterval(),
                            brc.getRecurrentTimes(),
                            brc.getRecurrentEndDate()
                    )
            );
        }

        /*
         * Restore additional event fields with updated event IDs.
         */
        List<app.model.AdditionalEventFields> mergedAdditional =
                new ArrayList<>(app.util.AdditionalFileHandler.readAdditional());

        for (String[] row : backupAdditional) {
            int oldId;
            try {
                oldId = Integer.parseInt(row[0].trim());
            } catch (NumberFormatException nfe) {
                continue;
            }

            Integer newId = idMap.get(oldId);
            if (newId == null) continue;

            String location = row[1].trim();
            String category = row[2].trim();

            mergedAdditional.removeIf(a -> a.getEventId() == newId);
            mergedAdditional.add(
                    new app.model.AdditionalEventFields(newId, location, category)
            );
        }

        /*
         * Restore reminders with updated event IDs.
         */
        List<Reminder> mergedReminders =
                new ArrayList<>(ReminderFileHandler.readReminders());

        for (Reminder br : backupReminders) {
            Integer newId = idMap.get(br.getEventId());
            if (newId == null) continue;

            // Ensure only one reminder exists per event
            mergedReminders.removeIf(r -> r.getEventId() == newId);
            mergedReminders.add(new Reminder(newId, br.getMinutesBefore()));
        }

        // Write merged data back to CSV files
        EventFileHandler.writeEvents(mergedEvents);
        RecurringFileHandler.writeRecurringEvents(mergedRecurring);
        app.util.AdditionalFileHandler.writeAdditional(mergedAdditional);
        ReminderFileHandler.writeReminders(mergedReminders);

        System.out.println("Restore completed from " + backupPath);
    }

    /*
     * Checks whether a new event conflicts with existing events.
     */
    private static boolean hasConflict(Event newEvent, List<Event> events) {

        LocalDateTime newStart = newEvent.getStartDateTime();
        LocalDateTime newEnd = newEvent.getEndDateTime();

        // Normalize time range
        if (newEnd.isBefore(newStart)) {
            LocalDateTime tmp = newStart;
            newStart = newEnd;
            newEnd = tmp;
        }

        // Compare with all existing events
        for (Event e : events) {
            LocalDateTime start = e.getStartDateTime();
            LocalDateTime end = e.getEndDateTime();

            if (end.isBefore(start)) {
                LocalDateTime tmp = start;
                start = end;
                end = tmp;
            }

            // Check for overlapping time intervals
            boolean overlap =
                    newStart.isBefore(end) &&
                    newEnd.isAfter(start);

            if (overlap) {
                return true;
            }
        }
        return false;
    }
}
