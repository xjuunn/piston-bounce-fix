# PistonBounceFix

## 1. 项目概述

开发一个针对 Minecraft 26.3 的 Fabric Server-side 修复 Mod，项目名称为：

**PistonBounceFix**

项目目标是修复 Minecraft 26.3 中出现的一个原版行为回归问题：

> 当活塞推动粘液块时，站在粘液块上的玩家无法正常被弹起，或者服务器与客户端对玩家弹射后的运动状态不同步。

该项目不是一个新的游戏机制 Mod，也不是 Slime Block 功能增强 Mod。

它的核心定位是：

**针对 Minecraft 26.3 原版 Piston + Slime Block + Player Movement/Velocity Synchronization 问题的最小侵入式兼容性修复。**

---

# 2. 项目初衷

Minecraft 的活塞、粘液块和蜂蜜块是大量红石机械、移动结构、小游戏、服务器机关和技术玩法的基础。

在 Minecraft 26.3 中，出现了与活塞、实体位置同步、玩家运动状态相关的一系列回归问题。

其中一个实际表现是：

```text
Piston
    ↓
Slime Block
    ↓
Player
    ↓
Player should be launched upward
```

但是在 Minecraft 26.3 中：

```text
Piston
    ↓
Slime Block moves
    ↓
Player is affected by the movement
    ↓
Player does not visibly / correctly bounce
```

即使使用：

- 原版 Minecraft 26.3 Server
- Fabric Loader
- Fabric Client
- 没有任何第三方 Mod

仍然可以复现该问题。

因此该问题不能简单归因于：

- Lithium
- Carpet
- Carpet TIS Addition
- SmoothChunk
- JourneyMap
- Voice Chat
- 其他 Fabric Mod

之前测试过的 `Player Stutter Fix` 也没有解决这个问题。

这说明问题更可能位于 Minecraft 26.3 本身的：

- Player movement
- Entity velocity
- Piston / Slime Block interaction
- Server-side entity tracking
- Client synchronization

相关路径，而不是普通的客户端实体插值问题。

---

# 3. 已知相关问题

开发过程中需要重点参考以下 Minecraft 问题。

## MC-89043

主题：

`Slime blocks moved by pistons often fail to bounce up the player`

该问题描述了：

> 活塞推动粘液块时，玩家有时无法正确被弹起。

历史讨论中已经出现过非常接近本项目目标的思路：

当玩家受到 Slime Block 推动并产生新的运动速度后，需要确保该速度变化能够正确进入 Entity Tracking / Client Synchronization 流程。

因此需要重点调查：

```text
Player velocity change
        ↓
Entity tracking
        ↓
Velocity synchronization
        ↓
Client
```

但是不要直接假设旧版本的实现方式仍然适用于 Minecraft 26.3。

必须首先检查 Minecraft 26.3 的实际源码、Mappings 和字节码，确认当前版本真正对应的方法和字段。

---

## MC-191256

主题：

`Pistons with Slime Blocks still bounce players inconsistently`

该问题同样说明：

Piston + Slime Block + Player Bounce 并不是一个简单的客户端视觉问题，而可能涉及玩家运动状态同步。

---

## Minecraft 26.3 相关同步问题

Minecraft 26.3 还存在其他与：

- Entity interpolation
- Player position synchronization
- Piston client desynchronization
- Player position updates

有关的问题。

例如：

- MC-295756
- MC-311727
- MC-311974
- MC-311976

这些问题不应直接等同于本项目要修复的问题。

它们只能作为调查 Minecraft 26.3 Entity/Player synchronization 架构的参考。

尤其需要避免错误地把：

```text
Player Stutter
```

和：

```text
Piston + Slime Player Bounce
```

当成同一个问题。

---

# 4. 当前问题的实际复现条件

目标环境：

```text
Minecraft 26.3
Fabric Loader
Fabric Server
Fabric Client
无其他 Mod
```

建立最简单的测试装置：

```text
Button
   ↓
Piston
   ↓
Slime Block
   ↓
Player
```

