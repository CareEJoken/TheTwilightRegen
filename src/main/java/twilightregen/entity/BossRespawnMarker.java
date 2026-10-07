package twilightregen.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Interaction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import twilightforest.util.landmarks.LandmarkUtil;
import twilightforest.world.components.structures.start.TFStructureStart;
import twilightregen.TwilightRegen;
import twilightregen.config.Config;

/**
 * Boss 重生倒计时标记：一个世界空间的 3D 倒计时文本，外观对齐 TF 最终城堡的 "WIP" 文本
 * （{@link Display.TextDisplay} + VERTICAL 广告牌，不设任何 flags → 同样的白字无背景）。
 *
 * <p>实体自身就是全部状态（NBT 随区块存档持久化，不需要额外的 SavedData）：
 * 记录原刷怪笼位置、要放回的刷怪笼方块、倒计时结束的世界时间（绝对 game time）
 * 与所属结构。只在区块被加载时 tick，因此「倒计时到点」在玩家不在场时不会强制加载区块，
 * 而是等区块下次加载时结算——把刷怪笼放回原位并清除结构的"已征服"标记（地图叉号随之消失）。</p>
 *
 * <p><b>为什么"结构已征服"在首 tick 才校验</b>：标记由 {@code LivingDeathEvent} 当场生成，
 * 而 TF 的 postmortem（标记征服）在同一 die() 栈里稍后才执行，事件里查不到。
 * 不能靠 {@code server.execute} 延后（服务端线程上它同步执行），所以校验放在实体自己的
 * 下一个 tick（新增实体当 tick 不 tick，参见 EntityTickList 的 copy-on-write 语义），
 * 那时征服标记一定已经写好。校验失败（KnightPhantom 前几具、mobLoot 关掉没进 postmortem 等）
 * 说明不是"真死"，静默 discard。</p>
 *
 * <p><b>可被打掉</b>（与 TF 最终城堡 "WIP" 文本同款机制，可由 config 的
 * {@code bossRespawnMarkerBreakable} 关闭 → 不挂判定箱、不可打断）：Display 文本没有判定箱、不可被攻击，
 * TF 的做法是另放一个 {@code Interaction} 实体当攻击判定箱——玩家左键打到它，由
 * {@code AttackEntityEvent} 处理器把文本清掉。本实体沿用：校验通过后挂箱（区块重载/被 /kill
 * 后按标签邻近查找自愈重建），标记移除时一并清掉。一旦被打掉，标记（= 全部状态）discard，
 * 这次重生彻底取消——刷怪笼不回填、结构保持"已征服"（地图叉号保留）。</p>
 */
public class BossRespawnMarker extends Display.TextDisplay {

    private static final String TAG_SPAWNER_POS = "SpawnerPos";
    private static final String TAG_END_GAME_TIME = "EndGameTime";
    private static final String TAG_SPAWNER_BLOCK = "SpawnerBlock";
    private static final String TAG_STRUCTURE = "Structure";
    private static final String TAG_BOSS_TYPE = "BossType";
    private static final String TAG_VERIFIED = "Verified";

    /** 攻击判定箱的实体标签；带命名空间，避免误伤其它模组（含 TF 自己的 WIP 箱）的 Interaction */
    public static final String INTERACTION_TAG = "twilightregen_boss_respawn";
    /** 判定箱尺寸（方块）：只占文本悬浮的那一格（1×1×1），不拦截旁边的东西 */
    private static final float ANCHOR_WIDTH = 1.0F;
    private static final float ANCHOR_HEIGHT = 1.0F;

    /** 落笼位置被占用时的重试间隔（tick） */
    private static final int RETRY_INTERVAL = 100;

    private BlockPos spawnerPos = BlockPos.ZERO;
    private long endGameTime;
    @Nullable
    private ResourceLocation spawnerBlockId;
    @Nullable
    private ResourceLocation structureId;
    @Nullable
    private ResourceLocation bossTypeId;
    /** 首 tick 征服校验通过后置 true 并持久化 */
    private boolean verified;
    private int retryCooldown;
    private boolean readyShown;
    /** 文本上已显示的剩余秒数（-1 表示未显示），避免每 tick 无意义地同步文本 */
    private long shownSeconds = -1L;
    /** 攻击判定箱（Interaction）；不持久化，区块重载后由 ensureAnchor 按标签邻近查找重建 */
    @Nullable
    private Interaction anchor;

