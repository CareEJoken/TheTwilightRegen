package twilightregen.client.renderer;

import net.minecraft.client.renderer.entity.DisplayRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

/**
 * 渲染走原版 {@link DisplayRenderer.TextDisplayRenderer}（与 TF 最终城堡 WIP 文本同一套显示效果）。
 * 这里继承只是因为它构造器是 protected，跨包不能直接 new 出来当渲染器工厂用。
 */
public class BossRespawnMarkerRenderer extends DisplayRenderer.TextDisplayRenderer {

    public BossRespawnMarkerRenderer(EntityRendererProvider.Context context) {
        super(context);
    }
}
