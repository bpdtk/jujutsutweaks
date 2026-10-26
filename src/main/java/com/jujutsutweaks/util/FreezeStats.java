package com.jujutsutweaks.util;

import java.util.concurrent.atomic.LongAdder;

/**
 * 冻结统计。只在服务端主线程累加，读取可以来自任意线程。
 */
public final class FreezeStats {

    private static final LongAdder totalSkipped = new LongAdder();

    private static int frozenThisTick;
    private static int lastTickFrozen;

    private FreezeStats() {
    }

    /** 由注入点在 HEAD 处调用。 */
    public static void recordSkip() {
        frozenThisTick++;
        totalSkipped.increment();
    }

    /** 每个服务端 tick 结束时调用，把本 tick 计数转成「上一 tick 计数」。 */
    public static void endTick() {
        lastTickFrozen = frozenThisTick;
        frozenThisTick = 0;
    }

    /** 上一完整 tick 被跳过的实体数量。 */
    public static int lastTickFrozen() {
        return lastTickFrozen;
    }

    /** 自服务器启动以来累计跳过的实体 tick 次数。 */
    public static long totalSkipped() {
        return totalSkipped.sum();
    }
}
