// $antlr-format alignTrailingComments true, columnLimit 150, minEmptyLines 1, maxEmptyLinesToKeep 1, reflowComments false, useTab false
// $antlr-format allowShortRulesOnASingleLine false, allowShortBlocksOnASingleLine true, alignSemicolons hanging, alignColons hanging

grammar krl;

/*
    This file is the grammar for the KUKA Robot Language.
    Copyright (C) 2010-2011  Jan Schlößin

    This grammar is free software: you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
/*
    Antlr4 port by Tom Everett, 2016
*/
/*
    Adapted for krl 8.x by 刘可(Cody Liu), 2025
*/


//小写字母开始为语法分析器的规则，用于定义语法结构。语法规则先后顺序不会影响匹配。//

//程序入口
//注意，当使用标签时，会导致Antlr4不再生成visitStart()方法，而是生成:visitDataFile()和visitSourceFile()这两个方法。
start
    : NEWLINE* krlControlHead moduleData EOF   # dataFile  // 数据文件
    | NEWLINE* krlControlHead moduleSource EOF   # sourceFile // 代码文件
    ;

//控制块，放在文件开头，一系列以‘&’开头的行，用于定义程序属性和访问权限。
krlControlHead
    : (KrlControlLine NEWLINE+)*
    ;


//=== 数据文件部分定义 === //
moduleData
    : DEFDAT moduleName PUBLIC? NEWLINE+ dataList ENDDAT NEWLINE*
    ;
//模块名字
moduleName
    : krlIdentifier
    ;
//数据列表，包含导入语句、枚举定义、结构体定义、变量声明、数组初始化、等。
dataList
    : (dataLine)* NEWLINE*

    ;

dataLine
    : NEWLINE
    | forwardDeclaration  NEWLINE //前置申明。
    | enumDefinition NEWLINE //枚举定义。
    | structureDefinition NEWLINE //结构体定义。
    | variableDeclaration NEWLINE //变量定义。
    | expression NEWLINE          // 赋值表达式

    //古老的语法(KSS4.x/5.2中存在过)。在8.x版本中已被弃用。现使用GLOBAL关键字彻底改革了数据共享机制。
    | importStatement NEWLINE  //数据导入语句
    ;

//前置申明。在当前模块定义前，使用 EXT 或 EXTFCT 关键字申明一个在另一个模块的 .SRC 文件中定义的子程序（Subroutine）或函数（Function）。
//由于KRL采用了一种宽松的、基于文件名的自动链接机制，因此通常及时不需要申明也能够正常调用
forwardDeclaration
    // 申明将要调用无返回值的「过程」，用于声明一个在另一个模块的 .SRC 文件中定义的子程序（Subroutine）。必须放在调用它的 .SRC 文件的头部，在任何 DEF 之前。
    // 语法: EXT ProgramName( [参数声明] )
    : EXT procedureName '(' parameterList ')'

    // 申明将要调用无返回值的「函数」，用于声明一个在另一个模块的 .SRC 文件中定义的函数（Function）。
    // 必须放在当前程序的头部时，允许当前程序调用另一个 .SRC 文件中定义的带返回值的函数 (Function)。
    //语法: EXTFCT ReturnType FunctionName( [参数声明] )
    | EXTFCT typeName functionName '(' parameterList ')'
    ;

parameterList
    :  (parameterWithType (',' parameterWithType )*)?
    ;

parameterWithType
    : parameterName ':' parameterCallType
    ;

parameterName
    : variableName //普通变量名，比如GLOBAL Def ExitZone(ZoneNumber : IN)
    | typeName //对于EXT中声明调用外部函数时，声明语句的参数名也可以是类型名，比如INT、REAL、CHAR、BOOL等。
    ;

parameterCallType
    : IN        //只读参数。 子程序只能读取这个参数的值，不能在子程序内部修改它。
    | OUT       //只写/只出参数。 子程序必须将结果写入这个参数，但不需要读取它的初始值。
    | INOUT     //可读可写参数。 子程序可以读取参数的初始值，也可以在子程序内部修改它。
    ;


//枚举类型的定义，比如：
// GLOBAL ENUM SG_MoveType_T SG_PTP,SG_LIN,SG_CIRC,SG_SPTP,SG_SLIN,SG_SCIRC
enumDefinition
    : GLOBAL? ENUM userType enumValue (',' enumValue)*
    ;
//枚举值。枚举值在定义时不需要'#' 前缀，调用时需要以'#'开头。
enumValue
    : krlIdentifier
    ;

//结构体类型的定义，比如：
// GLOBAL STRUC SG_IOConfig_T INT Index, SG_IOBoolType_T Type, BOOL ErrorDefault
structureDefinition
    : GLOBAL? STRUC userType typeName  variableList (',' typeName variableList)*
    ;