    public BossRespawnMarker(EntityType<? extends BossRespawnMarker> type, Level level) {
        super(type, level);
        this.setInvulnerable(true);
    }

    /** 由死亡挂钩调用：在刷怪笼原位上方生成倒计时标记（文本在首 tick 校验通过后才出现）。 */
    public BossRespawnMarker(Level level, BlockPos spawnerPos, Block spawnerBlock, long endGameTime,
                             @Nullable ResourceKey<Structure> structure, @Nullable EntityType<?> bossType) {
        this(TwilightRegen.BOSS_RESPAWN_MARKER.get(), level);
        this.spawnerPos = spawnerPos.immutable();
        this.spawnerBlockId = BuiltInRegistries.BLOCK.getKey(spawnerBlock);
        this.structureId = structure != null ? structure.location() : null;
        this.bossTypeId = bossType != null ? BuiltInRegistries.ENTITY_TYPE.getKey(bossType) : null;
        this.endGameTime = endGameTime;
        this.setBillboardConstraints(Display.BillboardConstraints.VERTICAL);
        this.moveTo(spawnerPos.getX() + 0.5D, spawnerPos.getY() + 1.2D, spawnerPos.getZ() + 0.5D);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(this.level() instanceof ServerLevel server)) return;

        if (!this.verified) {
            if (!this.isStructureConquered(server)) {
                // 不是"真死"（分身/未启用战利品规则等），或 postmortem 没能定位结构
                this.discard();
                return;
            }
            this.verified = true;
            TwilightRegen.LOGGER.info("{} defeated; spawner respawn scheduled at {} ({}s)",
                this.bossTypeId, this.spawnerPos, Math.max(0L, (this.endGameTime - server.getGameTime()) / 20L));
        }

        this.ensureAnchor(server);

        long remainingTicks = this.endGameTime - server.getGameTime();
        if (remainingTicks > 0L) {
            long seconds = (remainingTicks + 19L) / 20L;
            if (seconds != this.shownSeconds) {
                this.shownSeconds = seconds;
                this.setText(Component.translatable("twilightregen.boss_respawn.countdown",
                    this.bossName(), formatTime(seconds)));
            }
            return;
        }

