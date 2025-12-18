package tech.waitforu;

import tech.waitforu.pojo.Config;
import tech.waitforu.pojo.IgnoreRuleByStr;
import tech.waitforu.pojo.ast.AstNode;
import tech.waitforu.pojo.ast.KrlRoot;
import tech.waitforu.pojo.ast.expression.Expression;
import tech.waitforu.pojo.ast.expression.Invocation;
import tech.waitforu.pojo.ast.programunit.ProgramUnit;
import tech.waitforu.pojo.ast.programunit.ProgramUnitType;
import tech.waitforu.pojo.ast.statements.*;
import tech.waitforu.pojo.carcallgraph.CallNode;
import tech.waitforu.pojo.carcallgraph.CarCode;
import tech.waitforu.pojo.carcallgraph.CarReferenceNode;
import tech.waitforu.pojo.carcallgraph.NodeType;
import tech.waitforu.pojo.krl.KrlFile;
import tech.waitforu.pojo.krl.KrlModule;

import java.util.List;

/**
 * ClassName: CarCallReferenceAnalyze
 * Package: tech.waitforu.pojo
 * Description: 用于从KRL模块中分析出车型调用关系
 * Author: LiuKe
 * Create: 2025/12/17 17:00
 * Version 1.0
 */
public class CarCallReferenceAnalyze {
    //存放解析时临时生成的AST节点。
    private ModuleRepository moduleRepository;

    //解析规则。
    private IgnoreRuleByStr invokerParseRule;

    public CarCallReferenceAnalyze(ModuleRepository moduleRepository, IgnoreRuleByStr invokerParseRule) {
        this.moduleRepository = moduleRepository;
        this.invokerParseRule = invokerParseRule;
    }

    public CallNode analyzeCell() {
        KrlModule cellModule = moduleRepository.findByModuleName("cell");
        String callableName = "cell";


        // 解析KRL模块
        ModuleParser moduleParser = new ModuleParser(cellModule);
        AstNode astNode = moduleParser.getSrcAst();

        //判断解析是否正确。如果不是KrlRoot节点(包括为NULL)，抛出异常。同时如果astNode是KrlRoot节点，将其转换为KrlRoot类型。
        if (!(astNode instanceof KrlRoot krlRoot)) {
            throw new RuntimeException("解析出错，" + cellModule.getModuleName() + "模块中不存在KrlRoot节点");
        }

        String nodeValue = cellModule.getModuleName();
        NodeType nodeType = NodeType.CEll;
        String id = nodeType + ":" + nodeValue;
        String relevantInfo = krlRoot.getTextContent();
        CallNode callNode = new CallNode(id, nodeValue, nodeType, relevantInfo);

        Statement statement = krlRoot.getBody().getMainProgramUnit().getStatementFirst(StatementType.LOOP);
        if (!(statement instanceof LoopStatement loopStatement)) {
            throw new RuntimeException("解析出错，" + cellModule.getModuleName() + "模块中不存在LOOP语句");
        }

        statement = loopStatement.getChildStatementFirst(StatementType.SWITCH);
        if (!(statement instanceof SwitchStatement switchStatement)) {
            throw new RuntimeException("解析出错，" + cellModule.getModuleName() + "模块中不存在SWITCH语句");
        }

        String switchExpression = switchStatement.getSwitchExpression();

        if (!switchExpression.equalsIgnoreCase("PGNO")) {
            throw new RuntimeException("解析出错，" + cellModule.getModuleName() + "模块中SWITCH语句不是用来判断PGNO车型号的");
        }

        List<CaseBlock> caseBlockList = switchStatement.getCaseBlocks();

        caseBlockList.forEach(
                caseBlock ->
                {
                    List<String> caseLabel = caseBlock.getCaseLabel();
                    List<Statement> childStatementList = caseBlock.getChildStatement(StatementType.EXPRESSION);
                    childStatementList.forEach(
                            childStatement ->
                            {
                                if (!(childStatement instanceof ExpressionStatement expressionStatement)) {
                                    throw new RuntimeException("解析出错，" + cellModule.getModuleName() + "模块中的CASE块中未找到表达式语句");
                                }
                                Expression expression = expressionStatement.getExpression();
                                if (expression instanceof Invocation invocation) {
                                    String targetName = invocation.getTargetName();
                                    ProgramUnitType targetType = invocation.getTargetType();

                                    if (!invokerParseRule.isIgnore(targetName)) {

                                        //如果一个case对应多个标签，name会被解析为多个结点。
                                        caseLabel.forEach(
                                                label ->
                                                {
                                                    int majorIndexOfCar = Integer.parseInt(label);
                                                    KrlModule module = moduleRepository.findByCallableName(targetName);
                                                    CallNode pProgramNode = parsePProgram(module, targetName, majorIndexOfCar);
                                                    pProgramNode.setRelevantInfo(caseBlock.getTextContent());
                                                    callNode.addChild(pProgramNode);
                                                }
                                        );
                                    }
                                }
                            }
                    );
                }
        );


        return callNode;
    }

