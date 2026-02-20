package pl.majami.daycounter;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.HytaleServerConfig;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.events.RemoveWorldEvent;
import com.hypixel.hytale.server.core.util.Config;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;

/**
 * Main plugin entrypoint for DayCounter.
 */
public class DayCounter extends JavaPlugin {

    /** Logger for plugin messages. */
    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static final String CONFIG_NAME = "daycounter";
    private static final String CONFIG_FILE_NAME = CONFIG_NAME + ".json";

    @Nonnull
    private final Config<DayCounterConfig> config;
    @Nonnull
    private final Path configDirectory;
    @Nonnull
    private final Path configFile;
    @Nonnull
    private final DayCounterTickSystem tickSystem;

    /**
     * Creates the plugin instance and prepares configuration paths.
     *
     * @param init plugin initialization context
     */
    public DayCounter(@Nonnull JavaPluginInit init) {
        super(init);

        this.configDirectory = resolveConfigDirectory();
        this.configFile = this.configDirectory.resolve(CONFIG_FILE_NAME);
        this.config = new Config<>(this.configDirectory, CONFIG_NAME, DayCounterConfig.CODEC);
        this.tickSystem = new DayCounterTickSystem(this.config::get);
    }

    /**
     * Registers commands, systems, and event listeners.
     */
    @Override
    protected void setup() {
        super.setup();

        ensureConfigDirectory();
        loadConfig();

        DayCounterWorldStateResource.getResourceType();

        LOGGER.atInfo().log("DayCounter enabled (config=%s)", this.configFile.toAbsolutePath().toString());

        this.getCommandRegistry().registerCommand(
                new DayCounterCommand(
                        "daycounter",
                        "Shows the current day counter or runs subcommands",
                        false,
                        this.tickSystem,
                        this::reloadConfig
                )
        );
        this.getEventRegistry().registerGlobal(RemoveWorldEvent.class, event -> this.tickSystem.cleanupWorld(event.getWorld()));

        // Runs as part of the world's ECS tick (no polling thread).
        this.getEntityStoreRegistry().registerSystem(this.tickSystem);
    }

    /**
     * Ensures the configuration directory exists on disk.
     */
    private void ensureConfigDirectory() {
        try {
            Files.createDirectories(this.configDirectory);
        } catch (IOException e) {
            LOGGER.at(Level.WARNING).log("DayCounter: failed to create config directory %s", this.configDirectory);
        }
    }

    /**
     * Loads configuration and writes defaults when missing.
     */
    private void loadConfig() {
        boolean shouldWriteDefaults = Files.notExists(this.configFile);
        try {
            this.config.load().join();
            if (shouldWriteDefaults) {
                this.config.save().join();
            }
        } catch (RuntimeException e) {
            LOGGER.at(Level.WARNING).log("DayCounter: failed to load config %s", this.configFile);
        }
    }

    /**
     * Reloads configuration from disk.
     */
    private void reloadConfig() {
        loadConfig();
        LOGGER.atInfo().log("DayCounter: config reloaded from %s", this.configFile.toAbsolutePath().toString());
    }

    /**
     * Resolves the server-level config directory.
     *
     * @return path to the server config directory
     */
    @Nonnull
    private static Path resolveConfigDirectory() {
        Path serverConfigPath = HytaleServerConfig.PATH.toAbsolutePath();
        Path serverRoot = serverConfigPath.getParent();
        if (serverRoot == null) {
            serverRoot = Path.of(".").toAbsolutePath().normalize();
        }
        return serverRoot.resolve("config");
    }
}
