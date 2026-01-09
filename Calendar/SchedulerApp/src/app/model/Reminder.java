package app.model;

public class Reminder {

    private final int eventId;
    private final int minutesBefore;

    public Reminder(int eventId, int minutesBefore) {
        this.eventId = eventId;
        this.minutesBefore = minutesBefore;
    }

    public int getEventId() {
        return eventId;
    }

    public int getMinutesBefore() {
        return minutesBefore;
    }
}
