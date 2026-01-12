package app.model;

/*
 * Reminder
 * --------
 * This class represents a reminder configuration for an event.
 * It specifies how many minutes before an event the user should
 * be notified.
 *
 * This is a model class and is used only to store reminder data.
 * It does not contain any logic for triggering notifications.
 *
 * Reminder objects are immutable once created.
 */
public class Reminder {

    // The ID of the event that this reminder is associated with
    // This acts as a link (foreign key) to the Event class
    private final int eventId;

    // Number of minutes before the event start time
    // when the reminder should be triggered
    private final int minutesBefore;

    /*
     * Constructor for creating a Reminder object.
     */
    public Reminder(int eventId, int minutesBefore) {

        // Assign the associated event ID
        this.eventId = eventId;

        // Assign the reminder offset in minutes
        this.minutesBefore = minutesBefore;
    }

    /*
     * Returns the ID of the event associated with this reminder.
     */
    public int getEventId() {
        return eventId;
    }

    /*
     * Returns the number of minutes before the event
     * when the reminder should trigger.
     */
    public int getMinutesBefore() {
        return minutesBefore;
    }
}
