package app.view;

import app.model.Event;
import app.model.RecurringEvent;
import app.model.AdditionalEventFields;
import app.util.AdditionalFileHandler;
import app.util.RecurringFileHandler;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public class EventListView {

    public static void display(List<Event> events) {

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        Map<Integer, AdditionalEventFields> additionalMap = AdditionalFileHandler.readAdditionalMap();

        if (events.isEmpty()) {
            System.out.println("No events.");
            return;
        }

        for (Event e : events) {
            System.out.println("[" + e.getEventId() + "] " + e.getTitle());
            System.out.println("    " +
                    e.getStartDateTime().format(fmt) + " → " +
                    e.getEndDateTime().format(fmt));

            AdditionalEventFields a = additionalMap.get(e.getEventId());
            if (a != null) {
                if (a.getLocation() != null && !a.getLocation().trim().isEmpty()) {
                    System.out.println("    Location: " + a.getLocation());
                }
                if (a.getCategory() != null && !a.getCategory().trim().isEmpty()) {
                    System.out.println("    Category: " + a.getCategory());
                }
            }

            RecurringEvent r =
                    RecurringFileHandler.findByEventId(e.getEventId());

            if (r != null) {
                System.out.print("    (Repeats every ");

                if (r.getInterval().equals("1d")) {
                    System.out.print("1 day");
                } else if (r.getInterval().equals("1w")) {
                    System.out.print("1 week");
                }

                if (r.getRecurrentEndDate() != null) {
                    System.out.println(" until " + r.getRecurrentEndDate() + ")");
                } else {
                    System.out.println(" for " + r.getRecurrentTimes() + " times)");
                }
            }

            System.out.println();
        }
    }
}
