package pl.majami.daycounter;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Resource;
import com.hypixel.hytale.component.ResourceType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

/**
 * Persistent day counter state stored as a world resource.
 */
final class DayCounterWorldStateResource implements Resource<EntityStore> {

    static final BuilderCodec<DayCounterWorldStateResource> CODEC = BuilderCodec
            .builder(DayCounterWorldStateResource.class, DayCounterWorldStateResource::new)
            .append(
                    new KeyedCodec<>("LastAnnouncedDay", Codec.LONG),
                    (resource, value) -> resource.lastAnnouncedDay = value,
                    resource -> resource.lastAnnouncedDay
            )
            .add()
            .append(
                    new KeyedCodec<>("NextSubtitleIndex", Codec.INTEGER),
                    (resource, value) -> resource.nextSubtitleIndex = value,
                    resource -> resource.nextSubtitleIndex
            )
            .add()
            .build();

    private static final ResourceType<EntityStore, DayCounterWorldStateResource> RESOURCE_TYPE =
            EntityStore.REGISTRY.registerResource(
                    DayCounterWorldStateResource.class,
                    "daycounter_world_state",
                    CODEC
            );

    long lastAnnouncedDay = Long.MIN_VALUE;
    int nextSubtitleIndex;

    DayCounterWorldStateResource() {
    }

    DayCounterWorldStateResource(long lastAnnouncedDay, int nextSubtitleIndex) {
        this.lastAnnouncedDay = lastAnnouncedDay;
        this.nextSubtitleIndex = Math.max(0, nextSubtitleIndex);
    }

    @Nonnull
    static ResourceType<EntityStore, DayCounterWorldStateResource> getResourceType() {
        return RESOURCE_TYPE;
    }

    @Override
    public Resource<EntityStore> clone() {
        return new DayCounterWorldStateResource(this.lastAnnouncedDay, this.nextSubtitleIndex);
    }
}
