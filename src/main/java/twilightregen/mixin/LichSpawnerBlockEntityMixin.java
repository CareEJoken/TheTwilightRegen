package twilightregen.mixin;

import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import twilightforest.block.entity.spawner.LichSpawnerBlockEntity;

/**
 * 巫妖出生下落（{@code spawnMyBoss}）：TF 沿空气往下落到最低点，并把落点记为归位点；
 * 而落箱点 = 归位点下方一格——若那格留着上一轮的战利品箱，下落会被挡住、归位点每轮 +1，
 * 落箱点随之漂移（"刷怪笼越来越高"的实际原因，也是"顶替"顶不到同一格的原因）。
 *
 * <p>这里把下落判据里的 {@code isAir()} 放宽为"空气或箱子"：巫妖穿过旧箱子落到真正的地面，
 * 归位点稳定、落箱点固定同一格，于是每轮死亡落箱都在同一个位置顶掉上一个箱子
 * （见 {@code twilightregen.loot.BossChestPlacement}）。</p>
 *
 * <p><b>为什么只有巫妖需要这段</b>：全 TF 只有巫妖刷怪笼写了下落找落点（本类覆写
 * {@code spawnMyBoss}）；其它 boss 走基类 {@code BossSpawnerBlockEntity#spawnMyBoss}，
 * 归位点写死为刷怪笼方块位置（如 {@code NagaSpawnerBlockEntity} 只改了探测距离和粒子），
 * 落箱点固定、不存在漂移。若将来 TF 给别的 boss 也加下落逻辑，需要在这里一并扩展。</p>
 */
@Mixin(value = LichSpawnerBlockEntity.class, remap = false)
public abstract class LichSpawnerBlockEntityMixin {

    @Redirect(
        method = "spawnMyBoss",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;isAir()Z")
    )
    private static boolean twilightregen$chestIsPassable(BlockState state) {
        return state.isAir() || state.getBlock() instanceof ChestBlock;
    }
}
