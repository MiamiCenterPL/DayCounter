package pl.majami.daycounter;

import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.system.tick.TickingSystem;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.EventTitleUtil;
import com.hypixel.hytale.server.core.modules.time.WorldTimeResource;

import javax.annotation.Nonnull;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

/**
 * Tick system responsible for detecting sunrise and announcing new days.
 */
final class DayCounterTickSystem extends TickingSystem<EntityStore> {

    private final Map<String, WorldState> stateByWorld = new ConcurrentHashMap<>();
    private final java.util.function.Supplier<DayCounterConfig> configSupplier;

    /**
     * Creates a tick system with access to configuration.
     *
     * @param configSupplier supplier for the latest config
     */
    DayCounterTickSystem(@Nonnull java.util.function.Supplier<DayCounterConfig> configSupplier) {
        this.configSupplier = configSupplier;
    }

    /**
     * Runs every world tick and announces at sunrise.
     *
     * @param delta frame delta
     * @param tick tick index
     * @param store entity store
     */
    @Override
    public void tick(float delta, int tick, @Nonnull Store<EntityStore> store) {
        EntityStore entityStore = store.getExternalData();

        World world = entityStore.getWorld();
        if (!world.isAlive() || !world.isTicking()) {
            return;
        }

        WorldTimeResource time = store.getResource(WorldTimeResource.getResourceType());

        float sunriseProgress = (float) WorldTimeResource.SUNRISE_SECONDS / (float) WorldTimeResource.SECONDS_PER_DAY;
        float currentProgress = time.getDayProgress();

        String worldName = Objects.toString(world.getName(), "<unknown>");

        WorldState state = this.stateByWorld.computeIfAbsent(worldName, name -> new WorldState());

        if (!state.loggedInit) {
            state.loggedInit = true;
            DayCounter.LOGGER.at(Level.INFO).log(
                    "DayCounter: tick active for world=%s (players=%s)",
                    worldName,
                    world.getPlayerCount()
            );
        }

        float previousProgress = state.lastDayProgress;
        long previousSeenDay = state.lastSeenDay;

        long dayNumber = computeDayNumber(time.getGameDateTime());
        state.lastDayProgress = currentProgress;
        state.lastSeenDay = dayNumber;

        // First observation: don't spam on plugin load mid-day.
        if (!state.hasSnapshot) {
            state.hasSnapshot = true;
            return;
        }

        boolean crossedSunrise = previousProgress < sunriseProgress && currentProgress >= sunriseProgress;
        boolean dayChanged = previousSeenDay != dayNumber;

        // Normal flow: we trigger when crossing sunrise.
        // Sleep flow: time may jump directly to morning; in that case dayChanged=true and currentProgress is already >= sunrise.
        boolean shouldAnnounce = crossedSunrise || (dayChanged && currentProgress >= sunriseProgress);
        if (!shouldAnnounce) {
            return;
        }

        if (state.lastAnnouncedDay == dayNumber) {
            return;
        }

        DayCounter.LOGGER.at(Level.INFO).log(
                "DayCounter: announce for world=%s progress=%.4f sunrise=%.4f date=%s",
                worldName,
                currentProgress,
                sunriseProgress,
                time.getGameDateTime().toString()
        );

        DayCounterConfig config = this.configSupplier.get();
        String subtitleText = resolveSubtitle(config, state);

        announceToWorld(world, time.getGameDateTime(), subtitleText);

        state.lastAnnouncedDay = dayNumber;
    }

    /**
     * Removes state when a world is unloaded.
     *
     * @param world world being removed
     */
    void cleanupWorld(@Nonnull World world) {
        String worldName = Objects.toString(world.getName(), "<unknown>");
        this.stateByWorld.remove(worldName);
    }

    /**
     * Announces immediately for manual testing.
     *
     * @param world world to announce in
     * @param localDateTime date-time to format
     */
    void announceNow(@Nonnull World world, @Nonnull LocalDateTime localDateTime) {
        String subtitleText = resolveSubtitleForWorld(world);
        announceToWorld(world, localDateTime, subtitleText);
    }

    /**
     * Resolves the next subtitle for a specific world.
     *
     * @param world world to resolve for
     * @return subtitle text
     */
    String resolveSubtitleForWorld(@Nonnull World world) {
        String worldName = Objects.toString(world.getName(), "<unknown>");
        WorldState state = this.stateByWorld.computeIfAbsent(worldName, name -> new WorldState());
        DayCounterConfig config = this.configSupplier.get();
        return resolveSubtitle(config, state);
    }

    /**
     * Sends a title announcement to all players in the world.
     *
     * @param world target world
     * @param localDateTime date-time for the title
     * @param subtitleText subtitle to show
     */
    private static void announceToWorld(
            @Nonnull World world,
            @Nonnull LocalDateTime localDateTime,
            @Nonnull String subtitleText
    ) {
        Message title = Message.raw(formatTitle(localDateTime));
        Message subtitle = Message.raw(subtitleText);

        Collection<PlayerRef> targets = world.getPlayerRefs();
        if (targets.isEmpty()) {
            Universe universe = Universe.get();
            if (universe != null) {
                targets = universe.getPlayers();
            }
        }

        for (PlayerRef playerRef : targets) {
            if (playerRef == null) {
                continue;
            }
            EventTitleUtil.showEventTitleToPlayer(playerRef, title, subtitle, true);
        }
    }

    /**
     * Formats the title text for the given date.
     *
     * @param localDateTime date-time from world time
     * @return formatted title string
     */
    static String formatTitle(@Nonnull LocalDateTime localDateTime) {
        int year = localDateTime.getYear();
        int dayOfYear = localDateTime.getDayOfYear();

        if (year == 0) {
            return "DAY " + dayOfYear;
        }
        return "YEAR " + year + " DAY " + dayOfYear;
    }

    /**
     * Chooses a subtitle based on the configured strategy.
     *
     * @param config current config
     * @param state world state
     * @return subtitle text or empty string
     */
    @Nonnull
    private static String resolveSubtitle(DayCounterConfig config, WorldState state) {
        if (config == null) {
            return "";
        }

        String[] subtitles = config.getSubtitles();
        if (subtitles == null || subtitles.length == 0) {
            return "";
        }

        SubtitleStrategy strategy = config.getSubtitleStrategy();
        if (strategy == SubtitleStrategy.QUEUE) {
            int index = Math.floorMod(state.nextSubtitleIndex, subtitles.length);
            state.nextSubtitleIndex = index + 1;
            return safeSubtitle(subtitles[index]);
        }

        int index = ThreadLocalRandom.current().nextInt(subtitles.length);
        return safeSubtitle(subtitles[index]);
    }

    /**
     * Ensures the subtitle text is safe to display.
     *
     * @param subtitle subtitle candidate
     * @return non-null subtitle
     */
    @Nonnull
    private static String safeSubtitle(String subtitle) {
        return subtitle == null ? "" : subtitle;
    }

    /**
     * Computes a continuous day number from the world date.
     *
     * @param localDateTime date-time from world time
     * @return day number starting at 1
     */
    private static long computeDayNumber(@Nonnull LocalDateTime localDateTime) {
        // WorldTimeResource uses a 360-day year (DAYS_PER_YEAR) in its calendar.
        long year = localDateTime.getYear();
        long dayOfYear = localDateTime.getDayOfYear();

        long dayNumber = (year * (long) WorldTimeResource.DAYS_PER_YEAR) + dayOfYear;
        return Math.max(1L, dayNumber);
    }
}