正常情况下：

```text
Piston extends
    ↓
Slime Block moves
    ↓
Player receives upward movement
    ↓
Player is launched
```

Minecraft 26.3 当前表现：

```text
Piston extends
    ↓
Slime Block moves
    ↓
Player does not correctly bounce
```

同时此前的服务器日志中还观察到了：

```text
Can't keep up!
Running 2597ms or 51 ticks behind

Can't keep up!
Running 19694ms or 393 ticks behind
```

因此测试时必须区分两个问题：

### 问题 A：Piston Bounce Regression

玩家无法被粘液块正常弹起。

### 问题 B：Server Tick Lag

服务器出现严重 Tick 延迟。

PistonBounceFix 的目标是：

**只解决问题 A。**

不能通过修改 Tick Rate、Thread、Scheduler、TPS 或人为增加运动速度来掩盖问题 A。

---

# 5. 项目目标

第一阶段只实现一个目标：

> 在 Minecraft 26.3 Fabric Server 中，让原版 Piston + Slime Block 对玩家的弹射行为恢复正常。

要求：

1. 不修改客户端。
2. 不要求客户端安装 Mod。
3. 不修改 Minecraft 网络协议。
4. 不修改客户端物理。
5. 不增加新的游戏机制。
6. 不修改正常的 Piston 行为。
7. 不修改正常的 Slime Block 行为。
8. 不修改普通玩家移动。
9. 不修改玩家手动设置的 velocity。
10. 不修改其他实体不相关的运动逻辑。
11. 尽可能只针对 Piston + Slime Block + Player 场景。
12. 保持原版行为作为基础，只修复必要的同步缺陷。

最终目标：

```text
PistonBounceFix
        ↓
Server-side Fabric Mod
        ↓
Minecraft 26.3
        ↓
修复 Piston + Slime Block + Player Bounce
```

---

# 6. 不应该采用的方案

不要简单实现以下方案：

## 6.1 不要每 Tick 强制设置玩家 velocity

错误方案：

```java
player.setVelocity(0, 1, 0);
```

或者：

```java
player.setVelocity(player.getVelocity().add(0, X, 0));
```

这种方式会改变游戏物理，而不是修复原版同步问题。

---

## 6.2 不要人为模拟 Slime Block

不要重新实现：

```text
Piston
Slime Block
Player Collision
Bounce Physics
```

整个物理系统。

Minecraft 本身已经有这些逻辑。

Mod 应该尽可能：

```text
保留 Vanilla Physics
        ↓
修复缺失的同步/通知
```

---

## 6.3 不要修改所有 Entity

不要使用一个全局：

```text
Entity → velocityChanged → force sync
```

方案。

这可能导致：

- 网络流量增加
- 其他实体行为变化
- 性能下降
- 玩家移动异常
- 与其他 Mod 冲突

应该限制到真正需要修复的场景。

---

## 6.4 不要修改客户端

当前第一目标是：

```text
Server-side only
```

如果服务器正确发送原版协议中的 Entity Velocity / Movement 信息，原版客户端应该能够正常工作。

如果经过源码分析后发现客户端也存在必须修复的 26.3 bug，才考虑 Client-side companion Mod。

默认情况下不要这么做。

---

# 7. 推荐调查路线

在编写任何 Mixin 之前，必须先调查 Minecraft 26.3 的真实实现。

不要根据旧版本 Minecraft 的源码直接猜测。

重点调查以下模块：

```text
PistonBlock
PistonHandler
SlimeBlock
Entity
LivingEntity
PlayerEntity
ServerPlayerEntity
ServerEntity
EntityTracker
EntityTrackerEntry
ClientboundSetEntityMotionPacket
Entity velocity synchronization
Entity movement synchronization
```

具体名称以 Minecraft 26.3 Fabric Yarn / Mojmap 实际映射为准。

---

# 8. 第一阶段：源码定位

需要回答以下问题。

### 问题 1

Minecraft 26.3 中：

```text
Slime Block
```

