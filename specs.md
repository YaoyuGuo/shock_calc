# Specifications for the programs
## Overview of the procedure

### OpenRocket 结合降落伞冲击与隔舱板载荷计算完整流程

为了为 STARR 项目构建一套完善的计算器，并完成 U 型螺栓（U-Bolt）及隔舱板（Bulkhead）的结构尺寸选型，你需要将 **OpenRocket 的气动与仿真数据** 作为输入，结合**固体力学与空气动力学冲击公式**进行后处理。

以下是详细的操作与计算流程：

---

### 第一步：在 OpenRocket 中建立模型并提取分段质量 ($m_{\text{suspended}}$)

降落伞展开时，作用在某个特定隔舱板上的力，仅取决于**该隔舱板下方所悬挂/连带的组件质量**，而非火箭的总质量。

1. **精准建模与质量覆盖（Mass Override）：**
* 在 OpenRocket 中按照实际结构建立组件树（Component Tree）。
* 对所有标准件（如飞控板、锂电池、降落伞、软绳、U 型螺栓等）使用 **Override**（质量覆盖）功能，手动输入实际称重数据，确保重心的几何分布与实际物理模型一致。


2. **提取挂载质量 ($m_{\text{suspended}}$)：**
* 点击菜单栏 **Tools $\rightarrow$ Component Analysis**（组件分析）。
* 找到回收系统中被冲击缆绳挂载的独立分段（例如：整流罩 + 头部箭体 + 载荷舱）。
* 记下该分段的总质量，即为悬挂质量 $m_{\text{suspended}}$（单位：$\text{kg}$）。



---

### 第二步：运行飞行仿真并提取开伞参数

在 OpenRocket 中设置好目标发动机、风速、风向及发射架角度后，运行 6-DOF 飞行仿真：

1. **导出仿真数据：**
* 进入 **Simulations $\rightarrow$ Plot / Export $\rightarrow$ Export Data**。
*勾选以下关键变量导出为 CSV 文件：
* `Time`（时间 $t$）
* `Altitude`（高度 $h$）
* `Total Velocity`（合速度 $V$）
* `Air Density`（空气密度 $\rho$）




2. **定位关键物理量：**
* **开伞瞬间速度 ($V_d$)：** 在导出数据中找到开伞事件（Apogee 或特定高度设定点）发生的时刻，提取该时刻的合速度 $V_d$。
* **开伞点空气密度 ($\rho$)：** 提取开伞高度下的空气密度（若未导出，可基于当地大气模型计算）。
* **稳态下降终速 ($V_{\text{term}}$)：** 查看降落伞完全张开后，火箭在匀速下降段的速度。



---

### 第三步：计算连续稳态载荷 (Continuous Estimated Load)

连续稳态载荷是指降落伞完全张开后，火箭在平稳降落过程中，悬挂质量对隔舱板施加的静态重力拉力。

$$\text{Load}_{\text{continuous}} = m_{\text{suspended}} \cdot g$$

* **物理意义：** 这是隔舱板和 U 型螺栓受到的**基线静载荷**。
* **单位：** 牛顿 ($\text{N}$)，其中 $g = 9.81\text{ m/s}^2$。

---

### 第四步：计算开伞动态冲击载荷 (Opening Shock Load)

降落伞从弹出到完全充气充盈（Inflation）的几百毫秒内，会产生极高的瞬态减速冲击力。常用的计算方法有两种：

#### 方法 A：动压与开伞冲击系数法 (Dynamic Pressure Method)

$$F_{\text{shock}} = C_k \cdot \left( \frac{1}{2} \rho V_d^2 \right) \cdot (C_d A)_{\text{chute}}$$

* $V_d$：OpenRocket 导出的开伞瞬间速度 ($\text{m/s}$)。
* $\rho$：开伞高度处的空气密度 ($\text{kg/m}^3$)。
* $(C_d A)_{\text{chute}}$：降落伞的风阻面积乘积（可在 OpenRocket 的 Parachute 参数页直接获取）。
* $C_k$：开伞冲击系数（Opening Shock Factor，取决于伞型，通常在 $1.2 \sim 1.8$ 之间；圆顶伞常用 $1.5$ 左右）。

#### 方法 B：速度平方比法 (Terminal Velocity Ratio Method)

$$F_{\text{shock}} = C_k \cdot m_{\text{suspended}} \cdot g \cdot \left( \frac{V_d}{V_{\text{term}}} \right)^2$$

* $V_{\text{term}}$：OpenRocket 仿真出的主伞/减速伞稳态下降速度 ($\text{m/s}$)。

---

### 第五步：结构设计与校验（U 型螺栓与隔舱板厚度）

