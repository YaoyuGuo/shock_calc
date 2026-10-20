在 OpenRocket 中，进行精准的火箭气动分析与 6-DOF 飞行仿真需要为各个几何模块、内部组件及环境设置提供详细参数。以下按**模块与零件分类**详细列出所有必须及可选输入的参数：

---

### 一、 整体与几何外形模块 (External Aerodynamic Components)

#### 1. 整流罩 / 鼻锥 (Nose Cone)

[ ] **几何形状 (Shape Type)**：圆锥形 (Conical)、抛物线形 (Parabolic)、圆弧形 (Ogive)、椭圆形 (Elliptical)、Power Series、Haack Series (如 Sears-Haack)。
[ ] **形状系数 (Shape Parameter)**：某些特定曲线形状所需的形变因子。
[ ] **物理尺寸**：
[ ] 长度 (Length)
[ ] 基底外径 (Base Diameter)
[ ] 壁厚 (Wall Thickness) 或选择“实心 (Solid)”


[ ] **轴肩参数 (Shoulder Parameters)**：
[ ] 轴肩外径 (Shoulder Diameter)
[ ] 轴肩长度 (Shoulder Length)
[ ] 轴肩壁厚 (Shoulder Wall Thickness)


[ ] **材料与表面**：组件材料 (Material)、表面粗糙度 (Surface Roughness，如 Smooth, Normal, Rough)。

#### 2. 箭体管 (Body Tube)

[ ] **物理尺寸**：
[ ] 外径 (Outer Diameter)
[ ] 内径 (Inner Diameter) 或 壁厚 (Wall Thickness)
[ ] 轴向总长度 (Length)


[ ] **材料与表面**：组件材料（决定密度与质量）、表面粗糙度。

#### 3. 渐变段 / 缩径管 (Transition)

[ ] **形状类型**：圆锥形 (Conical)、抛物线形 (Parabolic) 等。
[ ] **物理尺寸**：
[ ] 前端外径 (Fore Diameter)
[ ] 后端外径 (Aft Diameter)
[ ] 轴向长度 (Length)
[ ] 壁厚 (Wall Thickness)


[ ] **前/后轴肩 (Fore / Aft Shoulders)**：前端及后端轴肩的长度与外径。
[ ] **材料与表面**：材料类型、表面粗糙度。

#### 4. 尾翼组 (Fins) - 支持梯形、椭圆、自由绘制形状

[ ] **通用参数**：
[ ] 尾翼数量 (Number of Fins，如 3 或 4)
[ ] 根弦长 (Root Chord)
[ ] 顶弦长 (Tip Chord)
[ ] 翼展/高度 (Fin Span / Height)
[ ] 沿箭体周向的角度偏角 (Cant Angle，引起自旋)
[ ] 前缘后掠距离 (Sweep Length) 或 后掠角 (Sweep Angle)


[ ] **剖面形状 (Fin Cross-Section)**：
[ ] 形状：方形/平板 (Square/Flat)、圆角 (Rounded)、翼型/流线型 (Airfoil/Streamlined)。


[ ] **位置与材料**：
[ ] 轴向安装位置 (Position relative to parent tube)
[ ] 材料与表面粗糙度。



#### 5. 发射导轨块 / 导向纽扣 (Launch Lug / Rail Buttons)

[ ] **物理尺寸**：外径 (Outer Diameter)、内径/螺栓直径 (Inner Diameter)、长度 (Length)。
[ ] **位置**：轴向安装位置、周向角度。
[ ] **材料**：材料类型。

---

### 二、 内部结构与重力分配模块 (Internal & Structural Components)

#### 1. 内管 / 发动机座管 (Inner Tube / Motor Mount)

[ ] **尺寸参数**：外径、内径/壁厚、长度、轴向安装位置。
[ ] **发动机座标记 (Motor Mount Designation)**：勾选是否作为发动机安装座（允许装载发动机）。

#### 2. 定位环 (Centering Ring) / 隔舱板 (Bulkhead)

[ ] **尺寸参数**：
[ ] 外径 (Outer Diameter，配合外管内径)
[ ] 内径 (Inner Diameter，隔舱板此项设为 0)
[ ] 板材厚度 (Thickness)
[ ] 轴向安装位置 (Position)


[ ] **材料**：材料类型（如胶合板、玻纤、铝合金 6061-T6 等）。

#### 3. 箭体连接管 / 耦合器 (Coupler)

[ ] **尺寸参数**：外径 (Outer Diameter)、内径/壁厚、轴向长度 (Length)、安装位置。

