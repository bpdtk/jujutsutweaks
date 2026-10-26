# 咒灵操术（CT18）跨版本 diff 报告 —— v48 / v49 / v49.1 / v50 / v50.1 / v51

> 目的：在动手做「咒灵维度大量存储导致卡顿」的优化前，先判定 **一份实现能否覆盖多个主体版本**。
> 判定依据：字节码哈希 + 反编译源码逐行 diff + 方法签名比对，三者互相印证。
> 分析日期：2026-10-27

---

## 一、结论先行

**6 个版本按「咒灵操术」代码可以分成 3 组，而不是 6 个独立版本：**

| 分组 | 版本 | 热点路径状态 | 能否共用一份实现 |
|---|---|---|---|
| **A 组（推荐范围）** | v49 · v49.1 · v50 · v50.1 · v51 | 存储热点路径**逐字节相同** | ✅ 可以 |
| **B 组（孤立）** | v48 | 作者自己在 v49 做过一轮实体扫描优化，热点实现不同 | ⚠️ 需独立分支 |
| **C 组（等价）** | v50 与 v50.1 | 除一个类被改名外**完全相同** | ✅ 视为同一版 |

**一句话：v49 是作者自己的「性能分水岭」。** v48→v49 他把 10 处实体扫描里的
`.stream().sorted(Comparator.comparingDouble(...)).toList()` 全部删掉了，并把
`EntityActiveProcedure` 从 667 行重构成 377 行。所以我们面对的其实是「v48 之前」和「v49 之后」两套存储实现，而不是六个版本。

**按你的规则（有 diff 就不纳入优化范围）→ 建议范围定为 v49 / v49.1 / v50 / v50.1 / v51，排除 v48。**

---

## 二、方法与可复现性

### 2.1 拆版本

各版本 jar 的类名**没有混淆**（MCreator 导出保留 `net.mcreator.jujutsucraft.*` 完整包名），只是被 reobf 成 SRG 名。所以可以直接按类名定位。

```
主体版本/JujutsuCraft-ver{48,49,49.1,50,50.1,51}-forge-1.20.1.jar
  → .analysis/v{版本}/net/mcreator/**            （全量类文件，每版 3469–3611 个）
  → .analysis/subset/v{版本}/**                   （只抽咒灵操术相关的 19 个类）
  → .analysis/src/v{版本}/**                      （Vineflower 反编译源码）
  → .analysis/sig/v{版本}.txt                     （javap -p 方法签名）
```

工具链（全部离线，复用你已有的）：`mdk3/tools/vineflower.jar`、`mdk3/tools/cfr.jar`、JDK 17。

### 2.2 定位「咒灵操术」代码

`咒灵操术` 在语言文件里不是一个技能条目，而是：

```json
"biome.jujutsucraft.cursed_spirit_manipulation_biome": "咒灵操术"
"advancements.skill_geto.title": "咒灵操术"
```

真正对应的是 **`jujutsucraft:cursed_spirit_manipulation_dimension` 这个储物维度**。所以定位方式是**扫常量池**：找出所有引用了 `cursed_spirit_manipulation_dimension` / `cursed_spirit_manipulation_biome` 字符串的类。这个办法与版本无关，不依赖 mdk3 里已被你改过的源码。

结果：**6 个版本都是 20 个类（19 个顶层 + 1 个内部类），集合完全一致。**

---

## 三、咒灵操术代码地图

| 类 | 职责 | 挂载点 | v49–v51 稳定性 |
|---|---|---|---|
| **Test1Procedure** | **收服存入**：右键实体 → 分配 y 地址 → 加 `NoAI/Invulnerable/PersistenceRequired` → `tp` 到 `(0,y,0)` → 发咒灵球 | `EntityInteract` | 一致 |
| **Test2Procedure** | **取出释放**：从 `(0,y,0)` 查实体 → 传回玩家身边 | 内部调用 | 一致 |
| **HealCurseSpiritManipulationProcedure** | 玩家 tick 每 100t 对维度内 **256³ 范围**扫描治疗 | `PlayerTickEvent END` | 一致 |
| **EntityActiveProcedure** | 全局 `LivingTickEvent`：目标/战斗/魔虚罗适应。维度内实体跳过目标逻辑 | `LivingTickEvent` | 存储分支一致（v51 另有 25 行无关改动） |
| GetoHappySetProcedure | 咒灵操术主流程（夏油杰/羂索），含 `tp @s 0 y 0` | 内部调用 | 一致 |
| GetoCancelTechniqueProcedure | 解除术式时把咒灵收回维度 | 内部调用 | 一致 |
| SelectGetoProcedure | 选择咒灵（含 `forceload add`） | 内部调用 | **6 版全同** |
| EntityDieProcedure | 死亡时回收/归属转移 | `LivingDeathEvent` | 存储分支一致（其余改动是魔虚罗头盔） |
| WhenRespawnProcedure | 玩家重生恢复咒灵 | `PlayerRespawnEvent` | 一致（v51 仅随机上限 47→48） |
| KeyChangeTechniqueOnKeyPressed2Procedure | 按键切换术式 | 内部调用 | **6 版全同** |
| SkillUzumakiProcedure | 漩涡（消耗咒灵） | 内部调用 | 一致 |
| CursedSpiritBallFoodEatenProcedure | 吃咒灵球 | 内部调用 | 一致 |
| AICursedSpirit15 / 23 | 特定咒灵 AI（维度内整段跳过） | 实体 tick | 15 全同；23 在 v50 有改动（见 §5.4） |
| AICursedSpiritsFlyingAttack / 2 | 飞行咒灵 AI | 实体 tick | 一致 |
| AIRikaProcedure / AITamamonoMaeProcedure | 里香 / 玉犬 AI | 实体 tick | 一致 |
| WhenEntityAttacked1Procedure | 受击处理 | `LivingAttackEvent` | **改过两次名**（见 §5.5） |
| CursedSpiritManipulationDimensionDimension | **仅客户端雾效**，不参与存储 | `RegisterDimensionSpecialEffectsEvent` | 一致 |

