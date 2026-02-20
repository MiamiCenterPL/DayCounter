package pl.majami.daycounter;

/**
 * Per-world state used by the tick system.
 */
final class WorldState {
    boolean loggedInit;
    boolean hasSnapshot;
    float lastDayProgress;
    long lastSeenDay;
    long lastAnnouncedDay = Long.MIN_VALUE;
    int nextSubtitleIndex;

    WorldState() {
    }

    WorldState(long lastAnnouncedDay, int nextSubtitleIndex) {
        this.lastAnnouncedDay = lastAnnouncedDay;
        this.nextSubtitleIndex = Math.max(0, nextSubtitleIndex);
    }
}
