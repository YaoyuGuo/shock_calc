# Shock Load Calculator 3.0: User Guide

[English](#english) | [中文](#中文)

The theory behind every number is in [THEORY.md](THEORY.md).

---

<a id="english"></a>
# English

## 1. What the plugin does

The plugin runs inside an OpenRocket simulation. Every time a parachute deploys, it reads the flight state and the rocket model, and computes:

- the **opening force** of the parachute (Pflanz method, finite mass);
- the **snatch load** when the shock cord between two separated sections goes taut;
- the **sustained load** (weight of the rocket hanging under the parachute);
- the **governing load** (the largest of the three) and from it the minimum **bolt thread size** and **bulkhead thickness**.

Nothing has to be exported from OpenRocket by hand. Speed, altitude, air density, mass and parachute data come from the simulation; section masses come from the component tree.

## 2. Requirements and installation

- OpenRocket **24.12**. Other versions are not supported.
- At least one parachute that deploys during the simulation. If none deploys, no results window appears.

Installation:

1. Copy `shock-calc-plugin-3.0.jar` into the OpenRocket plugins folder (create the folder if it does not exist):
   - Windows: `%APPDATA%\OpenRocket\Plugins`
   - macOS: `~/Library/Application Support/OpenRocket/Plugins`
   - Linux: `~/.openrocket/Plugins`
2. Delete any older `shock-calc-plugin-*.jar` from the same folder.
3. Close OpenRocket completely and start it again. Plugins are loaded only at startup.

Simulations saved with version 2.0 open normally. Their stored settings are kept, and the new settings start at their defaults.

## 3. Prepare the rocket model

The results are only as good as the model. Before running the plugin, check these points.

| Item | Why it matters | What to do |
|---|---|---|
| Component masses | Section masses and the snatch load come from them | Weigh the real parts and enter the values with **Override mass**. The plugin respects overrides, including "override subcomponents" |
| Component positions | A component belongs to the section that contains its axial centre | Place avionics, ballast and recovery hardware at their real positions |
| Body tubes | By default every body tube starts a new section | Model each airframe segment that separates as its own body tube; see section 5 if your model has extra tubes |
| Parachutes | Cd, diameter and position feed the opening force and the joint matching | Enter the measured or catalogue Cd and the nominal diameter. Put each parachute near the joint that releases it |
| Deployment events | The plugin uses the state at the moment of deployment | Set the events (apogee, altitude, delay) as you will fly them |
| Motor | Its mass is not part of any component | The plugin adds it to the last (aft) section automatically |

## 4. Add the extension and run

1. Open the rocket and go to **Flight simulations**.
2. Create or edit a simulation and open **Simulation options**.
3. Click **Add extension → Shock Loads → Shock Load Calculator**.
4. Set the parameters (section 6) and close the settings dialog.
5. Run the simulation. When it finishes, the results window opens.

All settings are saved in the `.ork` file together with the simulation. Each simulation keeps its own settings, so you can compare variants side by side, for example two cord stiffnesses or two bolt materials.

## 5. Sections and multiple separation points

The plugin works with any number of sections and parachutes.

### 5.1 How the rocket is split

- **Automatic (default).** The start of every body tube is a joint. N joints give N + 1 sections. The nose cone is section 1.
- **Manual.** Type the joint positions in the settings, in cm from the nose tip, separated by commas, for example `61, 92.7`. When this field is not empty, the automatic joints are ignored.

Each component is assigned to the section that contains its axial centre. Mass that does not belong to any component (normally the motor) is added to the last section.

### 5.2 How a parachute is matched to a joint

Each parachute is matched to the joint **closest to the centre of the parachute component**. The masses in front of and behind that joint are used for the snatch load and for splitting the opening force.

### 5.3 Check this every time

Open the results window and compare the **Sections** table and the **Joint (cm)** column of the events table with your design.

Example from the Ringo III model with the automatic split:

- Five sections were found, including "Nose Cone Shoulder" (45.7 to 61.0 cm) and "Slimline Retainer" (from 303.5 cm). Neither is a real separation plane.
- Both the drogue and the main parachute were matched to the joint at 92.7 cm.

If Ringo III separates at two different places, the drogue row uses the wrong joint. The fix is to type the real joint positions into **Joint positions**, then check that each parachute lands on the right one. If a parachute still picks the wrong joint, move the parachute component in the model closer to the joint that releases it.

### 5.4 What is not supported

- Multi-stage rockets. The mass of stages that have already separated shows up as a negative unassigned mass in the results window.
- A parachute that loads more than one joint at once. Only the joint matched to the parachute is reported.
- Assigning a parachute to a joint by hand. Matching is by distance only.

## 6. Settings

### 6.1 Opening force

| Setting | Default | How to choose it |
|---|---|---|
| Method | Pflanz, finite mass | Keep Pflanz for design. "Infinite mass" is the version 2.0 formula and gives an upper bound |
| Opening-force factor Cx | 1.5 | Depends on the canopy type, see the table below |
| Canopy fill constant n | 8 | Filling time = n × nominal diameter / speed. Larger n means slower filling and a lower peak |
| Inflation exponent j | 2 | How fast the drag area grows. 2 for solid cloth, 1 for slotted, 0.5 for reefed canopies |

Typical starting values (verify against your own data):

| Canopy | Cx | n | j |
|---|---|---|---|
| Solid cloth (flat, hemispherical, elliptical) | 1.5 to 1.8 | 8 | 2 |
| Cruciform (cross) | about 1.2 | 8 to 12 | 2 |
| Slotted (ribbon, ringslot) | 1.0 to 1.2 | about 14 | 1 |
| Reefed canopy | as the base canopy | as the base canopy | 0.5 |

For Ringo III, n = 17 reproduced the measured main inflation (6.4 g predicted, 5 to 6.6 g measured). The default n = 8 gave 9.4 g, about 1.5 times the measured value. The default is therefore conservative for that rocket.

### 6.2 Shock cord snatch load

| Setting | Default | How to choose it |
|---|---|---|
| Separation speed (m/s) | 10 | Relative speed of the two bodies when the cord goes taut |
| Shock cord stiffness k (N/m) | 10000 | Spring rate of the whole cord between the two bulkheads |

These two settings decide the snatch load, and the snatch load often decides the bolt size. Set them from your own hardware rather than keeping the defaults.

- **Separation speed.** Best taken from flight or ground ejection test data. When a parachute pulls one body back while the other keeps moving, the relative speed can approach the descent speed at deployment.
- **Cord stiffness.** Estimate it from the cord data sheet:

  `k = breaking strength / (elongation at break × cord length)`

  Example: a cord rated 6700 N (1500 lbf) that stretches 4 % at break, 5 m long: `k = 6700 / (0.04 × 5) ≈ 33500 N/m`. A stiff cord (Kevlar) gives a much larger k and a much larger snatch load than an elastic one (nylon) of the same length. A longer cord gives a smaller k.

For Ringo III, the defaults gave a snatch load of 2.5 kN, while the largest measured pulse on the upper body was about 5.4 kN. With 13 m/s and 40000 N/m the plugin gives 6.6 kN, which bounds the measurement.

### 6.3 Safety factor sweep

| Setting | Default | Meaning |
|---|---|---|
| Safety factor, minimum | 1.0 | Lower end of the exported sweep |
| Safety factor, maximum | 3.0 | Upper end of the exported sweep; always included |

The step is fixed at 0.05. The sweep only scales the limit loads; it is a lookup table for the exported CSV and chart.

### 6.4 Bolt and bulkhead sizing

| Setting | Default | How to choose it |
|---|---|---|
| Sizing safety factor | 2.0 | Design load = this factor × governing limit load. Common practice for recovery hardware is 2 to 2.5 for general-purpose parts |
| Threaded legs sharing the load | 2 | 2 for a U-bolt, 1 for an eye-bolt |
| Bolt material | Stainless A2-70 | Sets the allowable tensile stress (proof strength). Choose **Custom** to enter your own value |
| Custom bolt strength (MPa) | 450 | Used only with Custom |
| Bulkhead material | Aluminum 6061-T6 | Sets the allowable bending and shear stress. Choose **Custom** for your own values |
| Custom bulkhead bending / shear strength (MPa) | 276 / 159 | Used only with Custom |
| Load circle radius (mm) | 15 | Half the leg spacing of the U-bolt, or the washer radius for an eye-bolt |
| Washer or nut outer diameter (mm) | 12 | The bearing diameter under each leg |
| Bulkhead radius (mm) | 0 | 0 = automatic: inner radius of the body tube at the joint |

Built-in materials (typical handbook values, verify against your data sheets):

| Bolt material | Allowable tensile stress |
|---|---|
| Stainless A2-70 / 18-8 | 450 MPa |
| Stainless A4-80 / 316 | 600 MPa |
| Stainless 304, annealed | 205 MPa |
| Steel ISO 8.8 / 10.9 / 12.9 | 580 / 830 / 970 MPa |
| Steel SAE Grade 2 / 5 / 8 | 379 / 586 / 827 MPa |

| Bulkhead material | Bending | Shear | Poisson's ratio |
|---|---|---|---|
| Aluminum 6061-T6 | 276 MPa | 159 MPa | 0.33 |
| Birch plywood | 40 MPa | 8 MPa | 0.30 |
| G10 / FR4 | 380 MPa | 130 MPa | 0.13 |
| Carbon fibre plate, quasi-isotropic | 500 MPa | 80 MPa | 0.30 |

## 7. Reading the results

The results window has four tables. All loads in the first two tables are **limit loads**, without any safety factor.

**Sections.** Each section, the body components in it, its axial range and its mass. Below the table: the mass not assigned to any component (usually the motor). A negative value means something is wrong, most often a multi-stage rocket.

**Recovery events.** One row per parachute, in order of deployment:

| Column | Meaning |
|---|---|
| Time, Altitude, Speed | Flight state at deployment |
| Joint (cm) | Joint matched to this parachute |
| Upper / Lower (kg) | Mass in front of and behind that joint |
| Opening | Opening force by the selected method |
| Snatch | Shock cord snatch load |
| Sustained | Weight of the whole rocket |
| Governing | Largest of the three; the sizing uses this value |

**Canopy inflation.** The Pflanz calculation for each parachute: air density, drag area CdS, nominal diameter D0, filling time, ballistic parameter A, force reduction factor X1 (without and with gravity), infinite-mass and finite-mass force, peak deceleration of the rocket in g, and how the opening force splits between the bodies in front of and behind the joint.

How to read X1: close to 1 means the rocket hardly slows down during filling (small parachute, heavy rocket) and the force is close to the infinite-mass value. A small X1 means the rocket slows down a lot while the canopy fills (large parachute, light rocket).

**Sizing.** Design load, required stress area per leg, smallest metric and UNC coarse thread that pass, bulkhead radius used, and minimum thickness from bending, from shear and the larger of the two.

The thread size is a **strength minimum**. Handling, bending of the U-bolt legs and thread engagement usually lead to a larger size. For eye-bolts, quick links and swivels, use the rated working load of the actual part.

Buttons:

- **Export table (CSV)...**: the safety factor sweep, one row per event and safety factor.
- **Export chart (PNG)...**: design load against safety factor.
- **Export inflation curves (PNG)...**: opening force against time after line stretch, one curve per parachute.
- **Copy summary**: all four tables as tab-separated text, ready to paste into Excel.

## 8. Exported table (CSV)

UTF-8 with a byte order mark, so Excel opens it correctly. An empty cell means "not available" (for example, a parachute with no joint has no snatch load).

| Column | Meaning |
|---|---|
| `safety_factor` | From the minimum to the maximum in steps of 0.05 |
| `event`, `device` | Event number and parachute name |
| `opening_limit_N`, `opening_design_N` | Opening force by the selected method |
| `snatch_limit_N`, `snatch_design_N` | Snatch load |
| `sustained_limit_N`, `sustained_design_N` | Sustained load |
| `opening_force_factor`, `cord_stiffness_N_per_m` | Cx and k used |
| `opening_infinite_mass_limit_N` | Infinite-mass opening force, for comparison |
| `governing_limit_N`, `governing_design_N` | Largest of the three loads |
| `opening_method`, `fill_constant`, `inflation_exponent`, `separation_speed_m_per_s` | Settings used |

## 9. Recommended workflow

1. Build the model with weighed masses (section 3).
2. Run once with the defaults. Check the Sections table and the joint of each parachute (section 5.3). Enter manual joints if needed.
3. Enter the separation speed and cord stiffness of your actual hardware (section 6.2).
4. Choose the canopy parameters (section 6.1). If you have accelerometer data from an earlier flight, adjust n until the peak deceleration in the inflation table matches it.
5. Enter the bolt and bulkhead materials and geometry (section 6.4).
6. Read the governing load and the sizing table. Check which load governs: if it is the snatch load, the cord and the ejection matter more than the canopy.
7. Run the worst-case simulations (highest deployment speed, heaviest configuration, strongest wind) and size for the largest result.

## 10. Troubleshooting

| Problem | Check |
|---|---|
| Extension missing from the menu | The jar is in the Plugins folder, OpenRocket was restarted, and only one version of the jar is present |
| No results window | At least one parachute deploys (look for a deployment event in the flight events) |
| Unexpected section masses | Compare the Sections table with the model; enter manual joints |
| Wrong joint for a parachute | Enter manual joints, or move the parachute closer to its joint |
| Opening force much lower than in version 2.0 | Expected: the default is now Pflanz. The infinite-mass value is in the inflation table |
| Negative unassigned mass | Multi-stage rocket, which is not supported |
| Settings dialog taller than the screen | It scrolls; the Close button stays at the bottom |
| Garbled CSV in Excel | Use Data → From Text/CSV and choose UTF-8 |
| Errors at startup or during the run | Start OpenRocket from a terminal with `java -jar OpenRocket-24.12.jar` and read the console |

---

<a id="中文"></a>
# 中文

## 1. 插件做什么

插件在 OpenRocket 的仿真过程中运行。每当一个降落伞展开时，它读取当时的飞行状态和火箭模型，计算：

- 降落伞的**开伞力**（Pflanz 方法，有限质量）；
- 两段分离箭体之间的减震绳被拉直时的**拉直冲击**；
- **持续载荷**（吊在伞下的火箭的重量）；
- **控制载荷**（三者中最大的一个），并据此给出**螺栓的最小螺纹规格**和**隔舱板的最小厚度**。

不需要手动从 OpenRocket 导出任何数据。速度、高度、空气密度、质量和降落伞参数来自仿真；各段质量来自组件树。

## 2. 使用要求与安装

- OpenRocket **24.12**，不支持其他版本。
- 仿真中至少有一个降落伞会展开。如果没有降落伞展开，就不会弹出结果窗口。

安装步骤：

1. 把 `shock-calc-plugin-3.0.jar` 复制到 OpenRocket 的插件文件夹（不存在就新建）：
   - Windows：`%APPDATA%\OpenRocket\Plugins`
   - macOS：`~/Library/Application Support/OpenRocket/Plugins`
   - Linux：`~/.openrocket/Plugins`
2. 删除同一文件夹里旧版本的 `shock-calc-plugin-*.jar`。
3. 完全关闭 OpenRocket 后重新启动。插件只在启动时加载。

用 2.0 版保存的仿真可以正常打开，原有设置会保留，新增的设置取默认值。

## 3. 准备火箭模型

结果的好坏取决于模型。运行插件之前，请检查以下几点。

| 项目 | 为什么重要 | 怎么做 |
|---|---|---|
| 构件质量 | 各段质量和拉直冲击都由它们得出 | 称量实物，用 **Override mass（质量覆盖）** 输入。插件会计入覆盖值，包括"覆盖子构件"的情况 |
| 构件位置 | 构件按其轴向中心归入所在的段 | 把航电、配重和回收硬件放在实际位置 |
| 箭体管 | 默认每根箭体管的起点都是一个分离位置 | 每个会分离的箭体段单独建成一根箭体管；如果模型里有多余的管，见第 5 节 |
| 降落伞 | Cd、直径和位置决定开伞力和分离位置的匹配 | 输入实测或厂家给的 Cd 和名义直径。把每个降落伞放在释放它的分离位置附近 |
| 展开事件 | 插件使用展开那一刻的飞行状态 | 按实际飞行方案设置事件（最高点、指定高度、延时） |
| 发动机 | 它的质量不属于任何构件 | 插件会自动把它加到最后一段（尾段） |

## 4. 添加扩展并运行

1. 打开火箭，进入 **Flight simulations（飞行仿真）**。
2. 新建或编辑一个仿真，打开 **Simulation options（仿真选项）**。
3. 点击 **Add extension → Shock Loads → Shock Load Calculator**。
4. 设置参数（见第 6 节），关闭设置窗口。
5. 运行仿真。仿真结束后弹出结果窗口。

所有设置都随仿真保存在 `.ork` 文件里。每个仿真有自己独立的设置，所以可以并排比较不同方案，例如两种绳子刚度或两种螺栓材料。

## 5. 分段与多个分离位置

插件支持任意数量的分段和降落伞。

### 5.1 火箭如何分段

- **自动（默认）：** 每根箭体管的起点是一个分离位置。N 个分离位置把火箭分成 N + 1 段。鼻锥是第 1 段。
- **手动：** 在设置里输入分离位置，单位为距箭头尖端的厘米数，用逗号分隔，例如 `61, 92.7`。这一栏不为空时，自动划分的分离位置不再使用。

每个构件归入其轴向中心所在的段。不属于任何构件的质量（通常是发动机）加到最后一段。

### 5.2 降落伞如何对应到分离位置

每个降落伞对应到**离降落伞构件中心最近**的那个分离位置。该位置前后两部分的质量用来计算拉直冲击和分配开伞力。

### 5.3 每次都要检查

打开结果窗口，把 **Sections（分段）** 表和事件表的 **Joint (cm)** 列与你的设计对照。

以 Ringo III 模型的自动分段为例：

- 找到了 5 段，其中包括 "Nose Cone Shoulder"（45.7 到 61.0 cm）和 "Slimline Retainer"（303.5 cm 以后）。这两处都不是真正的分离面。
- 减速伞和主伞都被对应到了 92.7 cm 处的分离位置。

如果 Ringo III 实际在两个不同位置分离，减速伞那一行用的分离位置就是错的。解决办法是在 **Joint positions（分离位置）** 里输入真实的分离位置，再确认每个降落伞对应到了正确的位置。如果某个降落伞还是对应错了，就在模型里把这个降落伞构件移到更靠近释放它的分离位置。

### 5.4 不支持的情况

- 多级火箭。已经分离的级的质量会在结果窗口里表现为负的"未归入组件的质量"。
- 一个降落伞同时给多个分离位置加载。只报告与该降落伞对应的那一个分离位置。
- 手动指定降落伞对应哪个分离位置。目前只按距离匹配。

## 6. 参数设置

### 6.1 开伞力

| 参数 | 默认值 | 如何选取 |
|---|---|---|
| Method（方法） | Pflanz, finite mass | 设计时保持 Pflanz。"Infinite mass" 是 2.0 版的公式，给出的是上限 |
| Opening-force factor Cx（开伞力系数） | 1.5 | 取决于伞型，见下表 |
| Canopy fill constant n（充气常数） | 8 | 充气时间 = n × 名义直径 / 速度。n 越大，充气越慢，峰值越低 |
| Inflation exponent j（充气指数） | 2 | 阻力面积增长的快慢。实心伞取 2，开缝伞取 1，收口伞取 0.5 |

典型的初始值（请用你自己的数据核对）：

| 伞型 | Cx | n | j |
|---|---|---|---|
| 实心伞（平面、半球、椭圆） | 1.5 到 1.8 | 8 | 2 |
| 十字伞 | 约 1.2 | 8 到 12 | 2 |
| 开缝伞（带条伞、环缝伞） | 1.0 到 1.2 | 约 14 | 1 |
| 收口伞 | 同基础伞型 | 同基础伞型 | 0.5 |

对 Ringo III，n = 17 能复现实测的主伞充气（预测 6.4 g，实测 5 到 6.6 g）。默认的 n = 8 算出 9.4 g，约为实测值的 1.5 倍。因此对这枚火箭来说，默认值是偏保守的。

### 6.2 减震绳拉直冲击

| 参数 | 默认值 | 如何选取 |
|---|---|---|
| Separation speed (m/s)（分离速度） | 10 | 绳子被拉直时两段箭体的相对速度 |
| Shock cord stiffness k (N/m)（减震绳刚度） | 10000 | 两块隔舱板之间整根绳子的弹簧刚度 |

这两个参数决定拉直冲击，而拉直冲击往往决定螺栓尺寸。请按你实际使用的硬件来设置，不要直接用默认值。

- **分离速度：** 最好从飞行数据或地面抛射试验中获得。当降落伞把一段箭体往回拉、另一段还在往前运动时，相对速度可以接近开伞时的下降速度。
- **绳子刚度：** 按绳子的数据表估算：

  `k = 断裂强度 / (断裂伸长率 × 绳长)`

  例如：额定 6700 N（1500 lbf）、断裂伸长率 4 %、长 5 m 的绳子：`k = 6700 / (0.04 × 5) ≈ 33500 N/m`。同样长度下，刚性的绳子（Kevlar）比弹性的绳子（尼龙）k 大得多，拉直冲击也大得多。绳子越长，k 越小。

对 Ringo III，默认值算出的拉直冲击是 2.5 kN，而上段实测的最大脉冲约为 5.4 kN。取 13 m/s 和 40000 N/m 时插件给出 6.6 kN，可以覆盖实测值。

### 6.3 安全系数扫描

| 参数 | 默认值 | 含义 |
|---|---|---|
| Safety factor, minimum（安全系数下限） | 1.0 | 导出扫描表的下端 |
| Safety factor, maximum（安全系数上限） | 3.0 | 导出扫描表的上端，一定包含在内 |

步长固定为 0.05。扫描只是对极限载荷做线性缩放，是给导出的 CSV 和图表用的查找表。

### 6.4 螺栓与隔舱板选型

| 参数 | 默认值 | 如何选取 |
|---|---|---|
| Sizing safety factor（选型安全系数） | 2.0 | 设计载荷 = 该系数 × 控制载荷。对通用零件，回收硬件常用 2 到 2.5 |
| Threaded legs sharing the load（分担载荷的螺纹腿数） | 2 | U 型螺栓取 2，吊环螺栓取 1 |
| Bolt material（螺栓材料） | Stainless A2-70 | 决定许用拉应力（保证应力）。选 **Custom** 可输入自己的值 |
| Custom bolt strength (MPa) | 450 | 仅在选 Custom 时使用 |
| Bulkhead material（隔舱板材料） | Aluminum 6061-T6 | 决定许用弯曲应力和剪应力。选 **Custom** 可输入自己的值 |
| Custom bulkhead bending / shear strength (MPa) | 276 / 159 | 仅在选 Custom 时使用 |
| Load circle radius (mm)（载荷作用圆半径） | 15 | U 型螺栓两腿间距的一半，吊环螺栓则取垫圈半径 |
| Washer or nut outer diameter (mm)（垫圈或螺母外径） | 12 | 每条腿下的承压直径 |
| Bulkhead radius (mm)（隔舱板半径） | 0 | 0 表示自动：取分离位置处箭体管的内半径 |

内置材料（手册典型值，请对照数据表核对）：

| 螺栓材料 | 许用拉应力 |
|---|---|
| 不锈钢 A2-70 / 18-8 | 450 MPa |
| 不锈钢 A4-80 / 316 | 600 MPa |
| 不锈钢 304（退火） | 205 MPa |
| 钢 ISO 8.8 / 10.9 / 12.9 | 580 / 830 / 970 MPa |
| 钢 SAE Grade 2 / 5 / 8 | 379 / 586 / 827 MPa |

| 隔舱板材料 | 弯曲 | 剪切 | 泊松比 |
|---|---|---|---|
| 铝 6061-T6 | 276 MPa | 159 MPa | 0.33 |
| 桦木胶合板 | 40 MPa | 8 MPa | 0.30 |
| G10 / FR4 玻纤板 | 380 MPa | 130 MPa | 0.13 |
| 碳纤维板（准各向同性） | 500 MPa | 80 MPa | 0.30 |

## 7. 阅读结果

结果窗口有四张表。前两张表中的载荷都是**极限载荷**，没有乘安全系数。

**Sections（分段）：** 每一段、其中的箭体构件、轴向范围和质量。表下方是没有归入任何构件的质量（通常是发动机）。如果是负值，说明有问题，最常见的原因是多级火箭。

**Recovery events（回收事件）：** 每个降落伞一行，按展开顺序排列：

| 列 | 含义 |
|---|---|
| Time、Altitude、Speed | 展开时的飞行状态 |
| Joint (cm) | 与该降落伞对应的分离位置 |
| Upper / Lower (kg) | 该分离位置前、后两部分的质量 |
| Opening | 按所选方法计算的开伞力 |
| Snatch | 减震绳拉直冲击 |
| Sustained | 整枚火箭的重量 |
| Governing | 三者中最大的一个，选型用的就是这个值 |

**Canopy inflation（伞衣充气）：** 每个降落伞的 Pflanz 计算：空气密度、阻力面积 CdS、名义直径 D0、充气时间、弹道参数 A、折减系数 X1（不计重力和计入重力）、无限质量和有限质量开伞力、火箭的峰值减速度（g），以及开伞力在分离位置前后两部分之间的分配。

X1 的含义：接近 1 表示充气期间火箭几乎没有减速（伞小、箭重），开伞力接近无限质量的值；X1 很小表示伞衣充气期间火箭减速很多（伞大、箭轻）。

**Sizing（选型）：** 设计载荷、每条腿所需的应力面积、满足要求的最小公制和 UNC 粗牙螺纹、所用的隔舱板半径，以及按弯曲、按剪切算出的最小厚度和两者中的较大值。

螺纹规格只是**强度上的下限**。考虑到装拆、U 型螺栓腿的弯曲和螺纹旋合长度，实际通常要选更大的规格。吊环螺栓、快挂和转环请按实际零件的额定工作载荷选用。

按钮：

- **Export table (CSV)...**：安全系数扫描表，每个事件、每个安全系数一行。
- **Export chart (PNG)...**：设计载荷随安全系数变化的图。
- **Export inflation curves (PNG)...**：开伞力随伞绳拉直后时间变化的曲线，每个降落伞一条。
- **Copy summary**：把四张表以制表符分隔的文本复制到剪贴板，可以直接粘贴到 Excel。

## 8. 导出的表格（CSV）

文件为带字节顺序标记（BOM）的 UTF-8 编码，Excel 可以正确打开。空单元格表示"不适用"（例如没有对应分离位置的降落伞没有拉直冲击）。

| 列 | 含义 |
|---|---|
| `safety_factor` | 从下限到上限，步长 0.05 |
| `event`、`device` | 事件编号和降落伞名称 |
| `opening_limit_N`、`opening_design_N` | 按所选方法计算的开伞力 |
| `snatch_limit_N`、`snatch_design_N` | 拉直冲击 |
| `sustained_limit_N`、`sustained_design_N` | 持续载荷 |
| `opening_force_factor`、`cord_stiffness_N_per_m` | 使用的 Cx 和 k |
| `opening_infinite_mass_limit_N` | 无限质量开伞力，用于对比 |
| `governing_limit_N`、`governing_design_N` | 三个载荷中最大的一个 |
| `opening_method`、`fill_constant`、`inflation_exponent`、`separation_speed_m_per_s` | 使用的设置 |

## 9. 推荐的工作流程

1. 用称量过的质量建立模型（第 3 节）。
2. 先用默认值运行一次。检查分段表和每个降落伞对应的分离位置（第 5.3 节），需要时输入手动分离位置。
3. 输入实际硬件的分离速度和绳子刚度（第 6.2 节）。
4. 选择伞型参数（第 6.1 节）。如果有以往飞行的加速度数据，调整 n，使充气表中的峰值减速度与实测吻合。
5. 输入螺栓和隔舱板的材料与几何尺寸（第 6.4 节）。
6. 读取控制载荷和选型表。看清是哪个载荷在控制：如果是拉直冲击，那么绳子和抛射比伞衣更重要。
7. 运行最不利工况的仿真（开伞速度最高、质量最大、风最大），按其中最大的结果选型。

## 10. 常见问题

| 问题 | 检查 |
|---|---|
| 菜单里没有扩展 | jar 在 Plugins 文件夹里，OpenRocket 已重启，并且文件夹里只有一个版本的 jar |
| 没有结果窗口 | 至少有一个降落伞展开（在飞行事件里找展开事件） |
| 分段质量不符合预期 | 对照分段表和模型，输入手动分离位置 |
| 降落伞对应的分离位置不对 | 输入手动分离位置，或把降落伞移到更靠近其分离位置的地方 |
| 开伞力比 2.0 版小很多 | 正常现象：默认方法已改为 Pflanz。无限质量的值在充气表里 |
| 未归入组件的质量为负 | 多级火箭，目前不支持 |
| 设置窗口比屏幕还高 | 可以滚动，Close 按钮始终在底部 |
| Excel 打开 CSV 乱码 | 用"数据 → 从文本/CSV"导入并选择 UTF-8 |
| 启动或运行时出错 | 在终端里用 `java -jar OpenRocket-24.12.jar` 启动 OpenRocket，查看控制台输出 |