    public CallNode parsePProgram(KrlModule pProgramModule, String callableName, int majorIndexOfCar) {
        ModuleParser moduleParser = new ModuleParser(pProgramModule);
        AstNode astNode = moduleParser.getSrcAst();
        //判断解析是否正确。如果不是KrlRoot节点(包括为NULL)，抛出异常。同时如果astNode是KrlRoot节点，将其转换为KrlRoot类型。
        if (!(astNode instanceof KrlRoot krlRoot)) {
            throw new RuntimeException("解析出错，" + pProgramModule.getModuleName() + "模块中不存在KrlRoot节点");
        }

        String nodeValue = pProgramModule.getModuleName();
        NodeType nodeType = NodeType.P_PROGRAM;
        String id = nodeType + ":" + nodeValue;
        CallNode pProgramNode = new CallNode(id, nodeValue, nodeType, null);


        // 从KRL根节点中获取所有程序单元列表，筛选出名称与调用目标名称匹配的程序单元。
        // 每个模块只有一个与调用目标名称匹配的程序单元,因此可以直接获取第一个匹配项。
        ProgramUnit callProgramUnit = krlRoot.getBody().getProgramUnitList().stream()
                .filter(programUnit -> programUnit.getName().equalsIgnoreCase(callableName))
                .toList().getFirst();

        Statement statement = callProgramUnit.getStatementFirst(StatementType.SWITCH);
        if (!(statement instanceof SwitchStatement switchStatement)) {
            throw new RuntimeException("解析出错，" + pProgramModule.getModuleName() + "模块中不存在SWITCH语句");
        }

        String switchExpression = switchStatement.getSwitchExpression();

        if (!switchExpression.equalsIgnoreCase("GIPGNO2")) {
            throw new RuntimeException("解析出错，" + pProgramModule.getModuleName() + "模块中SWITCH语句不是用来判断PGNO车型号的");
        }

        List<CaseBlock> caseBlockList = switchStatement.getCaseBlocks();
        caseBlockList.forEach(
                caseBlock ->
                {
                    List<String> caseLabel = caseBlock.getCaseLabel();
                    List<Statement> childStatementList = caseBlock.getChildStatement(StatementType.EXPRESSION);
                    List<Invocation> invocationList = childStatementList.stream()
                            .filter(childStatement -> childStatement instanceof ExpressionStatement expressionStatement)
                            .map(childStatement -> ((ExpressionStatement) childStatement).getExpression())
                            .filter(expression -> expression instanceof Invocation)
                            .map(expression -> (Invocation) expression)
                            .toList();


                    childStatementList.forEach(
                            childStatement ->
                            {
                                if (!(childStatement instanceof ExpressionStatement expressionStatement)) {
                                    throw new RuntimeException("解析出错，" + pProgramModule.getModuleName() + "模块中的CASE块中未找到表达式语句");
                                }
                                Expression expression = expressionStatement.getExpression();
                                if (expression instanceof Invocation invocation) {
                                    String targetName = invocation.getTargetName();
                                    ProgramUnitType targetType = invocation.getTargetType();

                                    if (!invokerParseRule.isIgnore(targetName)) {

                                        //如果一个case对应多个标签，name会被解析为多个结点。
                                        caseLabel.forEach(
                                                label ->
                                                {
                                                    int minorIndexOfCar = Integer.parseInt(label);

                                                    CallNode carCodeNode = parseCarCode(majorIndexOfCar, minorIndexOfCar);

                                                    //将carCodeNode的相关信息就直接设置为carCode的值，比如622、105、1202。
                                                    carCodeNode.setRelevantInfo(carCodeNode.getValue());

                                                    KrlModule module = moduleRepository.findByCallableName(targetName);

                                                    //解析出车型程序的结点。
                                                    CallNode carProgramNode = parseCarProgram(module, targetName);
                                                    carProgramNode.setRelevantInfo(caseBlock.getTextContent());


                                                    //将车型程序连接在车型码下面。将车型码连接在P程序的下面。
                                                    carCodeNode.addChild(carProgramNode);
                                                    pProgramNode.addChild(carCodeNode);
                                                }
                                        );
                                    }
                                }
                            }
                    );
                }
        );

        return pProgramNode;
    }