        // 到点：先换成"即将重生"，若落笼位置被占则保持文本并稍后重试
        if (!this.readyShown) {
            this.readyShown = true;
            this.setText(Component.translatable("twilightregen.boss_respawn.ready", this.bossName()));
        }
        if (this.retryCooldown > 0) {
            this.retryCooldown--;
            return;
        }
        if (this.tryRespawn(server)) {
            this.discard();
        } else {
            this.retryCooldown = RETRY_INTERVAL;
        }
    }

    /**
     * 保证攻击判定箱存在：Display 自身没有判定箱、不可被攻击（TF 给 WIP 文本也是这么绕的），
     * 所以另挂一个 {@link Interaction}。字段为空或实体已被移除（区块重载丢字段、箱被 /kill）时，
     * 先在刷怪笼附近按标签找现有的箱复用，找不到才新建——可自愈。
     * config 的 {@code bossRespawnMarkerBreakable} 关闭时不挂箱，并收掉已挂的（文本变为不可打断）。
     */
    private void ensureAnchor(ServerLevel server) {
        if (!Config.BOSS_RESPAWN_MARKER_BREAKABLE.get()) {
            // 选项关闭：不建判定箱；运行时切回关闭时收掉已挂的箱
            if (this.anchor != null) {
                this.anchor.discard();
                this.anchor = null;
            }
            return;
        }
        if (this.anchor != null && !this.anchor.isRemoved()) return;
        this.anchor = server.getEntitiesOfClass(Interaction.class, new AABB(this.spawnerPos).inflate(4.0D),
                i -> i.getTags().contains(INTERACTION_TAG)).stream().findFirst().orElseGet(() -> {
            Interaction interaction = new Interaction(EntityType.INTERACTION, server);
            interaction.addTag(INTERACTION_TAG);
            interaction.setWidth(ANCHOR_WIDTH);
            interaction.setHeight(ANCHOR_HEIGHT);
            // 判定箱 = 文本悬浮的那一格（刷怪笼正上方，Interaction 以实体位置为底面中心向上展开）。
            // 不能放低到刷怪笼那格：玩家站在下方死亡宝箱前开箱时眼睛就在那一格里，右键会被吞掉
            interaction.setPos(this.spawnerPos.getX() + 0.5D, this.spawnerPos.getY() + 1.0D, this.spawnerPos.getZ() + 0.5D);
            server.addFreshEntity(interaction);
            return interaction;
        });
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        super.remove(reason);
        // 标记消失（正常回填 / 首 tick 校验失败 / 被打掉 / /kill）→ 判定箱一并清掉；
        // 若是区块卸载引发的移除，箱也随区块存档，下次加载由 ensureAnchor 重新认领
        if (this.anchor != null) {
            this.anchor.discard();
            this.anchor = null;
        }
    }

    /** 结构此刻应已被 postmortem 标记征服，否则这次死亡不该开倒计时。 */
    private boolean isStructureConquered(ServerLevel server) {
        ResourceKey<Structure> key = this.structureKey();
        if (key == null) return false;
        return LandmarkUtil.locateNearestLandmarkStart(server, key, this.spawnerPos)
            .filter(start -> start instanceof TFStructureStart)
            .map(start -> ((TFStructureStart) start).isConquered())
            .orElse(false);
    }

    /** 把刷怪笼放回原位并清除征服标记；位置被占用时返回 false（保留标记稍后重试）。 */
    private boolean tryRespawn(ServerLevel server) {
        BlockPos pos = this.spawnerPos;
        if (!server.isLoaded(pos)) return false;

        Block spawner = this.spawnerBlockId != null ? BuiltInRegistries.BLOCK.get(this.spawnerBlockId) : Blocks.AIR;
        if (spawner == Blocks.AIR) {
            TwilightRegen.LOGGER.warn("Boss respawn marker at {} has unknown spawner block {}, discarding", pos, this.spawnerBlockId);
            return true;
        }

        BlockState state = server.getBlockState(pos);
        if (state.getBlock() != spawner) {
            if (!state.canBeReplaced()) return false; // 玩家在原位放了别的方块，等下次重试
            server.setBlockAndUpdate(pos, spawner.defaultBlockState());
        }
        this.clearConquered(server);
        TwilightRegen.LOGGER.info("Boss spawner {} restored at {}", this.spawnerBlockId, pos);
        return true;
    }

    /** 结构重新变为未征服：魔法地图叉号消失，结构内自然刷怪抑制解除。 */
    private void clearConquered(ServerLevel server) {
        ResourceKey<Structure> key = this.structureKey();
        if (key == null) return;
        LandmarkUtil.locateNearestLandmarkStart(server, key, this.spawnerPos)
            .filter(start -> start instanceof TFStructureStart)
            .map(start -> (TFStructureStart) start)
            .ifPresent(start -> start.setConquered(false, server));
    }

    @Nullable
    private ResourceKey<Structure> structureKey() {
        return this.structureId != null ? ResourceKey.create(Registries.STRUCTURE, this.structureId) : null;
    }

    private Component bossName() {
        return this.bossTypeId != null && BuiltInRegistries.ENTITY_TYPE.containsKey(this.bossTypeId)
            ? BuiltInRegistries.ENTITY_TYPE.get(this.bossTypeId).getDescription()
            : Component.empty();
    }

    private static String formatTime(long totalSeconds) {
        return String.format("%d:%02d", totalSeconds / 60L, totalSeconds % 60L);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putLong(TAG_SPAWNER_POS, this.spawnerPos.asLong());
        tag.putLong(TAG_END_GAME_TIME, this.endGameTime);
        tag.putBoolean(TAG_VERIFIED, this.verified);
        if (this.spawnerBlockId != null) tag.putString(TAG_SPAWNER_BLOCK, this.spawnerBlockId.toString());
        if (this.structureId != null) tag.putString(TAG_STRUCTURE, this.structureId.toString());
        if (this.bossTypeId != null) tag.putString(TAG_BOSS_TYPE, this.bossTypeId.toString());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.spawnerPos = BlockPos.of(tag.getLong(TAG_SPAWNER_POS));
        this.endGameTime = tag.getLong(TAG_END_GAME_TIME);
        this.verified = tag.getBoolean(TAG_VERIFIED);
        this.spawnerBlockId = ResourceLocation.tryParse(tag.getString(TAG_SPAWNER_BLOCK));
        this.structureId = tag.contains(TAG_STRUCTURE) ? ResourceLocation.tryParse(tag.getString(TAG_STRUCTURE)) : null;
        this.bossTypeId = tag.contains(TAG_BOSS_TYPE) ? ResourceLocation.tryParse(tag.getString(TAG_BOSS_TYPE)) : null;
    }
}
