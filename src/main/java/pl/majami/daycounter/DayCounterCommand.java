package pl.majami.daycounter;

import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.EventTitleUtil;
import com.hypixel.hytale.server.core.modules.time.WorldTimeResource;

import javax.annotation.Nonnull;
import java.time.LocalDateTime;
import java.util.Locale;

/**
 * Player command handler for /daycounter.
 */
final class DayCounterCommand extends AbstractPlayerCommand {

    private final DayCounterTickSystem tickSystem;
    private final Runnable reloadAction;

    /**
     * Creates the command handler.
     *
     * @param name command name
     * @param description command description
     * @param requiresConfirmation whether confirmation is required
     * @param tickSystem tick system used for announcements
     * @param reloadAction reload callback for config
     */
    DayCounterCommand(
            @Nonnull String name,
            @Nonnull String description,
            boolean requiresConfirmation,
            @Nonnull DayCounterTickSystem tickSystem,
            @Nonnull Runnable reloadAction
    ) {
        super(name, description, requiresConfirmation);
        setAllowsExtraArguments(true);
        this.tickSystem = tickSystem;
        this.reloadAction = reloadAction;
    }

    /**
     * Executes /daycounter subcommands or shows the current day.
     *
     * @param commandContext command context
     * @param store entity store
     * @param ref entity store ref
     * @param playerRef player reference
     * @param world current world
     */
    @Override
    protected void execute(
            @Nonnull CommandContext commandContext,
            @Nonnull Store<EntityStore> store,
            @Nonnull com.hypixel.hytale.component.Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world
    ) {
        String input = commandContext.getInputString();
        String[] tokens = input.trim().split("\\s+");
        String subcommand = tokens.length > 1 ? tokens[1].trim().toLowerCase(Locale.ROOT) : "";

        if ("reload".equals(subcommand)) {
            this.reloadAction.run();
            EventTitleUtil.showEventTitleToPlayer(
                    playerRef,
                    Message.raw("DayCounter"),
                    Message.raw("Config reloaded"),
                    true
            );
            return;
        }

        WorldTimeResource time = store.getResource(WorldTimeResource.getResourceType());
        LocalDateTime dateTime = time.getGameDateTime();
        String titleText = DayCounterTickSystem.formatTitle(dateTime);

        if ("test".equals(subcommand)) {
            this.tickSystem.announceNow(store, world, dateTime);
            return;
        }

        if (!subcommand.isEmpty()) {
            EventTitleUtil.showEventTitleToPlayer(
                    playerRef,
                    Message.raw("DayCounter"),
                    Message.raw("Usage: /daycounter [test|reload]"),
                    true
            );
            return;
        }

        String subtitleText = this.tickSystem.resolveSubtitleForWorld(store, world);
        EventTitleUtil.showEventTitleToPlayer(
                playerRef,
                Message.raw(titleText),
                Message.raw(subtitleText),
                true
        );
    }
}
