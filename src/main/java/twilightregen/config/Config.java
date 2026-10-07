package twilightregen.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class Config {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    /** 总开关：关闭后 boss 死亡不再安排刷怪笼重生（原版 TF 行为）。 */
    public static final ModConfigSpec.BooleanValue BOSS_RESPAWN_ENABLED = BUILDER.comment(
            "Whether defeated Twilight Forest bosses schedule a respawn (spawner block is restored",
            "after the delay below, clearing the structure's conquered mark).",
            "Boss 被击杀后是否安排重生：倒计时结束后把刷怪笼放回原位并清除结构征服标记（地图叉号消失）。",
            "关闭时保持原版暮色森林行为（boss 永久不重生）。"
        ).translation("config.twilightregen.bossRespawnEnabled")
        .define("bossRespawnEnabled", true);

    /** 死亡到刷怪笼回填的倒计时（秒，游戏时间；默认 300 = 5 分钟）。 */
    public static final ModConfigSpec.IntValue BOSS_RESPAWN_DELAY_SECONDS = BUILDER.comment(
            "Delay in seconds (in-game time) between a boss's death and its spawner being restored.",
            "从 boss 死亡到刷怪笼重新出现的倒计时（秒，按游戏时间计；300 = 5 分钟）。",
            "倒计时只在区块被加载时结算，不会强制加载区块。"
        ).translation("config.twilightregen.bossRespawnDelaySeconds")
        .defineInRange("bossRespawnDelaySeconds", 300, 1, 86400);

    /** 倒计时文本是否可被玩家打掉（打掉 = 永久取消这次重生）。 */
    public static final ModConfigSpec.BooleanValue BOSS_RESPAWN_MARKER_BREAKABLE = BUILDER.comment(
            "Whether the respawn countdown text can be destroyed by attacking it.",
            "Breaking it permanently cancels that respawn: the spawner is never restored and the",
            "structure stays conquered (the map X is kept). Disable to make the countdown uninterruptible.",
            "Boss 重生倒计时文本是否可被玩家打掉：打掉 = 永久取消这次重生",
            "（刷怪笼不回填、结构保持已征服、地图叉号保留）。关闭后倒计时无法被打断。"
        ).translation("config.twilightregen.bossRespawnMarkerBreakable")
        .define("bossRespawnMarkerBreakable", true);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private Config() {}
}