**维度配置文件（`data/jujutsucraft/dimension{,_type}/`、`worldgen/biome/`）在 6 个版本中逐字节完全相同**，哈希一致，说明储物维度的物理规则从未变过：

- `dimension_type`：`natural:false`、`has_skylight:true`、`min_y:-64`、`height:384`、`coordinate_scale:1`
- `biome`：`spawners` 全部为空列表（不刷怪）、无 features
- 生成器是**完整的主世界噪声生成器**（带 aquifer / ore_vein），不是超平坦空维度

---

## 四、卡顿根因（优化要打的靶子）

从代码看，卡顿不是单一原因，是四层叠加：

### 4.1 储物区块被**永久** force-load

`forceload add` 出现在 11 个类里：

```
AICursedSpiritsFlyingAttack2Procedure / AICursedSpiritsFlyingAttackProcedure /
CursedSpiritBallFoodEatenProcedure / EntityDieProcedure / GetoCancelTechniqueProcedure /
GetoHappySetProcedure / SelectGetoProcedure / SkillUzumakiProcedure /
Test1Procedure / Test2Procedure / WhenRespawnProcedure
```

**6 个版本全项目扫描（每版 3469–3611 个类，逐字节搜字符串），没有一处 `forceload remove`。** 也就是说第一次收服之后，维度里 `(0,0)` 这个区块**永久常驻**，哪怕没有玩家在维度里、哪怕玩家下线。所有存放的咒灵每 tick 都在跑。

### 4.2 咒灵是**真实体**，不是数据

储物地址就是 y 坐标：`y_pos = 64 + random * 64`，实体被 `tp` 到 `(0, y, 0)`。

收服时下发的命令是：

```
data merge entity @s {Invulnerable:1b, PersistenceRequired:1b, NoAI:1b}
execute in jujutsucraft:cursed_spirit_manipulation_dimension run tp @s 0 y 0
```

`NoAI:1b` 只是跳过 `Mob.aiStep` 里的 goal 选择，**`LivingEntity.tick` 照跑**（重力、碰撞、药水、着火、窒息、消失检查、同步）。几百个实体挤在**同一个区块列**（x=0,z=0，y 跨 64–128 共 4 个 section），实体碰撞与推送的 broadphase 会在同一 section 内互相扫，实体数一多就是近似 O(n²) 的邻居查询。

### 4.3 全局 tick 处理器对每个存储实体都跑一遍

`EntityActiveProcedure` 挂在 `LivingTickEvent` 上——**服务器上每一个活体实体每 tick 都进一次**，包括维度里那几百个。进入后立刻做：

- `entity.m_6095_().m_204039_(TagKey<EntityType> "forge:ranged_ammo")` —— 每 tick 一次实体类型标签查询
- `entity.getPersistentData().m_128459_("cnt_target")` + 写回 `m_128347_("cnt_target", 0.0)` —— 每 tick 对每个存储实体**写一次 NBT**
- `entity.f_19797_ % 11 == 0` 分支里读 `MapVariables.get(world).BlastGame`

维度判断（`!= ResourceKey...cursed_spirit_manipulation_dimension`）只挡掉了**目标锁定**那一段，前面的 NBT 读写挡不住。

### 4.4 每 5 秒一次 256³ 全量扫描

`HealCurseSpiritManipulationProcedure`（玩家 tick，CT18 玩家每 100 tick）：

```java
Vec3 _center = new Vec3(0.0, 128.0, 0.0);
for (Entity e : level.getEntities(Entity.class, new AABB(_center,_center).expandTowards(128.0), x -> true))
```

