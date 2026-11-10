# Shock Load Calculator: an OpenRocket Plugin

[English](#shock-load-calculator-an-openrocket-plugin) | [中文](#中文)

**English** | [中文](#中文)

A simulation extension for [OpenRocket](https://openrocket.info/) 24.12 that estimates the loads a recovery system puts on a rocket and sizes the hardware that carries them. It reads the deployment conditions from the simulation and the section masses from the rocket model, computes the parachute opening force with the Pflanz method, the shock cord snatch load and the sustained load, and from the largest of them the minimum U-bolt thread size and bulkhead thickness. After a simulation finishes it opens a results window and lets you export the full table (CSV) and charts (PNG).

> **Disclaimer:** the formulas are first-order estimates. If you use the results for structural design, verify them against your team's reference method or test data.

## Features

- Adds a simulation extension, configured from **Edit simulation → Simulation options → Add extension**.
- Opening force by the **Pflanz method** (finite mass), integrated with gravity, with the force history of the inflation. The infinite-mass value of version 2.0 is still reported as an upper bound.
- Shock cord snatch load from the separation speed and the cord stiffness.
- Splits the rocket into sections automatically and reads the section masses from the model, including mass overrides.
- Handles several parachutes: every deployment is analysed separately, using the conditions at its own deployment time.
- Sizes the attachment bolt (metric and UNC thread) and the bulkhead thickness for the governing load.
- Sweeps the safety factor over a range (step 0.05) and reports limit loads and design loads.
- Exports the full sweep table as CSV, and the sweep chart and the inflation force curves as PNG.
- All settings are stored in the `.ork` file together with the simulation. Simulations saved with version 2.0 open normally.

### What changed in 3.0

- The opening shock is now the Pflanz finite-mass force by default. Version 2.0 used `½ρv²·CdS·Cx`, which ignores that the rocket slows down while the canopy fills; for a large main parachute this overestimates the force several times.
- The snatch load now uses a **separation speed** that you enter. Version 2.0 used the flight speed at deployment, which is not the speed at which the two bodies pull the cord taut.
- Section masses now respect mass overrides, and the mass of a fin set is no longer counted once per fin.
- New: governing load, bolt and bulkhead sizing, inflation table, inflation curve export.

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

1. Open a rocket that has at least one parachute, with the deployment events set as you will fly them.
2. Go to **Flight simulations**, create or edit a simulation, and open **Simulation options**.
3. Click **Add extension** and choose **Shock Loads → Shock Load Calculator**.
4. Set the parameters (see [docs/USAGE.md](docs/USAGE.md#6-settings)) and close the dialog.
5. Run the simulation. The results window opens when it finishes.
6. In the results window, use **Export table (CSV)...**, **Export chart (PNG)...** and **Export inflation curves (PNG)...** to save the results. **Copy summary** puts all four tables on the clipboard.

## Documentation

| Document | Contents |
|---|---|
| [docs/USAGE.md](docs/USAGE.md) | Preparing the model, sections and multiple separation points, every setting and how to choose it, reading the results, CSV columns, recommended workflow, troubleshooting |
| [docs/THEORY.md](docs/THEORY.md) | Derivation of every formula (Pflanz method with and without gravity, snatch load, bolt and bulkhead sizing), assumptions, numerical checks, comparison with the Ringo III flight, references |
| [specs.md](specs.md) | The original design notes of the project |

Main limitations, explained in the documents above:

- The opening force covers the inflation of the canopy. Short pulses between the separated bodies are represented only by the snatch load, whose inputs (separation speed, cord stiffness) you have to supply.
- A parachute is matched to the nearest joint, and by default every body tube starts a section. Check the Sections table after every change to the model.
- Only tension in the bolt thread and bending and shear of the bulkhead are checked. Single-stage rockets only. Tested with OpenRocket 24.12.

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

OpenRocket is referenced with `compileOnly`, so it is not bundled into the plugin jar. The PNG export uses JFreeChart from the OpenRocket jar; if it does not resolve, delete `ChartExporter.java` and the two PNG buttons in `ResultsDialog.java`.

## Project structure

```
shock_calc/
├── README.md
├── specs.md                          original design notes
├── docs/
│   ├── USAGE.md                      user guide (English and Chinese)
│   └── THEORY.md                     theory and verification (English and Chinese)
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
        │   ├── PflanzModel.java           opening force by the Pflanz method
        │   ├── LoadCalculator.java        the other load formulas
        │   ├── Sizing.java                bolt and bulkhead sizing, material tables
        │   ├── EventResult.java           results of one deployment event
        │   ├── Report.java                everything one simulation run produced
        │   ├── SafetyFactorSweep.java     sweep table and CSV export
        │   ├── ChartExporter.java         PNG chart export (sweep and inflation curves)
        │   └── ResultsDialog.java         results window
        └── resources/META-INF/services/
            ├── info.openrocket.core.simulation.extension.SimulationExtensionProvider
            └── info.openrocket.swing.simulation.extension.SwingSimulationExtensionConfigurator
```

## Troubleshooting

More cases are in [docs/USAGE.md](docs/USAGE.md#10-troubleshooting).

| Problem | Check |
|---|---|
| Extension missing from the menu | The jar is in the Plugins folder, OpenRocket was restarted, and the jar contains `MyExtProvider.class` and the `META-INF/services` files (`jar tf <jar>`) |
| No results window | At least one parachute deploys during the simulation (look for a deployment event in the flight events) |
| Unexpected section masses | Compare the sections table with your model; set the joint positions manually if the automatic split does not match your design |
| Opening shock much lower than in version 2.0 | Expected: the default method is now Pflanz. The infinite-mass value is in the inflation table, and the method can be switched in the settings |
| Wrong joint for a parachute | Enter the joint positions manually, or move the parachute closer to the joint that releases it |
| Garbled text when opening the CSV | Use Excel's Data → From Text/CSV and choose UTF-8 |
| Startup or runtime errors | Launch with `java -jar OpenRocket-24.12.jar` from a terminal and read the console output |

## Roadmap

- Write the results into OpenRocket's own flight data and plots.
- Model the two bodies and the cord after separation, so that the drogue phase is covered.
- Read the shock cord length and material from the model to estimate the cord stiffness.
- Support multi-stage rockets.
- Make `installPlugin` work on macOS and Linux.

## License

To be decided. OpenRocket is released under the GPL, and this plugin depends on its classes, so choose a compatible license before publishing.

---

# 中文

[English](#shock-load-calculator-an-openrocket-plugin) | **中文**

这是一个适用于 [OpenRocket](https://openrocket.info/) 24.12 的仿真扩展插件，用来估算回收系统施加在火箭上的载荷，并给承载这些载荷的零件选尺寸。它从仿真中读取开伞时的飞行状态，从火箭模型中读取各分离段的质量，用 Pflanz 方法计算开伞力，同时计算减震绳拉直冲击和持续载荷，再按其中最大的一个给出 U 型螺栓的最小螺纹规格和隔舱板的最小厚度。仿真结束后会弹出结果窗口，可以导出完整的表格（CSV）和图片（PNG）。

> **免责声明：** 公式均为一阶估算。如果要将结果用于结构设计，请用团队认可的方法或试验数据复核。

## 功能

- 添加一个仿真扩展，通过 **编辑仿真 → 仿真选项 → 添加扩展** 配置。
- 用 **Pflanz 方法**（有限质量）计算开伞力，积分时计入重力，并给出充气过程的力–时间曲线。2.0 版的无限质量算法仍会作为上限一并列出。
- 根据分离速度和减震绳刚度计算减震绳拉直冲击。
- 自动把火箭划分为若干段，并从模型中读取各段质量，质量覆盖（Override）也会被计入。
- 支持多个降落伞：每次展开单独分析，使用各自展开时刻的条件。
- 按控制载荷给出连接螺栓的规格（公制和 UNC 螺纹）和隔舱板厚度。
- 对安全系数做范围扫描（步长 0.05），同时给出极限载荷和设计载荷。
- 完整的扫描表格导出为 CSV，扫描图和充气力曲线导出为 PNG。
- 所有设置随仿真一起保存在 `.ork` 文件中。用 2.0 版保存的仿真可以正常打开。

### 3.0 版的变化

- 开伞冲击默认改为 Pflanz 有限质量算法。2.0 版使用 `½ρv²·CdS·Cx`，没有考虑伞衣充气期间火箭在减速；对大面积主伞，这会把力高估好几倍。
- 拉直冲击改用由你输入的**分离速度**。2.0 版用的是开伞时的飞行速度，而那并不是两段箭体把绳子拉直时的相对速度。
- 各段质量现在会计入质量覆盖；尾翼组的质量不再按每片尾翼重复计算。
- 新增：控制载荷、螺栓与隔舱板选型、充气参数表、充气曲线导出。

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

1. 打开一个至少带有一个降落伞的火箭，展开事件按实际飞行的方案设置。
2. 进入 **飞行仿真**，新建或编辑一个仿真，打开 **仿真选项**。
3. 点击 **添加扩展**，选择 **Shock Loads → Shock Load Calculator**。
4. 设置参数（见 [docs/USAGE.md](docs/USAGE.md#6-参数设置)）并关闭对话框。
5. 运行仿真，结束后会弹出结果窗口。
6. 在结果窗口里，用 **Export table (CSV)...**、**Export chart (PNG)...** 和 **Export inflation curves (PNG)...** 保存结果。**Copy summary** 会把四张表全部复制到剪贴板。

## 文档

| 文档 | 内容 |
|---|---|
| [docs/USAGE.md](docs/USAGE.md#中文) | 模型准备、分段与多个分离位置、每个参数的含义与取值方法、结果解读、CSV 各列、推荐流程、常见问题 |
| [docs/THEORY.md](docs/THEORY.md#中文) | 每个公式的推导（计入与不计重力的 Pflanz 方法、拉直冲击、螺栓与隔舱板选型）、假设、数值检验、与 Ringo III 飞行的对比、参考文献 |
| [specs.md](specs.md) | 项目最初的设计笔记 |

主要限制（详见上面的文档）：

- 开伞力描述的是伞衣充气过程。分离后两段箭体之间的短脉冲只由拉直冲击近似，而它的输入（分离速度、绳子刚度）需要你自己提供。
- 降落伞按"最近"原则对应到分离位置，默认每根箭体管都开始新的一段。每次修改模型后都要检查分段表。
- 只校核螺栓螺纹的拉伸以及隔舱板的弯曲和剪切。只支持单级火箭。仅在 OpenRocket 24.12 上测试过。

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

OpenRocket 使用 `compileOnly` 引用，不会被打包进插件 jar。PNG 导出使用 OpenRocket jar 里的 JFreeChart；如果无法解析，删除 `ChartExporter.java` 和 `ResultsDialog.java` 里的两个 PNG 按钮即可。

## 项目结构

```
shock_calc/
├── README.md
├── specs.md                          最初的设计笔记
├── docs/
│   ├── USAGE.md                      使用说明（中英文）
│   └── THEORY.md                     理论与验证（中英文）
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
        │   ├── PflanzModel.java           Pflanz 方法计算开伞力
        │   ├── LoadCalculator.java        其余载荷公式
        │   ├── Sizing.java                螺栓与隔舱板选型、材料表
        │   ├── EventResult.java           单次展开事件的结果
        │   ├── Report.java                一次仿真产生的全部结果
        │   ├── SafetyFactorSweep.java     扫描表格与 CSV 导出
        │   ├── ChartExporter.java         PNG 图表导出（扫描图和充气曲线）
        │   └── ResultsDialog.java         结果窗口
        └── resources/META-INF/services/
            ├── info.openrocket.core.simulation.extension.SimulationExtensionProvider
            └── info.openrocket.swing.simulation.extension.SwingSimulationExtensionConfigurator
```

## 常见问题

更多情况见 [docs/USAGE.md](docs/USAGE.md#10-常见问题)。

| 问题 | 检查项 |
|---|---|
| 菜单里没有这个扩展 | jar 是否在 Plugins 目录、是否重启过 OpenRocket、jar 里是否包含 `MyExtProvider.class` 和 `META-INF/services` 文件（`jar tf <jar>`） |
| 没有弹出结果窗口 | 仿真中是否至少有一个降落伞展开（查看飞行事件里有没有展开事件） |
| 分离段质量不符合预期 | 对照分离段表和你的模型；如果自动划分与设计不符，手动设置分离位置 |
| 开伞冲击比 2.0 版小很多 | 属于正常现象：默认方法已改为 Pflanz。无限质量的结果在充气表里，方法可以在设置中切换 |
| 某个降落伞对应了错误的分离位置 | 手动输入分离位置，或者把降落伞移到释放它的分离位置附近 |
| 打开 CSV 出现乱码 | 用 Excel 的"数据 → 从文本/CSV"导入，并选择 UTF-8 |
| 启动或运行时报错 | 在终端用 `java -jar OpenRocket-24.12.jar` 启动，查看控制台输出 |

## 后续计划

- 把计算结果写入 OpenRocket 自己的飞行数据和图表。
- 对分离后的两段箭体和绳子建模，覆盖减速伞阶段。
- 从模型中读取减震绳的长度和材料，用来估算绳子刚度。
- 支持多级火箭。
- 让 `installPlugin` 支持 macOS 和 Linux。

## 许可证

待定。OpenRocket 以 GPL 协议发布，本插件依赖它的类，公开发布前请选择与之兼容的许可证。
