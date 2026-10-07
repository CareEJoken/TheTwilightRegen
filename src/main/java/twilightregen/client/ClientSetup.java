package twilightregen.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import twilightregen.TwilightRegen;
import twilightregen.client.renderer.BossRespawnMarkerRenderer;

@EventBusSubscriber(modid = TwilightRegen.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {

    private ClientSetup() {}

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(TwilightRegen.BOSS_RESPAWN_MARKER.get(), BossRespawnMarkerRenderer::new);
    }
}
