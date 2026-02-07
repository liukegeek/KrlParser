package tech.waitforu.parser;

import tech.waitforu.loader.KrlZipLoader;
import tech.waitforu.loader.YamlConfigLoad;
import tech.waitforu.pojo.ast.statements.CaseBlock;
import tech.waitforu.pojo.ast.statements.ExpressionStatement;
import tech.waitforu.pojo.ast.statements.Statement;
import tech.waitforu.pojo.ast.statements.StatementType;
import tech.waitforu.pojo.ast.statements.SwitchStatement;
import tech.waitforu.pojo.carcallgraph.CarReferenceNode;
import tech.waitforu.pojo.config.Config;
import tech.waitforu.pojo.ast.AstNode;
import tech.waitforu.pojo.ast.KrlRoot;
import tech.waitforu.pojo.ast.expression.Expression;
import tech.waitforu.pojo.ast.expression.Invocation;
import tech.waitforu.pojo.ast.expression.Variable;
import tech.waitforu.pojo.ast.programunit.ProgramUnit;
import tech.waitforu.pojo.ast.programunit.ProgramUnitType;
import tech.waitforu.pojo.carcallgraph.CallNode;
import tech.waitforu.pojo.carcallgraph.CarCode;
import tech.waitforu.pojo.carcallgraph.NodeType;
import tech.waitforu.pojo.krl.KrlFile;
import tech.waitforu.pojo.krl.KrlModule;
import tech.waitforu.rule.IgnoreRuleByStr;

import java.io.IOException;
import java.util.*;

/**
 * ClassName: parser.tech.waitforu.CarCallReferenceAnalyze
 * Package: tech.waitforu.pojo
 * Description: 用于从KRL模块中分析出车型调用关系
 * Author: LiuKe
 * Create: 2025/12/17 17:00
 * Version 1.0
 */
public class CarCallReferenceAnalyze {
    //存放解析时临时生成的AST节点。
    private final ModuleRepository moduleRepository;

    //解析规则。
    private final IgnoreRuleByStr invokerParseRule;

    // 用于处理当前已经处理过的节点ID，避免一个机器人中出现相同节点。
    private final Map<String, CarReferenceNode> existingNodes = new HashMap<>();

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
        CallNode cellNode;
        if (existingNodes.containsKey(id) && existingNodes.get(id) instanceof CallNode) {
            // 如果节点已存在，为体现KRL中模块名字唯一的思想，直接返回已存在的节点。
            return (CallNode) existingNodes.get(id);
        }

        // 如果节点不存在，则创建新节点
        cellNode = new CallNode(id, nodeValue, nodeType, relevantInfo);
        //设置结点的补充信息,关于模块文件的。
        this.setPropertyAboutFile(cellNode, cellModule);


        List<SwitchStatement> astNodeList = krlRoot.getBody().getMainProgramUnit().findNodesByType(SwitchStatement.class);
        SwitchStatement switchStatement = null;

