package twilightregen.loot;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import twilightforest.config.TFConfig;
import twilightforest.entity.boss.IBossLootBuffer;
import twilightregen.TwilightRegen;

import java.util.ArrayList;
import java.util.List;

/**
 * 替代 TF 原版 {@link IBossLootBuffer#depositDropsIntoChest} 的落箱逻辑（由 mixin 重定向调用，
 * 覆盖<b>所有</b>会落箱的 boss：BaseTFBoss 全体——娜迦、巫妖、九头蛇等——外加 KnightPhantom）。
 *
 * <p><b>通用问题</b>（与 boss 种类无关，所以对全部 boss 统一修）：指定位置（= 归位点下方一格）
 * 被上一轮的战利品箱占住时，原版行为是——同类箱子：逐格 {@code setItem} 覆盖进去，旧战利品被
 * 静默吞掉；异类箱子或其它方块：从指定位置<b>向上逐格找空位</b>到世界高度上限（"箱子越堆越高"）。
 * 而 TF 死亡宝箱的木种有的 boss 是<b>每次死亡随机二选一</b>（如 {@code Naga#getDeathContainer}），
 * 连续两轮约一半概率不同 → 爬升分支在刷怪笼重生循环里非常常见，并非巫妖独有。</p>
 *
 * <p><b>本实现</b>：指定位置空着 / 只有可替换软方块 → 原版行为；其余一律<b>顶掉占用物</b>——
 * 旧容器内容物取出后撒到相邻空位（优先原位上方），方块移除后再把新箱子放回原位，
 * 任何情况下都不向上搜索；旧战利品以掉落物形式爆出，而不是被覆盖吞掉。</p>
 *
 * <p><b>巫妖的附加问题</b>（唯一需要额外处理的 boss）：巫妖的归位点由下落算法算出（与地形相关），
 * 落箱点 = 归位点 - 1 会随归位点每轮 +1 而上移；其它 boss 的归位点写死在刷怪笼方块位置、不会漂。
 * 该部分由 {@code LichSpawnerBlockEntityMixin} 单独处理。</p>
 */
public final class BossChestPlacement {

    /** 倾倒方向优先级：优先原位上方（原刷怪笼格），再水平方向，最后下方 */
    private static final Direction[] SPILL_ORDER = {
        Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, Direction.DOWN
    };

    private BossChestPlacement() {}

    public static <T extends LivingEntity & IBossLootBuffer> void deposit(T boss, BlockState chest, BlockPos pos, ServerLevel level) {
        if (!TFConfig.bossDropChests || boss.getItemStacks().isEmpty()) return;

        // 指定位置空着 / 只有可替换软方块 → 原版行为（直接安放）
        if (!level.getBlockState(pos).is(chest.getBlock()) && IBossLootBuffer.tryDeposit(boss, chest, pos, level)) return;

        // 占位（同类箱子或别的方块）→ 破坏占用物：内容物取出、方块移走，再原位安放新箱子
        BlockState blocker = level.getBlockState(pos);
        List<ItemStack> spilled = drainContents(level, pos);
        if (level.destroyBlock(pos, false) && IBossLootBuffer.tryDeposit(boss, chest, pos, level)) {
            spill(level, spillPos(level, pos), spilled);
            TwilightRegen.LOGGER.info("Replaced {} at {} with {} boss loot chest", blocker.getBlock(), pos, chest.getBlock());
            return;
        }

        // 兜底：占用物破坏不了（基岩、保护区等）→ 物品掉在原地附近，仍然不向上爬
        spill(level, spillPos(level, pos), spilled);
        for (int i = 0; i < IBossLootBuffer.CONTAINER_SIZE; i++) {
            Block.popResource(level, pos, boss.getItem(i));
        }
        IBossLootBuffer.celebrateAt(boss, pos.getCenter(), level);
    }

    /**
     * 把容器内容物从容器中取出（取出后容器清空）；非容器返回空列表。
     * 必须"取出"而非复制：方块移除时引擎会把容器里剩下的内容物自动掉出，
     * 复制会让同一批战利品掉两份。
     */
    private static List<ItemStack> drainContents(ServerLevel level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof Container container)) return List.of();
        List<ItemStack> contents = new ArrayList<>();
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (!stack.isEmpty()) {
                contents.add(stack);
                container.setItem(i, ItemStack.EMPTY);
            }
        }
        return contents;
    }

    private static void spill(ServerLevel level, BlockPos pos, List<ItemStack> stacks) {
        for (ItemStack stack : stacks) {
            Block.popResource(level, pos, stack);
        }
    }

    /** 掉落物落点：优先原位上方（原刷怪笼格），否则第一个无碰撞的相邻格。 */
    private static BlockPos spillPos(ServerLevel level, BlockPos pos) {
        for (Direction dir : SPILL_ORDER) {
            BlockPos neighbor = pos.relative(dir);
            if (level.getBlockState(neighbor).getCollisionShape(level, neighbor).isEmpty()) return neighbor;
        }
        return pos;
    }
}
