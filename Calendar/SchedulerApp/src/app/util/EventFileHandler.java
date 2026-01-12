package app.util;

import app.model.Event;

import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/*
 * EventFileHandler
 * ----------------
 * This utility class is responsible for handling all file operations
 * related to Event objects.
 *
 * It provides methods to:
 * - read events from a CSV file
 * - write events to a CSV file
 * - update and delete events
 * - generate unique event IDs
 * - detect time conflicts between events
 * - search events by date or date range
 *
 * This class separates persistence logic from core logic,
 * following good software design principles.
 */
public class EventFileHandler {

    // File path where event data is stored
    private static final String FILE_PATH = "data/event.csv";

    /*
     * Reads all events from the CSV file and returns them as a list.
     */
    public static List<Event> readEvents() {

        // List to store all events read from the file
        List<Event> events = new ArrayList<>();

        // Try-with-resources ensures the file is closed automatically
        try (BufferedReader br = new BufferedReader(new FileReader(FILE_PATH))) {

            // Read and ignore the header line
            String line = br.readLine(); // skip header

            // Read the file line by line
            while ((line = br.readLine()) != null) {

                // Skip empty lines to avoid parsing errors
                if (line.trim().isEmpty()) {
                    continue;
                }

                // Split the CSV line into individual fields
                String[] parts = line.split(",");

                // Parse event data from CSV fields
                int id = Integer.parseInt(parts[0]);
                String title = parts[1];
                String desc = parts[2];
                LocalDateTime start = LocalDateTime.parse(parts[3]);
                LocalDateTime end = LocalDateTime.parse(parts[4]);

                // Create a new Event object and add it to the list
                events.add(new Event(id, title, desc, start, end));
            }

        } catch (IOException e) {
            // If the file does not exist, start with an empty event list
            System.out.println("File not found, starting fresh.");
        }

        return events;
    }

    /*
     * Writes all events to the CSV file.
     */
    public static void writeEvents(List<Event> events) {

        try (PrintWriter pw = new PrintWriter(new FileWriter(FILE_PATH))) {

            // Write CSV header
            pw.println("eventId,title,description,startDateTime,endDateTime");

            // Write each event as a CSV row
            for (Event e : events) {
                pw.println(
                    e.getEventId() + "," +
                    e.getTitle() + "," +
                    e.getDescription() + "," +
                    e.getStartDateTime() + "," +
                    e.getEndDateTime()
                );
            }

        } catch (IOException e) {
            // Print stack trace for debugging purposes
            e.printStackTrace();
        }
    }

    /*
     * Generates the next available event ID.
     */
    public static int getNextEventId(List<Event> events) {

        int max = 0;

        // Find the maximum existing event ID
        for (Event e : events) {
            if (e.getEventId() > max) {
                max = e.getEventId();
            }
        }

        // Return the next ID
        return max + 1;
    }

    /*
     * Updates an existing event in the CSV file.
     */
    public static boolean updateEvent(Event updatedEvent) {

        List<Event> events = readEvents();
        boolean found = false;

        // Search for the event with the matching ID
        for (int i = 0; i < events.size(); i++) {
            if (events.get(i).getEventId() == updatedEvent.getEventId()) {
                // Replace the old event with the updated event
                events.set(i, updatedEvent);
                found = true;
                break;
            }
        }

        // Save changes only if the event was found
        if (found) {
            writeEvents(events);
        }

        return found;
    }

    /*
     * Deletes an event from the CSV file using its event ID.
     */
    public static boolean deleteEvent(int eventId) {

        List<Event> events = readEvents();

        // Remove the event with the matching ID
        boolean removed = events.removeIf(e -> e.getEventId() == eventId);

        // Save changes if an event was removed
        if (removed) {
            writeEvents(events);
        }

        return removed;
    }

    /*
     * Checks whether a new or updated event conflicts with existing events.
     */
    public static boolean hasConflict(Event newEvent) {

        List<Event> events = readEvents();

        // Normalize the new event time window
        LocalDateTime newStart = newEvent.getStartDateTime();
        LocalDateTime newEnd = newEvent.getEndDateTime();

        // Swap times if end is before start (defensive check)
        if (newEnd.isBefore(newStart)) {
            LocalDateTime tmp = newStart;
            newStart = newEnd;
            newEnd = tmp;
        }

        // Compare the new event against all existing events
        for (Event e : events) {

            // Skip comparing the same event (important during updates)
            if (e.getEventId() == newEvent.getEventId()) {
                continue;
            }

            LocalDateTime existingStart = e.getStartDateTime();
            LocalDateTime existingEnd = e.getEndDateTime();

            // Normalize existing event times if necessary
            if (existingEnd.isBefore(existingStart)) {
                LocalDateTime tmp = existingStart;
                existingStart = existingEnd;
                existingEnd = tmp;
            }

            // Check for overlapping time intervals
            boolean overlap =
                    newStart.isBefore(existingEnd) &&
                    newEnd.isAfter(existingStart);

            if (overlap) {
                return true;
            }
        }

        return false;
    }

    /*
     * Searches for events occurring on a specific date.
     */
    public static List<Event> searchByDate(LocalDate date) {

        List<Event> results = new ArrayList<>();

        // Check each event's start date
        for (Event e : readEvents()) {
            if (e.getStartDateTime().toLocalDate().equals(date)) {
                results.add(e);
            }
        }

        return results;
    }

    /*
     * Searches for events within a date range (inclusive).
     */
    public static List<Event> searchByDateRange(LocalDate start, LocalDate end) {

        List<Event> results = new ArrayList<>();

        for (Event e : readEvents()) {
            LocalDate eventDate = e.getStartDateTime().toLocalDate();

            // Check if the event date is within the given range
            if (!eventDate.isBefore(start) && !eventDate.isAfter(end)) {
                results.add(e);
            }
        }

        return results;
    }
}
