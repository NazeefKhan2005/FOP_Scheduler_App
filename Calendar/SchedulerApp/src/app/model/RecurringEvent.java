package app.model;

import java.time.LocalDate;

public class RecurringEvent {

    private int eventId;
    private String interval;        // "1d" or "1w"
    private int recurrentTimes;     // 0 if unused
    private LocalDate recurrentEndDate; // null if unused

    public RecurringEvent(int eventId, String interval,
                          int recurrentTimes, LocalDate recurrentEndDate) {
        this.eventId = eventId;
        this.interval = interval;
        this.recurrentTimes = recurrentTimes;
        this.recurrentEndDate = recurrentEndDate;
    }

    public int getEventId() {
        return eventId;
    }

    public String getInterval() {
        return interval;
    }

    public int getRecurrentTimes() {
        return recurrentTimes;
    }

    public LocalDate getRecurrentEndDate() {
        return recurrentEndDate;
    }
}