在什么方法中处理实体碰撞/弹射？

找到：

```text
SlimeBlock → Entity
```

的实际调用链。

---

### 问题 2

玩家被 Slime Block 弹起以后：

```text
Player velocity
```

在哪里被修改？

需要找到真正执行：

```text
velocity = ...
```

或者等价操作的方法。

---

### 问题 3

修改 velocity 后：

```text
Server
```

通过什么机制把 velocity 同步给客户端？

调查：

```text
ServerEntity
EntityTracker
EntityTrackerEntry
velocity packet
movement packet
```

---

### 问题 4

普通实体和玩家是否走不同的同步路径？

重点比较：

```text
Player
LivingEntity
Entity
```

为什么某些实体能够正确表现，而 Player 无法正确弹起。

---

### 问题 5

确认问题到底属于：

```text
A. Physics calculation
B. Player velocity update
C. Entity tracking
D. Velocity packet synchronization
E. Position synchronization
F. Client interpolation
G. Multiple issues combined
```

不能在调查之前直接假定答案一定是 D。

---

# 9. Mixin 设计原则

找到真正的问题后，再选择 Mixin 点。

优先级：

```text
最小范围
    ↓
最靠近 bug 原因
    ↓
最少修改原版行为
    ↓
最容易测试
```

例如，如果最终确认：

```text
Player velocity 已经正确计算
        ↓
但是没有触发 velocity synchronization
```

那么优先考虑：

```text
在 velocity 更新完成之后
通知 EntityTracker / velocity synchronization
```

而不是重新计算 velocity。

---

# 10. 可能的修复方向

一个候选方向是：

```text
Piston
    ↓
Slime Block moves
    ↓
Player velocity changed
    ↓
Mark velocity synchronization required
    ↓
ServerEntity sends velocity update
    ↓
Client receives vanilla velocity packet
    ↓
Player bounces normally
```

历史 MC-89043 中曾出现过类似：

```text
velocityChanged
```

的修复思路。

但是：

**Minecraft 26.3 必须重新验证该机制。**

不要机械地把旧版本：

```java
velocityChanged = true;
```

直接移植到新版本。

如果 26.3 已经移除了这个字段或修改了 Entity Tracking 机制，应寻找对应的新机制。

---

# 11. Server-side 优先

第一版必须尽可能保持：

```text
Server:
    PistonBounceFix

Client:
    Vanilla Minecraft
```

即：

```text
Client
    ↓
无需安装 Mod
    ↓
连接普通 Fabric / Vanilla Client
```

服务器发送的仍然是 Minecraft 原生协议。

这样可以最大程度保证兼容性。

---

# 12. 兼容性目标

第一阶段目标：

```text
Minecraft 26.3
Fabric Server
Vanilla Client
```

必须支持：

```text
Fabric Client
Vanilla Client
```

如果协议没有发生变化，则理论上不需要客户端 Mod。

同时不能要求：

```text
PistonBounceFix Client
```

作为服务器运行的必要条件。

---

# 13. 测试方案

必须建立一个最小化测试环境。

## Test 1：普通玩家

玩家正常行走：

```text
没有任何行为变化
```

---

## Test 2：普通 Piston

没有 Slime Block：

```text
Piston 正常
```

---

## Test 3：Slime Block + Piston + 非玩家实体

测试：

```text
Item
Boat
Minecart
Mob
```

确认 Mod 不会改变这些实体的正常行为。

---

## Test 4：Slime Block + Piston + Player

核心测试：

```text
Player standing on slime block
        ↓
Piston activates
        ↓
Player launches
```

必须恢复正常。

---

## Test 5：不同方向

测试：

```text
Up
Down
North
South
East
West
```

---

## Test 6：不同速度

测试：

```text
单个 piston
多个 piston
连续 piston
```

---

## Test 7：服务器 TPS 正常

确保测试服务器：

```text
20 TPS
```

附近运行。

不要在服务器严重 Lag 时判断 Mod 是否有效。

---

