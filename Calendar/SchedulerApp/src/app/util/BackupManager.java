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

public class BackupManager {

    private static final String EVENT_FILE = "data/event.csv";
    private static final String RECURRENT_FILE = "data/recurrent.csv";
    private static final String ADDITIONAL_FILE = "data/additional.csv";
    private static final String REMINDER_FILE = "data/reminder.csv";

    public static void backup(String backupPath) {

        try (PrintWriter pw = new PrintWriter(new FileWriter(backupPath))) {

            pw.println("#EVENTS");
            copyFile(EVENT_FILE, pw);

            pw.println();
            pw.println("#RECURRENT");
            copyFile(RECURRENT_FILE, pw);

            pw.println();
            pw.println("#ADDITIONAL");
            copyFile(ADDITIONAL_FILE, pw);

            pw.println();
            pw.println("#REMINDERS");
            copyFile(REMINDER_FILE, pw);

            System.out.println("Backup completed to " + backupPath);

        } catch (IOException e) {
            System.out.println("Backup failed.");
        }
    }

    private static void copyFile(String source, PrintWriter pw) {
        try (BufferedReader br = new BufferedReader(new FileReader(source))) {
            String line;
            while ((line = br.readLine()) != null) {
                pw.println(line);
            }
        } catch (IOException e) {
            pw.println(); // file may not exist yet
        }
    }

    public static void restore(String backupPath) {

        // Desired behavior:
        // - Don't overwrite any events that currently exist (e.g., newly added after backup)
        // - Bring back events from the backup, but assign them NEW IDs so they don't collide
        // - Update recurrent.csv entries to point to the new IDs

        List<Event> currentEvents = EventFileHandler.readEvents();
        int nextId = EventFileHandler.getNextEventId(currentEvents);

        List<Event> backupEvents = new ArrayList<>();
        List<RecurringEvent> backupRecurring = new ArrayList<>();
        List<String[]> backupAdditional = new ArrayList<>();
        List<Reminder> backupReminders = new ArrayList<>();

        enum Section { NONE, EVENTS, RECURRENT, ADDITIONAL, REMINDERS }
        Section section = Section.NONE;

        try (BufferedReader br = new BufferedReader(new FileReader(backupPath))) {
            String line;

            while ((line = br.readLine()) != null) {
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

                if (section == Section.NONE) {
                    continue;
                }

                if (line.trim().isEmpty()) {
                    continue;
                }

                // Skip CSV header rows
                if (line.startsWith("eventId,")) {
                    continue;
                }

                String[] parts = line.split(",");

                if (section == Section.EVENTS) {
                    if (parts.length < 5) {
                        continue;
                    }

                    int oldId = Integer.parseInt(parts[0]);
                    String title = parts[1];
                    String desc = parts[2];
                    LocalDateTime start = LocalDateTime.parse(parts[3]);
                    LocalDateTime end = LocalDateTime.parse(parts[4]);
                    backupEvents.add(new Event(oldId, title, desc, start, end));
                } else if (section == Section.RECURRENT) {
                    if (parts.length < 4) {
                        continue;
                    }

                    int oldId = Integer.parseInt(parts[0]);
                    String interval = parts[1];
                    int times = Integer.parseInt(parts[2]);
                    LocalDate endDate = parts[3].equals("0") ? null : LocalDate.parse(parts[3]);
                    backupRecurring.add(new RecurringEvent(oldId, interval, times, endDate));
                } else if (section == Section.ADDITIONAL) {
                    if (parts.length < 3) {
                        continue;
                    }
                    // parts: oldEventId, location, category
                    backupAdditional.add(parts);
                } else if (section == Section.REMINDERS) {
                    if (parts.length < 2) {
                        continue;
                    }

                    try {
                        int oldEventId = Integer.parseInt(parts[0].trim());
                        int minutesBefore = Integer.parseInt(parts[1].trim());
                        backupReminders.add(new Reminder(oldEventId, minutesBefore));
                    } catch (NumberFormatException ignored) {
                        // Skip malformed rows
                    }
                }
            }

        } catch (IOException | RuntimeException e) {
            System.out.println("Restore failed.");
            return;
        }

        // Build mapping from backup event IDs to newly assigned IDs.
        // Only create mapping entries for events we actually restore.
        Map<Integer, Integer> idMap = new HashMap<>();
        List<Event> mergedEvents = new ArrayList<>(currentEvents);

        for (Event be : backupEvents) {
            // Skip restore if this event would overlap anything that already exists.
            Event candidate = new Event(-1, be.getTitle(), be.getDescription(), be.getStartDateTime(), be.getEndDateTime());
            if (hasConflict(candidate, mergedEvents)) {
                continue;
            }

            int newId = nextId++;
            idMap.put(be.getEventId(), newId);
            mergedEvents.add(new Event(newId, be.getTitle(), be.getDescription(), be.getStartDateTime(), be.getEndDateTime()));
        }

        List<RecurringEvent> mergedRecurring = new ArrayList<>(RecurringFileHandler.readRecurringEvents());
        for (RecurringEvent brc : backupRecurring) {
            Integer newEventId = idMap.get(brc.getEventId());
            if (newEventId == null) {
                continue;
            }

            // Ensure we don't accidentally add duplicate recurring config for the same new ID.
            mergedRecurring.removeIf(r -> r.getEventId() == newEventId);
            mergedRecurring.add(new RecurringEvent(newEventId, brc.getInterval(), brc.getRecurrentTimes(), brc.getRecurrentEndDate()));
        }

        // Merge additional fields with adjusted IDs.
        // We keep this file separate for marking purposes, so it must restore too.
        List<app.model.AdditionalEventFields> mergedAdditional = new ArrayList<>(app.util.AdditionalFileHandler.readAdditional());
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
            mergedAdditional.add(new app.model.AdditionalEventFields(newId, location, category));
        }

        // Merge reminders with adjusted IDs.
        // Reminders reference eventId, so they must be remapped the same way recurrent/additional are.
        List<Reminder> mergedReminders = new ArrayList<>(ReminderFileHandler.readReminders());
        for (Reminder br : backupReminders) {
            Integer newId = idMap.get(br.getEventId());
            if (newId == null) {
                continue;
            }

            // Ensure single reminder per event (same logic as Main.setReminder())
            mergedReminders.removeIf(r -> r.getEventId() == newId);
            mergedReminders.add(new Reminder(newId, br.getMinutesBefore()));
        }

        EventFileHandler.writeEvents(mergedEvents);
        RecurringFileHandler.writeRecurringEvents(mergedRecurring);
        app.util.AdditionalFileHandler.writeAdditional(mergedAdditional);
        ReminderFileHandler.writeReminders(mergedReminders);

        System.out.println("Restore completed from " + backupPath);
    }

    private static boolean hasConflict(Event newEvent, List<Event> events) {
        LocalDateTime newStart = newEvent.getStartDateTime();
        LocalDateTime newEnd = newEvent.getEndDateTime();
        if (newEnd.isBefore(newStart)) {
            LocalDateTime tmp = newStart;
            newStart = newEnd;
            newEnd = tmp;
        }

        for (Event e : events) {
            LocalDateTime start = e.getStartDateTime();
            LocalDateTime end = e.getEndDateTime();
            if (end.isBefore(start)) {
                LocalDateTime tmp = start;
                start = end;
                end = tmp;
            }

            boolean overlap = newStart.isBefore(end) && newEnd.isAfter(start);
            if (overlap) {
                return true;
            }
        }
        return false;
    }

}