//声明变量与信号，声明基本数据类型(INT REAL CHAR BOOL)时，DECL可以省略，且可以批量定义。
variableDeclaration
    : modifier* DECL? modifier* typeName expression (',' expression)*      #commonVariableDecl

    //信号变量的定义方式，比如:
    // SIGNAL START_PROCESS $OUT[7]
    // GLOBAL SIGNAL GI_SpeedFromPLC $IN[20]  TO $IN[22]
    | modifier*  SIGNAL modifier* variableName variableName (TO variableName)? #signalVariableDecl
    ;

//变量序列，比如 int x, y, dog, cat中的x, y, dog, cat
variableList
    : variableName (',' variableName)*
    ;
// 变量，包括普通变量与数组变量
variableName
    : krlIdentifier (arrayVariableSuffix)?
    ;

// 数组索引后缀，krl最多支持三维数组，比如 array[]、array[0]、array[0,1]、array[0,1,2]
// 对于部分数组甚至可以省略索引，保留空的索引位置，比如 array[0, ,2]，LOAD_NAME[1,]=" "
arrayVariableSuffix
    : '[' ']'
    | '[' expression']'
    | '[' (expression)? ',' (expression)? ']'
    | '[' (expression)? ',' (expression)? ',' (expression)? ']'
    ;


/*
在现代 KRL (KSS 8.x / KRC4 及以后) 中，IMPORT 关键字已经被移除且不再支持。但是，在非常古老的 KRL 版本（如 KRC1/KRC2 时代的 KSS 4.x/5.x）中，它确实存在过。
在引入强大的 GLOBAL 关键字之前，KRL 需要一种机制来允许一个程序访问另一个程序的私有数据（定义在 .DAT 文件中）。
IMPORT 的作用是显式地将另一个模块中的特定变量“拉”进当前模块的作用域。

语法特征： IMPORT [类型] [本地别名] IS [路径]..[外部变量名]
IMPORT INT MY_LOCAL_COUNT IS /R1/MYDATA..COUNT    将 /R1/MYDATA 中的 COUNT 变量导入，并在此处命名为 MY_LOCAL_COUNT
*/
importStatement
    : IMPORT typeName variableName IS '/R1/' moduleName '..' variableName
    ;


//==========================================//
//============ 代码文件部分定义 ==============//
//==========================================//
moduleSource
    : mainRoutine NEWLINE? (subRoutine | NEWLINE)*
    ;

mainRoutine
    : procedureDefinition
    | functionDefinition
    ;

subRoutine
    : procedureDefinition
    | functionDefinition
    ;

//「过程」定义
procedureDefinition
    : GLOBAL? DEF procedureName '(' parameterList')' NEWLINE routineBody END
    ;

    procedureName
        : krlIdentifier
        ;

//「函数」定义
functionDefinition
    : GLOBAL? DEFFCT typeName functionName '(' parameterList')' NEWLINE routineBody ENDFCT
    ;

    functionName
        : krlIdentifier
        ;



routineBody
    : routineDataSection routineImplementationSection
    ;

routineDataSection
    : (
        forwardDeclaration NEWLINE
        | variableDeclaration NEWLINE
        | enumDefinition NEWLINE
        | structureDefinition NEWLINE
        | NEWLINE
    )*
    ;

routineImplementationSection
    : statementList
    ;

modifier
    : GLOBAL
    | CONST
    | PUBLIC
    ;




// 语句列表，由多个语句组成，必定以换行结束
statementList
    : statement *
    ;

