package tech.waitforu;

import org.antlr.v4.runtime.tree.TerminalNode;
import tech.waitforu.antlr4.krl.krlBaseVisitor;
import tech.waitforu.antlr4.krl.krlParser;
import tech.waitforu.pojo.ast.AstNode;
import tech.waitforu.pojo.ast.KrlRoot;
import tech.waitforu.pojo.ast.KrlBody;
import tech.waitforu.pojo.ast.KrlControlLine;
import tech.waitforu.pojo.ast.expression.Expression;
import tech.waitforu.pojo.ast.expression.Invocation;
import tech.waitforu.pojo.ast.programunit.*;
import tech.waitforu.pojo.ast.statements.*;
import tech.waitforu.pojo.krl.KrlFile;

import java.util.ArrayList;
import java.util.List;

/**
 * ClassName: AstBuilderVisitor
 * Package: tech.waitforu
 * Description:
 * Author: LiuKe
 * Create: 2025/12/12 10:48
 * Version 1.0
 */
public class AstBuilderVisitor extends krlBaseVisitor<AstNode> {
    KrlFile krlFile;

    public AstBuilderVisitor(KrlFile krlFile) {
        this.krlFile = krlFile;
    }

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
            throw new RuntimeException("提取到moduleData，无法转换为KrlBody");
        }

        return krlRoot;
    }

    // 提取moduleData，添加进krlBody中。
    // dataUnit未设计完全，故不继续向下遍历了。
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

    @Override
    public AstNode visitDataList(krlParser.DataListContext ctx) {
        System.out.println("暂未定义visitDataList相关方法");
        return null;
    }

    // 提取sourceFile，添加进krlRoot中。
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
            throw new RuntimeException("提取到moduleSource，无法转换为KrlBody");
        }
        return krlRoot;
    }

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


    @Override
    public AstNode visitProcedureDefinition(krlParser.ProcedureDefinitionContext ctx) {
//        System.out.println("进入了visitProcedureDefinition");
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

        List<krlParser.StatementContext> statementList = ctx.routineBody().routineImplementationSection().statementList().statement();
        for (krlParser.StatementContext statement : statementList) {
            procedureUnit.addStatement((Statement) visit(statement));
        }


        return procedureUnit;
    }

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
        List<krlParser.StatementContext> statementList = ctx.routineBody().routineImplementationSection().statementList().statement();
        statementList.forEach(statementContext ->
                functionUnit.addStatement((Statement) visit(statementContext))
        );

        return functionUnit;
    }

    @Override
    public AstNode visitSwitchStatement(krlParser.SwitchStatementContext ctx) {
        String expression = ctx.expression().getText(); //switch中用于匹配比较的表达式

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
                    labelExpression->{
                        //一个caseLabel可能会有多个标签表达式，比如case:1,2,3+4，这里把'1'、'2'、'3+4'都添加进去
                        caseBlock.addCaseLabel(labelExpression.getText());
                    }
            );
            //添加case块中的语句
             statementListList.get(i).statement().forEach(
                    statement -> caseBlock.addChildStatement((Statement) visit(statement))
             );

             switchStatement.addCaseBlock(caseBlock);
        }

        //添加default块中的语句
        if (switchBody.defaultLabel()!=null) {
            if (switchBody.defaultBody!=null) {
                List<krlParser.StatementContext> statementList = switchBody.defaultBody.statement();
                statementList.forEach(
                        statement -> switchStatement.addDefaultStatement((Statement) visit(statement))
                );

            }
        }

        return switchStatement;
    }


    @Override
    public AstNode visitLoopStatement(krlParser.LoopStatementContext ctx) {
        LoopStatement loopStatement = LoopStatement.builder()
                .withStatementType(StatementType.LOOP)
                .withStartIndex(ctx.getStart().getStartIndex())
                .withStopIndex(ctx.getStop().getStopIndex())
                .withKrlFile(krlFile)
                .withChildStatementList(new ArrayList<>())
                .build();

        ctx.statementList().statement().forEach(
                statement -> loopStatement.addChildStatement((Statement) visit(statement))
        );

        return loopStatement;
    }

    @Override
    public AstNode visitExpressionStatement(krlParser.ExpressionStatementContext ctx) {
        return ExpressionStatement.builder()
                .withKrlFile(krlFile)
                .withStartIndex(ctx.getStart().getStartIndex())
                .withStopIndex(ctx.getStop().getStopIndex())
                .withStatementType(StatementType.EXPRESSION)
                .withChildStatementList(new ArrayList<>())
                .withExpression(  (Expression) visit(ctx.expression()) )
                .build();
    }


    @Override
    public AstNode visitPrimaryExpression(krlParser.PrimaryExpressionContext ctx) {
        //返回primary表达式的结果
        return visit(ctx.primary());
    }

    @Override
    public AstNode visitInvokeCallablePrimary(krlParser.InvokeCallablePrimaryContext ctx) {
        Invocation invocation = Invocation.builder()
                .withKrlFile(krlFile)
                .withStartIndex(ctx.getStart().getStartIndex())
                .withStopIndex(ctx.getStop().getStopIndex())
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


}
