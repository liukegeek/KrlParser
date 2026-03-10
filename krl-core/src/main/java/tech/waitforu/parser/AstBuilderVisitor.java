package tech.waitforu.parser;

import org.antlr.v4.runtime.tree.TerminalNode;
import tech.waitforu.antlr4.krlBaseVisitor;
import tech.waitforu.antlr4.krlParser;
import tech.waitforu.exception.KrlParseException;
import tech.waitforu.pojo.ast.AstNode;
import tech.waitforu.pojo.ast.KrlRoot;
import tech.waitforu.pojo.ast.KrlBody;
import tech.waitforu.pojo.ast.KrlControlLine;
import tech.waitforu.pojo.ast.expression.Expression;
import tech.waitforu.pojo.ast.expression.ExpressionType;
import tech.waitforu.pojo.ast.expression.Invocation;
import tech.waitforu.pojo.ast.expression.VariableExpression;
import tech.waitforu.pojo.ast.programunit.DataUnit;
import tech.waitforu.pojo.ast.programunit.FunctionUnit;
import tech.waitforu.pojo.ast.programunit.ProcedureUnit;
import tech.waitforu.pojo.ast.programunit.ProgramUnit;
import tech.waitforu.pojo.ast.programunit.ProgramUnitType;
import tech.waitforu.pojo.ast.statements.CaseBlock;
import tech.waitforu.pojo.ast.statements.ExpressionStatement;
import tech.waitforu.pojo.ast.statements.ForStatement;
import tech.waitforu.pojo.ast.statements.IfStatement;
import tech.waitforu.pojo.ast.statements.LoopStatement;
import tech.waitforu.pojo.ast.statements.RepeatStatement;
import tech.waitforu.pojo.ast.statements.Statement;
import tech.waitforu.pojo.ast.statements.StatementType;
import tech.waitforu.pojo.ast.statements.SwitchStatement;
import tech.waitforu.pojo.ast.statements.WhileStatement;
import tech.waitforu.pojo.krl.KrlFile;

import java.util.ArrayList;
import java.util.List;

/**
 * KRL 语法树到业务 AST 的构建访问器。
 * <p>
 * 该 Visitor 在 ANTLR 解析树基础上提炼业务关心的结构：
 * - 文件根节点（KrlRoot/KrlBody）
 * - 程序单元（Procedure/Function/Data）
 * - 语句（If/For/While/Repeat/Switch/Loop/Expression）
 * - 表达式（Invocation/Variable）
 */
public class AstBuilderVisitor extends krlBaseVisitor<AstNode> {
    /** 当前正在构建 AST 的源文件元信息。 */
    KrlFile krlFile;

    /**
     * 创建 AST 构建访问器。
     *
     * @param krlFile 当前被解析的 KRL 文件
     */
    public AstBuilderVisitor(KrlFile krlFile) {
        this.krlFile = krlFile;
    }

    /**
     * 访问 dat 文件入口并构建 KrlRoot。
     *
     * @param ctx dat 文件语法上下文
     * @return dat 文件 AST 根节点
     */
    @Override
    public AstNode visitDataFile(krlParser.DataFileContext ctx) {
        KrlRoot krlRoot = new KrlRoot(ctx.getStart().getStartIndex(), ctx.getStop().getStopIndex(), krlFile);

        List<TerminalNode> krlControlLineList = ctx.krlControlHead().KrlControlLine();
        for (TerminalNode terminalNode : krlControlLineList) {
            int startIndex = terminalNode.getSymbol().getStartIndex();
            int stopIndex = terminalNode.getSymbol().getStopIndex();
            krlRoot.addKrlControlLine(new KrlControlLine(startIndex, stopIndex, krlFile));
        }

        AstNode dataUnit = visit(ctx.moduleData());
        if (dataUnit instanceof KrlBody) {
            krlRoot.setBody((KrlBody) dataUnit);
        } else {
            throw new KrlParseException("文件 " + krlFile.getPath() + " 的 moduleData 无法转换为 KrlBody");
        }

        return krlRoot;
    }

