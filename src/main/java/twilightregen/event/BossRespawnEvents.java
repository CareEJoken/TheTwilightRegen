package twilightregen.event;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Interaction;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import twilightforest.entity.boss.BaseTFBoss;
import twilightforest.entity.boss.Lich;
import twilightregen.TwilightRegen;
import twilightregen.config.Config;
import twilightregen.entity.BossRespawnMarker;

/**
 * Boss 死亡挂钩：TF 里刷怪笼刷出 boss 后会自毁，boss 死亡也不会补回，
 * 这里在 boss 死亡时生成倒计时标记，由 {@link BossRespawnMarker} 回填刷怪笼；
 * 同一批监听里还处理「倒计时文本被打掉」（{@link #onMarkerAttacked}）。
 *
 * <p><b>注意</b>：这里只做「轻量」判定（有刷怪笼归位点、不是 Lich 影子克隆、附近没有已有标记），
 * <b>不</b>在这里校验"结构已征服"——LivingDeathEvent 触发时 TF 的 postmortem 还没跑
 * （它在同一 die() 栈里稍后才执行），而 {@code server.execute(...)} 在服务端线程上会<b>同步执行</b>
 * （BlockableEventLoop.scheduleExecutables() == !isSameThread()），起不到延后一 tick 的作用。
 * 征服校验因此放进标记实体的首个 tick，见 {@link BossRespawnMarker#tick()}。</p>
 */
@EventBusSubscriber(modid = TwilightRegen.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class BossRespawnEvents {

    private BossRespawnEvents() {}

    @SubscribeEvent
    public static void onBossDeath(LivingDeathEvent event) {
        if (!Config.BOSS_RESPAWN_ENABLED.get()) return;
        if (!(event.getEntity() instanceof BaseTFBoss boss)) return;
        if (boss instanceof Lich lich && lich.isShadowClone()) return;
        if (!(boss.level() instanceof ServerLevel level)) return;

        GlobalPos home = boss.getRestrictionPoint();
        if (home == null || !home.dimension().equals(level.dimension())) return;

        BlockPos spawnerPos = home.pos();
        if (hasMarkerNear(level, spawnerPos)) return;

        long endGameTime = level.getGameTime() + Config.BOSS_RESPAWN_DELAY_SECONDS.get() * 20L;
        level.addFreshEntity(new BossRespawnMarker(level, spawnerPos, boss.getBossSpawner(), endGameTime,
            boss.getHomeStructure(), boss.getType()));
    }

    private static boolean hasMarkerNear(ServerLevel level, BlockPos pos) {
        return !level.getEntitiesOfClass(BossRespawnMarker.class, new AABB(pos).inflate(4.0D)).isEmpty();
    }

    /**
     * 倒计时文本"可被打掉"（对齐 TF 最终城堡 WIP 文本：左键打到 Interaction 判定箱 → 清掉文本），
     * 由 config 的 {@code bossRespawnMarkerBreakable} 开关（关闭时标记也不挂判定箱，此处再兜一层）。
     * 标记实体自身携带全部状态，这里 discard 掉它就等于把这次重生彻底取消——刷怪笼不回填、
     * 结构保持"已征服"（地图叉号保留），即"打掉后就不会 returns"。
     */
    @SubscribeEvent
    public static void onMarkerAttacked(AttackEntityEvent event) {
        if (!Config.BOSS_RESPAWN_MARKER_BREAKABLE.get()) return;
        if (!(event.getTarget() instanceof Interaction interaction)) return;
        if (!interaction.getTags().contains(BossRespawnMarker.INTERACTION_TAG)) return;
        if (!(interaction.level() instanceof ServerLevel level)) return;

        level.getEntities(interaction, interaction.getBoundingBox(), e -> e instanceof BossRespawnMarker)
            .forEach(marker -> {
                TwilightRegen.LOGGER.info("Boss respawn marker at {} knocked out; respawn cancelled", marker.blockPosition());
                marker.discard();
            });
        interaction.discard();
    }
}
