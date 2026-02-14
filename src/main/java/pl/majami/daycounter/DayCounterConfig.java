package pl.majami.daycounter;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.logger.HytaleLogger;

import javax.annotation.Nonnull;
import java.util.Locale;

/**
 * Configuration model for DayCounter.
 */
final class DayCounterConfig {
    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    /** Codec for reading and writing config JSON. */
    static final BuilderCodec<DayCounterConfig> CODEC = BuilderCodec
            .builder(DayCounterConfig.class, DayCounterConfig::new)
            .append(
                    new KeyedCodec<>("_comment", Codec.STRING),
                    (config, value) -> config.commentHeader = value,
                    config -> config.commentHeader
            )
            .add()
            .append(
                    new KeyedCodec<>("_commentStrategy", Codec.STRING),
                    (config, value) -> config.commentStrategy = value,
                    config -> config.commentStrategy
            )
            .add()
            .append(
                    new KeyedCodec<>("_commentSubtitles", Codec.STRING),
                    (config, value) -> config.commentSubtitles = value,
                    config -> config.commentSubtitles
            )
            .add()
            .append(
                    new KeyedCodec<>("SubtitleStrategy", Codec.STRING),
                    (config, value) -> config.subtitleStrategy = value,
                    config -> config.subtitleStrategy
            )
            .add()
            .append(
                    new KeyedCodec<>("Subtitles", Codec.STRING_ARRAY),
                    (config, value) -> config.subtitles = value,
                    config -> config.subtitles
            )
            .add()
            .build();

    private String commentHeader = "DayCounter config";
    private String commentStrategy = "SubtitleStrategy: random | queue";
    private String commentSubtitles = "Subtitles used for day announcements";
    private String subtitleStrategy = "random";
    private String[] subtitles = new String[] { "Have a great day!", "Good Morning World!" };

    /**
     * Gets the subtitle strategy parsed from config.
     *
     * @return subtitle strategy
     */
    @Nonnull
    SubtitleStrategy getSubtitleStrategy() {
        String raw = this.subtitleStrategy;
        if (raw == null) {
            return SubtitleStrategy.RANDOM;
        }
        return switch (raw.trim().toLowerCase(Locale.ROOT)) {
            case "queue" -> SubtitleStrategy.QUEUE;
            case "random" -> SubtitleStrategy.RANDOM;
            default -> {
                LOGGER.atInfo().log("Unknown SubtitleStrategy: %s - using RANDOM strategy.", raw);
                yield SubtitleStrategy.RANDOM;
            }
        };
    }

    /**
     * Gets the configured subtitles list.
     *
     * @return subtitles array
     */
    String[] getSubtitles() {
        return this.subtitles;
    }
}