    public CallNode parseCarCode(int majorIndexOfCar, int minorIndexOfCar) {
        String id = null;
        String value = null;
        NodeType nodeType = NodeType.CAR_CODE;
        CarCode carCode = new CarCode(id, nodeType, majorIndexOfCar, minorIndexOfCar);
        value = carCode.getValue();
        id = value + ":" + minorIndexOfCar;
        carCode.setId(id);

        return carCode;
    }

    public CallNode parseCarProgram(KrlModule carProgramModule, String callableName) {

        ModuleParser moduleParser = new ModuleParser(carProgramModule);
        AstNode astNode = moduleParser.getSrcAst();
        //判断解析是否正确。如果不是KrlRoot节点(包括为NULL)，抛出异常。同时如果astNode是KrlRoot节点，将其转换为KrlRoot类型。
        if (!(astNode instanceof KrlRoot krlRoot)) {
            throw new RuntimeException("解析出错，" + carProgramModule.getModuleName() + "模块中不存在KrlRoot节点");
        }

        String nodeValue = carProgramModule.getModuleName();
        NodeType nodeType = NodeType.CAR_PROGRAM;
        String id = nodeType + ":" + nodeValue;
        CallNode carProgramNode = new CallNode(id, nodeValue, nodeType, null);


        // 从KRL根节点中获取所有程序单元列表，筛选出名称与调用目标名称匹配的程序单元。
        // 每个模块只有一个与调用目标名称匹配的程序单元,因此可以直接获取第一个匹配项。
        ProgramUnit callProgramUnit = krlRoot.getBody().getProgramUnitList().stream()
                .filter(programUnit -> programUnit.getName().equalsIgnoreCase(callableName))
                .toList().getFirst();

        List<Invocation> invocationList = callProgramUnit.findNodesByType(Invocation.class);

        invocationList.forEach(
                invocation ->
                {
                    String targetName = invocation.getTargetName().toLowerCase();

                    if (!invokerParseRule.isIgnore(targetName)) {


                        String routeNodeValue = targetName;
                        NodeType routNodeType = NodeType.ROUTE_PROCESS;
                        String routeNodeId = routNodeType + ":" + routeNodeValue;
                        CallNode routeProcessNode = new CallNode(routeNodeId, routeNodeValue, routNodeType, null);

                        String routeNodeRelevantInfo = invocation.getTextContent();
                        routeProcessNode.setRelevantInfo(routeNodeRelevantInfo);
                        carProgramNode.addChild(routeProcessNode);
                    }

                }
        );


        return carProgramNode;
    }

    public static void main(String[] args) {
        YamlConfigService yamlConfigService = new YamlConfigService("/Users/liuke/IdeaProjects/KRLParser/src/main/resources/config.yml");
        Config config = yamlConfigService.loadConfig(new Config());
        IgnoreRuleByStr fileLoadRule = new IgnoreRuleByStr(config.fileLoadSection);
        IgnoreRuleByStr invokerParseRule = new IgnoreRuleByStr(config.invokerParseSection);
        String zipFilePath = "/Desktop/EC010_L1.zip";
        KrlZipLoader krlZipLoader = new KrlZipLoader(zipFilePath, fileLoadRule);
        List<KrlFile> krlFileList = krlZipLoader.getKrlFileList();
        ModuleRepository moduleRepository = new ModuleRepository();
        moduleRepository.assembleFromFileList(krlFileList);

        CarCallReferenceAnalyze carCallReferenceAnalyze = new CarCallReferenceAnalyze(moduleRepository, invokerParseRule);

        CallNode carReferenceNode = carCallReferenceAnalyze.analyzeCell();

        System.out.println(carReferenceNode.getValue());


    }
}
