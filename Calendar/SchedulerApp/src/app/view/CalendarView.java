package app.view;

import app.model.Event;
import app.model.RecurringEvent;
import app.model.AdditionalEventFields;
import app.util.AdditionalFileHandler;
import app.util.RecurringFileHandler;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/*
 * CalendarView
 * ------------
 * This view class is responsible for displaying calendar-based
 * representations of events to the user.
 *
 * It supports:
 * - Month view
 * - Week view
 * - Day view
 *
 * This class does not modify data or perform file writing.
 * It only reads data and formats output for display.
 */
public class CalendarView {

    /*
     * Expands recurring events into actual event occurrences
     * within a specified date range.
     *
     * This method does NOT modify the original list of events.
     */
    private static List<Event> withRecurringOccurrences(
            List<Event> events,
            LocalDate rangeStart,
            LocalDate rangeEnd) {

        // Create a new list to avoid mutating the original events list
        List<Event> expanded = new ArrayList<>(events);

        // Read all recurrence rules from storage
        List<RecurringEvent> recurringRules = RecurringFileHandler.readRecurringEvents();
        if (recurringRules.isEmpty()) {
            return expanded;
        }

        // Process each recurrence rule
        for (RecurringEvent rule : recurringRules) {

            // Find the base event associated with this recurrence rule
            Event base = null;
            for (Event e : events) {
                if (e.getEventId() == rule.getEventId()) {
                    base = e;
                    break;
                }
            }

            // Skip orphan recurrence rules
            if (base == null) {
                continue;
            }

            // Parse recurrence interval string into a Period
            java.time.Period period = parseInterval(rule.getInterval());
            if (period == null) {
                // Unsupported or malformed interval
                continue;
            }

            // Retrieve base event start and end times
            LocalDateTime start = base.getStartDateTime();
            LocalDateTime end = base.getEndDateTime();

            // Calculate event duration in minutes
            long durationMinutes = ChronoUnit.MINUTES.between(start, end);

            /*
             * The CSV stores recurrence count as total desired occurrences
             * (including the base event).
             */
            int maxOccurrences = rule.getRecurrentTimes();
            LocalDate endDateLimit = rule.getRecurrentEndDate();

            // Start generating from the next occurrence (base event already exists)
            int generated = 0;
            int occurrenceIndex = 1;

            while (true) {

                // Calculate start time of the next occurrence
                LocalDateTime occStart = start
                        .plusYears((long) period.getYears() * occurrenceIndex)
                        .plusMonths((long) period.getMonths() * occurrenceIndex)
                        .plusDays((long) period.getDays() * occurrenceIndex);

                // Stop if recurrence count limit is reached
                if (maxOccurrences > 0 && generated >= maxOccurrences) {
                    break;
                }

                generated++;
                LocalDate occDate = occStart.toLocalDate();

                // Stop if end date limit is exceeded
                if (endDateLimit != null && occDate.isAfter(endDateLimit)) {
                    break;
                }

                // Stop if beyond display range
                if (occDate.isAfter(rangeEnd)) {
                    break;
                }

                // Add occurrence if it falls within display range
                if (!occDate.isBefore(rangeStart)) {
                    LocalDateTime occEnd = occStart.plusMinutes(durationMinutes);
                    expanded.add(new Event(
                            base.getEventId(),
                            base.getTitle(),
                            base.getDescription(),
                            occStart,
                            occEnd
                    ));
                }

                occurrenceIndex++;
            }
        }

        return expanded;
    }

    // ===================== MONTH VIEW =====================
    /*
     * Displays a month-based calendar view.
     */
    public static void showMonthView(List<Event> events, int year, int month) {

        YearMonth ym = YearMonth.of(year, month);
        LocalDate rangeStart = ym.atDay(1);
        LocalDate rangeEnd = ym.atEndOfMonth();

        // Expand recurring events within the month
        List<Event> allEvents = withRecurringOccurrences(events, rangeStart, rangeEnd);

        // Load recurrence rules into a map for quick lookup
        List<RecurringEvent> recurringRules = RecurringFileHandler.readRecurringEvents();
        Map<Integer, RecurringEvent> recurringMap = new java.util.HashMap<>();
        for (RecurringEvent r : recurringRules) {
            recurringMap.put(r.getEventId(), r);
        }

        LocalDate firstDay = ym.atDay(1);

        // Print calendar header
        System.out.println("\n" + ym.getMonth() + " " + year);
        System.out.println("Su Mo Tu We Th Fr Sa");

        // Determine offset for first day of the month
        int startOffset = firstDay.getDayOfWeek().getValue() % 7;

        for (int i = 0; i < startOffset; i++) {
            System.out.print("   ");
        }

        // Print day numbers with event markers
        for (int day = 1; day <= ym.lengthOfMonth(); day++) {
            LocalDate date = ym.atDay(day);

            boolean hasEvent = allEvents.stream()
                    .anyMatch(e -> e.getStartDateTime().toLocalDate().equals(date));

            System.out.printf("%2d%s ", day, hasEvent ? "*" : " ");

            if (date.getDayOfWeek() == DayOfWeek.SATURDAY) {
                System.out.println();
            }
        }
        System.out.println("\n");

        // Display detailed event list for the month
        Map<Integer, AdditionalEventFields> additionalMap =
                AdditionalFileHandler.readAdditionalMap();

        for (Event e : allEvents) {
            if (e.getStartDateTime().getMonthValue() == month &&
                e.getStartDateTime().getYear() == year) {

                RecurringEvent r = recurringMap.get(e.getEventId());
                String recurLabel = r == null ? "" : " [recurring " + r.getInterval() + "]";

                System.out.println("* " +
                        e.getStartDateTime().toLocalDate() + ": " +
                        e.getTitle() +
                        " (" + e.getStartDateTime().toLocalTime() + ")" +
                        recurLabel);

                // Print optional additional fields
                AdditionalEventFields a = additionalMap.get(e.getEventId());
                if (a != null) {
                    if (a.getLocation() != null && !a.getLocation().trim().isEmpty()) {
                        System.out.println("    Location: " + a.getLocation());
                    }
                    if (a.getCategory() != null && !a.getCategory().trim().isEmpty()) {
                        System.out.println("    Category: " + a.getCategory());
                    }
                }

                System.out.println();
            }
        }
    }

