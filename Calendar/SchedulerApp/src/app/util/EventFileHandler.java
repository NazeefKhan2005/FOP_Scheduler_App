package app.util;

import app.model.Event;

import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


public class EventFileHandler {

    private static final String FILE_PATH = "data/event.csv";

    public static List<Event> readEvents() {
        List<Event> events = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(FILE_PATH))) {
            String line = br.readLine(); // skip header

            while ((line = br.readLine()) != null) {

            // ✅ SKIP empty lines
            if (line.trim().isEmpty()) {
                continue;
            }

            String[] parts = line.split(",");

            int id = Integer.parseInt(parts[0]);
            String title = parts[1];
            String desc = parts[2];
            LocalDateTime start = LocalDateTime.parse(parts[3]);
            LocalDateTime end = LocalDateTime.parse(parts[4]);

            events.add(new Event(id, title, desc, start, end));
            }

        } catch (IOException e) {
            System.out.println("File not found, starting fresh.");
        }

        return events;
    }

    public static void writeEvents(List<Event> events) {
        try {
            // 1. Target the file
            File file = new File(FILE_PATH);
            
            // 2. FORCE create the "data" folder if it's missing
            if (file.getParentFile() != null) {
                file.getParentFile().mkdirs();
            }

            // 3. Write to the file
            PrintWriter pw = new PrintWriter(new FileWriter(file));
            pw.println("eventId,title,description,startDateTime,endDateTime");

            for (Event e : events) {
                pw.println(
                    e.getEventId() + "," +
                    e.getTitle() + "," +
                    e.getDescription() + "," +
                    e.getStartDateTime() + "," +
                    e.getEndDateTime()
                );
            }
            pw.close(); // Don't forget to close!

        } catch (IOException e) {
            System.out.println("Error saving events: " + e.getMessage());
            e.printStackTrace();
        }
    }


    public static int getNextEventId(List<Event> events) {
        int max = 0;
        for (Event e : events) {
            if (e.getEventId() > max) {
                max = e.getEventId();
            }
        }
        return max + 1;
    }

    public static boolean updateEvent(Event updatedEvent) {
        List<Event> events = readEvents();
        boolean found = false;

        for (int i = 0; i < events.size(); i++) {
            if (events.get(i).getEventId() == updatedEvent.getEventId()) {
                events.set(i, updatedEvent);
                found = true;
                break;
            }
     }

        if (found) {
            writeEvents(events);
        }

        return found;
    }

    public static boolean deleteEvent(int eventId) {
        List<Event> events = readEvents();
        boolean removed = events.removeIf(e -> e.getEventId() == eventId);

        if (removed) {
            writeEvents(events);
        }
        return removed;
    }

    public static boolean hasConflict(Event newEvent) {
        List<Event> events = readEvents();

        // Normalize the new event window (in case of bad legacy data)
        LocalDateTime newStart = newEvent.getStartDateTime();
        LocalDateTime newEnd = newEvent.getEndDateTime();
        if (newEnd.isBefore(newStart)) {
            LocalDateTime tmp = newStart;
            newStart = newEnd;
            newEnd = tmp;
        }

        for (Event e : events) {

            // Skip comparing the same event (important for update)
            if (e.getEventId() == newEvent.getEventId()) {
                continue;
            }

            LocalDateTime existingStart = e.getStartDateTime();
            LocalDateTime existingEnd = e.getEndDateTime();

            // Guard against legacy rows where end < start
            if (existingEnd.isBefore(existingStart)) {
                LocalDateTime tmp = existingStart;
                existingStart = existingEnd;
                existingEnd = tmp;
            }

            boolean overlap = newStart.isBefore(existingEnd) && newEnd.isAfter(existingStart);

            if (overlap) {
                return true;
            }
        }
        return false;
    }

    public static List<Event> searchByDate(LocalDate date) {
        List<Event> results = new ArrayList<>();

        for (Event e : readEvents()) {
            if (e.getStartDateTime().toLocalDate().equals(date)) {
                results.add(e);
            }
        }
        return results;
    }

    public static List<Event> searchByDateRange(LocalDate start, LocalDate end) {
        List<Event> results = new ArrayList<>();

        for (Event e : readEvents()) {
            LocalDate eventDate = e.getStartDateTime().toLocalDate();

            if (!eventDate.isBefore(start) && !eventDate.isAfter(end)) {
                results.add(e);
            }
        }
        return results;
    }
}