// 语句，必定以换行结束
statement
    //空语句
    : NEWLINE       # emptyStatement

    //continue语句
    | CONTINUE NEWLINE # continueStatement

    // EXIT语句
    | EXIT NEWLINE # exitStatement

    //if条件语句
    | IF expression THEN NEWLINE statementList (ELSE NEWLINE statementList)? ENDIF NEWLINE # ifStatement

    //FOR循环语句
    | FOR krlIdentifier '=' expression TO expression (STEP expression)? NEWLINE statementList ENDFOR NEWLINE # forStatement

    //Loop 循环语句
    | LOOP NEWLINE statementList ENDLOOP NEWLINE # loopStatement

    // Repeat 循环语句
    | REPEAT NEWLINE statementList UNTIL expression NEWLINE  # repeatStatement

    //While 循环语句
    | WHILE expression NEWLINE statementList ENDWHILE NEWLINE # whileStatement

    //标签语句
    | GOTO krlIdentifier NEWLINE # gotoStatement

    //Switch 语句
    | SWITCH expression NEWLINE+ switchBlockStatementGroups ENDSWITCH NEWLINE # switchStatement

    //Wait 语句
    | WAIT FOR expression NEWLINE # waitForStatement
    | WAIT SEC expression NEWLINE # waitSecStatement

    //Return 语句
    | RETURN (expression)? NEWLINE # returnStatement

    //表达式也是语句
    | expression NEWLINE # expressionStatement

    //标签
    | gotoLabel # labelStatement

    //PTP 运动到命名点。C_PTP决定是否采用近似定位（圆滑过渡），C_DIS、C_ORI、C_VEL 分别表示是否采用距离、姿态、速度百分比来进行过渡约束。
    //WITH用于在运动指令的背景下并行地执行其他任务。
    // 比如:SPTP XHOME WITH $VEL_AXIS[1] = SVEL_JOINT(100.0), $TOOL = STOOL2(FHOME), $BASE = SBASE(FHOME.BASE_NO), $IPO_MODE = SIPO_MODE(FHOME.IPO_FRAME), $LOAD = SLOAD(FHOME.TOOL_NO), $ACC_AXIS[1] = SACC_JOINT(PDEFAULT), $APO = SAPO_PTP(PDEFAULT), $GEAR_JERK[1] = SGEAR_JERK(PDEFAULT), $COLLMON_TOL_PRO[1] = USE_CM_PRO_VALUES(0)
    | (PTP | SPTP | PTP_REL) expression (C_DIS | C_ORI | C_VEL|C_SPL|C_PTP)* (WITH expression(',' expression)*)? (C_DIS | C_ORI | C_VEL | C_SPL)* NEWLINE # ptpMoveStatement

    //LIN 运动到命名点。C_DIS、C_ORI、C_VEL 分别表示是否采用距离、姿态、速度百分比来进行过渡约束。
    | (LIN | SLIN) expression  (C_DIS | C_ORI | C_VEL | C_SPL|C_PTP)* (WITH expression(',' expression)*)? (C_DIS | C_ORI | C_VEL | C_SPL)* NEWLINE # linMoveStatement

    //LIN_REL 运动到相对命名点。enumLiteral表示是否有枚举关键字，比如 #FINE #COARSE等。
    | LIN_REL expression (C_DIS | C_ORI | C_VEL | C_SPL|C_PTP )* enumLiteral? (WITH expression(',' expression)*)? (C_DIS | C_ORI | C_VEL | C_SPL)* NEWLINE # linRelMoveStatement

    /*
        圆弧运动。通过 两个点加 角度确定圆弧轨迹。比如
        CIRC {X 5,Y 0, Z 9.2},{X 12.3,Y 0,Z -5.3,A 9.2,B -5,C 20}, CA 260 C_ORI
    */
    | (SCIRC | CIRC | CIRC_REL) expression ',' expression (',' krlIdentifier expression)? ( C_DIS | C_ORI| C_VEL |C_SPL)*  (WITH expression(',' expression)*)? ( C_DIS | C_ORI| C_VEL |C_SPL)* NEWLINE # circMoveStatement

    //运动停止语句，使用BRAKE指令使机器人当前运动立即减速停止。执行 BRAKE 后，只有当机器人真正停止运动后，中断服务程序才会继续往下执行后续代码。
    //BRAKE 以正常速度刹停机器人。BRAKE 以最大允许减速度紧急制动。
    | BRAKE (krlIdentifier)? NEWLINE # brakeStatement

    //中断语句申明: {Global} INTERRUPT DECL 优先级 WHEN 条件 DO 子程序名
    | GLOBAL? INTERRUPT DECL primary WHEN expression DO expression NEWLINE # interruptDeclStatement

    //中断激活、停用、删除,比如 INTERRUPT  ON 1，INTERRUPT OFF，INTERRUPT DEL 3
    | INTERRUPT (ON | OFF | DEL) primary? NEWLINE  # interruptControlStatement

    //异步PTP运动
    |ASYPTP expression NEWLINE # asyPtpStatement
    //取消异步
    | ASYCANCEL expression NEWLINE # asyCancelStatement

    //停止语句
    | HALT NEWLINE  # haltStatement

    /*
        Trigger 语句，将一个动作（DO 后面的指令）绑定到一条运动指令上。当机器人执行该运动指令时，控制器会实时计算何时满足触发条件，并在那个精确的时刻执行 DO 动作。
        TRIGGER 语句是异步执行的，这意味着触发的动作会在运动指令（PTP、LIN、CIRC 等）的后台执行，不会中断或减慢机器人的预定轨迹。
        比如:  TRIGGER WHEN DISTANCE=0 DELAY=130 DO $OUT[8]=TRUE
              TRIGGER WHEN DISTANCE=1 DELAY=10 DO UP2(A) PRIO=5
        DISTANCE=0 (Default): 动作将在运动指令的终点（目标位置）被触发。
        DISTANCE=1: 动作将在运动指令的起点（当前位置）被触发。
        DELAY=时间 (ms): 指定动作相对于触发点的时间偏移。

        正值 (Positive Delay): 动作在触发点之后延迟执行。
        负值 (Negative Delay): 动作在触发点之前提前执行。
        应用： 负延时非常强大，它允许在机器人到达目标点之前就开始执行动作（例如，提前打开焊枪）。

        TRIGGER 语句的有效范围是从它被声明的那一行开始，直到遇到下一个非连续运动指令（即没有 C_DIS/C_VEL 修饰符的运动，或程序结束）。

     */
    | TRIGGER WHEN (DISTANCE) '=' INTLITERAL DELAY '=' expression DO expression (PRIO '=' expression)? NEWLINE # triggerStatement

    // 模拟输入输出语句
    | analogInputStatement NEWLINE # aiStatement
    | analogOutputStatement NEWLINE # aoStatement
    ;


