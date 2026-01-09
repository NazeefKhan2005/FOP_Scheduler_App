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

public class CalendarView {

    private static List<Event> withRecurringOccurrences(List<Event> events, LocalDate rangeStart, LocalDate rangeEnd) {
        // Generate additional Event objects for recurring rules within [rangeStart, rangeEnd]
        // without mutating the original list.
        List<Event> expanded = new ArrayList<>(events);

        List<RecurringEvent> recurringRules = RecurringFileHandler.readRecurringEvents();
        if (recurringRules.isEmpty()) {
            return expanded;
        }

        for (RecurringEvent rule : recurringRules) {
            Event base = null;
            for (Event e : events) {
                if (e.getEventId() == rule.getEventId()) {
                    base = e;
                    break;
                }
            }
            if (base == null) {
                continue;
            }

            java.time.Period period = parseInterval(rule.getInterval());
            if (period == null) {
                continue; // unsupported or malformed interval
            }

            LocalDateTime start = base.getStartDateTime();
            LocalDateTime end = base.getEndDateTime();
            long durationMinutes = ChronoUnit.MINUTES.between(start, end);

            int maxOccurrences = rule.getRecurrentTimes();
            LocalDate endDateLimit = rule.getRecurrentEndDate();

            // Start from the NEXT occurrence (don't duplicate the base event itself)
            int generated = 0;
            int occurrenceIndex = 1;

            while (true) {
                if (maxOccurrences > 0 && generated >= maxOccurrences) {
                    break;
                }

                LocalDateTime occStart = start
                        .plusYears((long) period.getYears() * occurrenceIndex)
                        .plusMonths((long) period.getMonths() * occurrenceIndex)
                        .plusDays((long) period.getDays() * occurrenceIndex);
                LocalDate occDate = occStart.toLocalDate();

                if (endDateLimit != null && occDate.isAfter(endDateLimit)) {
                    break;
                }

                if (occDate.isAfter(rangeEnd)) {
                    break;
                }

                if (!occDate.isBefore(rangeStart)) {
                    LocalDateTime occEnd = occStart.plusMinutes(durationMinutes);
                    // Use the same eventId so it shows as the same event series.
                    expanded.add(new Event(base.getEventId(), base.getTitle(), base.getDescription(), occStart, occEnd));
                    generated++;
                }

                occurrenceIndex++;
            }
        }

        return expanded;
    }

    // ========== MONTH VIEW ==========
    public static void showMonthView(List<Event> events, int year, int month) {

        YearMonth ym = YearMonth.of(year, month);
        LocalDate rangeStart = ym.atDay(1);
        LocalDate rangeEnd = ym.atEndOfMonth();
        List<Event> allEvents = withRecurringOccurrences(events, rangeStart, rangeEnd);
        List<RecurringEvent> recurringRules = RecurringFileHandler.readRecurringEvents();
        Map<Integer, RecurringEvent> recurringMap = new java.util.HashMap<>();
        for (RecurringEvent r : recurringRules) {
            recurringMap.put(r.getEventId(), r);
        }
        LocalDate firstDay = ym.atDay(1);

        System.out.println("\n" + ym.getMonth() + " " + year);
        System.out.println("Su Mo Tu We Th Fr Sa");

        int startOffset = firstDay.getDayOfWeek().getValue() % 7;

        for (int i = 0; i < startOffset; i++) {
            System.out.print("   ");
        }

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

        // Show event legend (one line per occurrence)
        Map<Integer, AdditionalEventFields> additionalMap = AdditionalFileHandler.readAdditionalMap();
        for (Event e : allEvents) {
            if (e.getStartDateTime().getMonthValue() == month &&
                e.getStartDateTime().getYear() == year) {

        RecurringEvent r = recurringMap.get(e.getEventId());
        String recurLabel = r == null ? "" : " [recurring " + r.getInterval() + "]";

        System.out.println("* " +
            e.getStartDateTime().toLocalDate() + ": " +
            e.getTitle() +
            " (" + e.getStartDateTime().toLocalTime() + ")" + recurLabel);

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

    // ========== WEEK VIEW ==========
    public static void showWeekView(List<Event> events, LocalDate weekStart) {

        LocalDate rangeStart = weekStart;
        LocalDate rangeEnd = weekStart.plusDays(6);
        List<Event> allEvents = withRecurringOccurrences(events, rangeStart, rangeEnd);
        Map<Integer, AdditionalEventFields> additionalMap = AdditionalFileHandler.readAdditionalMap();
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
                " (" + e.getStartDateTime().toLocalTime() + ")" + recurLabel);

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

    private static java.time.Period parseInterval(String interval) {
        if (interval == null) return null;
        String trimmed = interval.trim().toLowerCase();
        if (trimmed.isEmpty()) return null;

        // Support pure numbers (days), Nd, Nw, Nm, Ny, and legacy "1" meaning weekly
        if (trimmed.equals("1")) {
            return java.time.Period.ofDays(7);
        }

        int len = trimmed.length();
        char last = trimmed.charAt(len - 1);

        try {
            if (Character.isDigit(last)) {
                int days = Integer.parseInt(trimmed);
                return days > 0 ? java.time.Period.ofDays(days) : null;
            }

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
