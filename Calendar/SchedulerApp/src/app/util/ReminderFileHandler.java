package app.util;

import app.model.Reminder;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/*
 * ReminderFileHandler
 * -------------------
 * This utility class is responsible for handling file input and output
 * operations related to Reminder objects.
 *
 * It reads reminder configurations from a CSV file and writes reminder
 * data back to the file for persistence.
 *
 * This class strictly handles file operations and does not perform
 * any reminder calculation or business logic.
 */
public class ReminderFileHandler {

    // File path where reminder data is stored
    private static final String REMINDER_FILE = "data/reminder.csv";

    /*
     * Reads all reminders from the CSV file.
     */
    public static List<Reminder> readReminders() {

        // List to store all reminders read from the file
        List<Reminder> reminders = new ArrayList<>();

        // Create a File object for the reminder CSV
        File file = new File(REMINDER_FILE);

        // If the file does not exist, return an empty list
        if (!file.exists()) {
            return reminders;
        }

        // Try-with-resources ensures the file is closed automatically
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {

            String line;
            boolean firstLine = true;

            // Read the file line by line
            while ((line = br.readLine()) != null) {

                // Remove leading and trailing whitespace
                line = line.trim();

                // Skip empty lines
                if (line.isEmpty()) {
                    continue;
                }

                // Skip header row if present
                if (firstLine && line.toLowerCase().startsWith("eventid,")) {
                    firstLine = false;
                    continue;
                }
                firstLine = false;

                // Split the CSV line into fields
                String[] parts = line.split(",");

                // Skip malformed rows
                if (parts.length < 2) {
                    continue;
                }

                try {
                    // Parse event ID and reminder offset
                    int eventId = Integer.parseInt(parts[0].trim());
                    int minutesBefore = Integer.parseInt(parts[1].trim());

                    // Create a Reminder object and add it to the list
                    reminders.add(new Reminder(eventId, minutesBefore));

                } catch (NumberFormatException ignored) {
                    // Skip malformed numeric values without stopping the program
                }
            }

        } catch (IOException e) {
            // Display an error message if file reading fails
            System.out.println("Error reading reminders: " + e.getMessage());
        }

        return reminders;
    }

    /*
     * Writes all reminders to the CSV file.
     */
    public static void writeReminders(List<Reminder> reminders) {

        // Create a File object for the reminder CSV
        File file = new File(REMINDER_FILE);

        // Ensure the parent directory exists before writing
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        // Try-with-resources ensures the file is closed automatically
        try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {

            // Write CSV header
            pw.println("eventId,minutesBefore");

            // Write each reminder as a CSV row
            for (Reminder reminder : reminders) {
                pw.println(
                        reminder.getEventId() + "," +
                        reminder.getMinutesBefore()
                );
            }

        } catch (IOException e) {
            // Display an error message if file writing fails
            System.out.println("Error writing reminders: " + e.getMessage());
        }
    }
}
