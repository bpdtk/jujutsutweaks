package com.jujutsutweaks.command;

import com.jujutsutweaks.JujutsuTweaks;
import com.jujutsutweaks.config.OptimizerConfig;
import com.jujutsutweaks.util.FreezeStats;
import com.jujutsutweaks.util.StorageDimension;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * /jujutsutweaks status —— 用来确认补丁真的在工作。
 */
@Mod.EventBusSubscriber(modid = JujutsuTweaks.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class OptimizerCommand {

    private OptimizerCommand() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("jujutsutweaks")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("status").executes(OptimizerCommand::status)));
    }

    private static int status(CommandContext<CommandSourceStack> ctx) {
        ServerLevel dim = StorageDimension.resolve(ctx.getSource().getServer());

        Component freezeState = Component.translatable(
                OptimizerConfig.freezeEnabled ? "jujutsutweaks.state.on" : "jujutsutweaks.state.off");
        Component dimState = Component.translatable(
                dim != null ? "jujutsutweaks.state.loaded" : "jujutsutweaks.state.unloaded");

        ctx.getSource().sendSuccess(() -> Component.translatable("jujutsutweaks.status.line",
                freezeState,
                FreezeStats.lastTickFrozen(),
                FreezeStats.totalSkipped(),
                dimState), false);
        return 1;
    }
}