//跳转标签
gotoLabel
    : krlIdentifier ':' NEWLINE
    ;


//输入信号映射,将模拟量输入信号映射到一个变量上
//比如 ANIN ON $OV_PRO = 1.0 * SIGNAL_1   将SIGNAL_1的模拟量输入值按照某个公式映射到$OV_PRO变量上，范围为0-1000
//    ANIN OFF SIGNAL_1    禁用对 SIGNAL_1 的实时监控。一旦禁用，$OV_PRO 将不再实时跟随 SIGNAL_1 的变化，其值会保持在禁用时的瞬时值。
analogInputStatement
    : ANIN ON expression    # aiOnStatement
    | ANIN OFF expression    # aiOffStatement
    ;

//输出信号映射，将一个变量的值映射到模拟量输出信号上
//比如机器人的实际运动速度 ($VEL_ACT)，实时地映射到模拟量输出信号 (GLUE) 上。这在涂胶应用中非常常见：
//   ANOUT ON GLUE = 0.5 * $VEL_ACT DELAY=-0.5 MINIMUM=0.30
//   ANOUT OFF GLUE             禁用对 GLUE 的实时控制。一旦禁用，GLUE 的输出值将保持在禁用时的瞬时值。
// $VEL_ACT: 机器人的实际运动速度，单位为 mm/s
// DELAY =-0.5: 提前执行。 模拟量输出信号 GLUE 会比计算的理论触发点提前 500 毫秒 (0.5 秒)执行。这用于补偿胶阀的物理响应延迟。
// MINIMUM=0.30: 输出信号的最小值为 30%。这确保了即使计算结果低于 30%，模拟量输出信号也不会低于 30%。
analogOutputStatement
    : ANOUT ON expression (DELAY '=' literal)? (MINIMUM '=' literal)? (MAXIMUM '=' literal)?    # aoOnStatement
    | ANOUT OFF expression    # aoOffStatement
    ;

switchBlockStatementGroups
    : (caseLabels NEWLINE+ statementList)* (defaultLabel NEWLINE+ defaultBody=statementList)?
    ;

caseLabels
    : CASE firstLabel=expression (',' otherLabel=expression)*
    ;

defaultLabel
    : DEFAULT
    ;

// 表达式(expression)，包括基础表达式、一元运算符表达式、二元运算符表达式
expression
    // 基础表达式(primary expression)，包括小括号、字面量、变量名、结构体成员访问、函数调用
    : primary   #primaryExpression
    // 一元运算符表达式(unary operator expression)，包括逻辑非、加减号
    |  op=(NOT | B_NOT) expression   #notExpression   // `非` 逻辑表达式
    |  op=('+' | '-') expression   #negAndPosExpression   // 正负运算表达式

    // 二元运算符
    /*
    几何运算表达式(KRL 提供了几何运算符 : 用于坐标系之间的变换叠加(框架连接运算)),比如
    ```
    P1 = {X 100, Y 0, Z 100, A 0, B 0, C 0, S 0, T 0}
    Offset = {X 50, Y 0, Z 0, A 0, B 0, C 0}  ; 沿X轴移动50mm
    P2 = P1 : Offset
    ```
    P2的坐标将会是：{X 150, Y 0, Z 100, A 0, B 0, C 0, S 0, T 0}
    注意，这里本质上是矩阵乘法运算 P2 = P1 · Offset   ，不是简单加法运算，具体参考相关资料
    */
    | expression op=':' expression   # geometryExpression   // 几何运算表达式
    | expression op=('*' | '/') expression   #multiplyDivideExpression   // 乘除算数表达式
    | expression op=('+' | '-') expression   #plusMinusExpression   // 加减算数表达式
    | expression op=(AND | B_AND) expression   #andExpression   // `与` 逻辑表达式
    | expression op=(EXOR | B_EXOR) expression   #exorExpression   // `异或` 逻辑表达式
    | expression op=(OR | B_OR) expression   #orExpression   // `或` 逻辑表达式
    | expression op=('==' | '<>' | '<=' | '>=' | '<' | '>') expression   #relationExpression   // 关系表达式，注意到krl这里与主流语言先反，逻辑表达式优先级高于关系表达式
    | expression op='=' expression   #assignmentExpression   // 赋值表达式,krl中不支持链式赋值，比如 a=b=c，因此这里虽然是左结合，但是不会产生实际影响。
    ;

