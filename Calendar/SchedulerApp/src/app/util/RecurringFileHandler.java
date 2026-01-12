package app.util;

import app.model.RecurringEvent;

import java.io.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/*
 * RecurringFileHandler
 * --------------------
 * This utility class is responsible for handling all file operations
 * related to recurring event rules.
 *
 * It reads and writes recurrence configurations to a CSV file and
 * provides helper methods to retrieve recurrence rules for specific events.
 *
 * This class focuses only on persistence logic and does not contain
 * any business logic or user interaction code.
 */
public class RecurringFileHandler {

    // File path where recurring event rules are stored
    private static final String FILE_PATH = "data/recurrent.csv";

    /*
     * Reads all recurring event rules from the CSV file.
     */
    public static List<RecurringEvent> readRecurringEvents() {

        // List to store all recurrence rules
        List<RecurringEvent> list = new ArrayList<>();

        // Try-with-resources ensures the file is closed automatically
        try (BufferedReader br = new BufferedReader(new FileReader(FILE_PATH))) {

            // Read and ignore the header line
            br.readLine(); // skip header
            String line;

            // Read each line from the CSV file
            while ((line = br.readLine()) != null) {

                // Skip empty lines (common after manual edits or restore operations)
                if (line.trim().isEmpty()) {
                    continue;
                }

                // Split the CSV row into individual fields
                String[] parts = line.split(",");

                // Skip malformed rows rather than crashing the whole app
                if (parts.length < 4) {
                    continue;
                }

                // Parse recurrence rule data
                int eventId = Integer.parseInt(parts[0]);
                String interval = parts[1];
                int times = Integer.parseInt(parts[2]);

                // Parse recurrence end date
                // A value of "0" indicates no end date (unlimited recurrence)
                LocalDate endDate = parts[3].equals("0")
                        ? null
                        : LocalDate.parse(parts[3]);

                // Create a RecurringEvent object and add it to the list
                list.add(new RecurringEvent(eventId, interval, times, endDate));
            }

        } catch (IOException e) {
            // If the file does not exist yet, return an empty list
            System.out.println("No recurrent events file yet.");
        }

        return list;
    }

    /*
     * Writes all recurring event rules to the CSV file.
     */
    public static void writeRecurringEvents(List<RecurringEvent> list) {

        try (PrintWriter pw = new PrintWriter(new FileWriter(FILE_PATH))) {

            // Write CSV header
            pw.println("eventId,recurrentInterval,recurrentTimes,recurrentEndDate");

            // Write each recurrence rule as a CSV row
            for (RecurringEvent r : list) {
                pw.println(
                    r.getEventId() + "," +
                    r.getInterval() + "," +
                    r.getRecurrentTimes() + "," +
                    (r.getRecurrentEndDate() == null ? "0" : r.getRecurrentEndDate())
                );
            }

        } catch (IOException e) {
            // Print stack trace for debugging purposes
            e.printStackTrace();
        }
    }

    /*
     * Finds and returns the recurrence rule associated with a specific event ID.
     */
    public static RecurringEvent findByEventId(int eventId) {

        // Read all recurrence rules
        List<RecurringEvent> list = readRecurringEvents();

        // Search for the recurrence rule with the matching event ID
        for (RecurringEvent r : list) {
            if (r.getEventId() == eventId) {
                return r;
            }
        }

        // Return null if no matching recurrence rule is found
        return null;
    }
}
