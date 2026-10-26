# JujutsuTweaks

> 咒术回战模组 [JujutsuCraft](https://www.curseforge.com/minecraft/mc-mods/jujutsucraft) 的性能附属。
> 针对**咒灵操术（夏油杰 / 羂索）**：当玩家在储物维度里堆积了大量咒灵时，消除由此产生的持续卡顿。
>
> A performance addon for JujutsuCraft. Stops stored cursed spirits from ticking while they sit
> inside the Cursed Spirit Manipulation storage dimension.

---

## 问题出在哪

JujutsuCraft 的咒灵操术会把收服的咒灵以**真实体**形式存放在专属维度
`jujutsucraft:cursed_spirit_manipulation_dimension` 里，坐标固定在 `(0, y, 0)`，y 就是储物格地址。

这些实体所在的区块被**永久 force-load**（整个模组里找不到任何一处 `forceload remove`），于是：

- 咒灵即使带 `NoAI:1b`，`LivingEntity.tick()` 照样跑 —— 物理、碰撞、药水、消失检查一个不少
- 几百个实体挤在同一个区块列（x=0, z=0），实体碰撞的邻居查询近似 O(n²)
- 主体挂在 `LivingTickEvent` 上的 `EntityActiveProcedure` 会对**每个存储实体每 tick** 做一次
  实体类型标签查询 + `cnt_target` 的 NBT 读写

存到几百个之后，主线程每 tick 的固定开销就压不住了。

## 怎么解决的

本模组**不修改、不注入主体模组的任何类**，只注入原版 `ServerLevel#tickNonPassenger` 的 HEAD：
储物维度内的非玩家实体直接跳过 tick。

这样做的好处：

| | 效果 |
|---|---|
| 实体依然存在 | 仍能被 `getEntities` 查到，取出 / 释放 / 治疗 / 漩涡全部照常 |
| 不写任何存档数据 | 删除本模组后立即恢复，零残留 |
| 不占用主体的注入点 | 与其他附属（JJSK、jujutsualm 等）无冲突 |
| 只依赖维度 ID 字符串 | 对主体用 MCreator 重新打包导致的签名变动免疫 |

## 兼容范围

| 主体版本 | 状态 |
|---|---|
| v49 / v49.1 / v50 / v50.1 / v51 | ✅ 支持 |
| v48 | ❌ 不纳入（存储实现不同） |

- Minecraft 1.20.1 / Forge 47.x
- 只需要装在**服务端**；客户端不装也能进服

## 安装

1. 装好 JujutsuCraft 及其前置（GeckoLib、PlayerAnimator）
2. 把 `jujutsutweaks-1.0.0.jar` 放进 `mods/`
3. 启动即可，无需额外配置

## 配置

首次启动后生成 `config/jujutsutweaks-common.toml`：

```toml
[storage]
    # 冻结储物维度内实体的 tick。关闭后本模组不产生任何效果，可用来排查兼容问题。
    freezeStoredSpirits = true

    # 每 30 秒在服务端日志输出一次冻结统计。
    logSummary = false
```

## 验证是否生效

游戏内（需要 OP / 权限等级 2）：

```
/jujutsutweaks status
```

输出示例：

```
冻结: 开 | 上一 tick 跳过实体: 137 | 累计跳过实体 tick: 412233 | 储物维度: 已加载
```

「上一 tick 跳过实体」就是当前维度里存着的咒灵数量。

## 从源码构建

```bash
./gradlew build
# 产物：build/libs/jujutsutweaks-1.0.0.jar
```

需要 JDK 17。首次构建需要联网拉取 Forge 与 Mixin 依赖。

## 声明

- 本项目为第三方附属，与 JujutsuCraft 作者无关。
- 仓库内**不包含**主体模组的任何代码、资源或 jar 文件。
- 实现细节与跨版本 diff 分析见 `docs/`。
