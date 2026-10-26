package com.jujutsutweaks.util;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/**
 * 储物维度识别。
 *
 * <p>只依赖维度 ID 字符串，不依赖主体模组的任何 Java 类 —— 这是本模组能跨版本通用的根本原因。
 * 维度 ID 在 JujutsuCraft v48 ~ v51 中完全一致（见 docs/01 报告）。
 */
public final class StorageDimension {

    /** jujutsucraft:cursed_spirit_manipulation_dimension */
    public static final ResourceLocation ID =
            new ResourceLocation("jujutsucraft", "cursed_spirit_manipulation_dimension");

    public static final ResourceKey<Level> KEY = ResourceKey.create(Registries.DIMENSION, ID);

    private static MinecraftServer cachedServer;
    private static ServerLevel cachedLevel;

    private StorageDimension() {
    }

    /**
     * 判断某个 ServerLevel 是否为咒灵储物维度。
     *
     * <p>用「缓存 ServerLevel 实例 + 引用比较」代替每次比较 ResourceLocation，
     * 这样热路径上只是一次引用判等。维度未加载时返回 false。
     */
    public static boolean isStorage(ServerLevel level) {
        MinecraftServer server = level.getServer();
        if (server == null) {
            return false;
        }
        if (cachedServer != server) {
            cachedServer = server;
            cachedLevel = server.getLevel(KEY);
        }
        return cachedLevel == level;
    }

    /** 当前缓存的储物维度（未加载时为 null），仅供状态命令使用。 */
    public static ServerLevel cached() {
        return cachedLevel;
    }

    /** 供状态命令主动探测一次维度是否存在。 */
    public static ServerLevel resolve(MinecraftServer server) {
        if (server == null) {
            return null;
        }
        if (cachedServer != server) {
            cachedServer = server;
            cachedLevel = server.getLevel(KEY);
        }
        return cachedLevel;
    }
}
