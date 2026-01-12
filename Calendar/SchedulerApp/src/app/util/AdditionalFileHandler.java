package app.util;

import app.model.AdditionalEventFields;

import java.io.*;
import java.util.*;

/*
 * AdditionalFileHandler
 * ---------------------
 * This utility class handles file operations related to additional
 * (optional) event information such as location and category.
 *
 * It reads from and writes to a CSV file that stores metadata linked
 * to events using eventId.
 *
 * This class focuses only on persistence logic and does not contain
 * any business logic or user interaction code.
 */
public class AdditionalFileHandler {

    // File path where additional event information is stored
    private static final String FILE_PATH = "data/additional.csv";

    /*
     * Reads all additional event fields from the CSV file.
     */
    public static List<AdditionalEventFields> readAdditional() {

        // List to store all additional event fields
        List<AdditionalEventFields> list = new ArrayList<>();

        // Try-with-resources ensures the file is closed automatically
        try (BufferedReader br = new BufferedReader(new FileReader(FILE_PATH))) {

            // Read and ignore the header line
            String header = br.readLine(); // skip header
            if (header == null) {
                return list;
            }

            String line;
            while ((line = br.readLine()) != null) {

                // Skip empty lines to prevent parsing errors
                if (line.trim().isEmpty()) continue;

                // Split CSV line, keeping empty trailing fields
                String[] parts = line.split(",", -1);
                if (parts.length < 3) continue;

                int eventId;

                // Parse event ID safely
                try {
                    eventId = Integer.parseInt(parts[0].trim());
                } catch (NumberFormatException nfe) {
                    // Skip malformed rows
                    continue;
                }

                // Read location and category values
                String location = parts[1].trim();
                String category = parts[2].trim();

                // Convert empty strings to null before storing
                list.add(new AdditionalEventFields(
                        eventId,
                        emptyToNull(location),
                        emptyToNull(category)
                ));
            }

        } catch (IOException e) {
            // File may not exist yet (first program run)
        }

        return list;
    }

    /*
     * Writes all additional event fields to the CSV file.
     */
    public static void writeAdditional(List<AdditionalEventFields> list) {

        // Ensure the data directory exists before writing
        ensureDataDirExists();

        try (PrintWriter pw = new PrintWriter(new FileWriter(FILE_PATH))) {

            // Keep header matching the marking specification
            // (includes "Catagory" typo intentionally)
            pw.println("eventId,Location,Catagory");

            // Write each additional field as a CSV row
            for (AdditionalEventFields a : list) {
                pw.println(
                        a.getEventId() + "," +
                                nullToEmpty(a.getLocation()) + "," +
                                nullToEmpty(a.getCategory())
                );
            }

        } catch (IOException e) {
            // Print stack trace for debugging purposes
            e.printStackTrace();
        }
    }

    /*
     * Reads additional event fields and returns them as a map
     * keyed by eventId for fast lookup.
     */
    public static Map<Integer, AdditionalEventFields> readAdditionalMap() {

        Map<Integer, AdditionalEventFields> map = new HashMap<>();

        // Populate the map from the list of additional fields
        for (AdditionalEventFields a : readAdditional()) {
            map.put(a.getEventId(), a);
        }

        return map;
    }

    /**
     * Inserts or updates additional event fields for a specific event.
     */
    public static void upsert(AdditionalEventFields fields) {

        List<AdditionalEventFields> all = readAdditional();

        // Remove any existing record for the same event ID
        all.removeIf(a -> a.getEventId() == fields.getEventId());

        // Add the new or updated record
        all.add(fields);

        // Save changes to the file
        writeAdditional(all);
    }

    /**
     * Deletes additional event fields associated with a specific event ID.
     */
    public static void deleteByEventId(int eventId) {

        List<AdditionalEventFields> all = readAdditional();

        // Remove matching additional fields
        boolean removed = all.removeIf(a -> a.getEventId() == eventId);

        // Save changes only if a record was removed
        if (removed) {
            writeAdditional(all);
        }
    }

    /*
     * Ensures that the "data" directory exists before file operations.
     */
    private static void ensureDataDirExists() {

        File f = new File("data");

        if (!f.exists()) {
            //noinspection ResultOfMethodCallIgnored
            f.mkdirs();
        }
    }

    /*
     * Converts a null string to an empty string.
     * Used when writing CSV files.
     */
    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    /*
     * Converts an empty string to null.
     * Used when reading CSV files.
     */
    private static String emptyToNull(String s) {

        if (s == null) return null;

        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