半径 128 → 256×256×256 的 AABB → 覆盖 16×16×16 = **4096 个 chunk section**，每个玩家的 section 实体表都要过一遍，只为给咒灵回血。

### 4.5 顺带记录：v48 比 v49 多出来的开销

v48 在上述 10 处实体扫描上都多了：

```java
.getEntities(...).stream().sorted(Comparator.comparingDouble(e -> e.distanceToSqr(_center))).toList()
```

即每次扫描都要**装箱 + 分配 List + 排序**，只为让"第一个匹配"变成"最近的那个"。v49 作者直接删掉了，改为按原始顺序取第一个匹配。**这是作者本人已经认可的优化方向，我们的方案不要把它加回去。**

---

## 五、逐版本 diff 明细

### 5.1 方法签名：6 个版本全一致

19 个类的**所有声明方法**（构造器、`execute` 两个重载、`@SubscribeEvent` 回调）签名**在 6 个版本中完全相同**。

需要排除的两类「假差异」：

1. **被删掉的 lambda**（`lambda$execute$1` 这类合成方法）——v48 的排序删掉后留下的残留，mixin 不会碰；
2. **匿名内部类** `EntityActiveProcedure$1 / $2 / $3`——v48 用的是 MCreator 的 `checkGamemode(Entity)` / `apply(LevelAccessor, String)` 匿名对象，v49+ 换成了 `convert(String)` 解析器。这些是编译期合成类，**不是 mixin 目标**。

除这两类之外，**0 个类签名不一致**。

→ **这意味着：以方法名为锚点的 mixin，6 个版本都能挂上，不会因为签名变化而注入失败。**

### 5.2 改动量总表（源码行级，`+`/`-` 行数）

| 类 | 48→49 | 49→49.1 | 49.1→50 | 50→50.1 | 50.1→51 |
|---|---:|---:|---:|---:|---:|
| AICursedSpirit15Procedure | 0 | 0 | 0 | 0 | 0 |
| AICursedSpirit23Procedure | 15 | 0 | **74** | 0 | 0 |
| AICursedSpiritsFlyingAttack2Procedure | 15 | 0 | 0 | 0 | 0 |
| AICursedSpiritsFlyingAttackProcedure | 21 | 0 | 0 | 0 | 0 |
| AIRikaProcedure | 15 | 0 | 0 | 0 | 0 |
| AITamamonoMaeProcedure | 16 | 0 | 0 | 0 | 0 |
| CursedSpiritBallFoodEatenProcedure | 11 | 0 | 0 | 0 | 0 |
| EntityActiveProcedure | **752** | 0 | 0 | 0 | 25 |
| EntityDieProcedure | 45 | 17 | 65 | 0 | 0 |
| GetoCancelTechniqueProcedure | 11 | 0 | 0 | 0 | 0 |
| GetoHappySetProcedure | 11 | 0 | 0 | 0 | 0 |
| HealCurseSpiritManipulationProcedure | 6 | 0 | 0 | 0 | 0 |
| KeyChangeTechniqueOnKeyPressed2Procedure | 26 | 0 | 0 | 0 | 0 |
| SelectGetoProcedure | 0 | 0 | 0 | 0 | 0 |
| SkillUzumakiProcedure | 16 | 0 | 0 | 0 | 0 |
| Test1Procedure | 6 | 0 | 0 | 0 | 0 |
| Test2Procedure | 6 | 0 | 0 | 0 | 0 |
| WhenEntityAttacked1Procedure | 改名 | — | — | 改名 | 5 |
| WhenRespawnProcedure | **241** | 1 | 0 | 0 | 2 |

### 5.3 关键差异解读

**（1）48→49：作者的性能轮**
- 10 个类删掉 `.stream().sorted(...).toList()`
- `EntityActiveProcedure` 667 → 377 行，`WhenRespawnProcedure` 409 → 308 行，整体重构
- 其余类只是同步这个小改动（6–26 行）
- **性质：重构 + 优化，存储语义未变**

**（2）49→49.1：几乎零改动**
- `EntityDieProcedure` 17 行、`WhenRespawnProcedure` 1 行，都是魔虚罗/随机数相关的边角

**（3）49.1→50：两处实体改动，都不在存储路径**
- `AICursedSpirit23Procedure` 74 行：删掉了 `data merge entity @s {NoAI:0b}`，换成 `BathynomusGiganteusEntity` 的专属分支。**注意：这段代码整体被 `!= 储物维度` 包裹，只在咒灵"在维度外"时执行，不影响存储态实体的 tick 开销。**
- `EntityDieProcedure` 65 行：魔虚罗头盔 NBT 清理（`DATA1..800` 循环 → `DistributionItem` 判断）

