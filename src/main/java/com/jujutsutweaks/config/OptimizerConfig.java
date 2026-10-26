package com.jujutsutweaks.config;

import com.jujutsutweaks.JujutsuTweaks;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

/**
 * 配置项。
 *
 * <p>热路径（每 tick 每个实体）不做 try/catch，而是把配置值同步到两个 volatile 字段，
 * 注入点只读字段，代价接近一次普通读。
 */
@Mod.EventBusSubscriber(modid = JujutsuTweaks.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class OptimizerConfig {

    public static final ForgeConfigSpec SPEC;
    private static final ForgeConfigSpec.BooleanValue FREEZE;
    private static final ForgeConfigSpec.BooleanValue LOG_SUMMARY;

    /** 热路径读取：是否冻结储物维度内实体的 tick。 */
    public static volatile boolean freezeEnabled = true;
    /** 是否每 30 秒输出一次统计日志。 */
    public static volatile boolean logSummary = false;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment(
                "JujutsuTweaks",
                "咒灵操术会把收服的咒灵以真实体形式存放在专属维度里，",
                "这些实体所在区块被永久 forceload，实体数量一多就会持续吃掉 tick 预算。",
                "本模组让该维度内的实体不再 tick（不写入存档，移除模组即恢复）。"
        ).push("storage");

        FREEZE = builder
                .comment(
                        "冻结储物维度内实体的 tick。",
                        "关闭后本模组不产生任何效果，可用来排查兼容问题。"
                )
                .define("freezeStoredSpirits", true);

        LOG_SUMMARY = builder
                .comment("每 30 秒在服务端日志输出一次冻结统计。")
                .define("logSummary", false);

        builder.pop();
        SPEC = builder.build();
    }

    private OptimizerConfig() {
    }

    @SubscribeEvent
    public static void onLoad(ModConfigEvent.Loading event) {
        // 注意：ModConfigEvent 会对**所有模组**的配置广播，必须先过滤。
        // 否则别的模组的配置先加载时会去读我们尚未载入的 spec，直接抛 IllegalStateException。
        if (!isOurs(event)) {
            return;
        }
        sync();
    }

    @SubscribeEvent
    public static void onReload(ModConfigEvent.Reloading event) {
        if (!isOurs(event)) {
            return;
        }
        sync();
    }

    private static boolean isOurs(ModConfigEvent event) {
        return JujutsuTweaks.MODID.equals(event.getConfig().getModId());
    }

    private static void sync() {
        freezeEnabled = FREEZE.get();
        logSummary = LOG_SUMMARY.get();
        JujutsuTweaks.LOGGER.info("[JujutsuTweaks] freezeStoredSpirits={}, logSummary={}",
                freezeEnabled, logSummary);
    }
}
