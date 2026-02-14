package pl.majami.daycounter;

/**
 * Strategy used to select the next subtitle.
 */
enum SubtitleStrategy {
    /** Choose a random subtitle each time. */
    RANDOM,
    /** Cycle through subtitles in order. */
    QUEUE
}