primary
    : '(' expression ')'  #bracketPrimary    // 小括号包裹的内容
    | literal       #literalPrimary        // 字面量
    | variableName          #variablePrimary        // 变量名，包括变量与数组。$IN[5]、$OUT[8]、$ANIN[10]、$ANOUT[11]等包含在此类中
    | variableName ('.' variableName)+  #structMemberPrimary        // 通过`.` 访问结构体成员
    | callableName=variableName '('    (firstAugment=expression (',' otherAugments=expression?)*)?   ')'   #invokeCallablePrimary        // 函数调用（参数可带也可不带）
    ;


//类型名，包括基本类型与自定义类型
typeName
    : primitiveType         // 基本类型
    | userType           // 自定义类型
    ;

//基本类型
primitiveType
    : BOOL (arrayVariableSuffix)?
    | CHAR (arrayVariableSuffix)?
    | INT (arrayVariableSuffix)?
    | REAL (arrayVariableSuffix)?
    ;

// 自定义类型。包括定义的结构体类型、枚举类型等
userType
    :krlIdentifier (arrayVariableSuffix)?
    ;


/*=========================字面量语法定义=============================*/
/*
krl中类型变量能赋予的值：
 1. 整数（INT）：可以赋予整数值，例如 10、-5、0 等。
 2. 实数（REAL）：可以赋予实数值，例如 3.14、-2.5、0.0 等。
 3. 字符（CHAR）：可以赋予字符值，例如 'A'、'b'、'1' 等。
 4. 字符串（STRING）：可以赋予字符串值，例如 "Hello"、"World" 等。
 5. 结构体（STRUCT）：可以赋予结构体值，例如
 6. 枚举值（ENUM）：可以赋予枚举值，例如 #RED、#GREEN、#BLUE 等。
 7. 布尔值（BOOL）：可以赋予布尔值，例如 TRUE、FALSE 等。
*/
literal
    : INTLITERAL
    | REALLITERAL
    | CHARLITERAL
    | STRINGLITERAL
    | structLiteral
    | enumLiteral
    | TRUE
    | FALSE
    ;

// 枚举值的语法要求：枚举值必须以 # 开头，后面跟着一个或多个字母或数字字符。
enumLiteral
    : '#' enumValue
    ;

/*
结构体的语法要求：
定义结构体: STRUC Structure_name Data_Type1 A,B, Data_Type2 C,D ...
   比如:STRUC E6POS REAL X, Y, Z, A, B, C, E1, E2, E3, E4, E5, E6, INT S,T

结构体literal的语法要求：结构体必须以 { 开头，以 } 结尾，中间包含零个或多个结构体元素。
   比如定义结构体类型的变量并赋值:DECL E6POS myPos = {X 34.4, Y -23.2}
    有时也会带上类型名和冒号，比如:DECL E6POS myPos = {REAL:X 34.4,REAL:Y -23.2}
*/
structLiteral
    : '{' ( (typeName ':')? structField expression ) (',' (typeName ':')? structField expression)*  '}'
    ;

structField
    : variableName
    ;

/*
    在krl中，某些 词组，可能既是某些语法中的关键字，又能在变量名中使用。
    因此在krl中identifier需要扩充，包括除关键字外的标识符(IDENTIFIER) 以及某些特定关键字(DISTANCE, DELAY, STEP, SEC)
    比如：SEC既可以与WAIT搭配使用，也可以作为普通变量名。
      wait sec 10;
      time = time + actDate.sec;

*/
krlIdentifier
    : IDENTIFIER
    | DISTANCE
    | STEP
    | SEC
    | WITH
    | ASYCANCEL
    | BRAKE
    | DELAY
    | DO
    | MAXIMUM
    | MINIMUM
    | ON
    | OFF
    | DEL
    | PRIO
    | WAIT
    | TRIGGER
    | IN
    | OUT
    | INOUT
    | INTERRUPT
    | UNTIL
    ;



