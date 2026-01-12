package app.model;

import java.time.LocalDateTime;

/**
 * Event
 * -----
 * This class represents a single calendar event in the Scheduler application.
 * It is a model class used to store core event information such as:
 * - unique event ID
 * - title
 * - description
 * - start date and time
 * - end date and time
 *
 * This class follows the principles of encapsulation by keeping all fields
 * private and providing public getter methods for access.
 *
 * Note:
 * This class does NOT contain any logic, file handling, or user
 * interaction code. It is purely a data holder.
 */
public class Event {

    // Unique identifier for the event
    // Used to link this event with reminders, recurrence rules, and additional fields
    private int eventId;

    // Title or name of the event (e.g., "Exam", "Meeting")
    private String title;

    // Optional description providing more details about the event
    private String description;

    // Date and time when the event starts
    private LocalDateTime startDateTime;

    // Date and time when the event ends
    private LocalDateTime endDateTime;

    /*
     * Constructor for creating an Event object.
     * This constructor initializes all attributes of the Event object.
     */
    public Event(int eventId, String title, String description,
                 LocalDateTime startDateTime, LocalDateTime endDateTime) {

        // Assign the provided event ID to the object
        this.eventId = eventId;

        // Assign the event title
        this.title = title;

        // Assign the event description
        this.description = description;

        // Assign the start date and time
        this.startDateTime = startDateTime;

        // Assign the end date and time
        this.endDateTime = endDateTime;
    }

    /*
     * Returns the unique ID of the event.
     */
    public int getEventId() {
        return eventId;
    }

    /*
     * Returns the title of the event.
     */
    public String getTitle() {
        return title;
    }

    /*
     * Returns the description of the event.
     */
    public String getDescription() {
        return description;
    }

    /*
     * Returns the start date and time of the event.
     */
    public LocalDateTime getStartDateTime() {
        return startDateTime;
    }

    /*
     * Returns the end date and time of the event.
     */
    public LocalDateTime getEndDateTime() {
        return endDateTime;
    }
}
