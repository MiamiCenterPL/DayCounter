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
}