// ================================词法规则=================================== //
//
//大写字母为词法分析器的规则，用于定义关键字。词法规则定义的先后顺序会影响匹配结果。
// 因此要把特定关键字写在标识符这些之前。
// 把具体的规则，放在泛化规则之前。


//注释，以;开头的注释内容。注释中不能有换行('\n')与回车('\r')。
Comment
    /*
        Parser 只会看 默认通道（DEFAULT_TOKEN_CHANNEL） 的 token，
        而不会看其他通道的 token。
        因此，为了让注释不干扰语法分析，我们需要将注释 token 放到 HIDDEN 通道。之后，Parser 就不会再看这些 token 了。
        但我们可以在语义分析阶段，通过 CommonTokenStream.getHiddenTokensToLeft/Right 提取出来进行解析。
    */
    : ';' (~('\n' | '\r'))* -> channel(HIDDEN)
    ;


/*
以&符号开头的行是特殊的控制行（Control Lines），它们提供了关于程序属性和访问权限的元数据（metadata），供系统和编译器使用。这些行不是常规的可执行指令。
比如:
&REL 110
&COMMENT SpotWeld FuncTion
&USER BYD
&PARAM EDITMASK = *
&PARAM TEMPLATE = C:\KRC\Roboter\Template\vorgabe
&PARAM DISKPATH = KRC:\R1\BYDFunction
*/
ast.pojo.tech.waitforu.KrlControlLine
    : '&' (~('\n' | '\r'))*
    ;

IN
    :I N
    ;
OUT
    :O U T
    ;
INOUT
    :I N O U T
    ;

ON
    : O N
    ;

OFF
    : O F F
    ;

DEL
    : D E L
    ;

STEP
    :S T E P
    ;

WITH
    : W I T H
    ;


//用于triiger语句。
// DISTANCE=0 (Default): 动作将在运动指令的终点（目标位置）被触发。
// DISTANCE=1: 动作将在运动指令的起点（当前位置）被触发。
DISTANCE
    : D I S T A N C E
    ;


/*用于取消并删除异步外部轴的 ASYPTP 运动。ASYCANCEL Axis_Number或者 ASYCANCEL=Axis_Number
其中 Axis_Number 是 INT 类型：
0 表示取消所有异步外轴；
1…$EX_AX_NUM 表示某一根异步外轴的编号。
*/
ASYCANCEL
    : A S Y C A N C E L
    ;
//用于触发某一轴的异步运动。比如: ASYPTP {E1 10}
// 实际上kuka焊机焊接时的原理就与其有关。在焊接时焊枪被设置成了异步，并进行运动。
// 具体见: KRC\R1\TP\ServoGun\SG_Submit_Motion.src 中的第1349行。或者查找 ASYPTP关键字
ASYPTP
    : A S Y P T P
    ;



AND
    : A N D
    ;

//模拟输入信号
ANIN
    : A N I N
    ;

//模拟输出信号
ANOUT
    : A N O U T
    ;

//二进制与
B_AND
    : B '_' A N D
    ;

//按位取反
B_NOT
    : B '_' N O T
    ;
//二进制或
B_OR
    : B '_' O R
    ;
//二进制异或
B_EXOR
    : B '_' E X O R
    ;

BOOL
    : B O O L
    ;

//在中断服务程序中，可以使用 BRAKE 指令使机器人当前运动立即减速停止。在执行 BRAKE 后，只有当机器人真正停止运动后，中断服务程序才会继续往下执行后续代码。
//BRAKE      ; 以正常减速度刹停机器人（减速停机）
//BRAKE F    ; 以最大允许减速度紧急制动（仍保持路径）

BRAKE
    : B R A K E
    ;

//轨迹逼近距离采用距离准则（以距离决定过渡开始），比如：LIN Point_Name C_DIS
C_DIS
    : C '_' D I S
    ;

//轨迹逼近距离采用方位准则（以工具姿态变化决定过渡开始），比如：LIN Point_Name C_ORI
C_ORI
    : C '_' O R I
    ;

C_PTP
    : C '_' P T P
    ;

//轨迹逼近距离采用速度准则（以速度百分比决定过渡开始），比如：LIN Point_Name C_ORI
C_VEL
    : C '_' V E L
    ;

//轨迹逼近距离采用
C_SPL
    : C '_' S P L
    ;

CASE
    : C A S E
    ;

//从特定类型中提取数据：用于从一个特定类型的变量中提取数据，并将其视为另一个指定的数据类型。
CAST_FROM
    : C A S T '_' F R O M
    ;

//将数据转换为特定类型
CAST_TO
    : C A S T '_' T O
    ;

CHAR
    : C H A R
    ;


//轨迹编程时的 圆弧运动关键字
CIRC
    : C I R C
    ;

//轨迹编程时，圆弧运动实现相对上个点偏移的关键字。
CIRC_REL
    : C I R C '_' R E L
    ;