## Test 8：高延迟环境

模拟：

```text
50ms
100ms
200ms
```

确认玩家最终状态正确。

---

## Test 9：多人

```text
Player A
Player B
```

其中 A 被弹射，B 观察。

确认：

```text
A 自己看到正确运动
B 看到 A 正确运动
```

---

## Test 10：不安装客户端 Mod

最终必须测试：

```text
Server:
    PistonBounceFix

Client:
    Vanilla/Fabric
    无 PistonBounceFix
```

如果依然正确，则证明 server-side 修复成功。

---

# 14. 回归测试

修复不能破坏：

```text
普通玩家移动
跳跃
飞行
鞘翅
船
矿车
活塞
红石
粘液块
蜂蜜块
实体碰撞
```

特别注意：

```text
不要让每一次 velocity 修改都发送额外网络包。
```

只在真正相关的 Piston + Slime Player 场景触发修复。

---

# 15. 日志与 Debug

默认情况下不要打印大量日志。

提供一个可选 Debug 配置：

```properties
debug=false
```

开启以后可以输出：

```text
PistonBounceFix:
  player=<uuid>
  piston=<position>
  velocity=<x,y,z>
  synchronization=<triggered>
```

Debug 日志必须：

- 默认关闭
- 不刷屏
- 不输出敏感信息
- 不影响正常性能

---

# 16. 项目结构

推荐使用标准 Fabric Mod 结构：

```text
piston-bounce-fix/
├── gradle/
├── gradlew
├── gradlew.bat
├── build.gradle
├── gradle.properties
├── settings.gradle
├── LICENSE
├── README.md
├── CHANGELOG.md
│
└── src/
    ├── main/
    │   ├── java/
    │   │   └── ...
    │   │       └── pistonbouncefix/
    │   │           ├── PistonBounceFix.java
    │   │           ├── mixin/
    │   │           │   └── ...
    │   │           └── config/
    │   │               └── ...
    │   │
    │   └── resources/
    │       ├── fabric.mod.json
    │       └── pistonbouncefix.mixins.json
    │
    └── test/
        └── ...
```

不要过度工程化。

这是一个针对特定 Minecraft 版本的兼容性 Mod，不需要一开始引入复杂架构。

---

# 17. 版本策略

第一版只支持：

```text
Minecraft 26.3
```

不要为了兼容多个 Minecraft 版本而增加大量条件分支。

例如不要一开始实现：

```text
if MC >= 26.3
if MC >= 26.4
if MC >= 27
...
```

因为不同 Minecraft 版本的：

- mappings
- Entity API
- tracking
- piston logic

可能不同。

应该采用：

```text
PistonBounceFix 1.x
    ↓
Minecraft 26.3
```

以后：

```text
PistonBounceFix 2.x
    ↓
Minecraft 26.4
```

或者根据实际情况单独发布对应版本。

---

# 18. 构建目标

最终应该能够：

```bash
./gradlew build
```

生成：

```text
build/libs/piston-bounce-fix-<version>.jar
```

服务器安装：

```text
server/
└── mods/
    └── piston-bounce-fix-<version>.jar
```

客户端：

```text
无需安装
```

---

# 19. README 应该说明什么

README 必须明确：

### 项目是什么

```text
PistonBounceFix is a server-side Fabric compatibility fix for
the Minecraft 26.3 piston/slime-player bounce regression.
```

### 为什么存在

说明 Minecraft 26.3 中出现了原版行为回归。

### 修复什么

```text
Piston + Slime Block + Player bounce synchronization
```

### 不修复什么

明确说明：

```text
不是性能 Mod
不是红石优化 Mod
不是 Slime Block 增强 Mod
不是通用 Entity Physics Mod
不是客户端动画 Mod
```

### 安装

```text
Server:
    Install PistonBounceFix

Client:
    No mod required
```

### 版本

```text
Minecraft 26.3
Fabric Loader
```

---

# 20. 开发原则

整个项目必须遵守以下原则。

## 原则 1：先证明，再修改