    /**
     * 访问 moduleData 并构建数据单元。
     * <p>
     * 当前仅保留数据单元框架，具体 data 声明细节暂未展开。
     *
     * @param ctx moduleData 上下文
     * @return KrlBody
     */
    @Override
    public AstNode visitModuleData(krlParser.ModuleDataContext ctx) {
        KrlBody krlBody = new KrlBody(ctx.getStart().getStartIndex(), ctx.getStop().getStopIndex(), krlFile);

        DataUnit dataUnit = new DataUnit(ctx.getStart().getStartIndex(), ctx.getStop().getStopIndex(), krlFile);
        dataUnit.setName(ctx.moduleName().getText());
        dataUnit.setType(ProgramUnitType.DATA);

        // 数据定义部分，暂时没用，没写！
        visit(ctx.dataList());

        krlBody.addProgramUnit(dataUnit);

        return krlBody;
    }

    /**
     * 访问数据列表节点。
     * <p>
     * 当前版本暂未实现 dataList 精细化抽象。
     *
     * @param ctx dataList 上下文
     * @return null（占位实现）
     */
    @Override
    public AstNode visitDataList(krlParser.DataListContext ctx) {
        throw new KrlParseException("暂未实现 DAT 数据列表解析: " + krlFile.getPath());
//        return null;
    }

    /**
     * 访问 src 文件入口并构建 KrlRoot。
     *
     * @param ctx src 文件语法上下文
     * @return src 文件 AST 根节点
     */
    @Override
    public AstNode visitSourceFile(krlParser.SourceFileContext ctx) {

        KrlRoot krlRoot = new KrlRoot(ctx.getStart().getStartIndex(), ctx.getStop().getStopIndex(), krlFile);

        List<TerminalNode> krlControlLineList = ctx.krlControlHead().KrlControlLine();
        for (TerminalNode terminalNode : krlControlLineList) {
            int startIndex = terminalNode.getSymbol().getStartIndex();
            int stopIndex = terminalNode.getSymbol().getStopIndex();
            krlRoot.addKrlControlLine(new KrlControlLine(startIndex, stopIndex, krlFile));
        }

        AstNode krlBody = visit(ctx.moduleSource());
        if (krlBody instanceof KrlBody) {
            krlRoot.setBody((KrlBody) krlBody);
        } else {
            throw new KrlParseException("文件 " + krlFile.getPath() + " 的 moduleSource 无法转换为 KrlBody");
        }
        return krlRoot;
    }

    /**
     * 访问模块源码主体，提取主程序与子程序。
     *
     * @param ctx moduleSource 上下文
     * @return 包含主/子程序单元的 KrlBody
     */
    @Override
    public AstNode visitModuleSource(krlParser.ModuleSourceContext ctx) {
        KrlBody krlBody = new KrlBody(ctx.getStart().getStartIndex(), ctx.getStop().getStopIndex(), krlFile);

        // 提取主程序单元。  由于mainRoutine只有一种路径，故而这里直接拿第一个子结点语句，只可能拿到Procedure或Function
        ProgramUnit mainProgramUnit = (ProgramUnit) visit(ctx.mainRoutine().children.getFirst());
        krlBody.addProgramUnit(mainProgramUnit);

        if (ctx.subRoutine() != null && !ctx.subRoutine().isEmpty()) {
            List<krlParser.SubRoutineContext> subRoutineContexts = ctx.subRoutine();
            for (krlParser.SubRoutineContext subRoutineContext : subRoutineContexts) {
                // 提取子程序单元。  由于subRoutine只有一种路径，故而这里直接拿第一个子结点语句，只可能拿到Procedure或Function
                ProgramUnit subProgramUnit = (ProgramUnit) visit(subRoutineContext.children.getFirst());
                krlBody.addProgramUnit(subProgramUnit);
            }
        }
        return krlBody;
    }