SCIRC
    : S C I R C
    ;

//关键字用于定义成一个常量，确保变量在程序执行期间保持不变，提高代码的可读性、可维护性和安全性。
CONST
    : C O N S T
    ;

CONTINUE
    : C O N T I N U E
    ;

//延时关键字。
DELAY
    : D E L A Y
    ;

DECL
    : D E C L
    ;

DEF
    : D E F
    ;

DEFAULT
    : D E F A U L T
    ;

//.dat文件定义，类似于.src用 DEF定义一样。
DEFDAT
    : D E F D A T
    ;

//函数定义
DEFFCT
    : D E F F C T
    ;

DO
    : D O
    ;

ELSE
    : E L S E
    ;

END
    : E N D
    ;

// .dat文件定义的尾部关键字： DEFDAT ... ENDDAT
ENDDAT
    : E N D D A T
    ;

//函数定义时的尾部关键字： DEFFCT ... ENDFCT
ENDFCT
    : E N D F C T
    ;

ENDFOR
    : E N D F O R
    ;

ENDIF
    : E N D I F
    ;

ENDLOOP
    : E N D L O O P
    ;

ENDSWITCH
    : E N D S W I T C H
    ;

ENDWHILE
    : E N D W H I L E
    ;

ENUM
    : E N U M
    ;

EXIT
    : E X I T
    ;


EXT
    : E X T
    ;


EXTFCT
    : E X T F C T
    ;

FALSE
    : F A L S E
    ;

FOR
    : F O R
    ;

//用于将某个变量、某个子程序 定义为全局可见。
GLOBAL
    : G L O B A L
    ;

GOTO
    : G O T O
    ;


//HALT关键字用于暂停程序，比如:
// PTP Pos_4
// HALT      ;Stop until the Start key is pressed again
// PTP Pos_5
HALT
    : H A L T
    ;

IF
    : I F
    ;

IMPORT
    : I M P O R T
    ;

//中断编程关键字，比如：GLOBAL INTERRUPT DECL Priority WHEN Event DO Subprogram
INTERRUPT
    : I N T E R R U P T
    ;

INT
    : I N T
    ;

IS
    : I S
    ;

//直线运动到指定的位置增量（相对于当前点），例如 LIN_REL {X 300, Z 1000}
LIN_REL
    : L I N '_' R E L
    ;

LIN
    : L I N
    ;

SLIN
    : S L I N
    ;

LOOP
    : L O O P
    ;

MAXIMUM
    : M A X I M U M
    ;

MINIMUM
    : M I N I M U M
    ;

NOT
    : N O T
    ;

OR
    : O R
    ;

//关键字，用于设定优先级。
PRIO
    : P R I O
    ;

// PTP motion to aggregate specification (motion relative to the previous point)
// PTP 相对于前一点的偏移运动（使用坐标聚合指定增量），比如PTP_REL {z 40}
PTP_REL
    : P T P '_' R E L
    ;

PTP
    : P T P
    ;

SPTP
    : S P T P
    ;

//声明修饰符，声明公开变量或程序，常用于系统级的接口。
PUBLIC
    : P U B L I C
    ;

//基本数据类型，相当于浮点型
REAL
    : R E A L
    ;

REPEAT
    : R E P E A T
    ;

RETURN
    : R E T U R N
    ;

//秒，比如   wait 1 sec
SEC
    : S E C
    ;

//信号定义关键字
SIGNAL
    : S I G N A L
    ;

//krl中的结构体关键字
STRUC
    : S T R U C
    ;

SWITCH
    : S W I T C H
    ;

THEN
    : T H E N
    ;

TO
    : T O
    ;

TRIGGER
    : T R I G G E R
    ;

TRUE
    : T R U E
    ;

UNTIL
    : U N T I L
    ;

WAIT
    : W A I T
    ;

WHEN
    : W H E N
    ;

WHILE
    : W H I L E
    ;
//异或运算符
EXOR
    : E X O R
    ;






//空格、制表符、换页符 会自动跳过匹配。
WS
    : (' ' | '\t' | '\u000C') -> skip
    ;

//新一行
NEWLINE
    : '\r'? '\n'
    ;


//   ~( ... ) 的含义是：匹配不在括号中的任意字符。
// ~ ('\'' | '\\' | '\r' | '\n')代表: 一个普通字符，不能是单引号、不能是反斜杠、不能是回车、不能是换行

