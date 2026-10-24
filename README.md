# Shock Load Calculator: an OpenRocket Plugin

**English** | [中文](#中文)

A simulation extension for [OpenRocket](https://openrocket.info/) 24.12 that estimates the loads a recovery system puts on a rocket. It reads the section masses from the rocket model, handles any number of separation sections and parachutes, and sweeps a range of safety factors. After a simulation finishes it opens a results window and lets you export the full table (CSV) and a chart (PNG).

> **Disclaimer:** the formulas are first-order estimates. If you use the results for structural design, verify them against your team's reference method or test data.

## Features

- Adds a simulation extension, configured from **Edit simulation → Simulation options → Add extension**.
- Splits the rocket into sections automatically and reads the section masses from the model.
- Handles several parachutes: every deployment is analysed separately, using the conditions at its own deployment time.
- Sweeps the safety factor over a range (step 0.05) and reports limit loads and design loads.
- Exports the full sweep table as CSV and a chart as PNG.
- All settings are stored in the `.ork` file together with the simulation.

## Requirements

- OpenRocket **24.12**. The plugin is built against this version and other versions are not supported.
- A rocket with at least one parachute that actually deploys during the simulation. If no recovery device deploys, no results window appears.

## Installation (for users)

1. Download `shock-calc-plugin-<version>.jar`.
2. Copy it into your OpenRocket plugins folder (create it if it does not exist):
   - Windows: `%APPDATA%\OpenRocket\Plugins`
   - macOS: `~/Library/Application Support/OpenRocket/Plugins`
   - Linux: `~/.openrocket/Plugins`
3. Fully close and restart OpenRocket. Plugins are only loaded at startup.

When updating, delete the old jar from the plugins folder before adding the new one.

## Usage

1. Open a rocket that has at least one parachute. For a meaningful test, set the deployment event of each parachute so that it opens while the rocket is still moving (for example **Ejection charge** with a delay of 0). Deployment at apogee gives a speed near zero and therefore near-zero loads.
2. Go to **Flight simulations**, create or edit a simulation, and open **Simulation options**.
3. Click **Add extension** and choose **Shock Loads → Shock Load Calculator**.
4. Set the parameters (see below) and close the dialog.
5. Run the simulation. The results window opens when it finishes.
6. In the results window, use **Export table (CSV)...** and **Export chart (PNG)...** to save the results. **Copy summary** puts the section and event tables on the clipboard.

### Parameters

| Parameter | Default | Meaning |
|---|---|---|
| Opening-force factor Cx | 1.5 | Accounts for the peak force during canopy inflation |
| Safety factor, minimum | 1.0 | Lower end of the sweep |
| Safety factor, maximum | 3.0 | Upper end of the sweep (always included in the table) |
| Shock cord stiffness k (N/m) | 10000 | Used for the separation load |
| Joint positions (cm) | empty | Optional manual joints; empty means automatic (see step 3 below) |

All values use SI units. The sweep step is fixed at 0.05. During the sweep, Cx, k and everything read from the simulation stay constant.

### Results window

- **Sections table:** the sections found in the model, their components, their axial range and their mass.
- **Recovery events table:** one row per parachute, with the deployment time, altitude, speed, the associated joint, the masses above and below that joint, and the three **limit** loads in N and lbf.
- Design loads are not shown in the window; they are in the exported table and chart.

### Exported table (CSV)

One row per event and safety factor, ordered by event and then by safety factor. The file is UTF-8 with a byte order mark, so Excel opens it correctly. An empty cell means "not available" (for example, an event with no joint has no snatch load).

| Column | Meaning |
|---|---|
| `safety_factor` | Independent variable, from the minimum to the maximum in steps of 0.05 |
| `event` | Deployment event number, in order of deployment time |
| `device` | Name of the recovery device in the rocket model |
| `opening_limit_N`, `opening_design_N` | Opening shock: limit load and design load |
| `snatch_limit_N`, `snatch_design_N` | Separation (shock cord) load: limit load and design load |
| `sustained_limit_N`, `sustained_design_N` | Sustained load: limit load and design load |
| `opening_force_factor` | Cx used for the sweep |
| `cord_stiffness_N_per_m` | k used for the sweep |

### Exported chart (PNG)

Design load (N) against safety factor, with one line per event and load type. The lines are straight because the design load is the limit load multiplied by the safety factor. The chart uses the JFreeChart library that OpenRocket bundles. If you prefer, open the CSV in Excel, filter by `event`, and plot the `*_design_N` columns against `safety_factor`.

## Calculation process

For every parachute that deploys, the plugin does the following.

**1. Detect the deployment.** After every simulation step it checks which recovery devices have been deployed. Each newly deployed device is analysed once, using the state of that step.

**2. Read the flight conditions at deployment.**
- Speed `v` is the magnitude of the rocket velocity.
- Air density is computed from the simulated pressure `P` and temperature `T` with the ideal gas law, `ρ = P / (R · T)`, where `R = 287.05 J/(kg·K)`.
- Total mass `m` is the simulated mass at that moment, including the motor.
- `Cd` and `A` come from the parachute component.

**3. Split the rocket into sections.** Joints are the start positions of every body tube along the axis, so the nose cone is its own section and every body tube begins a new section. Transitions stay with the section in front of them. N joints give N + 1 sections. If you enter joint positions manually, they replace the automatic ones.

**4. Assign component masses to sections.** Every component in the model belongs to the section that contains its axial center, and its mass is added to that section. The part of the total mass that is not covered by any component (mainly the motor and its casing) is the difference between the simulated total mass and the sum of the component masses. It is added to the last, aft-most section. The results window shows this difference so you can check it.

**5. Find the joint of each parachute.** The joint closest to the center of the parachute along the rocket axis. This works because a parachute is normally packed right next to the joint that releases it.

**6. Compute the upper and lower mass.** For joint `j`, the upper mass `m₁` is the sum of all sections in front of the joint (including the section that ends at it), and the lower mass `m₂` is the sum of all sections behind it. Sections that separated earlier stay tethered by their shock cords, so they count towards the side they hang on.

**7. Compute the limit loads.**

| Load | Formula |
|---|---|
| Opening shock | `F = ½ · ρ · v² · Cd · A · Cx` |
| Separation (shock cord) load | `F = v · √(k · μ)`, with `μ = m₁ · m₂ / (m₁ + m₂)` |
| Sustained load | `F = m · g`, with `g = 9.80665 m/s²` |

These are limit loads: no safety factor has been applied.

**8. Sweep the safety factor.** The safety factors are `SF = min, min + 0.05, min + 0.10, ...` up to the maximum, which is always included. For each one, `design load = SF × limit load`, for all three loads. The safety factor is applied to every load, so the three loads are on the same footing when compared with allowable values. Cx already accounts for peak inflation force, so avoid adding the same margin twice.

### Worked example

Illustrative numbers: ρ = 1.2 kg/m³, v = 20 m/s, Cd = 1.0, A = 0.2 m², Cx = 1.5, k = 10000 N/m, upper mass 0.080 kg, lower mass 0.144 kg (total 0.224 kg).

- Opening shock: `0.5 × 1.2 × 20² × 1.0 × 0.2 × 1.5 = 72.0 N`
- Reduced mass: `μ = 0.080 × 0.144 / 0.224 = 0.0514 kg`, so the separation load is `20 × √(10000 × 0.0514) = 453.6 N`
- Sustained load: `0.224 × 9.80665 = 2.197 N`

| safety_factor | opening_design_N | snatch_design_N | sustained_design_N |
|---|---|---|---|
| 1.00 | 72.000 | 453.560 | 2.197 |
| 1.05 | 75.600 | 476.240 | 2.307 |
| 2.00 | 144.000 | 907.120 | 4.393 |

## Known limitations

- The opening shock and the separation load use the speed at the moment of deployment. The true peak force occurs during canopy inflation, which Cx is meant to approximate.
- The sustained load uses the full rocket mass at deployment for every parachute, which is conservative.
- The joint of a parachute is found by proximity, which is a heuristic. If it picks the wrong joint, enter the joint positions manually or move the parachute in the model.
- Multi-stage rockets are not handled. Masses of stages that have already separated would appear as a negative unassigned mass in the results window.
- Component masses come from the model's component mass values; mass overrides are not taken into account.
- The safety factor sweep is a linear scaling, so it is a convenience table rather than new physics.
- Tested only with OpenRocket 24.12.

## Build from source (for developers)

Requirements: JDK 17 and the OpenRocket 24.12 jar.

1. Clone this repository.
2. Download `OpenRocket-24.12.jar` and copy it to `plug_in/lib/`. The jar is not stored in this repository.
3. From the `plug_in` folder, run:
   ```
   .\gradlew.bat jar            # output: build/libs/shock-calc-plugin-<version>.jar
   .\gradlew.bat installPlugin  # builds and copies to the Windows Plugins folder
   ```
   On macOS and Linux, run `./gradlew jar` and copy the jar to your plugins folder manually. The `installPlugin` task currently supports Windows only.

OpenRocket is referenced with `compileOnly`, so it is not bundled into the plugin jar. The PNG export uses JFreeChart from the OpenRocket jar; if it does not resolve, delete `ChartExporter.java` and the PNG button in `ResultsDialog.java`.

## Project structure

```
shock_calc/
├── README.md
├── .gitignore
├── .gitattributes
└── plug_in/
    ├── build.gradle
    ├── settings.gradle
    ├── lib/                              (OpenRocket jar, not committed)
    └── src/main/
        ├── java/sck_calc_plug_in/
        │   ├── MyExt.java                 extension, parameters, simulation listener
        │   ├── MyExtProvider.java         registers the extension and its menu entry
        │   ├── MyExtConfigurator.java     configuration GUI (Swing)
        │   ├── RocketSections.java        splits the rocket and reads section masses
        │   ├── LoadCalculator.java        the load formulas
        │   ├── EventResult.java           limit loads of one deployment event
        │   ├── SafetyFactorSweep.java     sweep table and CSV export
        │   ├── ChartExporter.java         PNG chart export
        │   └── ResultsDialog.java         results window
        └── resources/META-INF/services/
            ├── info.openrocket.core.simulation.extension.SimulationExtensionProvider
            └── info.openrocket.swing.simulation.extension.SwingSimulationExtensionConfigurator
```

## Troubleshooting

| Problem | Check |
|---|---|
| Extension missing from the menu | The jar is in the Plugins folder, OpenRocket was restarted, and the jar contains `MyExtProvider.class` and the `META-INF/services` files (`jar tf <jar>`) |
| No results window | At least one parachute deploys during the simulation (look for a deployment event in the flight events) |
| Unexpected section masses | Compare the sections table with your model; set the joint positions manually if the automatic split does not match your design |
| Wrong joint for a parachute | Enter the joint positions manually, or move the parachute closer to the joint that releases it |
| Garbled text when opening the CSV | Use Excel's Data → From Text/CSV and choose UTF-8 |
| Startup or runtime errors | Launch with `java -jar OpenRocket-24.12.jar` from a terminal and read the console output |

## Roadmap

- Write the results into OpenRocket's own flight data and plots.
- Support multi-stage rockets.
- Make `installPlugin` work on macOS and Linux.

## License

To be decided. OpenRocket is released under the GPL, and this plugin depends on its classes, so choose a compatible license before publishing.

---

# 中文

[English](#shock-load-calculator-an-openrocket-plugin) | **中文**

这是一个适用于 [OpenRocket](https://openrocket.info/) 24.12 的仿真扩展插件，用来估算回收系统施加在火箭上的载荷。它从火箭模型中读取各分离段的质量，支持任意数量的分离段和降落伞，并对一个范围内的安全系数做扫描。仿真结束后会弹出结果窗口，可以导出完整的表格（CSV）和图片（PNG）。

> **免责声明：** 公式均为一阶估算。如果要将结果用于结构设计，请用团队认可的方法或试验数据复核。

## 功能

- 添加一个仿真扩展，通过 **编辑仿真 → 仿真选项 → 添加扩展** 配置。
- 自动把火箭划分为若干段，并从模型中读取各段质量。
- 支持多个降落伞：每次展开单独分析，使用各自展开时刻的条件。
- 对安全系数做范围扫描（步长 0.05），同时给出极限载荷和设计载荷。
- 完整的扫描表格导出为 CSV，图表导出为 PNG。
- 所有设置随仿真一起保存在 `.ork` 文件中。

## 使用要求

- OpenRocket **24.12**。插件针对该版本编译，不支持其他版本。
- 火箭至少有一个降落伞，且降落伞在仿真中确实展开。如果没有回收装置展开，则不会弹出结果窗口。

## 安装（面向用户）

1. 下载 `shock-calc-plugin-<版本号>.jar`。
2. 复制到 OpenRocket 的插件目录（不存在就手动新建）：
   - Windows：`%APPDATA%\OpenRocket\Plugins`
   - macOS：`~/Library/Application Support/OpenRocket/Plugins`
   - Linux：`~/.openrocket/Plugins`
3. 完全关闭并重新启动 OpenRocket。插件只在启动时加载。

更新插件时，请先删除插件目录里的旧 jar，再放入新的。

## 使用方法

1. 打开一个至少带有一个降落伞的火箭。为了得到有意义的测试结果，请把每个降落伞的展开事件设成让它在火箭还在运动时展开（例如 **弹射药**，延时为 0）。如果在最高点展开，速度接近 0，载荷也就接近 0。
2. 进入 **飞行仿真**，新建或编辑一个仿真，打开 **仿真选项**。
3. 点击 **添加扩展**，选择 **Shock Loads → Shock Load Calculator**。
4. 设置参数（见下表）并关闭对话框。
5. 运行仿真，结束后会弹出结果窗口。
6. 在结果窗口里，用 **Export table (CSV)...** 和 **Export chart (PNG)...** 保存结果。**Copy summary** 会把分离段表和事件表复制到剪贴板。

### 参数

| 参数 | 默认值 | 含义 |
|---|---|---|
| Opening-force factor Cx（开伞力系数） | 1.5 | 用来近似伞衣充气过程中的峰值力 |
| Safety factor, minimum（安全系数下限） | 1.0 | 扫描范围的下端 |
| Safety factor, maximum（安全系数上限） | 3.0 | 扫描范围的上端（一定会出现在表格里） |
| Shock cord stiffness k (N/m)（减震绳刚度） | 10000 | 用于计算分离冲击载荷 |
| Joint positions (cm)（分离位置） | 空 | 可选的手动分离位置，留空表示自动划分（见下面计算过程第 3 步） |

所有数值使用国际单位制。扫描步长固定为 0.05。扫描过程中，Cx、k 以及所有从仿真读取的量都保持不变。

### 结果窗口

- **分离段表：** 模型中划分出的各段、各段包含的构件、轴向范围和质量。
- **回收事件表：** 每个降落伞一行，包含展开时间、高度、速度、对应的分离位置、该位置上方和下方的质量，以及三个**极限**载荷（单位 N 和 lbf）。
- 窗口里不显示设计载荷，设计载荷在导出的表格和图里。

### 导出的表格（CSV）

每个事件、每个安全系数占一行，先按事件、再按安全系数排序。文件为带字节顺序标记的 UTF-8，Excel 可以直接正确打开。空单元格表示"无数据"（例如某个事件没有对应的分离位置，就没有分离冲击载荷）。

| 列 | 含义 |
|---|---|
| `safety_factor` | 自变量，从下限到上限，步长 0.05 |
| `event` | 展开事件序号，按展开时间排序 |
| `device` | 火箭模型中回收装置的名称 |
| `opening_limit_N`、`opening_design_N` | 开伞冲击：极限载荷和设计载荷 |
| `snatch_limit_N`、`snatch_design_N` | 分离（减震绳）冲击：极限载荷和设计载荷 |
| `sustained_limit_N`、`sustained_design_N` | 持续载荷：极限载荷和设计载荷 |
| `opening_force_factor` | 扫描所用的 Cx |
| `cord_stiffness_N_per_m` | 扫描所用的 k |

### 导出的图片（PNG）

设计载荷（N）随安全系数变化的曲线，每个事件、每种载荷一条线。曲线是直线，因为设计载荷就是极限载荷乘以安全系数。图表使用 OpenRocket 自带的 JFreeChart 库绘制。如果你更习惯 Excel，也可以直接打开 CSV，按 `event` 筛选，然后用 `*_design_N` 列对 `safety_factor` 作图。

## 计算过程

对每一个展开的降落伞，插件按下面的步骤计算。

**1. 检测展开。** 每个仿真步之后，检查有哪些回收装置已经展开。每个新展开的装置只分析一次，使用该步的状态。

**2. 读取展开时的飞行条件。**
- 速度 `v` 取火箭速度矢量的大小。
- 空气密度由仿真得到的气压 `P` 和温度 `T` 按理想气体定律计算：`ρ = P / (R · T)`，其中 `R = 287.05 J/(kg·K)`。
- 总质量 `m` 取该时刻仿真给出的质量，包含发动机。
- `Cd` 和 `A` 来自降落伞组件。

**3. 把火箭划分为若干段。** 分离位置取沿轴向每一根箭体管的起点，所以头锥自成一段，每根箭体管开始新的一段，过渡段归在它前面的那一段。N 个分离位置把火箭分成 N + 1 段。如果手动输入分离位置，则替换自动划分的结果。

**4. 把构件质量归入各段。** 模型中的每个构件归入其轴向中心所在的那一段，质量加到该段上。总质量里没有被任何构件覆盖的部分（主要是发动机和外壳），等于仿真总质量减去所有构件质量之和，计入最后（最靠尾部）的一段。结果窗口会显示这个差值，方便核对。

**5. 找到每个降落伞对应的分离位置。** 取沿火箭轴向离降落伞中心最近的那个分离位置。这个规则成立的原因是：降落伞通常紧挨着释放它的分离处放置。

**6. 计算上方质量和下方质量。** 对第 `j` 个分离位置，上方质量 `m₁` 是它前面所有段（包括在它处结束的那一段）的质量之和，下方质量 `m₂` 是它后面所有段的质量之和。更早分离的段仍然通过减震绳连着，所以算在它们所挂的那一侧。

**7. 计算极限载荷。**

| 载荷 | 公式 |
|---|---|
| 开伞冲击 | `F = ½ · ρ · v² · Cd · A · Cx` |
| 分离（减震绳）冲击 | `F = v · √(k · μ)`，其中 `μ = m₁ · m₂ / (m₁ + m₂)` |
| 持续载荷 | `F = m · g`，其中 `g = 9.80665 m/s²` |

这些都是极限载荷，没有乘安全系数。

**8. 扫描安全系数。** 安全系数取 `SF = 下限、下限 + 0.05、下限 + 0.10 ……` 直到上限，上限一定包含在内。对每个安全系数，三个载荷都按 `设计载荷 = SF × 极限载荷` 计算。安全系数作用在每一个载荷上，这样三个载荷与许用值比较时处在同一口径。Cx 已经考虑了充气峰值力，要避免把同一份裕度加两次。

### 算例

示意数据：ρ = 1.2 kg/m³，v = 20 m/s，Cd = 1.0，A = 0.2 m²，Cx = 1.5，k = 10000 N/m，上方质量 0.080 kg，下方质量 0.144 kg（总质量 0.224 kg）。

- 开伞冲击：`0.5 × 1.2 × 20² × 1.0 × 0.2 × 1.5 = 72.0 N`
- 约化质量：`μ = 0.080 × 0.144 / 0.224 = 0.0514 kg`，分离冲击为 `20 × √(10000 × 0.0514) = 453.6 N`
- 持续载荷：`0.224 × 9.80665 = 2.197 N`

| safety_factor | opening_design_N | snatch_design_N | sustained_design_N |
|---|---|---|---|
| 1.00 | 72.000 | 453.560 | 2.197 |
| 1.05 | 75.600 | 476.240 | 2.307 |
| 2.00 | 144.000 | 907.120 | 4.393 |

## 已知限制

- 开伞冲击和分离冲击使用展开瞬间的速度。真实的峰值力出现在伞衣充气过程中，Cx 就是用来近似这一点的。
- 持续载荷对每个降落伞都使用展开时的整箭质量，这是保守的做法。
- 降落伞对应的分离位置按"最近"原则确定，只是一种启发式规则。如果选错了，请手动输入分离位置，或者调整降落伞在模型里的位置。
- 不支持多级火箭。已经分离的级的质量，会在结果窗口里表现为负的"未归入组件的质量"。
- 构件质量取自模型中的构件质量值，不考虑质量覆盖设置。
- 安全系数扫描是线性缩放，只是一张方便使用的表格，并没有引入新的物理。
- 仅在 OpenRocket 24.12 上测试过。

## 从源码构建（面向开发者）

环境要求：JDK 17 和 OpenRocket 24.12 的 jar。

1. 克隆本仓库。
2. 下载 `OpenRocket-24.12.jar`，复制到 `plug_in/lib/`。本仓库不包含这个 jar。
3. 在 `plug_in` 目录下运行：
   ```
   .\gradlew.bat jar            # 输出：build/libs/shock-calc-plugin-<版本号>.jar
   .\gradlew.bat installPlugin  # 构建并复制到 Windows 的 Plugins 目录
   ```
   在 macOS 和 Linux 上，请运行 `./gradlew jar`，再手动把 jar 复制到插件目录。目前 `installPlugin` 任务只支持 Windows。

OpenRocket 使用 `compileOnly` 引用，不会被打包进插件 jar。PNG 导出使用 OpenRocket jar 里的 JFreeChart；如果无法解析，删除 `ChartExporter.java` 和 `ResultsDialog.java` 里的 PNG 按钮即可。

## 项目结构

```
shock_calc/
├── README.md
├── .gitignore
├── .gitattributes
└── plug_in/
    ├── build.gradle
    ├── settings.gradle
    ├── lib/                              （OpenRocket jar，不提交）
    └── src/main/
        ├── java/sck_calc_plug_in/
        │   ├── MyExt.java                 扩展、参数、仿真监听器
        │   ├── MyExtProvider.java         注册扩展及其菜单项
        │   ├── MyExtConfigurator.java     配置界面（Swing）
        │   ├── RocketSections.java        划分火箭并读取各段质量
        │   ├── LoadCalculator.java        载荷公式
        │   ├── EventResult.java           单次展开事件的极限载荷
        │   ├── SafetyFactorSweep.java     扫描表格与 CSV 导出
        │   ├── ChartExporter.java         PNG 图表导出
        │   └── ResultsDialog.java         结果窗口
        └── resources/META-INF/services/
            ├── info.openrocket.core.simulation.extension.SimulationExtensionProvider
            └── info.openrocket.swing.simulation.extension.SwingSimulationExtensionConfigurator
```

## 常见问题

| 问题 | 检查项 |
|---|---|
| 菜单里没有这个扩展 | jar 是否在 Plugins 目录、是否重启过 OpenRocket、jar 里是否包含 `MyExtProvider.class` 和 `META-INF/services` 文件（`jar tf <jar>`） |
| 没有弹出结果窗口 | 仿真中是否至少有一个降落伞展开（查看飞行事件里有没有展开事件） |
| 分离段质量不符合预期 | 对照分离段表和你的模型；如果自动划分与设计不符，手动设置分离位置 |
| 某个降落伞对应了错误的分离位置 | 手动输入分离位置，或者把降落伞移到释放它的分离位置附近 |
| 打开 CSV 出现乱码 | 用 Excel 的"数据 → 从文本/CSV"导入，并选择 UTF-8 |
| 启动或运行时报错 | 在终端用 `java -jar OpenRocket-24.12.jar` 启动，查看控制台输出 |

## 后续计划

- 把计算结果写入 OpenRocket 自己的飞行数据和图表。
- 支持多级火箭。
- 让 `installPlugin` 支持 macOS 和 Linux。

## 许可证

待定。OpenRocket 以 GPL 协议发布，本插件依赖它的类，公开发布前请选择与之兼容的许可证。
