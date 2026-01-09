package app.service;

import app.model.Event;
import app.model.RecurringEvent;
import app.model.Reminder;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

public class ReminderService {

    public static class NextReminderInfo {
        public final Event event;
        public final LocalDateTime occurrenceStart;
        public final LocalDateTime notifyAt;
        public final Duration timeUntilNotify;

        public NextReminderInfo(Event event, LocalDateTime occurrenceStart, LocalDateTime notifyAt, Duration timeUntilNotify) {
            this.event = event;
            this.occurrenceStart = occurrenceStart;
            this.notifyAt = notifyAt;
            this.timeUntilNotify = timeUntilNotify;
        }
    }

    public static Optional<NextReminderInfo> getNextUpcomingReminder(
            List<Event> events,
            List<RecurringEvent> recurringRules,
            List<Reminder> reminders,
            LocalDateTime now
    ) {
        if (events == null || reminders == null) {
            return Optional.empty();
        }

        Map<Integer, Reminder> reminderByEventId = new HashMap<>();
        for (Reminder r : reminders) {
            // If duplicates exist, keep the last one
            reminderByEventId.put(r.getEventId(), r);
        }

        // Quick lookup for recurring rules by eventId
        Map<Integer, RecurringEvent> recurringByEventId = new HashMap<>();
        if (recurringRules != null) {
            for (RecurringEvent re : recurringRules) {
                recurringByEventId.put(re.getEventId(), re);
            }
        }

        NextReminderInfo best = null;

        for (Event baseEvent : events) {
            Reminder reminder = reminderByEventId.get(baseEvent.getEventId());
            if (reminder == null) {
                continue;
            }

            LocalDateTime occurrenceStart = baseEvent.getStartDateTime();

            RecurringEvent rule = recurringByEventId.get(baseEvent.getEventId());
            if (rule != null) {
                occurrenceStart = nextOccurrenceStart(baseEvent.getStartDateTime(), rule, now);
                if (occurrenceStart == null) {
                    continue;
                }
            }

            LocalDateTime notifyAt = occurrenceStart.minusMinutes(reminder.getMinutesBefore());
            if (notifyAt.isBefore(now)) {
                // If notification time has already passed, we don't announce it at launch.
                continue;
            }

            Duration until = Duration.between(now, notifyAt);
            if (best == null || notifyAt.isBefore(best.notifyAt)) {
                best = new NextReminderInfo(baseEvent, occurrenceStart, notifyAt, until);
            }
        }

        return Optional.ofNullable(best);
    }

    private static LocalDateTime nextOccurrenceStart(LocalDateTime baseStart, RecurringEvent rule, LocalDateTime now) {
        java.time.Period period = parseInterval(rule.getInterval());
        if (period == null) {
            return null;
        }

        LocalDate endDate = rule.getRecurrentEndDate();
        int maxTimes = safeInt(rule.getRecurrentTimes());

        // occurrenceIndex = 0 means the base event
        int occurrenceIndex = 0;
        LocalDateTime candidate = baseStart;

        while (true) {
            // If base occurrence is already in future and within limits, use it
            if (!candidate.isBefore(now)) {
                if (endDate != null && candidate.toLocalDate().isAfter(endDate)) {
                    return null;
                }
                if (maxTimes > 0 && occurrenceIndex >= maxTimes) {
                    return null;
                }
                return candidate;
            }

            // Move to next occurrence
            occurrenceIndex++;
            if (maxTimes > 0 && occurrenceIndex >= maxTimes) {
                return null;
            }

            candidate = candidate
                    .plusYears((long) period.getYears())
                    .plusMonths((long) period.getMonths())
                    .plusDays((long) period.getDays());

            if (endDate != null && candidate.toLocalDate().isAfter(endDate)) {
                return null;
            }

            // loop until candidate >= now or exits via limits
        }
    }

    private static java.time.Period parseInterval(String interval) {
        if (interval == null) return null;
        String trimmed = interval.trim().toLowerCase();
        if (trimmed.isEmpty()) return null;

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

    private static int safeInt(int v) {
        return Math.max(0, v);
    }

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