#### 4. 质量块 / 额外载荷 / 航模电子舱 (Mass Component / Payload)

[ ] **质量 (Mass)**：具体重量值（可覆写算出的结构重量）。
[ ] **轴向位置 (Position)**：距参考点（如鼻锥尖端）的绝对位置。
[ ] **几何特性（可选）**：长度与半径（用于更精确计算转动惯量 $I_{xx}, I_{yy}$）。
[ ] **组件类型标记**：未指定 (Unspecified)、载荷 (Payload)、航电 (Altimeter/Avionics)、回收硬件 (Recovery Hardware)、配重 (Ballast)。

---

### 三、 回收系统模块 (Recovery System)

#### 1. 降落伞 (Parachute) / 减速伞 (Drogue)

[ ] **气动与几何参数**：
[ ] 伞面展开直径 (Diameter)
[ ] 阻力系数 ($C_d$，通常圆顶伞取 $0.75 \sim 0.80$，交叉伞/椭圆伞取 $1.5 \sim 2.2$)
[ ] 伞绳数量 (Number of Lines) 与 单条伞绳长度 (Line Length)


[ ] **打包物理属性**：打包状态的质量 (Packed Mass)、打包体积/轴向位置。
[ ] **触发事件 (Deployment Event)**：
[ ] 最高点触发 (At Apogee)
[ ] 指定高度触发 (At Specific Altitude)
[ ] 指定时间触发 (At Specific Time / Delay)



#### 2. 拔伞绳 / 弹力绳 (Shock Cord)

[ ] **参数**：绳长 (Length)、线密度/总质量 (Mass)、材料（如凯夫拉 Kevlar、尼龙）、安装位置。

---

### 四、 动力与仿真环境设置 (Motor & Simulation Parameters)

#### 1. 发动机配置 (Motor Configuration)

[ ] **发动机型号 (Motor Selection)**：从内置数据库选定（如 Estes, Aerotech, Cesaroni）或导入第三方 `.rse` / `.eng` 文件。
[ ] **延时药时间 (Ejection Charge Delay)**：延时秒数（如 4s, 8s, 或 0s / Unfilled）。
[ ] **轴向位置偏移 (Motor Position)**：发动机在座管内的深入程度。

#### 2. 发射场与大气环境 (Launch & Environmental Conditions)

[ ] **发射架参数**：
[ ] 发射轨/导向杆长度 (Launch Rail / Rod Length)
[ ] 发射架倾角 (Launch Angle，相对垂直线的倾角，如 $0^\circ \sim 15^\circ$)
[ ] 发射架方位角 (Heading Angle，相对正北方向)


[ ] **大气与风速设置**：
[ ] 发射场海拔高度 (Altitude)
[ ] 大气压 (Pressure) 与 环境温度 (Temperature)
[ ] 平均风速 (Average Wind Speed) 与 风向 (Wind Direction)
[ ] 湍流强度 (Turbulence Intensity，如 Light, Medium, Severe)



---

### 极度关键的功能：质量与重心覆盖 (Mass & CG Override)

对于任何组件或子系统，OpenRocket 均提供 **Override**（覆盖）选项页：

[ ] **Override Mass**：直接用实际称重值替代软件基于体积和材料密度自动计算出的质量。
[ ] **Override Center of Gravity**：直接手动输入实际测量得到的重心 (CG) 轴向位置。

---

### English Summary

To set up a complete and accurate flight simulation in OpenRocket, parameters must be configured across four main categories:

1. **External Aerodynamics:** Dimensions (length, diameters, thickness), geometry shapes (conical, ogive, parabolic, Haack), fin planforms (root/tip chord, span, sweep angle, cross-section profile), surface roughness, and material densities for the nose cone, body tubes, transitions, fins, and launch lugs.
2. **Internal Structure & Masses:** Inner tube dimensions, bulkhead/centering ring thickness and diameters, shock cord length/mass, avionics/ballast mass, axial position, and moment of inertia estimations.
3. **Recovery System:** Parachute canopy diameter, drag coefficient ($C_d$), line count and length, packed mass, deployment trigger events (apogee, specified altitude, or time delay), and shock cord properties.
4. **Motors & Environment:** Motor designation, ejection delay, launch rail length, launch angle, site altitude, air temperature/pressure, average wind velocity, wind direction, and turbulence level.
[ ]Note: The **Mass & Center of Gravity (CG) Override** tab allows users to manually replace software-calculated values with actual physical measurements for enhanced accuracy.[ ]