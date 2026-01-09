package app.util;

import app.model.Reminder;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class ReminderFileHandler {

    private static final String REMINDER_FILE = "data/reminder.csv";

    public static List<Reminder> readReminders() {
        List<Reminder> reminders = new ArrayList<>();
        File file = new File(REMINDER_FILE);
        if (!file.exists()) {
            return reminders;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            boolean firstLine = true;

            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }

                // Skip header if present
                if (firstLine && line.toLowerCase().startsWith("eventid,")) {
                    firstLine = false;
                    continue;
                }
                firstLine = false;

                String[] parts = line.split(",");
                if (parts.length < 2) {
                    continue;
                }

                try {
                    int eventId = Integer.parseInt(parts[0].trim());
                    int minutesBefore = Integer.parseInt(parts[1].trim());
                    reminders.add(new Reminder(eventId, minutesBefore));
                } catch (NumberFormatException ignored) {
                    // Skip malformed lines
                }
            }
        } catch (IOException e) {
            System.out.println("Error reading reminders: " + e.getMessage());
        }

        return reminders;
    }

    public static void writeReminders(List<Reminder> reminders) {
        File file = new File(REMINDER_FILE);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
            pw.println("eventId,minutesBefore");
            for (Reminder reminder : reminders) {
                pw.println(reminder.getEventId() + "," + reminder.getMinutesBefore());
            }
        } catch (IOException e) {
            System.out.println("Error writing reminders: " + e.getMessage());
        }
    }
}