        // 过滤出通过"PGNO"变量进行判断的SWITCH语句
        for (SwitchStatement statement : astNodeList) {
            String switchExpression = statement.getSwitchExpression();
            if (switchExpression.equalsIgnoreCase("PGNO") || switchExpression.equalsIgnoreCase("GIPGNO")) {
                switchStatement = statement;
                break;
            }
        }
        if (switchStatement == null) {
            throw new RuntimeException("解析出错，" + cellModule.getModuleName() + "cell模块中不存在通过PGNO变量进行判断的的SWITCH语句，请确认备份和修改本程序代码");
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
                                                    if (!cellNode.getChildren().contains(pProgramNode)) {
                                                        cellNode.addChild(pProgramNode);
                                                    }
                                                }
                                        );
                                    }
                                }
                            }
                    );
                }
        );

        existingNodes.put(id, cellNode);
        return cellNode;
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
        CallNode pProgramNode;

        if (existingNodes.containsKey(id) && existingNodes.get(id) instanceof CallNode) {
            // 如果节点已存在，为体现KRL中模块名字唯一的思想，直接赋值已存在的节点。
            // 注意，因为车型号(CarCodeNode)依赖于cell中不同case所传入的标签变量majorIndexOfCar，此时不一定遍历完cell中的所有case，该模块的车型号仍可能变动，故而不能像cell、carProgram节点一样直接return。
             pProgramNode = (CallNode) existingNodes.get(id);
        } else {
            pProgramNode = new CallNode(id, nodeValue, nodeType, null);
            //设置结点的补充信息,关于模块文件的。
            this.setPropertyAboutFile(pProgramNode, pProgramModule);
        }


        // 从KRL根节点中获取所有程序单元列表，筛选出名称与调用目标名称匹配的程序单元。
        // 每个模块只有一个与调用目标名称匹配的程序单元,因此可以直接获取第一个匹配项。
        ProgramUnit callProgramUnit = krlRoot.findNodesByType(ProgramUnit.class).stream()
                .filter(programUnit -> programUnit.getName().equalsIgnoreCase(callableName))
                .toList().getFirst();

        List<Variable> variableList = callProgramUnit.findNodesByType(Variable.class);
        for (int i = 0; i < variableList.size(); i++) {
            if (variableList.get(i).getVariableName().equalsIgnoreCase("GIPGNO2")) {
                break;
            }

            //如果遍历完所有变量都没有找到"GIPGNO2"变量，即该模块不是P程序，直接按照车型程序解析。
            if (i == variableList.size() - 1) {
                // 由于没有P程序，故而原来P程序的结点类型设置为VIRTUAL。
                pProgramNode.setNodeType(NodeType.VIRTUAL);

                CallNode carCodeNode = parseCarCode(majorIndexOfCar, 0);

                //将carCodeNode的相关信息就直接设置为carCode的值，比如622、105、1202。
                carCodeNode.setRelevantInfo(carCodeNode.getValue());

                // 此时车型程序就是当前调用的程序单元。
                KrlModule carProgramModule = pProgramModule;
                String carProgramName = callableName;
                //解析出车型程序的结点。
                CallNode carProgramNode = parseCarProgram(carProgramModule, carProgramName);
                //将车型程序的相关信息设置为车型程序的名称。
                carProgramNode.setRelevantInfo("无P程序，车型调用位于cell中");

                //将车型程序连接在车型码下面。将车型码连接在P程序的下面。
                if (!carCodeNode.getChildren().contains(carProgramNode)) {
                    // 如果车型码节点中不存在车型程序节点，将车型程序节点添加到车型码节点中。
                    carCodeNode.addChild(carProgramNode);
                }
                if (!pProgramNode.getChildren().contains(carCodeNode)) {
                    // 如果P程序中不存在车型码节点，将车型码节点添加到P程序中。
                    pProgramNode.addChild(carCodeNode);
                }

                existingNodes.put(id, pProgramNode);
                return pProgramNode;
            }
        }


        List<Statement> statementList = callProgramUnit.getStatementList(StatementType.SWITCH);
        SwitchStatement switchStatement = null;
        for (Statement statement : statementList) {
            // 遍历所有SWITCH语句，找到第一个表达式为"GIPGNO2"的SWITCH语句。
            if (((SwitchStatement) statement).getSwitchExpression().equalsIgnoreCase("GIPGNO2")) {
                switchStatement = (SwitchStatement) statement;
                break;
            }
        }
        // 判断是否存在SWITCH语句，且其匹配表达式为"GIPGNO2"
        if (switchStatement == null) {
            throw new RuntimeException("解析出错，" + pProgramModule.getModuleName() + "模块中未找到用于匹配`GIPGNO2`变量的SWITCH语句");
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
                                                    if (!carCodeNode.getChildren().contains(carProgramNode)) {
                                                        // 如果车型码节点中不存在车型程序节点，将车型程序节点添加到车型码节点中。
                                                        carCodeNode.addChild(carProgramNode);
                                                    }
                                                    if (!pProgramNode.getChildren().contains(carCodeNode)) {
                                                        // 如果P程序中不存在车型码节点，将车型码节点添加到P程序中。
                                                        pProgramNode.addChild(carCodeNode);
                                                    }
                                                }
                                        );
                                    }
                                }
                            }
                    );
                }
        );

        existingNodes.put(id, pProgramNode);
        return pProgramNode;
    }

    public CallNode parseCarCode(int majorIndexOfCar, int minorIndexOfCar) {
        String id = null;
        String value = null;
        NodeType nodeType = NodeType.CAR_CODE;
        CarCode carCode = new CarCode(id, nodeType, majorIndexOfCar, minorIndexOfCar);
        //设置补充信息,车型代码是虚构结点，自然没有对应模块。模块直接传入null，方法会判断并正确处理。
        this.setPropertyAboutFile(carCode, null);
        value = carCode.getValue();
        id = value + ":" + minorIndexOfCar;
        carCode.setId(id);
        if (existingNodes.containsKey(id) && existingNodes.get(id) instanceof CarCode) {
            // 如果存在，直接返回已存在的carCode节点。
            return (CallNode) existingNodes.get(id);
        }

        // 如果不存在，将carCode添加到existingNodes中,再返回。
        existingNodes.put(id, carCode);
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

        // 如果存在，直接返回已存在的carProgramNode节点。
        if (existingNodes.containsKey(id) && existingNodes.get(id) instanceof CallNode) {
            return (CallNode) existingNodes.get(id);
        }

        CallNode carProgramNode = new CallNode(id, nodeValue, nodeType, null);
        //设置结点的补充信息,关于模块文件的。
        this.setPropertyAboutFile(carProgramNode, carProgramModule);

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
                        //设置结点的补充信息,关于模块文件的。
                        this.setPropertyAboutFile(routeProcessNode, moduleRepository.findByCallableName(targetName));
                        String routeNodeRelevantInfo = invocation.findRootNode().getTextContent();
                        routeProcessNode.setRelevantInfo(routeNodeRelevantInfo);

                        if (!carProgramNode.getChildren().contains(routeProcessNode)) {
                            // 如果carProgramNode中不存在routeProcessNode，才添加。
                            carProgramNode.addChild(routeProcessNode);
                        }

                    }

                }
        );

        existingNodes.put(id, carProgramNode);
        return carProgramNode;
    }

    /**
     * 分析备份中的车型调用关系，并返回分析结果。
     *
     * @return 分析结果，包含车型调用关系的 carcallgraph.pojo.tech.waitforu.CallNode 树。
     */
    public CallNode analyze() {
        return analyzeCell();
    }

    private void setPropertyAboutFile(CallNode callNode, KrlModule krlModule) {
        if (callNode instanceof CarCode carCode) {
            //设置补充信息。
            carCode.addProperty("srcFilePath", "虚构结点，不存在物理目录路径"); //文件路径
            carCode.addProperty("createTime", "虚构结点，不存在文件创建时间"); //创建时间
            carCode.addProperty("modifyTime", "虚构结点，不存在文件修改时间"); //修改时间
        } else {
            //设置补充信息。
            callNode.addProperty("srcFilePath", krlModule.getModuleSrcFile().getPath()); //文件路径
            callNode.addProperty("createTime", krlModule.getModuleSrcFile().getCreateTime()); //创建时间
            callNode.addProperty("modifyTime", krlModule.getModuleSrcFile().getModifyTime()); //修改时间
        }
    }


    public static void main(String[] args) {
        YamlConfigLoad yamlConfigLoad = new YamlConfigLoad("/Users/liuke/IdeaProjects/KRLParser/krl-core/src/main/resources/config.yml");
        Config config = null;
        try {
            config = yamlConfigLoad.loadConfig();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        IgnoreRuleByStr fileLoadRule = new IgnoreRuleByStr(config.getFileLoadSection());
        IgnoreRuleByStr invokerParseRule = new IgnoreRuleByStr(config.getCarInvokerParseSection());
        String zipFilePath = "/Users/liuke//Desktop/EC010_L1.zip";
        KrlZipLoader krlZipLoader = new KrlZipLoader(zipFilePath, fileLoadRule);
        List<KrlFile> krlFileList = krlZipLoader.getKrlFileList();
        ModuleRepository moduleRepository = new ModuleRepository();
        moduleRepository.assembleFromFileList(krlFileList);

        CarCallReferenceAnalyze carCallReferenceAnalyze = new CarCallReferenceAnalyze(moduleRepository, invokerParseRule);

        CallNode carReferenceNode = carCallReferenceAnalyze.analyze();

        System.out.println(carReferenceNode.getValue());
    }
}