    // ===================== WEEK VIEW =====================
    /*
     * Displays a week-based calendar view.
     */
    public static void showWeekView(List<Event> events, LocalDate weekStart) {

        LocalDate rangeStart = weekStart;
        LocalDate rangeEnd = weekStart.plusDays(6);

        // Expand recurring events within the week
        List<Event> allEvents = withRecurringOccurrences(events, rangeStart, rangeEnd);

        Map<Integer, AdditionalEventFields> additionalMap =
                AdditionalFileHandler.readAdditionalMap();

        Map<Integer, RecurringEvent> recurringMap = new java.util.HashMap<>();
        for (RecurringEvent r : RecurringFileHandler.readRecurringEvents()) {
            recurringMap.put(r.getEventId(), r);
        }

        System.out.println("\n=== Week of " + weekStart + " ===");

        for (int i = 0; i < 7; i++) {
            LocalDate date = weekStart.plusDays(i);

            System.out.println(date.getDayOfWeek().toString().substring(0, 3)
                    + " " + date.getDayOfMonth() + ":");

            boolean found = false;

            for (Event e : allEvents) {
                if (e.getStartDateTime().toLocalDate().equals(date)) {

                    RecurringEvent r = recurringMap.get(e.getEventId());
                    String recurLabel = r == null ? "" : " [recurring " + r.getInterval() + "]";

                    System.out.println("  - " + e.getTitle() +
                            " (" + e.getStartDateTime().toLocalTime() + ")" +
                            recurLabel);

                    AdditionalEventFields a = additionalMap.get(e.getEventId());
                    if (a != null) {
                        if (a.getLocation() != null && !a.getLocation().trim().isEmpty()) {
                            System.out.println("      Location: " + a.getLocation());
                        }
                        if (a.getCategory() != null && !a.getCategory().trim().isEmpty()) {
                            System.out.println("      Category: " + a.getCategory());
                        }
                    }

                    found = true;
                }
            }

            if (!found) {
                System.out.println("  No events");
            }

            System.out.println();
        }
    }

    // ===================== DAY VIEW =====================
    /*
     * Displays all events occurring on a specific day.
     */
    public static void showDayView(List<Event> events, LocalDate date) {

        LocalDate rangeStart = date;
        LocalDate rangeEnd = date;

        // Expand recurring events for the selected day
        List<Event> allEvents = withRecurringOccurrences(events, rangeStart, rangeEnd);

        Map<Integer, AdditionalEventFields> additionalMap =
                AdditionalFileHandler.readAdditionalMap();

        Map<Integer, RecurringEvent> recurringMap = new java.util.HashMap<>();
        for (RecurringEvent r : RecurringFileHandler.readRecurringEvents()) {
            recurringMap.put(r.getEventId(), r);
        }

        System.out.println("\n=== " + date + " (" + date.getDayOfWeek() + ") ===");

        boolean found = false;

        for (Event e : allEvents) {
            if (e.getStartDateTime().toLocalDate().equals(date)) {

                RecurringEvent r = recurringMap.get(e.getEventId());
                String recurLabel = r == null ? "" : " [recurring " + r.getInterval() + "]";

                System.out.println("- " + e.getTitle() +
                        " (" + e.getStartDateTime().toLocalTime() +
                        " → " + e.getEndDateTime().toLocalTime() + ")" +
                        recurLabel);

                AdditionalEventFields a = additionalMap.get(e.getEventId());
                if (a != null) {
                    if (a.getLocation() != null && !a.getLocation().trim().isEmpty()) {
                        System.out.println("    Location: " + a.getLocation());
                    }
                    if (a.getCategory() != null && !a.getCategory().trim().isEmpty()) {
                        System.out.println("    Category: " + a.getCategory());
                    }
                }

                System.out.println();
                found = true;
            }
        }

        if (!found) {
            System.out.println("No events");
        }
    }

    /*
     * Parses a recurrence interval string into a Period object.
     */
    private static java.time.Period parseInterval(String interval) {

        if (interval == null) return null;

        String trimmed = interval.trim().toLowerCase();
        if (trimmed.isEmpty()) return null;

        // Legacy support: "1" means weekly recurrence
        if (trimmed.equals("1")) {
            return java.time.Period.ofDays(7);
        }

        int len = trimmed.length();
        char last = trimmed.charAt(len - 1);

        try {
            // Pure numeric value treated as days
            if (Character.isDigit(last)) {
                int days = Integer.parseInt(trimmed);
                return days > 0 ? java.time.Period.ofDays(days) : null;
            }

            // Numeric value followed by unit character
            int value = Integer.parseInt(trimmed.substring(0, len - 1));
            if (value <= 0) return null;

            return switch (last) {
                case 'd' -> java.time.Period.ofDays(value);
                case 'w' -> java.time.Period.ofDays(value * 7);
                case 'm' -> java.time.Period.ofMonths(value);
                case 'y' -> java.time.Period.ofYears(value);
                default -> null;
            };

        } catch (NumberFormatException e) {
            return null;
        }
    }
}
