package app.util;

import app.model.RecurringEvent;

import java.io.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class RecurringFileHandler {

    private static final String FILE_PATH = "data/recurrent.csv";

    public static List<RecurringEvent> readRecurringEvents() {
        List<RecurringEvent> list = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(FILE_PATH))) {
            br.readLine(); // skip header
            String line;

            while ((line = br.readLine()) != null) {
                // Skip empty lines (common after manual edits or restore operations)
                if (line.trim().isEmpty()) {
                    continue;
                }

                String[] parts = line.split(",");

                // Skip malformed rows rather than crashing the whole app.
                if (parts.length < 4) {
                    continue;
                }

                int eventId = Integer.parseInt(parts[0]);
                String interval = parts[1];
                int times = Integer.parseInt(parts[2]);

                LocalDate endDate = parts[3].equals("0")
                        ? null
                        : LocalDate.parse(parts[3]);

                list.add(new RecurringEvent(eventId, interval, times, endDate));
            }

        } catch (IOException e) {
            System.out.println("No recurrent events file yet.");
        }

        return list;
    }

    public static void writeRecurringEvents(List<RecurringEvent> list) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(FILE_PATH))) {
            pw.println("eventId,recurrentInterval,recurrentTimes,recurrentEndDate");

            for (RecurringEvent r : list) {
                pw.println(
                    r.getEventId() + "," +
                    r.getInterval() + "," +
                    r.getRecurrentTimes() + "," +
                    (r.getRecurrentEndDate() == null ? "0" : r.getRecurrentEndDate())
                );
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static RecurringEvent findByEventId(int eventId) {
        List<RecurringEvent> list = readRecurringEvents();

        for (RecurringEvent r : list) {
            if (r.getEventId() == eventId) {
                return r;
            }
        }
        return null;
    }
}