    /**
     * 构建过程（Procedure）程序单元。
     *
     * @param ctx 过程定义上下文
     * @return ProcedureUnit
     */
    @Override
    public AstNode visitProcedureDefinition(krlParser.ProcedureDefinitionContext ctx) {
        ProcedureUnit procedureUnit = new ProcedureUnit(ctx.getStart().getStartIndex(), ctx.getStop().getStopIndex(), krlFile);
        procedureUnit.setName(ctx.procedureName().getText());
        procedureUnit.setType(ProgramUnitType.PROCEDURE);

        if (ctx.GLOBAL() != null) {
            procedureUnit.setIsGlobal(true);
        }

        //参数部分，暂时不用，没写
        visit(ctx.parameterList());

        //数据定义部分，暂时不用，没写
        visit(ctx.routineBody().routineDataSection());

        List<Statement> statementList = visitStatementList(ctx.routineBody().routineImplementationSection().statementList().statement());
        statementList.forEach(procedureUnit::addStatement);


        return procedureUnit;
    }

    /**
     * 构建函数（Function）程序单元。
     *
     * @param ctx 函数定义上下文
     * @return FunctionUnit
     */
    @Override
    public AstNode visitFunctionDefinition(krlParser.FunctionDefinitionContext ctx) {
        FunctionUnit functionUnit = new FunctionUnit(ctx.getStart().getStartIndex(), ctx.getStop().getStopIndex(), krlFile);
        functionUnit.setName(ctx.functionName().getText());
        functionUnit.setType(ProgramUnitType.FUNCTION);

        if (ctx.GLOBAL() != null) {
            functionUnit.setIsGlobal(true);
        }

        //返回值部分，暂时不用，没写
        visit(ctx.typeName());

        //参数部分，暂时不用，没写
        visit(ctx.parameterList());

        //数据定义部分，暂时不用，没写
        visit(ctx.routineBody().routineDataSection());

        //语句部分
        List<Statement> statementList = visitStatementList(ctx.routineBody().routineImplementationSection().statementList().statement());
        statementList.forEach(functionUnit::addStatement);

        return functionUnit;
    }

    /**
     * 构建 switch 语句节点。
     *
     * @param ctx switch 语句上下文
     * @return SwitchStatement
     */
    @Override
    public AstNode visitSwitchStatement(krlParser.SwitchStatementContext ctx) {
        // Switch语句中的 表达式，就是变量。
        Expression expression = (Expression) visit(ctx.expression()); //switch中用于匹配比较的表达式

        SwitchStatement switchStatement = SwitchStatement.builder()
                .withKrlFile(krlFile)
                .withStartIndex(ctx.getStart().getStartIndex())
                .withStopIndex(ctx.getStop().getStopIndex())
                .withStatementType(StatementType.SWITCH)
                .withSwitchExpression(expression)
                .withCaseBlocks(new ArrayList<>())
                .build();


        krlParser.SwitchBlockStatementGroupsContext switchBody = ctx.switchBlockStatementGroups();

        List<krlParser.CaseLabelsContext> caseLabelsList = switchBody.caseLabels();
        List<krlParser.StatementListContext> statementListList = switchBody.statementList();

        // caseLabels 与 statementList 一一对应，逐组构建 CaseBlock。
        for (int i = 0; i < caseLabelsList.size(); i++) {
            CaseBlock caseBlock = CaseBlock.builder()
                    .withKrlFile(krlFile)
                    .withStartIndex(caseLabelsList.get(i).getStart().getStartIndex())
                    //注意这里一个 caseLabel+statementList 对应一个 caseBlock，因此开始和结束语句索引要对应起来
                    .withStopIndex(statementListList.get(i).getStop().getStopIndex())
                    .withStatementType(StatementType.CASE_BLOCK)
                    .withCaseLabel(new ArrayList<>())
                    .withChildStatementList(new ArrayList<>())
                    .build();

            //添加case标签表达式
            caseLabelsList.get(i).expression().forEach(
                    labelExpression -> {
                        //一个caseLabel可能会有多个标签表达式，比如case:1,2,3+4，这里把'1'、'2'、'3+4'都添加进去
                        caseBlock.addCaseLabel(labelExpression.getText());
                    }
            );
            //添加case块中的语句
            visitStatementList(statementListList.get(i).statement()).forEach(caseBlock::addChildStatement);

            switchStatement.addCaseBlock(caseBlock);
        }

        //添加default块中的语句
        if (switchBody.defaultLabel() != null) {
            if (switchBody.defaultBody != null) {
                visitStatementList(switchBody.defaultBody.statement()).forEach(switchStatement::addDefaultStatement);

            }
        }

        return switchStatement;
    }


