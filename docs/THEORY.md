# Shock Load Calculator 3.0: Theory

[English](#english) | [中文](#中文)

This document explains how every number in the results window is computed, which assumptions are behind it, and how the model was checked. How to use the plugin is in [USAGE.md](USAGE.md).

---

<a id="english"></a>
# English

## 1. Overview

During recovery, the bolts and bulkheads that hold the shock cord see three kinds of load at different times:

1. **Snatch load.** After the ejection charge separates the rocket, the two bodies move apart until the shock cord goes taut. The cord stops their relative motion within tens of milliseconds.
2. **Opening force.** The canopy fills with air. Its drag area grows from almost zero to the full value while the rocket is still fast, so the force peaks during inflation.
3. **Sustained load.** Once the canopy is open and the descent is steady, the cord carries the weight of the rocket.

The plugin computes each of them from the simulation, takes the largest as the **governing load**, multiplies it by a safety factor to get the **design load**, and sizes the bolt and the bulkhead for that load.

```
OpenRocket simulation ──► state at deployment (v, h, ρ, m) ─┐
OpenRocket model ───────► section masses, parachute Cd, S, D0 ┤
                                                               ▼
                 opening force (Pflanz)   snatch load   sustained load
                                    └──── max ────┘
                                         governing load × SF = design load
                                                      ▼
                                bolt stress area, bulkhead thickness
```

All quantities are in SI units: N, kg, m, s, Pa.

## 2. Inputs from the simulation

The plugin registers a listener in the simulation. After every simulation step it checks the set of deployed recovery devices. When a parachute appears in that set for the first time, the state at the end of that step is recorded:

| Quantity | Source |
|---|---|
| Velocity $\vec v$ | Rocket velocity from the simulation. Horizontal part $v_h = \sqrt{v_x^2 + v_y^2}$, vertical part $v_z$ (positive up), magnitude $v = \lvert \vec v \rvert$ |
| Altitude $h$ | Rocket position above the launch site |
| Mass $m$ | Total rocket mass at that time step (includes the remaining propellant) |
| Air density $\rho$ | $\rho = P / (R\,T)$, with pressure $P$ and temperature $T$ from the atmosphere model and $R = 287.05$ J/(kg·K) |
| $C_d$, $S$, $D_0$ | Parachute component: drag coefficient, reference area, nominal diameter |

The velocity is the velocity relative to the ground. Wind is not subtracted, so the airspeed at deployment can differ from $v$ when there is wind.

## 3. Sections and section masses

### 3.1 Joints

A **joint** is an axial position where the rocket separates. By default the start of every body tube is a joint. The user can instead give the joint positions explicitly. With $N$ joints at positions $x_1 < x_2 < \dots < x_N$ (measured from the nose tip), the rocket has $N + 1$ sections.

### 3.2 Mass of a section

Each component is assigned to the section that contains its axial centre $x_c = x_{start} + L/2$. Its mass is taken as OpenRocket uses it:

- the override value if the mass is overridden;
- zero if an ancestor overrides "subcomponent mass", because the ancestor's value already contains it;
- otherwise the computed component mass.

A component with several instances (a fin set, a cluster) has its mass shared equally between its instance positions.

The total rocket mass $m$ from the simulation includes the motor, which is not a component. The difference

$$m_{residual} = m - \sum_i m_i$$

is added to the last section. A negative residual means that the model contains mass that is not in the simulated rocket, which happens with multi-stage rockets.

### 3.3 Matching a parachute to a joint

A parachute is matched to the joint closest to its own centre. For that joint $k$:

$$m_{upper} = \sum_{i \le k} m_i, \qquad m_{lower} = \sum_{i > k} m_i, \qquad m_{upper} + m_{lower} = m$$

## 4. Opening force: the Pflanz method

### 4.1 Model

Between line stretch ($t = 0$) and the end of filling ($t = t_f$) the drag area of the canopy is assumed to grow as a power of time:

$$(C_dS)(t) = C_dS \cdot \left(\frac{t}{t_f}\right)^j, \qquad 0 \le t \le t_f$$

$C_dS$ is the drag area of the open canopy and $j$ the inflation exponent. Pflanz used $j = 2$ for solid cloth canopies, $j = 1$ for slotted canopies and $j = 1/2$ for reefed ones.

The filling time comes from the canopy fill constant $n$: a canopy fills after travelling about $n$ nominal diameters,

$$t_f = \frac{n\,D_0}{v_0}$$

where $v_0$ is the speed at line stretch. Solid cloth canopies have $n \approx 8$.

The opening force at time $t$ is the drag of the partly filled canopy, multiplied by the opening-force coefficient $C_x$:

$$F(t) = C_x \cdot \tfrac12 \rho\, v(t)^2 \cdot (C_dS)(t)$$

$C_x$ is the ratio between the peak opening force and the steady drag in an infinite-mass test (the speed stays constant), and covers effects such as overinflation of the canopy. Typical values are 1.5 to 1.8 for solid cloth canopies and 1.0 to 1.2 for slotted ones.

### 4.2 Infinite mass

If the rocket did not slow down during filling, $v(t) = v_0$ and the peak is reached at $t = t_f$:

$$F_\infty = C_x \cdot q_0 \cdot C_dS, \qquad q_0 = \tfrac12 \rho v_0^2$$

This is the formula of version 2.0. It is an upper bound: a real rocket slows down while the canopy fills, so the drag area grows while the speed falls.

### 4.3 Finite mass without gravity: the classical Pflanz solution

Let the whole rocket (mass $m$) decelerate under the drag of the filling canopy, with gravity neglected:

$$m\frac{dv}{dt} = -\tfrac12 \rho v^2 \, C_dS \left(\frac{t}{t_f}\right)^j$$

With $\tau = t / t_f$, divide by $-m v^2$ and integrate:

$$\frac{d}{dt}\left(\frac1v\right) = \frac{\rho\, C_dS}{2m}\,\tau^j
\quad\Rightarrow\quad
\frac1v = \frac1{v_0} + \frac{\rho\, C_dS\, t_f}{2m\,(j+1)}\,\tau^{\,j+1}$$

Define the **ballistic parameter**

$$A = \frac{2m}{\rho\, C_dS\, v_0\, t_f}$$

Then

$$\frac{v_0}{v} = 1 + \frac{\tau^{\,j+1}}{A\,(j+1)}$$

and the force, divided by its infinite-mass value, is

$$x(\tau) = \frac{F}{F_\infty} = \frac{\tau^{\,j}}{\left(1 + \dfrac{\tau^{\,j+1}}{A\,(j+1)}\right)^2}$$

The **force reduction factor** $X_1$ is the maximum of $x(\tau)$ for $0 \le \tau \le 1$. Setting $d \ln x / d\tau = 0$:

$$\frac{j}{\tau} = \frac{2\,\tau^{\,j}/A}{1 + \tau^{\,j+1}/(A(j+1))}
\quad\Rightarrow\quad
\tau_*^{\,j+1} = \frac{A\,j\,(j+1)}{j+2}$$

If $\tau_* \le 1$ the peak is during filling and $X_1 = x(\tau_*)$. If $\tau_* > 1$ the force is still rising when the canopy is full, and $X_1 = x(1)$. The opening force is

$$F = C_x \cdot q_0 \cdot C_dS \cdot X_1$$

Two properties are useful in practice:

- With $t_f = nD_0/v_0$ the speed cancels out: $A = 2m / (\rho\, C_dS\, n\, D_0)$. In the classical solution $X_1$ depends only on the mass, the canopy and the air density, and the force grows with $v_0^2$.
- A large $A$ (heavy rocket, small canopy) gives $X_1 \to 1$, the infinite-mass case. A small $A$ (light rocket, large canopy, which is typical for main parachutes of amateur rockets) gives a small $X_1$.

The results window shows this value as **X1 Pflanz**.

### 4.4 Finite mass with gravity

A main parachute usually opens while the rocket falls almost vertically. Gravity then keeps accelerating the rocket during filling, which the classical solution ignores. The plugin therefore integrates the vector equation of motion

$$\frac{d\vec v}{dt} = \vec g - \frac{\rho\, C_dS}{2m}\, r(t)\, \lvert \vec v \rvert\, \vec v, \qquad r(t) = \left(\frac{t}{t_f}\right)^j$$

in the vertical plane, starting from the horizontal and vertical speed at deployment. The force history is

$$F(t) = C_x \cdot \tfrac12 \rho\, \lvert \vec v(t) \rvert^2 \cdot C_dS \cdot r(t)$$

and the opening force is its maximum over $0 \le t \le t_f$. The results window reports the equivalent factor $X_1 = F_{max} / F_\infty$ as **X1 + gravity**. For a vertical descent it is larger than the classical value; for a horizontal path at high speed the two agree.

Numerical method: classical fourth-order Runge–Kutta with 4000 fixed steps over $t_f$. The force curve exported as PNG has 200 points.

Details:

- $C_x$ multiplies the force but not the deceleration in the equation of motion. The rocket is therefore decelerated by the nominal drag area only, which slightly overestimates the speed and is conservative.
- If $v_0 < 1$ m/s (for example a parachute released exactly at apogee), $t_f$ is evaluated with 1 m/s to avoid an infinite filling time.
- The mass $m$ is the whole rocket: after line stretch both bodies are assumed to move together.

### 4.5 How the opening force divides between the bodies

If the two bodies move together with the common acceleration $a = F/m$, the force that has to pass into each body is

$$F_{upper} = F\,\frac{m_{upper}}{m}, \qquad F_{lower} = F\,\frac{m_{lower}}{m}$$

The inflation table reports both shares. The sizing, however, uses the full force $F$, because the bolt that the parachute riser is tied to carries all of it.

## 5. Snatch load

### 5.1 Model

After separation the two bodies (masses $m_1$, $m_2$) move apart with relative speed $\Delta v$. When the cord becomes taut, it acts as a linear spring of stiffness $k$. In the centre-of-mass frame the kinetic energy of the relative motion is

$$E = \tfrac12\,\mu\,\Delta v^2, \qquad \mu = \frac{m_1 m_2}{m_1 + m_2}$$

At the largest stretch $x_{max}$ all of it is stored in the cord:

$$\tfrac12\,\mu\,\Delta v^2 = \tfrac12\,k\,x_{max}^2
\quad\Rightarrow\quad
F_{snatch} = k\,x_{max} = \Delta v\,\sqrt{k\,\mu}$$

The same force acts on both bulkheads, in opposite directions.

Assumptions: the cord is linear-elastic and undamped, it starts slack, and external forces (drag, gravity) are small during the short event. Damping and plastic stretch of a real cord reduce the peak, so the formula is conservative for a given $\Delta v$ and $k$.

The pulse lasts about half an oscillation period:

$$t_{pulse} \approx \pi\sqrt{\mu / k}$$

### 5.2 Cord stiffness

For a cord of length $L$, axial stiffness $EA$:

$$k = \frac{EA}{L} \approx \frac{F_{break}}{\varepsilon_{break}\,L}$$

where $F_{break}$ is the breaking strength and $\varepsilon_{break}$ the elongation at break from the data sheet, assuming a linear force–stretch curve. A stiffer material, or a shorter cord, gives a larger snatch load.

### 5.3 Separation speed

$\Delta v$ is a user input. Two effects contribute to it: the push of the ejection charge, and the difference in drag when the parachute starts to pull one body back while the other keeps moving. The second can approach the descent speed at deployment. Flight data are the best source.

## 6. Sustained load

$$F_{sustained} = m\,g, \qquad g = 9.80665\ \text{m/s}^2$$

The whole rocket mass at deployment is used for every parachute, which is conservative.

## 7. Governing load and design load

The three loads occur at different times, so they are not added. The governing (limit) load is

$$F_{limit} = \max\left(F_{open},\ F_{snatch},\ F_{sustained}\right)$$

where $F_{open}$ is the finite-mass value by default, or $F_\infty$ if the infinite-mass method is selected. Loads that are not available (no joint, hence no snatch load) are ignored.

The design load is

$$F_{design} = SF \cdot F_{limit}$$

The exported sweep repeats this for $SF$ from the minimum to the maximum in steps of 0.05. $C_x$ already contains the overshoot of the opening force, so it should not be counted again in the safety factor.

## 8. Bolt sizing

A U-bolt has two threaded legs that share the load; an eye-bolt has one. The full design load is assumed to pass through one attachment. The required **tensile stress area** per leg is

$$A_s \ge \frac{F_{design}}{n_{legs} \cdot \sigma_{allow}}$$

$\sigma_{allow}$ is the proof strength of the bolt material (the stress it carries without permanent set), or the 0.2 % yield strength where no proof strength is defined.

The plugin reports the smallest standard coarse thread whose stress area is at least $A_s$. Stress areas follow the usual definitions, $A_s = \frac{\pi}{4}(d - 0.9382\,P)^2$ for metric threads (pitch $P$) and $A_s = 0.7854\,(D - 0.9743/n)^2$ for UNC threads ($n$ threads per inch):

| Metric | $A_s$ (mm²) | UNC | $A_s$ (mm²) |
|---|---|---|---|
| M3 | 5.03 | #6-32 | 5.86 |
| M4 | 8.78 | #8-32 | 9.03 |
| M5 | 14.2 | #10-24 | 11.3 |
| M6 | 20.1 | 1/4-20 | 20.5 |
| M8 | 36.6 | 5/16-18 | 33.8 |
| M10 | 58.0 | 3/8-16 | 50.0 |
| M12 | 84.3 | 7/16-14 | 68.6 |
| M14 | 115 | 1/2-13 | 91.5 |
| M16 | 157 | 5/8-11 | 146 |
| M20 | 245 | 3/4-10 | 215 |
| M24 | 353 | | |

Only tension in the thread is checked. Bending of the U-bolt, thread stripping in the nut or the bulkhead, and the strength of quick links, swivels and the cord itself are outside the model.

## 9. Bulkhead sizing

### 9.1 Bending

The bulkhead is treated as a simply supported circular plate of radius $R$, loaded at the centre over a circle of radius $r$ (half the leg spacing of a U-bolt, or the washer radius of an eye-bolt). The largest bending stress is (Roark, simply supported plate with central load over a circle):

$$\sigma_{max} = \frac{3F}{2\pi t^2}\left[(1+\nu)\ln\frac{R}{r} + 1\right]$$

Setting $\sigma_{max} = \sigma_{allow}$ gives the minimum thickness

$$t_{bend} = \sqrt{\frac{3F_{design}}{2\pi\,\sigma_{allow}}\left[(1+\nu)\ln\frac{R}{r} + 1\right]}$$

$R$ is the inner radius of the body tube at the joint unless the user enters a value. A bulkhead that is bonded all around is closer to a clamped plate, which has lower stress; the simply supported case is the conservative one.

### 9.2 Pull-through shear

Each leg could push a cylinder of diameter $d$ (the washer or nut) out of the plate. The shear area is $n_{legs}\,\pi\,d\,t$, so

$$t_{shear} = \frac{F_{design}}{n_{legs}\,\pi\,d\,\tau_{allow}}$$

The required thickness is the larger of $t_{bend}$ and $t_{shear}$.

These formulas assume an isotropic plate. Plywood and composite plates are not isotropic, so the built-in strengths for them are rough values.

## 10. Verification

### 10.1 Numerical check

The integration without gravity was compared with the closed-form solution of section 4.3 for $j$ = 0.5, 1, 2, 6 and $A$ = 0.01 to 50. The largest relative difference in $X_1$ was $2\times10^{-5}$. The integration with gravity was checked against an independent solver (SciPy, tolerance $10^{-10}$) for the Ringo III main parachute: both give 2491.9 N at 0.446 s.

### 10.2 Comparison with the Ringo III flight

The Ringo III OpenRocket model was simulated with the plugin and compared with the 500 Hz accelerometer record of the flight of 20 August 2024.

| Quantity | Plugin | Flight |
|---|---|---|
| Apogee time | 37.1 s | about 36.5 s |
| Main deployment speed | 32.2 m/s | not measured |
| Main inflation, peak deceleration | 9.4 g (n = 8); 6.4 g (n = 17) | 5 to 6.6 g, lasting about 1.3 s |
| Main inflation, filling time | 0.61 s (n = 8); 1.29 s (n = 17) | about 1.3 s |
| Largest axial pulse on the upper body (10.5 kg) | snatch 2.5 kN (defaults); 6.6 kN ($\Delta v$ = 13 m/s, $k$ = 40 kN/m) | about 52 g for about 50 ms, i.e. about 5.4 kN |
| Drogue, peak deceleration | 0.4 g | 2 to 5 g for about 1 s |

Findings:

- **Main parachute.** With $n = 17$ both the duration and the peak of the measured inflation are reproduced. The default $n = 8$ is conservative by a factor of about 1.5 for this rocket.
- **Snatch.** The largest loads of the flight were short pulses of 10 to 60 ms, larger than the inflation load. With $\mu = 6.4$ kg and $k$ = 40 kN/m, the pulse duration from section 5.1 is $\pi\sqrt{6.4/40000} \approx 40$ ms, close to the measured 50 ms. The snatch settings therefore matter more than the canopy settings for bolt sizing.
- **Drogue.** Not reproduced. At apogee the speed is low, so the Pflanz force is small; the measured load comes from the two bodies moving independently on the cord after separation, which a single-body model does not describe.

These comparisons use one flight. Treat the calibrated values as a starting point for similar rockets, not as general constants.

## 11. Assumptions and limits

| Assumption | Effect |
|---|---|
| State at the simulation step of deployment; no speed gained between ejection and line stretch | Underestimates the opening force of a drogue released at apogee |
| Ground-relative velocity, wind not subtracted | Airspeed can differ from the value used |
| Whole rocket as one body during inflation | Pulses between the bodies are covered only by the snatch formula |
| Drag of an already open drogue ignored when the main inflates | Slightly conservative |
| $C_x$, $n$, $j$ empirical | Calibrate with flight data where possible |
| Cord linear and undamped | Conservative for a given $\Delta v$ and $k$ |
| Full design load through one bolt | Conservative for the second bolt of a joint |
| Plate simply supported, isotropic | Conservative for metal; approximate for wood and composites |
| Thread tension only | Bending, stripping and other hardware not checked |
| Single-stage rockets only | Multi-stage gives a negative residual mass |

## 12. References

1. T. W. Knacke, *Parachute Recovery Systems Design Manual*, NWC TP 6575, Naval Weapons Center, 1992. Chapter 5: opening force, Pflanz method, $C_x$, $X_1$, fill constant.
2. NSWC TR 86-142, Naval Surface Weapons Center, 1986 (DTIC ADA170962). Pflanz method with drag-area exponents 1/2, 1 and 2, ballistic mass ratio.
3. W. C. Young, R. G. Budynas, *Roark's Formulas for Stress and Strain*, McGraw-Hill. Circular plates with a central load.
4. ISO 898-1 (steel bolts), ISO 3506-1 (stainless steel bolts), SAE J429 (inch bolts): proof strengths.
5. ISO 898-1 and ASME B1.1: tensile stress areas of threads.

---

<a id="中文"></a>
# 中文

## 1. 概述

回收过程中，固定减震绳的螺栓和隔舱板会在不同时刻承受三种载荷：

1. **拉直冲击：** 抛射药把火箭分开后，两段箭体相互远离，直到减震绳被拉直。绳子在几十毫秒内消除两者的相对运动。
2. **开伞力：** 伞衣充气。它的阻力面积在火箭仍然很快时从几乎为零增长到全值，所以力在充气过程中出现峰值。
3. **持续载荷：** 伞衣张开、下降稳定后，绳子承受火箭的重量。

插件根据仿真计算这三种载荷，取最大的作为**控制载荷**，乘以安全系数得到**设计载荷**，再按这个载荷选螺栓和隔舱板。

```
OpenRocket 仿真 ──► 展开时的状态 (v, h, ρ, m) ─────┐
OpenRocket 模型 ──► 各段质量、伞的 Cd、S、D0 ──────┤
                                                    ▼
          开伞力（Pflanz）     拉直冲击      持续载荷
                       └──── 取最大 ────┘
                         控制载荷 × SF = 设计载荷
                                  ▼
                     螺栓应力面积、隔舱板厚度
```

所有量都使用国际单位：N、kg、m、s、Pa。

## 2. 来自仿真的输入

插件在仿真中注册一个监听器。每个仿真步结束后，它检查已展开的回收装置集合。当某个降落伞第一次出现在集合里时，记录该步结束时的状态：

| 量 | 来源 |
|---|---|
| 速度 $\vec v$ | 仿真中的火箭速度。水平分量 $v_h = \sqrt{v_x^2 + v_y^2}$，竖直分量 $v_z$（向上为正），大小 $v = \lvert \vec v \rvert$ |
| 高度 $h$ | 火箭相对发射场的高度 |
| 质量 $m$ | 该时刻火箭的总质量（包括剩余推进剂） |
| 空气密度 $\rho$ | $\rho = P / (R\,T)$，压强 $P$ 和温度 $T$ 来自大气模型，$R = 287.05$ J/(kg·K) |
| $C_d$、$S$、$D_0$ | 降落伞组件：阻力系数、参考面积、名义直径 |

速度是相对地面的速度，没有扣除风速。有风时，展开时的空速可能与 $v$ 不同。

## 3. 分段与各段质量

### 3.1 分离位置

**分离位置**是火箭分开的轴向位置。默认每根箭体管的起点都是一个分离位置；用户也可以直接输入分离位置。$N$ 个分离位置 $x_1 < x_2 < \dots < x_N$（从箭头尖端量起）把火箭分成 $N + 1$ 段。

### 3.2 每段的质量

每个构件归入其轴向中心 $x_c = x_{start} + L/2$ 所在的段。其质量按 OpenRocket 的方式取：

- 如果设置了质量覆盖，取覆盖值；
- 如果某个上级构件覆盖了"子构件质量"，取零，因为上级的值已经包含了它；
- 否则取计算出的构件质量。

有多个实例的构件（尾翼组、集束）把质量平均分配到各个实例的位置。

仿真给出的火箭总质量 $m$ 包括发动机，而发动机不是构件。差值

$$m_{residual} = m - \sum_i m_i$$

加到最后一段。差值为负说明模型里有不属于本次仿真火箭的质量，多级火箭会出现这种情况。

### 3.3 降落伞与分离位置的对应

降落伞对应到离其中心最近的分离位置。对该分离位置 $k$：

$$m_{upper} = \sum_{i \le k} m_i, \qquad m_{lower} = \sum_{i > k} m_i, \qquad m_{upper} + m_{lower} = m$$

## 4. 开伞力：Pflanz 方法

### 4.1 模型

从伞绳拉直（$t = 0$）到充气结束（$t = t_f$），假设伞衣的阻力面积按时间的幂次增长：

$$(C_dS)(t) = C_dS \cdot \left(\frac{t}{t_f}\right)^j, \qquad 0 \le t \le t_f$$

$C_dS$ 是伞衣张开后的阻力面积，$j$ 是充气指数。Pflanz 对实心伞取 $j = 2$，开缝伞取 $j = 1$，收口伞取 $j = 1/2$。

充气时间由充气常数 $n$ 决定：伞衣大约在走过 $n$ 个名义直径的距离后充满，

$$t_f = \frac{n\,D_0}{v_0}$$

其中 $v_0$ 是伞绳拉直时的速度。实心伞的 $n \approx 8$。

$t$ 时刻的开伞力是部分充气的伞衣的阻力乘以开伞力系数 $C_x$：

$$F(t) = C_x \cdot \tfrac12 \rho\, v(t)^2 \cdot (C_dS)(t)$$

$C_x$ 是无限质量试验（速度保持不变）中峰值开伞力与稳态阻力之比，包含了伞衣过度充气等效应。典型值：实心伞 1.5 到 1.8，开缝伞 1.0 到 1.2。

### 4.2 无限质量

如果充气期间火箭不减速，$v(t) = v_0$，峰值出现在 $t = t_f$：

$$F_\infty = C_x \cdot q_0 \cdot C_dS, \qquad q_0 = \tfrac12 \rho v_0^2$$

这就是 2.0 版的公式。它是一个上限：真实的火箭在伞衣充气期间会减速，阻力面积增长的同时速度在下降。

### 4.3 不计重力的有限质量情况：Pflanz 经典解

设整枚火箭（质量 $m$）在充气伞衣的阻力作用下减速，忽略重力：

$$m\frac{dv}{dt} = -\tfrac12 \rho v^2 \, C_dS \left(\frac{t}{t_f}\right)^j$$

令 $\tau = t / t_f$，两边除以 $-m v^2$ 后积分：

$$\frac{d}{dt}\left(\frac1v\right) = \frac{\rho\, C_dS}{2m}\,\tau^j
\quad\Rightarrow\quad
\frac1v = \frac1{v_0} + \frac{\rho\, C_dS\, t_f}{2m\,(j+1)}\,\tau^{\,j+1}$$

定义**弹道参数**

$$A = \frac{2m}{\rho\, C_dS\, v_0\, t_f}$$

则

$$\frac{v_0}{v} = 1 + \frac{\tau^{\,j+1}}{A\,(j+1)}$$

力与其无限质量值之比为

$$x(\tau) = \frac{F}{F_\infty} = \frac{\tau^{\,j}}{\left(1 + \dfrac{\tau^{\,j+1}}{A\,(j+1)}\right)^2}$$

**开伞力折减系数** $X_1$ 是 $x(\tau)$ 在 $0 \le \tau \le 1$ 上的最大值。令 $d \ln x / d\tau = 0$：

$$\frac{j}{\tau} = \frac{2\,\tau^{\,j}/A}{1 + \tau^{\,j+1}/(A(j+1))}
\quad\Rightarrow\quad
\tau_*^{\,j+1} = \frac{A\,j\,(j+1)}{j+2}$$

若 $\tau_* \le 1$，峰值出现在充气过程中，$X_1 = x(\tau_*)$；若 $\tau_* > 1$，伞衣充满时力还在上升，$X_1 = x(1)$。开伞力为

$$F = C_x \cdot q_0 \cdot C_dS \cdot X_1$$

有两个实用的性质：

- 代入 $t_f = nD_0/v_0$ 后速度被消去：$A = 2m / (\rho\, C_dS\, n\, D_0)$。在经典解中，$X_1$ 只取决于质量、伞衣和空气密度，力随 $v_0^2$ 增长。
- $A$ 大（箭重、伞小）时 $X_1 \to 1$，即无限质量情况。$A$ 小（箭轻、伞大，业余火箭的主伞通常如此）时 $X_1$ 很小。

结果窗口中这个值显示为 **X1 Pflanz**。

### 4.4 计入重力的有限质量情况

主伞通常在火箭几乎竖直下落时打开。这时重力在充气期间仍在使火箭加速，而经典解忽略了这一点。因此插件在竖直平面内对矢量运动方程

$$\frac{d\vec v}{dt} = \vec g - \frac{\rho\, C_dS}{2m}\, r(t)\, \lvert \vec v \rvert\, \vec v, \qquad r(t) = \left(\frac{t}{t_f}\right)^j$$

做数值积分，初始条件是展开时的水平和竖直速度。力的时程为

$$F(t) = C_x \cdot \tfrac12 \rho\, \lvert \vec v(t) \rvert^2 \cdot C_dS \cdot r(t)$$

开伞力取它在 $0 \le t \le t_f$ 上的最大值。结果窗口把等效系数 $X_1 = F_{max} / F_\infty$ 显示为 **X1 + gravity**。竖直下降时它比经典值大；高速水平飞行时两者一致。

数值方法：经典四阶 Runge–Kutta，在 $t_f$ 内取 4000 个固定步长。导出的 PNG 力曲线有 200 个点。

细节：

- $C_x$ 只乘在力上，不进入运动方程中的减速项。因此火箭只按名义阻力面积减速，速度略被高估，结果偏保守。
- 若 $v_0 < 1$ m/s（例如恰好在最高点释放的降落伞），$t_f$ 按 1 m/s 计算，以免充气时间变成无穷大。
- 质量 $m$ 取整枚火箭：假设伞绳拉直后两段箭体一起运动。

### 4.5 开伞力在两段箭体之间的分配

如果两段箭体以共同的加速度 $a = F/m$ 一起运动，传入每一段的力为

$$F_{upper} = F\,\frac{m_{upper}}{m}, \qquad F_{lower} = F\,\frac{m_{lower}}{m}$$

充气表中列出了这两部分。但选型使用全部的 $F$，因为系着降落伞主绳的那个螺栓要承受全部的力。

## 5. 拉直冲击

### 5.1 模型

分离后，两段箭体（质量 $m_1$、$m_2$）以相对速度 $\Delta v$ 相互远离。绳子拉直后，它相当于一根刚度为 $k$ 的线性弹簧。在质心参考系中，相对运动的动能为

$$E = \tfrac12\,\mu\,\Delta v^2, \qquad \mu = \frac{m_1 m_2}{m_1 + m_2}$$

在最大伸长量 $x_{max}$ 处，这部分动能全部储存在绳子里：

$$\tfrac12\,\mu\,\Delta v^2 = \tfrac12\,k\,x_{max}^2
\quad\Rightarrow\quad
F_{snatch} = k\,x_{max} = \Delta v\,\sqrt{k\,\mu}$$

两块隔舱板受到大小相同、方向相反的力。

假设：绳子是线弹性、无阻尼的，开始时是松弛的，在这一短暂过程中外力（阻力、重力）很小。真实绳子的阻尼和塑性伸长会降低峰值，所以在给定 $\Delta v$ 和 $k$ 的情况下，这个公式是保守的。

脉冲持续时间约为半个振荡周期：

$$t_{pulse} \approx \pi\sqrt{\mu / k}$$

### 5.2 绳子刚度

对长度为 $L$、轴向刚度为 $EA$ 的绳子：

$$k = \frac{EA}{L} \approx \frac{F_{break}}{\varepsilon_{break}\,L}$$

其中 $F_{break}$ 是断裂强度，$\varepsilon_{break}$ 是数据表上的断裂伸长率，这里假设力–伸长曲线是线性的。材料越硬、绳子越短，拉直冲击越大。

### 5.3 分离速度

$\Delta v$ 由用户输入。它来自两个方面：抛射药的推力，以及降落伞开始把一段往回拉、另一段还在往前运动时两者阻力的差别。后者可以接近展开时的下降速度。飞行数据是最好的依据。

## 6. 持续载荷

$$F_{sustained} = m\,g, \qquad g = 9.80665\ \text{m/s}^2$$

每个降落伞都使用展开时的整箭质量，这是保守的做法。

## 7. 控制载荷与设计载荷

三种载荷出现在不同的时刻，所以不相加。控制（极限）载荷为

$$F_{limit} = \max\left(F_{open},\ F_{snatch},\ F_{sustained}\right)$$

其中 $F_{open}$ 默认取有限质量的值；如果选择了无限质量方法，则取 $F_\infty$。不适用的载荷（没有分离位置，就没有拉直冲击）不参与比较。

设计载荷为

$$F_{design} = SF \cdot F_{limit}$$

导出的扫描表对 $SF$ 从下限到上限、以 0.05 为步长重复这一计算。$C_x$ 已经包含了开伞力的超调，不要在安全系数里再算一次。

## 8. 螺栓选型

U 型螺栓有两条螺纹腿分担载荷，吊环螺栓只有一条。假设全部设计载荷都经过同一个连接点。每条腿所需的**拉应力面积**为

$$A_s \ge \frac{F_{design}}{n_{legs} \cdot \sigma_{allow}}$$

$\sigma_{allow}$ 取螺栓材料的保证应力（不产生永久变形时能承受的应力）；没有定义保证应力的材料取 0.2 % 屈服强度。

插件给出应力面积不小于 $A_s$ 的最小标准粗牙螺纹。应力面积按通常的定义：公制螺纹 $A_s = \frac{\pi}{4}(d - 0.9382\,P)^2$（螺距 $P$），UNC 螺纹 $A_s = 0.7854\,(D - 0.9743/n)^2$（每英寸 $n$ 牙）：

| 公制 | $A_s$ (mm²) | UNC | $A_s$ (mm²) |
|---|---|---|---|
| M3 | 5.03 | #6-32 | 5.86 |
| M4 | 8.78 | #8-32 | 9.03 |
| M5 | 14.2 | #10-24 | 11.3 |
| M6 | 20.1 | 1/4-20 | 20.5 |
| M8 | 36.6 | 5/16-18 | 33.8 |
| M10 | 58.0 | 3/8-16 | 50.0 |
| M12 | 84.3 | 7/16-14 | 68.6 |
| M14 | 115 | 1/2-13 | 91.5 |
| M16 | 157 | 5/8-11 | 146 |
| M20 | 245 | 3/4-10 | 215 |
| M24 | 353 | | |

只校核螺纹处的拉伸。U 型螺栓自身的弯曲、螺母或隔舱板中的螺纹脱扣，以及快挂、转环和绳子本身的强度都不在模型范围内。

## 9. 隔舱板选型

### 9.1 弯曲

把隔舱板视为半径为 $R$ 的简支圆板，在中心半径为 $r$ 的圆上受载（U 型螺栓两腿间距的一半，或吊环螺栓垫圈的半径）。最大弯曲应力为（Roark，简支圆板中心圆面受载）：

$$\sigma_{max} = \frac{3F}{2\pi t^2}\left[(1+\nu)\ln\frac{R}{r} + 1\right]$$

令 $\sigma_{max} = \sigma_{allow}$，得到最小厚度

$$t_{bend} = \sqrt{\frac{3F_{design}}{2\pi\,\sigma_{allow}}\left[(1+\nu)\ln\frac{R}{r} + 1\right]}$$

除非用户输入，$R$ 取分离位置处箭体管的内半径。四周粘接的隔舱板更接近固支板，应力更低；按简支计算是保守的。

### 9.2 冲剪

每条腿可能把一个直径为 $d$（垫圈或螺母）的圆柱从板上推出。剪切面积为 $n_{legs}\,\pi\,d\,t$，所以

$$t_{shear} = \frac{F_{design}}{n_{legs}\,\pi\,d\,\tau_{allow}}$$

所需厚度取 $t_{bend}$ 和 $t_{shear}$ 中的较大值。

这些公式假设板是各向同性的。胶合板和复合材料板不是各向同性的，内置的强度对它们只是粗略的值。

## 10. 验证

### 10.1 数值检验

把不计重力的数值积分与第 4.3 节的解析解做了对比，$j$ 取 0.5、1、2、6，$A$ 取 0.01 到 50。$X_1$ 的最大相对误差为 $2\times10^{-5}$。计入重力的积分用独立的求解器（SciPy，容差 $10^{-10}$）对 Ringo III 主伞做了检验：两者都给出 0.446 s 时 2491.9 N。

### 10.2 与 Ringo III 飞行的对比

用插件对 Ringo III 的 OpenRocket 模型做了仿真，并与 2024 年 8 月 20 日飞行的 500 Hz 加速度记录对比。

| 量 | 插件 | 飞行实测 |
|---|---|---|
| 到达最高点的时间 | 37.1 s | 约 36.5 s |
| 主伞展开时的速度 | 32.2 m/s | 未测量 |
| 主伞充气的峰值减速度 | 9.4 g（n = 8）；6.4 g（n = 17） | 5 到 6.6 g，持续约 1.3 s |
| 主伞充气时间 | 0.61 s（n = 8）；1.29 s（n = 17） | 约 1.3 s |
| 上段（10.5 kg）最大的轴向脉冲 | 拉直冲击 2.5 kN（默认值）；6.6 kN（$\Delta v$ = 13 m/s，$k$ = 40 kN/m） | 约 52 g，持续约 50 ms，即约 5.4 kN |
| 减速伞峰值减速度 | 0.4 g | 2 到 5 g，持续约 1 s |

结论：

- **主伞：** 取 $n = 17$ 时，实测充气的持续时间和峰值都能复现。对这枚火箭，默认的 $n = 8$ 约偏保守 1.5 倍。
- **拉直冲击：** 这次飞行中最大的载荷是 10 到 60 ms 的短脉冲，比充气载荷还大。取 $\mu = 6.4$ kg、$k$ = 40 kN/m 时，按第 5.1 节算出的脉冲持续时间为 $\pi\sqrt{6.4/40000} \approx 40$ ms，与实测的 50 ms 接近。因此对螺栓选型来说，拉直冲击的设置比伞衣的设置更重要。
- **减速伞：** 没有复现。最高点处速度很低，所以 Pflanz 力很小；实测的载荷来自分离后两段箭体在绳子上的各自运动，单体模型描述不了这个过程。

以上对比只基于一次飞行。标定出的数值可以作为同类火箭的起点，但不是通用常数。

## 11. 假设与适用范围

| 假设 | 影响 |
|---|---|
| 使用展开所在仿真步的状态；不计抛射到伞绳拉直之间增加的速度 | 低估在最高点释放的减速伞的开伞力 |
| 使用相对地面的速度，没有扣除风速 | 空速可能与所用值不同 |
| 充气期间把整枚火箭当作一个物体 | 两段箭体之间的脉冲只由拉直冲击公式近似 |
| 主伞充气时忽略已张开的减速伞的阻力 | 略偏保守 |
| $C_x$、$n$、$j$ 是经验参数 | 尽量用飞行数据标定 |
| 绳子线性、无阻尼 | 在给定 $\Delta v$ 和 $k$ 时偏保守 |
| 全部设计载荷经过一个螺栓 | 对同一分离位置的另一个螺栓偏保守 |
| 隔舱板简支、各向同性 | 对金属偏保守；对木材和复合材料只是近似 |
| 只校核螺纹拉伸 | 弯曲、脱扣和其他硬件没有校核 |
| 只支持单级火箭 | 多级火箭会出现负的剩余质量 |

## 12. 参考文献

1. T. W. Knacke, *Parachute Recovery Systems Design Manual*, NWC TP 6575, Naval Weapons Center, 1992。第 5 章：开伞力、Pflanz 方法、$C_x$、$X_1$、充气常数。
2. NSWC TR 86-142, Naval Surface Weapons Center, 1986（DTIC ADA170962）。阻力面积指数取 1/2、1、2 的 Pflanz 方法，弹道质量比。
3. W. C. Young, R. G. Budynas, *Roark's Formulas for Stress and Strain*, McGraw-Hill。中心受载的圆板。
4. ISO 898-1（钢制螺栓）、ISO 3506-1（不锈钢螺栓）、SAE J429（英制螺栓）：保证应力。
5. ISO 898-1 与 ASME B1.1：螺纹的拉应力面积。