//解析单个字符字面量，匹配形如： '\b'、'\n'、'\r'、'\323'、'\t'、'\"'、'\''、'\\'
//以及除了 '''、'\'、'回车符'、'换行符'等的其他字符
//换句话说：允许写 'a'、'%'、'\r'、'\n' 等，但不允许直接在引号中放一个真实的回车或换行符，也不允许裸写反斜杠或裸写单引号。
//    比如：char c = '\n'; 合法（通过转义表示换行字符）；但直接在引号中敲一个换行是不允许的。
// 注意，这种语法规则: '\''('\\n'|'\r\r'|~('\n'|'\r'))，即允许 char c = '\n'，但不允许 c=换行字符。 这里\n直接会被解析成换行符匹配，而\\n才会被解析成\n进行匹配。
CHARLITERAL
    : '\'' (EscapeSequence | ~ ('\'' | '\\' | '\r' | '\n')) '\''
    ;

//解析字符串，匹配形如:"\n2\t\b2132"、"23aax\nda\t"等双引号包含的字符串。
STRINGLITERAL
    : '"' (EscapeSequence | ~ ('\\' | '"' | '\r' | '\n'))* '"'
    ;

// 解析转义字符，比如: \b、\t、\n、\f、\r \" \' \\ \
fragment EscapeSequence
    : '\\' (
        'b'         //  \b:退格
        | 't'				//  \t: 制表符
        | 'n'				//  \n: 换行符
        | 'f'				//  \f：换页符
        | 'r'				//  \r：回车符
        | '"'				//  \":双引号
        | '\''			//  \': 单引号
        | '\\'			//  \\: 反斜杠
        | [0-3][0-7][0-7]   //八进制转义风格
        | [0-7][0-7]
        | [0-7]
    )
    ;

// 浮点数格式： 1.2E-17    -2.7E+11   .7E-9   6E5
REALLITERAL
    : ('0' .. '9')+ '.' ('0' .. '9')* Exponent?
    | '.' ('0' .. '9')+ Exponent?
    | ('0' .. '9')+ Exponent
    ;

fragment Exponent
    : E ('+' | '-')? ('0' .. '9')+
    ;

// INT类型允许赋值为 十进制、二进制、十六进制。
INTLITERAL
    : ('0' .. '9')+
    | HexPrefix HexDigit+ HexSuffix
    | BinPrefix BinDigit+ BinSuffix
    ;

//十六进制以  'H   开始， '结尾， 比如: INT x= 'H38FAB9'
fragment HexPrefix
    : '\'' H
    ;

fragment HexDigit
    : ('0' .. '9' | 'a' .. 'f' | 'A' .. 'F')
    ;

fragment HexSuffix
    : '\''
    ;

//二进制以  'B  开始，'结尾，比如： INT x = 'B10101'
fragment BinPrefix
    : '\'' B
    ;

fragment BinDigit
    : ('0' | '1')
    ;

fragment BinSuffix
    : '\''
    ;

// 定义允许的变量、模块、函数名字格式
//名字不能以数字开头，可以是 大小写字母、"_"、"$"
//名字中间可以出现数字。
IDENTIFIER
    : [a-zA-Z_$][a-zA-Z_$0-9]*
    ;


// =============================
//  fragment 片段规则
//  说明：
//  1) fragment 定义的规则本身不会产生独立的 Token（不会出现在词法输出里）；
//  2) 它们只能被其他词法规则引用，用于代码复用；
//  3) 这里 A~Z 的定义实现了“大小写不敏感”，例如：关键字 AND = A N D，即可匹配 and / And / AND 等。
// =============================
fragment A
    : ('a' | 'A')
    ;

fragment B
    : ('b' | 'B')
    ;

fragment C
    : ('c' | 'C')
    ;

fragment D
    : ('d' | 'D')
    ;

fragment E
    : ('e' | 'E')
    ;

fragment F
    : ('f' | 'F')
    ;

fragment G
    : ('g' | 'G')
    ;

fragment H
    : ('h' | 'H')
    ;

fragment I
    : ('i' | 'I')
    ;

fragment J
    : ('j' | 'J')
    ;

fragment K
    : ('k' | 'K')
    ;

fragment L
    : ('l' | 'L')
    ;

fragment M
    : ('m' | 'M')
    ;

fragment N
    : ('n' | 'N')
    ;

fragment O
    : ('o' | 'O')
    ;

fragment P
    : ('p' | 'P')
    ;

fragment Q
    : ('q' | 'Q')
    ;

fragment R
    : ('r' | 'R')
    ;

fragment S
    : ('s' | 'S')
    ;

fragment T
    : ('t' | 'T')
    ;

fragment U
    : ('u' | 'U')
    ;

fragment V
    : ('v' | 'V')
    ;

fragment W
    : ('w' | 'W')
    ;

fragment X
    : ('x' | 'X')
    ;

fragment Y
    : ('y' | 'Y')
    ;

fragment Z
    : ('z' | 'Z')
    ;