**（4）50→50.1：纯改名**
- `WhenEntityTakesProcedure` → `WhenEntityAttacked1Procedure`，**只改了类名一行**，其余字节级等价

**（5）50.1→51：三处，全部与存储无关**
- `EntityActiveProcedure` 25 行：魔虚罗适应阈值 `>=6` → `>=5`、`skill4815` 加成、新增「目标是自己的话重置目标」
- `WhenEntityAttacked1Procedure` 5 行：`@SubscribeEvent` 加 `priority = EventPriority.LOWEST`
- `WhenRespawnProcedure` 2 行：随机术式上限 `47` → `48`

### 5.4 一个必须注意的坑：类被改过两次名

| 版本 | 类名 |
|---|---|
| v48 / v49 / v49.1 | `WhenEntityTakesDamageNoSourceProcedure` |
| v50 | `WhenEntityTakesProcedure` |
| v50.1 / v51 | `WhenEntityAttacked1Procedure` |

**同一个功能，三个名字。** 如果你的 mixin 要挂这个类，必须按版本准备三个 target，或者干脆绕开它。

（这个类不是存储热点，只是受击处理，建议直接不碰。）

### 5.5 「热点窗口」比对结果

我把每个类里围绕 `cursed_spirit_manipulation` / `forceload` / `getEntities` / `NoAI` 的代码窗口单独抽出来比（避免被无关改动干扰）：

- **v49 / v49.1 / v50 / v50.1 / v51 的存储窗口完全一致**
- v48 在 16 个类上与其余版本不同（就是 §5.3(1) 那轮）
- `AICursedSpirit23Procedure` 在 49.1→50 有窗口差异，但如 §5.3(3) 所述位于「维度外」分支

---

## 六、优化范围建议（利弊）

### 方案甲：只做 v51 —— ❌ 不建议
- 利：改动面最小，只需对齐一个版本
- 弊：v51 是 2026-10 的最新版，绝大多数存档还停在 v49/v50；且 v51 相对 v50.1 的改动**全在存储路径之外**，也就是说**为 v51 写的优化，逻辑上 100% 适用于 v49–v50.1**，只做 v51 等于白扔 4 个版本的用户

### 方案乙：v49 / v49.1 / v50 / v50.1 / v51 —— ✅ 推荐
- 利：一份实现覆盖 5 个版本；方法签名全一致，mixin 锚点统一；存储路径逐字节相同，行为一致性有硬证据
- 利：v50 与 v50.1 字节级等价（仅类名），实际只需验证 4 个构建
- 弊：`WhenEntityAttacked1Procedure` 三度改名，若必须挂它需做版本适配（建议不碰）
- 弊：`EntityDieProcedure` 在 49→49.1→50 有非存储改动，若要在它身上做 `@Inject` 需注意**别依赖行号/ordinal**，只用方法名 + 描述符

### 方案丙：连 v48 一起做 —— ⚠️ 代价明显
- 利：覆盖面最广
- 弊：v48 的 `EntityActiveProcedure` 是另一套结构（667 行 vs 377 行），`WhenRespawnProcedure` 也不同（409 vs 308），存储扫描还带排序。等于**维护第二套实现**
- 弊：按你定的规则「有 diff 就不纳入范围」，v48 正好命中

**建议：方案乙。v48 先排除，若后续真有 v48 用户反馈再单开分支。**

---

## 七、给下一步的接口提示

优化实现时，以下锚点在 v49–v51 **全部稳定可用**：

| 目标 | 类 | 方法描述符 |
|---|---|---|
| 拦截收服 | `Test1Procedure` | `private static void execute(Event, LevelAccessor, double, double, double, Entity, Entity)` |
| 拦截释放 | `Test2Procedure` | 同上 |
| 拦截全局 tick | `EntityActiveProcedure` | `public static void onEntityTick(LivingTickEvent)` |
| 拦截周期治疗 | `HealCurseSpiritManipulationProcedure` | `public static void onPlayerTick(PlayerTickEvent)` |
| 维度 ResourceKey | 常量字符串 `jujutsucraft:cursed_spirit_manipulation_dimension` | — |
| 存储标记 | NBT `CursedSpirit` / `select` / `OWNER_UUID` / `friend_num` / `friend_num_worker` / `data_cursed_spirit_manipulation<N>` / `_name` / `_num` | — |

（`Test1Procedure` 有两个 `execute` 重载，mixin 必须写完整描述符区分，不能只写方法名。）

---

## 附：产物位置

```
.analysis/v{48,49,49.1,50,50.1,51}/   全量解包类文件（每版 ~3500 类）
.analysis/subset/v*/                  咒灵操术相关 19 类
.analysis/src/v*/                     Vineflower 反编译源码（可直接读）
.analysis/sig/v*.txt                  javap 方法签名
```
