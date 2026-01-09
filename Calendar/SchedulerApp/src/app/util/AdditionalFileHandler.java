package app.util;

import app.model.AdditionalEventFields;

import java.io.*;
import java.util.*;

public class AdditionalFileHandler {

    private static final String FILE_PATH = "data/additional.csv";

    public static List<AdditionalEventFields> readAdditional() {
        List<AdditionalEventFields> list = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(FILE_PATH))) {
            String header = br.readLine(); // skip header
            if (header == null) {
                return list;
            }

            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;

                String[] parts = line.split(",", -1);
                if (parts.length < 3) continue;

                int eventId;
                try {
                    eventId = Integer.parseInt(parts[0].trim());
                } catch (NumberFormatException nfe) {
                    continue;
                }

                String location = parts[1].trim();
                String category = parts[2].trim();

                list.add(new AdditionalEventFields(eventId, emptyToNull(location), emptyToNull(category)));
            }

        } catch (IOException e) {
            // File may not exist yet.
        }

        return list;
    }

    public static void writeAdditional(List<AdditionalEventFields> list) {
        ensureDataDirExists();

        try (PrintWriter pw = new PrintWriter(new FileWriter(FILE_PATH))) {
            // Keep header matching the marking spec (includes "Catagory" typo).
            pw.println("eventId,Location,Catagory");
            for (AdditionalEventFields a : list) {
                pw.println(
                        a.getEventId() + "," +
                                nullToEmpty(a.getLocation()) + "," +
                                nullToEmpty(a.getCategory())
                );
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Map<Integer, AdditionalEventFields> readAdditionalMap() {
        Map<Integer, AdditionalEventFields> map = new HashMap<>();
        for (AdditionalEventFields a : readAdditional()) {
            map.put(a.getEventId(), a);
        }
        return map;
    }

    public static void upsert(AdditionalEventFields fields) {
        List<AdditionalEventFields> all = readAdditional();
        all.removeIf(a -> a.getEventId() == fields.getEventId());
        all.add(fields);
        writeAdditional(all);
    }

    public static void deleteByEventId(int eventId) {
        List<AdditionalEventFields> all = readAdditional();
        boolean removed = all.removeIf(a -> a.getEventId() == eventId);
        if (removed) {
            writeAdditional(all);
        }
    }

    private static void ensureDataDirExists() {
        File f = new File("data");
        if (!f.exists()) {
            //noinspection ResultOfMethodCallIgnored
            f.mkdirs();
        }
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static String emptyToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
