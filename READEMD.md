# 原理概述

![image-20251227004043640](https://obj.waitforu.tech/imgs%2Fblog%2F2025%2F12%2F27%2F004045-a81829839e77f2a19c1cc5398ec1eec9.webp)

​	***KRLParser***是一款用于处理kuka机器人备份程序，并进行**车型解析**的跨平台(MacOS、Windows)、本地Web软件。其核心功能是从机器人备份程序中获取车型调用关系，并进行**可视化处理**。便于相关人员直观、清晰的分析现场车型信息。

​	KRLParser的词法分析(Lexer)、语法分析(Parser)部分依赖于开源软件**Antlr4**。其能够根据提供的词法规则(Lexer Rule)、语法规则(Parser Rule)生成对应的Java代码，并且按照对应规则解析字符串，生成**解析树(Parse tree)**。

​	KRLParser通过生成的解析树来进行**语义分析**，从而忽略细节、抓取主干，建立程序的抽象语法树：AST(Abstract Syntax Tree)。

​	通过备份文件的遍历，将一对`.src`、`.dat`文件聚合成**kuka模块(Kuka Module)**，且从不同模块提取到不同的AST。将所有模块进行统一存储，依据**cell模块**开始的AST中存储的信息、并按照相应规范(由传入**配置文件config**来约束)，最终生成车型调用数据结构：Cell->P_Program->Car_Code->Car_Program->ROUTE_PROCESS。其中**Car_Code**为依据大(PGNO)、小(GIPGNO2)车型对应的**Case**计算而来。

​	以上数据结构、算法分析、读写处理等均由Java语言编写。并通过**SpringBoot**框架来获取浏览器界面的HTTP请求(**Http Request**)。处理请求时将车型信息由Java结构转换成对应的**Json**格式传递给浏览器前端界面。

​	交互部分由浏览器前端代码实现。在用户上传`*.zip`**备份文件和**`*.yml`**配置文件**后，浏览器会携带文件发起**请求(Request)**。在Java侧进行逻辑处理后，发回**响应(Response)**。浏览器界面拿到响应的Json后，借助`cytoscape`进行可视化绘制。

​	综上，本软件是一个基于Anltr4语言分析框架、SpringBoot序列化接口框架、Cytoscape前端可视化渲染框架的跨平台、可交互的本地Web软件。

# 功能介绍

简要介绍本软件的功能。

- 本软件在MacOs下可直接解压出`KRLParser.app`,双击启动即可运行

- 本软件在Windows下会解压出`build`文件夹，其可执行文件路径为 `build->dist->KRLParser-> KRLParser.exe`。

本软件在运行时需要上传两个文件:

- 备份文件，比如`EC010_L1.zip` 。为维持程序正常运行，请保证使用原生备份，不要二次修改。
- 配置文件，比如`config.yml。`为维持程序正常运行，请保证配置文件遵循`*.yml`格式。(语法上要求严格遵循，文件后缀可以不遵循。)

## 1 调用关系图

在上传完两个文件后，点击**开始分析**即可。可以通过鼠标滚轮调整界面大小。

![image-20251226235229908](https://obj.waitforu.tech/imgs%2Fblog%2F2025%2F12%2F26%2F235238-16654d4ee49feec99014ee4e8c2347fe.webp)

可通过单击结点，从而高亮整个调用链路。

![image-20251226235559041](https://obj.waitforu.tech/imgs%2Fblog%2F2025%2F12%2F26%2F235601-2c084f36a92c1a7f9da41a48e4c26795.webp)

这在车型繁杂时十分有用。从下图中，我们可以很清晰看到:1102、1112、1122、1132、1152、1162、1172以及1104、1114、1154这些10款车型代码，总共调用`SRER`、`SREE`、`SRES`三款车型程序，但最终只调用一套轨迹，并且还包含空补:`sre_weld_02`

![image-20251226235751704](https://obj.waitforu.tech/imgs%2Fblog%2F2025%2F12%2F26%2F235753-7220491c9d8c9d4576132fbd0e0fbc49.webp)

可点击左下角**节点调整**来调整大小(当然，鼠标滚轮也可直接整体放大和缩小)

![image-20251226235447197](https://obj.waitforu.tech/imgs%2Fblog%2F2025%2F12%2F26%2F235449-599c3de170d0f4781f26a31de10dbf1e.webp)

![image-20251226235511846](https://obj.waitforu.tech/imgs%2Fblog%2F2025%2F12%2F26%2F235513-b731e763a1c10c5c4fbad91afd7eba9e.webp)

结点可以进行拖拽来获得更清晰的结果。

![image-20251227001859044](https://obj.waitforu.tech/imgs%2Fblog%2F2025%2F12%2F27%2F001901-4a69451e265122c5d15069e9eac0739e.webp)



## 2 文件信息栏

通过双击结点，可以打开该结点对应的模块中的`.src`文件的信息。包括具体路径、创建时间、修改时间。

![image-20251227000119649](https://obj.waitforu.tech/imgs%2Fblog%2F2025%2F12%2F27%2F000124-16637b224e6c45404e159ad12bb75f2e.webp)

**3 上下文信息**

通过右键点击结点可以看到上下文信息，比如对于P程序、车型程序，这种相对规范的调用结构，就会展示其所在Case的上下文。

![image-20251227000602512](https://obj.waitforu.tech/imgs%2Fblog%2F2025%2F12%2F27%2F000604-042a6ffa50a1a700f592e54decb26a17.webp)

对于繁琐混杂的轨迹调用、直接展示其对应车型程序的所有文件内容。cell程序由于没有上层，也将直接展示全部。

![image-20251227000751340](https://obj.waitforu.tech/imgs%2Fblog%2F2025%2F12%2F27%2F000753-a28fbf58febd8a164c7931376fd00354.webp)

## 4 机器人信息

通过点击左上角**信息**按钮，可以打开机器人相关信息。

![image-20251227000212843](https://obj.waitforu.tech/imgs%2Fblog%2F2025%2F12%2F27%2F000216-3904fd6ff026123b68667f50ba708f51.webp)

其显示了机器人的名字、版本、备份时间、内部的一些包(比如P网包、二代示教器包、焊接包等等)

![image-20251227000310503](https://obj.waitforu.tech/imgs%2Fblog%2F2025%2F12%2F27%2F000314-92aa3e71d20e150927701ab3d9657839.webp)

# 配置文件

由于历史包袱严重，机器人内部有各种奇奇怪怪名字的程序。我们可以通过上传的另一个文件`config.yml`进行约束。比如下图中的`use_cd_params()`调用在任何一个车型程序中都出现了，该文件显然不是我们关心的东西。

![image-20251227002123549](https://obj.waitforu.tech/imgs%2Fblog%2F2025%2F12%2F27%2F002125-9d824beeb87d44afffea6b446739f34c.webp)

因此可以通过编辑配置文件`config.yml`进行约束。`.yml`文件是文本格式的文件，可以直接用记事本、VsCode、NodePad等文本编辑器打开。打开后找到**invokerParseSection**区域，在其中添加不想要解析的调用即可。

> [!IMPORTANT]
>
> `.yml`文件是与`Python`一样的缩进敏感规范。因此务必保证`config.yml`文件的上下对齐。多敲/少敲一个空格均会导致解析错误。

> [!WARNING]
>
> - 如果要屏蔽，务必以`!`开始。比如不想解析waitSeg，就要写`!waitSeg`。如果直接写`waitSeg`表示想解析`waitSeg`。
> - 由于kuka不区分大小写，因此我们的配置规则也设定为不区分大小写。`waitSeg`即能匹配到`WAITSEG`，也可以匹配到`waitseg`
> - 配置文件中的规则，按照从上到下的优先级来进行执行。

![image-20251227002431083](https://obj.waitforu.tech/imgs%2Fblog%2F2025%2F12%2F27%2F002432-8bfa14a8cf87ea066cd8973cc81996cf.webp)

> [!CAUTION]
>
> 注意`prefix`和`suffix`。`prefix`代表按照开始部分匹配。`suffix`表示按照结束部分匹配。由于WaitSeg和EndSeg均以`Seg`结束，因此想要同时解决掉`waitSeg`和`endSeg`，只需要在suffix中添加`!seg`即可。

# 免责声明

- 本软件正常运行，基于现场的备份程序是正常情况下。因此对于下面这类情况，本软件必定会出现奇怪情况🤔🤔。(下图出现原因为cell中的case 62出现了三次，因此有三条线出现。)

![image-20251227000945531](https://obj.waitforu.tech/imgs%2Fblog%2F2025%2F12%2F27%2F000947-10a6f413c838c31fa58820b74bc8a5a8.webp)

![image-20251227001150546](https://obj.waitforu.tech/imgs%2Fblog%2F2025%2F12%2F27%2F001152-7417ca9e6c5b76540f420c9ce356d1de.webp)

- 本软件目前版本(2025-12-26，v1.0.0)暂时仅仅只能做参考，未经过充分验证，结果准确性无法保证，可能出现恶意Bug

- 本软件基于本地端口号2026进行工作，请避免其他软件抢占了这个端口号。
- 本软件运行依赖于`后台服务`，服务程序需要任务管理强制结束或通过指令来结束。在关机后，本软件后台会自动退出。重启后需要重新打开软件才能通过网页使用。
- 本软件后台启动情况下，可直接通过url地址进行访问。url地址为:`http://localhost:2026`。

最后，正如url地址`localhost:2026`所示，Happy new year 2026，祝大家元旦快乐！