获取最大冲击峰值 $F_{\text{shock}}$ 后，需引入**安全系数（Factor of Safety, $\text{FoS}$，航天模型通常取 $1.5 \sim 2.5$）** 计算设计载荷：

$$F_{\text{design}} = F_{\text{shock}} \cdot \text{FoS}$$

#### 1. U 型螺栓（U-Bolt）选型校验

标准 U 型螺栓通常有两个螺纹腿分担拉力，其所需最小应力截面积 $A_{\text{stress}}$ 为：

$$A_{\text{stress}} \ge \frac{F_{\text{design}}}{2 \cdot \sigma_{\text{yield}}}$$

* $\sigma_{\text{yield}}$：螺栓材料（如 304 不锈钢或 12.9 级高强度钢）的屈服强度。
* 据此计算出螺栓的最小公称直径 $d_{\text{bolt}}$。

#### 2. 隔舱板（Bulkhead）最大厚度与强度校验

隔舱板主要承受两种失效模式：**平板弯曲破坏** 与 **螺栓孔剪切冲切（Tear-Out）破坏**。

* **抗弯厚度校验（基于薄板弯曲理论）：**
将圆隔舱板简化为受 U 型螺栓集中力作用的圆盘，最小厚度 $t$ 应满足：

$$t \ge \sqrt{ \frac{3 F_{\text{design}}}{2 \pi \sigma_{\text{allowable}}} \left[ (1 + \nu) \ln\left(\frac{R}{r_{\text{bolt}}}\right) + 1 \right] }$$


* $\sigma_{\text{allowable}}$：隔舱板材料（如铝合金 6061-T6、玻纤或碳纤维板）的许用弯曲应力。
* $\nu$：材料的泊松比（如铝合金约 $0.33$）。
* $R$：隔舱板外半径；$r_{\text{bolt}}$：螺栓作用位置半径。


* **防冲剪/剪切撕裂校验（Shear Tear-Out）：**
防止 U 型螺栓直接从隔舱板中“撕裂拔出”，螺栓孔四周的最小厚度需满足：

$$t \ge \frac{F_{\text{design}}}{2 \pi d_{\text{hole}} \cdot \tau_{\text{allowable}}}$$


* $\tau_{\text{allowable}}$：隔舱板材料的许用剪切强度。



---

### 第六步：构建可调参数计算器 (Calculator Design Concept)

若要将上述逻辑编写为 Python 脚本或 Excel 计算工具，输入与输出架构建议如下：

* **输入参数（从 OpenRocket 导入或用户设定）：**
1. 悬挂质量 $m_{\text{suspended}}$ ($\text{kg}$)
2. 开伞速 $V_d$ 与 稳态速度 $V_{\text{term}}$ ($\text{m/s}$)
3. 空气密度 $\rho$ ($\text{kg/m}^3$) 与 降落伞 $C_d A$ ($\text{m}^2$)
4. 用户可调参数：安全系数 $\text{FoS}$、冲击系数 $C_k$、材料屈服强度/许用应力。


* **输出结果：**
1. 稳态连续载荷 $F_{\text{continuous}}$ ($\text{N}$)
2. 峰值冲击载荷 $F_{\text{shock}}$ ($\text{N}$)
3. U 型螺栓建议最小直径 $d_{\text{bolt}}$ ($\text{mm}$)
4. 隔舱板建议最小厚度 $t_{\text{bulkhead}}$ ($\text{mm}$)



---

### English Summary

To estimate recovery forces for the STARR project using OpenRocket:

1. **Extract Section Mass ($m_{\text{suspended}}$):** Use OpenRocket’s **Component Analysis** to record the isolated mass suspended below the specific recovery bulkhead.
2. **Export Simulation Parameters:** Run a 6-DOF simulation and export time-series data to extract deployment velocity ($V_d$), air density ($\rho$), and terminal descent velocity ($V_{\text{term}}$).
3. **Calculate Continuous Load:** Compute steady-state descent load:

$$\text{Load}_{\text{continuous}} = m_{\text{suspended}} \cdot g$$


4. **Calculate Opening Shock Load:** Apply dynamic inflation equations ($F_{\text{shock}} = C_k \cdot \frac{1}{2} \rho V_d^2 \cdot C_d A$) using OpenRocket kinematic outputs and an adjustable shock factor $C_k$.
5. **Structural Sizing (U-Bolts & Bulkheads):** Multiply $F_{\text{shock}}$ by a Factor of Safety ($\text{FoS}$) to derive design forces ($F_{\text{design}}$). Sizing formulas evaluate U-bolt tensile stress limits, bulkhead circular plate bending stresses, and hole shear tear-out to establish minimum required bulkhead thickness ($t$).