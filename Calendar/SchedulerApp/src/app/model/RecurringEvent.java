package app.model;

import java.time.LocalDate;

/*
 * RecurringEvent
 * --------------
 * This class represents the recurrence rule for an event in the Scheduler App.
 * It defines how an event repeats over time (daily, weekly, etc.).
 *
 * This class does NOT represent an actual event occurrence.
 * Instead, it stores the recurrence configuration that is linked
 * to a base Event using the eventId.
 *
 * This is a model class and contains no logic or file handling.
 */
public class RecurringEvent {

    // The ID of the event that this recurrence rule belongs to
    // This acts as a link (foreign key) to the Event class
    private int eventId;

    // Defines the recurrence interval
    // Examples:
    // "1d" -> repeats every 1 day
    // "1w" -> repeats every 1 week
    // "1m" -> repeats every 1 month
    // "1y" -> repeats every 1 year
    private String interval;  

    // Number of times the event should repeat
    // A value of 0 indicates that this option is not used
    private int recurrentTimes;     // 0 if unused

    // The date on which the recurrence should end
    // A null value indicates that this option is not used
    private LocalDate recurrentEndDate; // null if unused

    /*
     * Constructor for creating a RecurringEvent object.
     *
     * Only one of recurrentTimes or recurrentEndDate is typically used
     * at a time to define the recurrence limit.
     */
    public RecurringEvent(int eventId, String interval,
                          int recurrentTimes, LocalDate recurrentEndDate) {

        // Assign the associated event ID
        this.eventId = eventId;

        // Assign the recurrence interval
        this.interval = interval;

        // Assign the number of recurrence times
        this.recurrentTimes = recurrentTimes;

        // Assign the recurrence end date
        this.recurrentEndDate = recurrentEndDate;
    }

    /*
     * Returns the ID of the event associated with this recurrence rule.
     */
    public int getEventId() {
        return eventId;
    }

    /*
     * Returns the recurrence interval string.
     */
    public String getInterval() {
        return interval;
    }

    /**
     * Returns the number of times the event should recur.
     */
    public int getRecurrentTimes() {
        return recurrentTimes;
    }

    /**
     * Returns the end date of the recurrence.
     */
    public LocalDate getRecurrentEndDate() {
        return recurrentEndDate;
    }
}