不要根据猜测直接写 Mixin。

必须先：

```text
源码分析
    ↓
调用链确认
    ↓
复现
    ↓
确定 bug 点
    ↓
设计最小修改
    ↓
实现
```

---

## 原则 2：不要复制 Vanilla 代码

不要把 Minecraft 的整个 piston/entity 实现复制进 Mod。

应该使用：

```text
Mixin
```

对最小目标方法进行修改。

---

## 原则 3：不要改变游戏规则

目标是：

```text
Restore Vanilla intended behavior
```

而不是：

```text
Create a new behavior
```

---

## 原则 4：不要用性能优化掩盖 Bug

禁止通过：

```text
增加 TPS
修改 Tick
修改 Thread
降低 Entity Tick
修改网络频率
```

来假装解决问题。

---

## 原则 5：不要为了兼容而过度设计

这是一个针对 Minecraft 26.3 特定问题的修复项目。

优先：

```text
Small
Focused
Understandable
Testable
```

而不是：

```text
Large
Generic
Highly abstract
```

---

# 21. 最终成功标准

项目成功必须满足：

```text
Minecraft 26.3
+
Fabric Server
+
PistonBounceFix
+
Vanilla/Fabric Client
+
无客户端 PistonBounceFix
```

情况下：

```text
Player
  ↓
standing on Slime Block
  ↓
Piston activates
  ↓
Slime Block moves
  ↓
Player receives correct bounce velocity
  ↓
Player is launched
  ↓
Client sees correct movement
  ↓
Other players see correct movement
```

同时：

```text
普通 Piston
普通 Player Movement
普通 Slime Block
普通 Entity Movement
```

均保持原版行为。

---

# 22. 开发任务顺序

严格按照以下顺序执行：

### Phase 1：确认环境

- Minecraft 26.3
- Fabric Loader
- Fabric API
- Java 25+
- 建立最小测试服务器

### Phase 2：复现

- 创建最小 Slime + Piston launcher
- 确认 Vanilla/Fabric 26.3 可以稳定复现
- 确认服务器 TPS 正常

### Phase 3：源码调查

调查：

```text
Piston
SlimeBlock
Entity collision
Player velocity
Entity tracking
Velocity synchronization
Client movement
```

建立调用链。

### Phase 4：定位

确定问题属于：

```text
Physics
Velocity
Tracking
Synchronization
Position
Client interpolation
```

中的哪一层。

### Phase 5：设计

选择最小 Mixin 点。

### Phase 6：实现

实现第一个最小版本。

### Phase 7：验证

验证：

```text
Player bounce
Multiplayer
Different directions
Normal piston
Other entities
Vanilla client
```

### Phase 8：回归测试

确保没有改变正常 Minecraft 行为。

### Phase 9：发布

生成：

```text
piston-bounce-fix-x.x.x.jar
```

并提供：

- README
- Changelog
- Supported Minecraft version
- Installation instructions
- Known limitations

---

# 23. 最重要的开发要求

不要从“我要怎么写一个 Mixin”开始。

应该从：

```text
为什么 Minecraft 26.3 中玩家没有被正确弹起？
```

开始。

首先找到真实的 Minecraft 26.3 执行路径。

然后回答：

```text
玩家到底有没有获得正确 velocity？
```

如果：

```text
没有
```

修复物理/velocity 计算。

如果：

```text
有
```

继续检查：

```text
Server 是否知道 velocity 已经改变？
```

如果：

```text
不知道
```

修复 tracking notification。

如果：

```text
知道
```

继续检查：

```text
Velocity 是否发送给 Client？
```

如果：

```text
没有
```

修复 synchronization。

如果：

```text
发送了
```

最后检查：

```text
Client 是否正确应用 velocity？
```

只有在确认问题位于客户端时，才考虑客户端 Mixin。

最终目标不是：

**“让玩家看起来弹起来。”**

而是：

**“恢复 Minecraft 26.3 原本应该发生的 Piston + Slime Player Bounce 行为。”**