package app.model;

/*
 * AdditionalEventFields
 * --------------------
 * This class represents optional or additional information related to an event.
 * It is separated from the main Event class to keep the core event data simple
 * and to follow the principle of separation of concerns.
 *
 * This class stores metadata such as:
 * - location of the event
 * - category of the event
 *
 * These fields are optional and may not exist for every event.
 * This is a model class and does not contain any business logic or file handling.
 */
public class AdditionalEventFields {

    // The ID of the event that these additional fields belong to
    // This acts as a link (foreign key) to the Event class
    private final int eventId;

    // Location where the event takes place (e.g., "Lecture Hall A", "Online")
    // This value may be null if the user does not provide a location
    private final String location;

    // Category used to group or classify events (e.g., "Study", "Work", "Personal")
    // This value may be null if the user does not provide a category
    private final String category;

    /*
     * Constructor for creating an AdditionalEventFields object.
     *
     * Once created, the values of this object cannot be changed,
     * making it immutable and safe to use throughout the application.
     */
    public AdditionalEventFields(int eventId, String location, String category) {

        // Assign the associated event ID
        this.eventId = eventId;

        // Assign the location of the event
        this.location = location;

        // Assign the category of the event
        this.category = category;
    }

    /*
     * Returns the ID of the event associated with these additional fields.
     */
    public int getEventId() {
        return eventId;
    }

    /*
     * Returns the location of the event.
     */
    public String getLocation() {
        return location;
    }

    /*
     * Returns the category of the event.
     */
    public String getCategory() {
        return category;
    }
}
