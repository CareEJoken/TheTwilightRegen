package twilightregen.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import twilightforest.entity.boss.BaseTFBoss;
import twilightforest.entity.boss.IBossLootBuffer;
import twilightregen.loot.BossChestPlacement;

/**
 * 把 boss 死亡落箱重定向到 {@link BossChestPlacement}：覆盖所有 {@code BaseTFBoss} 子类
 * （娜迦、巫妖、九头蛇……）；KnightPhantom 不继承 BaseTFBoss，由 {@link KnightPhantomMixin}
 * 单独挂同一处重定向——两者合起来覆盖全部会落箱的 boss。占用一律原地顶掉、绝不向上爬升。
 * 目标类/方法都来自 TF（编译时已是正式名，运行时无重映射），remap = false。
 */
@Mixin(value = BaseTFBoss.class, remap = false)
public abstract class BaseTFBossMixin {

    @Redirect(
        method = "postRemoval",
        at = @At(
            value = "INVOKE",
            target = "Ltwilightforest/entity/boss/IBossLootBuffer;depositDropsIntoChest(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/server/level/ServerLevel;)V"
        )
    )
    private static void twilightregen$replaceChestPlacement(LivingEntity boss, BlockState chest, BlockPos pos, ServerLevel level) {
        if (boss instanceof IBossLootBuffer) {
            BossChestPlacement.deposit((LivingEntity & IBossLootBuffer) boss, chest, pos, level);
        }
    }
}
