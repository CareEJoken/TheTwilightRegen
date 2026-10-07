package twilightregen.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import twilightforest.entity.boss.IBossLootBuffer;
import twilightforest.entity.boss.KnightPhantom;
import twilightregen.loot.BossChestPlacement;

/**
 * KnightPhantom 重写了 postRemoval（仅最终骑士落箱），单独重定向它的落箱调用——
 * 与 {@link BaseTFBossMixin} 合起来覆盖全部会落箱的 boss。占用一律原地顶掉、绝不向上爬升。
 */
@Mixin(value = KnightPhantom.class, remap = false)
public abstract class KnightPhantomMixin {

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
