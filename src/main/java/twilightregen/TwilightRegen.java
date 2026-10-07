package twilightregen;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;
import twilightregen.config.Config;
import twilightregen.entity.BossRespawnMarker;

@Mod(TwilightRegen.MODID)
public class TwilightRegen {

    public static final String MODID = "twilightregen";
    public static final Logger LOGGER = LogUtils.getLogger();

    // --- Entity types ---
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
        DeferredRegister.create(Registries.ENTITY_TYPE, MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<BossRespawnMarker>> BOSS_RESPAWN_MARKER =
        ENTITY_TYPES.register("boss_respawn_marker", () -> EntityType.Builder
            .<BossRespawnMarker>of(BossRespawnMarker::new, MobCategory.MISC)
            .sized(0.1F, 0.1F)
            .clientTrackingRange(10)
            .build("boss_respawn_marker"));

    public TwilightRegen(IEventBus modEventBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        ENTITY_TYPES.register(modEventBus);
    }
}
