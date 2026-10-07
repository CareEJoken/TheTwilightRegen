package twilightregen.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import twilightregen.TwilightRegen;

/**
 * Client-only entry point. Registers NeoForge's default config screen so the mod's
 * configs become editable from the in-game Mods list (Config button).
 */
@Mod(value = TwilightRegen.MODID, dist = Dist.CLIENT)
public class ModClientSetup {

    public ModClientSetup(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class,
            (mc, parent) -> new ConfigurationScreen(container, parent));
    }
}
