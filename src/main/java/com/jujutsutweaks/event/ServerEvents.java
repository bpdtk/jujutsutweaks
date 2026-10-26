package com.jujutsutweaks.event;

import com.jujutsutweaks.JujutsuTweaks;
import com.jujutsutweaks.config.OptimizerConfig;
import com.jujutsutweaks.util.FreezeStats;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = JujutsuTweaks.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ServerEvents {

    private static int logTimer;

    private ServerEvents() {
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        FreezeStats.endTick();

        if (OptimizerConfig.logSummary && ++logTimer >= 600) {
            logTimer = 0;
            JujutsuTweaks.LOGGER.info(
                    "[JujutsuTweaks] frozen {} entities last tick, {} entity-ticks skipped in total",
                    FreezeStats.lastTickFrozen(), FreezeStats.totalSkipped());
        }
    }
}
