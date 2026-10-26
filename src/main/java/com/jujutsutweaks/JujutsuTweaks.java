package com.jujutsutweaks;

import com.jujutsutweaks.config.OptimizerConfig;
import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import org.slf4j.Logger;

/**
 * JujutsuTweaks —— 咒灵操术储物维度性能补丁。
 *
 * <p>设计原则（重要）：本模组<b>不引用、不 mixin 主体模组（JujutsuCraft）的任何类</b>，
 * 只挂原版 {@code ServerLevel} 并靠维度 ID 字符串识别储物维度。
 *
 * <p>这样做的原因：主体是 MCreator 生成的，每次重新打包即使功能没变，类名/签名也可能变动，
 * 任何直接依赖主体类的附属都会跟着失效。本模组只依赖「维度 ID 不变」这一个稳定事实，
 * 因此对主体重新打包免疫，也不会和其他附属（如 jujutsuslop 系）争抢同一个注入点。
 */
@Mod(JujutsuTweaks.MODID)
public class JujutsuTweaks {

    public static final String MODID = "jujutsutweaks";
    public static final Logger LOGGER = LogUtils.getLogger();

    public JujutsuTweaks() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, OptimizerConfig.SPEC);
        LOGGER.info("[JujutsuTweaks] loaded");
    }
}