    /**
     * 构建 loop 语句节点。
     *
     * @param ctx loop 语句上下文
     * @return LoopStatement
     */
    @Override
    public AstNode visitLoopStatement(krlParser.LoopStatementContext ctx) {
        LoopStatement loopStatement = LoopStatement.builder()
                .withStatementType(StatementType.LOOP)
                .withStartIndex(ctx.getStart().getStartIndex())
                .withStopIndex(ctx.getStop().getStopIndex())
                .withKrlFile(krlFile)
                .withChildStatementList(new ArrayList<>())
                .build();

        visitStatementList(ctx.statementList().statement()).forEach(loopStatement::addChildStatement);

        return loopStatement;
    }

    /**
     * 构建 IF/ELSE 语句节点。
     *
     * @param ctx IF/ELSE 语句上下文
     * @return IfStatement
     */
    @Override
    public AstNode visitIfStatement(krlParser.IfStatementContext ctx) {
        List<krlParser.StatementListContext> statementLists = ctx.statementList();
        List<Statement> thenStatements = statementLists.isEmpty()
                ? new ArrayList<>()
                : visitStatementList(statementLists.getFirst().statement());
        List<Statement> elseStatements = statementLists.size() < 2
                ? new ArrayList<>()
                : visitStatementList(statementLists.get(1).statement());

        return IfStatement.builder()
                .withKrlFile(krlFile)
                .withStartIndex(ctx.getStart().getStartIndex())
                .withStopIndex(ctx.getStop().getStopIndex())
                .withStatementType(StatementType.IF_ELSE)
                .withChildStatementList(new ArrayList<>())
                .withConditionExpression(visitExpressionNode(ctx.expression()))
                .withThenStatementList(thenStatements)
                .withElseStatementList(elseStatements)
                .build();
    }

    /**
     * 构建 FOR 语句节点。
     *
     * @param ctx FOR 语句上下文
     * @return ForStatement
     */
    @Override
    public AstNode visitForStatement(krlParser.ForStatementContext ctx) {
        return ForStatement.builder()
                .withKrlFile(krlFile)
                .withStartIndex(ctx.getStart().getStartIndex())
                .withStopIndex(ctx.getStop().getStopIndex())
                .withStatementType(StatementType.FOR)
                .withChildStatementList(new ArrayList<>())
                .withCounterName(ctx.krlIdentifier().getText())
                .withFromExpression(visitExpressionNode(ctx.expression(0)))
                .withToExpression(visitExpressionNode(ctx.expression(1)))
                .withStepExpression(ctx.expression().size() > 2 ? visitExpressionNode(ctx.expression(2)) : null)
                .withBodyStatementList(visitStatementList(ctx.statementList().statement()))
                .build();
    }

    /**
     * 构建 REPEAT/UNTIL 语句节点。
     *
     * @param ctx REPEAT/UNTIL 语句上下文
     * @return RepeatStatement
     */
    @Override
    public AstNode visitRepeatStatement(krlParser.RepeatStatementContext ctx) {
        return RepeatStatement.builder()
                .withKrlFile(krlFile)
                .withStartIndex(ctx.getStart().getStartIndex())
                .withStopIndex(ctx.getStop().getStopIndex())
                .withStatementType(StatementType.REPEAT)
                .withChildStatementList(new ArrayList<>())
                .withUntilExpression(visitExpressionNode(ctx.expression()))
                .withBodyStatementList(visitStatementList(ctx.statementList().statement()))
                .build();
    }

