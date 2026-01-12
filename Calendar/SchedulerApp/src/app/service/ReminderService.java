package app.service;

import app.model.Event;
import app.model.RecurringEvent;
import app.model.Reminder;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/*
 * ReminderService
 * ---------------
 * This service class contains the business logic related to reminders.
 * It is responsible for determining the next upcoming reminder based on:
 * - existing events
 * - recurrence rules
 * - reminder configurations
 *
 * This class does NOT handle file input/output or user interaction.
 * It operates purely on model objects and performs calculations.
 */
public class ReminderService {

    /*
     * NextReminderInfo
     * ----------------
     * A helper data structure used to group information about the next
     * upcoming reminder.
     *
     * This avoids returning multiple separate values from a method.
     */
    public static class NextReminderInfo {

        // The base event associated with the reminder
        public final Event event;

        // The actual start time of the event occurrence
        // (important for recurring events)
        public final LocalDateTime occurrenceStart;

        // The exact date and time when the reminder should trigger
        public final LocalDateTime notifyAt;

        // The duration from "now" until the reminder time
        public final Duration timeUntilNotify;

        /*
         * Constructor for creating a NextReminderInfo object.
         */
        public NextReminderInfo(Event event,
                                LocalDateTime occurrenceStart,
                                LocalDateTime notifyAt,
                                Duration timeUntilNotify) {

            this.event = event;
            this.occurrenceStart = occurrenceStart;
            this.notifyAt = notifyAt;
            this.timeUntilNotify = timeUntilNotify;
        }
    }

    /**
     * Determines the next upcoming reminder based on the current time.
     */
    public static Optional<NextReminderInfo> getNextUpcomingReminder(
            List<Event> events,
            List<RecurringEvent> recurringRules,
            List<Reminder> reminders,
            LocalDateTime now
    ) {

        // Defensive check to prevent NullPointerException
        if (events == null || reminders == null) {
            return Optional.empty();
        }

        // Map reminders by eventId for fast lookup
        Map<Integer, Reminder> reminderByEventId = new HashMap<>();
        for (Reminder r : reminders) {
            // If duplicates exist, keep the last one
            reminderByEventId.put(r.getEventId(), r);
        }

        // Map recurrence rules by eventId for fast lookup
        Map<Integer, RecurringEvent> recurringByEventId = new HashMap<>();
        if (recurringRules != null) {
            for (RecurringEvent re : recurringRules) {
                recurringByEventId.put(re.getEventId(), re);
            }
        }

        // Stores the best (earliest) upcoming reminder found
        NextReminderInfo best = null;

        // Iterate through all events
        for (Event baseEvent : events) {

            // Skip events that do not have a reminder
            Reminder reminder = reminderByEventId.get(baseEvent.getEventId());
            if (reminder == null) {
                continue;
            }

            // Default occurrence start time is the base event start
            LocalDateTime occurrenceStart = baseEvent.getStartDateTime();

            // Check if the event has a recurrence rule
            RecurringEvent rule = recurringByEventId.get(baseEvent.getEventId());
            if (rule != null) {
                // Calculate the next valid occurrence start time
                occurrenceStart = nextOccurrenceStart(
                        baseEvent.getStartDateTime(), rule, now
                );

                // If no future occurrence exists, skip this event
                if (occurrenceStart == null) {
                    continue;
                }
            }

            // Calculate the reminder notification time
            LocalDateTime notifyAt =
                    occurrenceStart.minusMinutes(reminder.getMinutesBefore());

            // Ignore reminders that should have already triggered
            if (notifyAt.isBefore(now)) {
                continue;
            }

            // Calculate duration until notification
            Duration until = Duration.between(now, notifyAt);

            // Keep the earliest upcoming reminder
            if (best == null || notifyAt.isBefore(best.notifyAt)) {
                best = new NextReminderInfo(
                        baseEvent, occurrenceStart, notifyAt, until
                );
            }
        }

        // Return result safely using Optional
        return Optional.ofNullable(best);
    }

    /**
     * Calculates the next valid occurrence start time for a recurring event.
     */
    private static LocalDateTime nextOccurrenceStart(
            LocalDateTime baseStart,
            RecurringEvent rule,
            LocalDateTime now
    ) {

        // Parse recurrence interval string into a Period
        java.time.Period period = parseInterval(rule.getInterval());
        if (period == null) {
            return null;
        }

        // Retrieve recurrence limits
        LocalDate endDate = rule.getRecurrentEndDate();
        int maxTimes = safeInt(rule.getRecurrentTimes());

        // occurrenceIndex = 0 refers to the base event
        int occurrenceIndex = 0;
        LocalDateTime candidate = baseStart;

        while (true) {

            // If candidate is in the future, check validity
            if (!candidate.isBefore(now)) {

                // Check end date limit
                if (endDate != null && candidate.toLocalDate().isAfter(endDate)) {
                    return null;
                }

                // Check repetition count limit
                if (maxTimes > 0 && occurrenceIndex >= maxTimes) {
                    return null;
                }

                return candidate;
            }

            // Move to the next occurrence
            occurrenceIndex++;

            // Stop if repetition limit reached
            if (maxTimes > 0 && occurrenceIndex >= maxTimes) {
                return null;
            }

            // Advance the candidate time by the recurrence period
            candidate = candidate
                    .plusYears((long) period.getYears())
                    .plusMonths((long) period.getMonths())
                    .plusDays((long) period.getDays());

            // Stop if end date limit exceeded
            if (endDate != null && candidate.toLocalDate().isAfter(endDate)) {
                return null;
            }
        }
    }

    /*
     * Parses a recurrence interval string into a Period object.
     *
     * Supported formats:
     * - "Nd" : every N days
     * - "Nw" : every N weeks
     * - "Nm" : every N months
     * - "Ny" : every N years
     * - "N"  : every N days
     * - "1"  : special case representing weekly recurrence
     */
    private static java.time.Period parseInterval(String interval) {

        if (interval == null) return null;

        String trimmed = interval.trim().toLowerCase();
        if (trimmed.isEmpty()) return null;

        // Special case: "1" represents weekly recurrence
        if (trimmed.equals("1")) {
            return java.time.Period.ofDays(7);
        }

        int len = trimmed.length();
        char last = trimmed.charAt(len - 1);

        try {
            // If the interval contains only digits, treat it as days
            if (Character.isDigit(last)) {
                int days = Integer.parseInt(trimmed);
                return days > 0
                        ? java.time.Period.ofDays(days)
                        : null;
            }

            // Parse numeric value and unit character
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
            // Invalid interval format
            return null;
        }
    }

    /*
     * Ensures recurrence count is non-negative.
     */
    private static int safeInt(int v) {
        return Math.max(0, v);
    }

    /*
     * Converts a Duration into a human-readable string.
     */
    public static String formatDuration(Duration d) {

        if (d == null) {
            return "0 minutes";
        }

        long totalMinutes = Math.max(0, d.toMinutes());
        long days = totalMinutes / (60 * 24);
        long hours = (totalMinutes % (60 * 24)) / 60;
        long minutes = totalMinutes % 60;

        StringBuilder sb = new StringBuilder();

        if (days > 0) {
            sb.append(days).append(days == 1 ? " day" : " days");
        }
        if (hours > 0) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(hours).append(hours == 1 ? " hour" : " hours");
        }
        if (minutes > 0 || sb.length() == 0) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(minutes).append(minutes == 1 ? " minute" : " minutes");
        }

        return sb.toString();
    }
}
