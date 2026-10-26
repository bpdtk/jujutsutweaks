package com.jujutsutweaks.mixin;

import com.jujutsutweaks.config.OptimizerConfig;
import com.jujutsutweaks.util.FreezeStats;
import com.jujutsutweaks.util.StorageDimension;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 核心注入点：让储物维度内的实体不再 tick。
 *
 * <p>为什么选 {@code ServerLevel#tickNonPassenger}：
 * <ul>
 *   <li>它是服务端每个实体每 tick 的<b>唯一入口</b>，在这里取消等于实体完全静止，
 *       连原版 {@code LivingEntity.tick()} 里的物理、碰撞、药水、消失检查全部省掉；</li>
 *   <li>JujutsuCraft 的 {@code EntityActiveProcedure} 挂在 {@code LivingTickEvent} 上，
 *       事件是在 {@code LivingEntity.tick()} 内部触发的，所以取消实体 tick 同时也把它挡掉了；</li>
 *   <li>相比注入 {@code Entity#tick}（几乎所有性能模组都挂那里），
 *       {@code tickNonPassenger} 冲突面小得多。</li>
 * </ul>
 *
 * <p>为什么安全：被冻结的实体仍然完整存在于 {@code EntityLookup} 中，
 * {@code level.getEntities(...)} 照样能查到它们，所以主体模组的
 * 「取出/释放/治疗」逻辑（Test1Procedure / Test2Procedure / HealCurseSpiritManipulationProcedure）
 * 全部不受影响；释放时 {@code teleportTo} 把它们送出维度，下一 tick 就恢复正常。
 *
 * <p>不写入任何存档数据 —— 冻结是纯运行时行为，移除本模组后实体立刻恢复 tick。
 */
@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {

    @Inject(method = "tickNonPassenger", at = @At("HEAD"), cancellable = true)
    private void jujutsutweaks$freezeStoredCursedSpirit(Entity entity, CallbackInfo ci) {
        if (!OptimizerConfig.freezeEnabled) {
            return;
        }
        // 玩家永远不冻结（玩家用命令进入该维度时仍可正常活动）
        if (entity instanceof Player) {
            return;
        }
        if (!StorageDimension.isStorage((ServerLevel) (Object) this)) {
            return;
        }
        FreezeStats.recordSkip();
        ci.cancel();
    }
}