    /**
     * 构建 WHILE 语句节点。
     *
     * @param ctx WHILE 语句上下文
     * @return WhileStatement
     */
    @Override
    public AstNode visitWhileStatement(krlParser.WhileStatementContext ctx) {
        return WhileStatement.builder()
                .withKrlFile(krlFile)
                .withStartIndex(ctx.getStart().getStartIndex())
                .withStopIndex(ctx.getStop().getStopIndex())
                .withStatementType(StatementType.WHILE)
                .withChildStatementList(new ArrayList<>())
                .withConditionExpression(visitExpressionNode(ctx.expression()))
                .withBodyStatementList(visitStatementList(ctx.statementList().statement()))
                .build();
    }

    /**
     * 构建表达式语句节点。
     *
     * @param ctx 表达式语句上下文
     * @return ExpressionStatement
     */
    @Override
    public AstNode visitExpressionStatement(krlParser.ExpressionStatementContext ctx) {
        return ExpressionStatement.builder()
                .withKrlFile(krlFile)
                .withStartIndex(ctx.getStart().getStartIndex())
                .withStopIndex(ctx.getStop().getStopIndex())
                .withStatementType(StatementType.EXPRESSION)
                .withChildStatementList(new ArrayList<>())
                .withExpression((Expression) visit(ctx.expression()))
                .build();
    }


    /**
     * 主表达式透传：返回其 primary 子节点的访问结果。
     *
     * @param ctx primaryExpression 上下文
     * @return primary 对应 AST 节点
     */
    @Override
    public AstNode visitPrimaryExpression(krlParser.PrimaryExpressionContext ctx) {
        //返回primary表达式的结果
        return visit(ctx.primary());
    }

    /**
     * 构建调用表达式（Invocation）。
     *
     * @param ctx 调用表达式上下文
     * @return Invocation 节点
     */
    @Override
    public AstNode visitInvokeCallablePrimary(krlParser.InvokeCallablePrimaryContext ctx) {
        Invocation invocation = Invocation.builder()
                .withKrlFile(krlFile)
                .withStartIndex(ctx.getStart().getStartIndex())
                .withStopIndex(ctx.getStop().getStopIndex())
                .withExpressionType(ExpressionType.INVOKE_CALLABLE)
                .withTargetName(ctx.callableName.getText())
                .withArgumentList(new ArrayList<>())
                .withCallTarget(null) //目标调用对象，此时无法确定，需要在模块表中查找然后设置
                .withTargetType(null)//目标调用类型，可能是procedure也可能是function，此时无法确定
                .build();
        //参数部分
        ctx.expression().forEach(
                expression -> invocation.addArgument(expression.getText())
        );

        return invocation;
    }

    /**
     * 构建变量表达式节点。
     *
     * @param ctx 变量表达式上下文
     * @return VariableExpression
     */
    @Override
    public AstNode visitVariablePrimary(krlParser.VariablePrimaryContext ctx) {
        return VariableExpression.builder()
                .withKrlFile(krlFile)
                .withStartIndex(ctx.getStart().getStartIndex())
                .withStopIndex(ctx.getStop().getStopIndex())
                .withExpressionType(ExpressionType.VARIABLE)
                .withVariableName(ctx.getText())
                .build();
    }

    /**
     * 将语法树中的 statement 列表转换为业务 AST 语句列表。
     * <p>
     * 未实现的语句类型会返回 null；这里统一过滤掉，避免在父节点中留下空槽位。
     *
     * @param statementContexts 语法树 statement 列表
     * @return 业务 AST 语句列表
     */
    private List<Statement> visitStatementList(List<krlParser.StatementContext> statementContexts) {
        List<Statement> statements = new ArrayList<>();
        for (krlParser.StatementContext statementContext : statementContexts) {
            AstNode node = visit(statementContext);
            if (node instanceof Statement statement) {
                statements.add(statement);
            }
        }
        return statements;
    }

    /**
     * 访问表达式节点并在返回值可转换时返回 Expression。
     *
     * @param expressionContext 表达式上下文
     * @return Expression；未构建出业务表达式时返回 null
     */
    private Expression visitExpressionNode(krlParser.ExpressionContext expressionContext) {
        AstNode node = visit(expressionContext);
        if (node instanceof Expression expression) {
            return expression;
        }
        return null;
    }
}
