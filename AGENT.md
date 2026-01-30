# StarWorld 项目规范（AGENT）

## 基本信息
- modid: `starworld`
- 包名（groupId/base package）: `top.xcyyds.starworld`
- 作者: `ZoranXC`
- 主页: `https://xcyyds.top`
- 当前平台: Forge `1.20.1`

## 分层架构（必须遵守）
目标：`common` 实现可迁移/可复用的核心逻辑；`forge` 仅作为 Forge 适配层。

- `top.xcyyds.starworld.common.*`
  - 领域模型、规则系统、算法、数据结构
  - 与平台交互的抽象接口（例如 `StarWorldPlatform`）
  - 不直接依赖/引用 Forge API（`net.minecraftforge.*`）
  - 尽量避免直接依赖 Minecraft 平台类（如 `Level/Entity/BlockPos`）；确有必要时用适配层隔离

- `top.xcyyds.starworld.forge.*`
  - Forge `@Mod` 入口、事件订阅、注册（物品/方块/实体/世界生成/命令/网络/能力/存档等）
  - 仅做“桥接/适配”：将 Forge 事件与对象转换后调用 `common`
  - 不承载复杂业务规则（复杂逻辑优先下沉到 `common`）

依赖方向：`forge -> common`（单向）。

## 模块化开发建议（按功能域拆包）
所有“玩法系统”优先归属 `common`，并按功能域拆包，避免大杂烩。

建议目录（可按需增删）：
- `common.api`：对外稳定接口/服务定义
- `common.core`：通用初始化、模块生命周期、基础工具（保持精简）
- `common.world`：世界/区域/边界/claim 等
- `common.nation`：国家/领土/外交/权限等 RPG 规则
- `common.structure`：自然造物/建筑生成规则与算法
- `common.npc`：NPC 定义、职业、关系、任务
- `common.ai`：决策/行为树/状态机等 AI 逻辑

Forge 侧对应薄适配包（示例）：
- `forge.world` / `forge.structure` / `forge.npc` / `forge.ai`

## 生命周期与初始化
- `forge` 的 `@Mod` 入口只负责：
  - 构造平台实现（例如 `ForgePlatform`）
  - 调用 `StarWorldCommon.init(platform)`
- `common` 的初始化应：
  - 幂等（多次调用无副作用）
  - 不直接访问平台资源（世界、注册表、网络）

## 命名规范
- 包名：全小写（已固定 `top.xcyyds.starworld`）
- 类名：UpperCamelCase
- 常量：`UPPER_SNAKE_CASE`
- 资源命名（文件/目录）：全小写 + 下划线（如 `some_block.json`）

## 资源与语言文件
- 资源根：`src/main/resources/assets/starworld/`
- 语言：
  - `assets/starworld/lang/en_us.json`
  - `assets/starworld/lang/zh_cn.json`
- 翻译 key 约定：
  - `mod.starworld.*`（通用）
  - `block.starworld.*` / `item.starworld.*` / `entity.starworld.*`

## 日志与错误处理
- 日志统一走平台接口（`StarWorldPlatform.logInfo(...)` 或后续扩展）
- `common` 内不直接使用 Forge 的 Logger；由 `forge` 注入
- 错误信息要可定位：包含模块名/对象 id/关键参数

## 数据与存档（约定）
- 任何与存档/网络/命令相关的实现放在 `forge`
- `common` 只定义：
  - 数据模型（POJO/record）、序列化格式（如 JSON/NBT 的中立表示）
  - 规则校验与业务约束

## 依赖与扩展原则
- 新增功能先问：能否做成 `common` 的纯逻辑？
- 只有以下情况才进入 `forge`：
  - 需要注册/事件/网络/存档/渲染/客户端特性
  - 必须引用 Forge 或 Minecraft 平台类

## 版本与兼容性
- 目标：让 `common` 尽可能与版本无关；升级 MC/Forge 时主要修改 `forge` 适配层
- 变更规则：
  - 公共 API（`common.api`）变更需保守，优先新增而非破坏性修改

## 开发约定（最低要求）
- 新增模块/系统必须：
  - 给出清晰的包归属（common/forge）
  - 说明与其它模块的依赖关系（避免环依赖）
  - 保持 `common` 可迁移性（不把 Forge 依赖带进去）
