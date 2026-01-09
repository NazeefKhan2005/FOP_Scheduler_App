package app.model;

public class AdditionalEventFields {
    private final int eventId;
    private final String location;
    private final String category;

    public AdditionalEventFields(int eventId, String location, String category) {
        this.eventId = eventId;
        this.location = location;
        this.category = category;
    }

    public int getEventId() {
        return eventId;
    }

    public String getLocation() {
        return location;
    }

    public String getCategory() {
        return category;
    }
}
