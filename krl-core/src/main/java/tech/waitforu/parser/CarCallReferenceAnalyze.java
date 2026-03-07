package tech.waitforu.parser;

import tech.waitforu.exception.KrlParseException;
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
import tech.waitforu.pojo.ast.expression.VariableExpression;
import tech.waitforu.pojo.ast.programunit.ProgramUnit;
import tech.waitforu.pojo.ast.programunit.ProgramUnitType;
import tech.waitforu.pojo.carcallgraph.CallNode;
import tech.waitforu.pojo.carcallgraph.CarCode;
import tech.waitforu.pojo.carcallgraph.NodeType;
import tech.waitforu.pojo.krl.KrlFile;
import tech.waitforu.pojo.krl.KrlModule;
import tech.waitforu.rule.IgnoreRuleByStr;

import java.util.*;

/**
 * 车型调用关系分析器。
 * <p>
 * 从模块仓库中以 cell 程序为入口，逐层解析并构建调用关系树：
 * Cell -> P程序(或虚拟P) -> 车型代码 -> 车型程序 -> 轨迹程序。
 * <p>
 * 同时维护 {@code existingNodes} 去重，保证同一语义节点只创建一次。
 */
public class CarCallReferenceAnalyze {
    /** 待分析模块仓库。 */
    private final ModuleRepository moduleRepository;

    /** 调用过滤规则（true=忽略）。 */
    private final IgnoreRuleByStr invokerParseRule;

    /** 已创建节点缓存，避免重复创建导致图中重复节点。 */
    private final Map<String, CarReferenceNode> existingNodes = new HashMap<>();

    /**
     * 创建分析器。
     *
     * @param moduleRepository 模块仓库
     * @param invokerParseRule 调用过滤规则
     */
    public CarCallReferenceAnalyze(ModuleRepository moduleRepository, IgnoreRuleByStr invokerParseRule) {
        this.moduleRepository = moduleRepository;
        this.invokerParseRule = invokerParseRule;
    }

    /**
     * 解析 cell 节点及其下游调用关系。
     *
     * @return 调用图根节点（cell）
     */
    public CallNode analyzeCell() {
        KrlModule cellModule = moduleRepository.findByModuleName("cell");

        // 解析KRL模块
        ModuleParser moduleParser = new ModuleParser(cellModule);
        AstNode astNode = moduleParser.getSrcAst();

        //判断解析是否正确。如果不是KrlRoot节点(包括为NULL)，抛出异常。同时如果astNode是KrlRoot节点，将其转换为KrlRoot类型。
        if (!(astNode instanceof KrlRoot krlRoot)) {
            throw new KrlParseException("解析出错，" + cellModule.getModuleName() + "模块中不存在KrlRoot节点");
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

        // 1) 从主程序中定位“按 PGNO/GIPGNO 分派程序”的 switch 语句。
        for (SwitchStatement statement : astNodeList) {
            if (statement.getSwitchExpression() instanceof VariableExpression variableExpression) {
                String switchVariableName = variableExpression.getVariableName();
                if (switchVariableName.equalsIgnoreCase("PGNO") || switchVariableName.equalsIgnoreCase("GIPGNO")) {
                    switchStatement = statement;
                    break;
                }
            }
        }
        if (switchStatement == null) {
            throw new KrlParseException("解析出错，" + cellModule.getModuleName() + "cell模块中不存在通过PGNO变量进行判断的的SWITCH语句，请确认备份和修改本程序代码");
        }

        List<CaseBlock> caseBlockList = switchStatement.getCaseBlocks();

        // 2) 遍历每个 case：提取被调用 P 程序，并根据 case 标签映射 major 车型码。
        caseBlockList.forEach(
                caseBlock ->
                {
                    List<String> caseLabel = caseBlock.getCaseLabel();
                    List<Statement> childStatementList = caseBlock.getChildStatement(StatementType.EXPRESSION);
                    childStatementList.forEach(
                            childStatement ->
                            {
                                if (!(childStatement instanceof ExpressionStatement expressionStatement)) {
                                    throw new KrlParseException("解析出错，" + cellModule.getModuleName() + "模块中的CASE块中未找到表达式语句");
                                }
                                Expression expression = expressionStatement.getExpression();
                                if (expression instanceof Invocation invocation) {
                                    String targetName = invocation.getTargetName();
                                    if (!invokerParseRule.isIgnore(targetName)) {
                                        // 一个 case 可能有多个标签（如 case 10,11），每个标签都要生成车型分支。
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

    /**
     * 解析 P 程序节点。
     * <p>
     * 该方法同时覆盖两类场景：
     * 1. 真实 P 程序（包含 GIPGNO2 分派）；
     * 2. cell 直接调用车型程序（无 P 程序，降级为 VIRTUAL）。
     *
     * @param pProgramModule P 程序模块
     * @param callableName 可调用程序名
     * @param majorIndexOfCar 主车型码（来自 cell case）
     * @return P 程序节点
     */
    public CallNode parsePProgram(KrlModule pProgramModule, String callableName, int majorIndexOfCar) {
        ModuleParser moduleParser = new ModuleParser(pProgramModule);
        AstNode astNode = moduleParser.getSrcAst();
        //判断解析是否正确。如果不是KrlRoot节点(包括为NULL)，抛出异常。同时如果astNode是KrlRoot节点，将其转换为KrlRoot类型。
        if (!(astNode instanceof KrlRoot krlRoot)) {
            throw new KrlParseException("解析出错，" + pProgramModule.getModuleName() + "模块中不存在KrlRoot节点");
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
                .findFirst()
                .orElseThrow(() -> new KrlParseException("解析出错，" + pProgramModule.getModuleName()
                        + "模块中未找到程序单元: " + callableName));

        List<VariableExpression> variableExpressionList = callProgramUnit.findNodesByType(VariableExpression.class);
        //用于判断是该程序是P程序，还是直接调用的车型程序！
        boolean isPProgram = false;
        for (VariableExpression variableExpression : variableExpressionList) {
            if (variableExpression.getVariableName().equalsIgnoreCase("GIPGNO2")) {
                isPProgram = true;
                break;
            }
        }
        // 如果没有 GIPGNO2，则说明当前 callable 不是 P 分派程序，而是“直接车型程序调用”。
        if (!isPProgram) {
            //是车型程序
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

        } else {
            // 标准 P 程序流程：在 P 程序内部继续按 GIPGNO2 分派车型程序。
            List<Statement> statementList = callProgramUnit.getStatementList(StatementType.SWITCH);
            SwitchStatement switchStatement = null;
            for (Statement statement : statementList) {
                // 遍历所有SWITCH语句，找到第一个表达式为"GIPGNO2"的SWITCH语句。
                if (((SwitchStatement) statement).getSwitchExpression() instanceof VariableExpression variableExpression) {
                    String switchVariableName = variableExpression.getVariableName();
                    if (switchVariableName.equalsIgnoreCase("GIPGNO2")) {
                        switchStatement = (SwitchStatement) statement;
                        break;
                    }
                }
            }
            // 判断是否存在SWITCH语句，且其匹配表达式为"GIPGNO2"
            if (switchStatement == null) {
                throw new KrlParseException("解析出错，" + pProgramModule.getModuleName() + "模块中未找到用于匹配`GIPGNO2`变量的SWITCH语句");
            }

            List<CaseBlock> caseBlockList = switchStatement.getCaseBlocks();
            // 逐 case 构建“车型代码 -> 车型程序”关系。
            caseBlockList.forEach(
                    caseBlock ->
                    {
                        List<String> caseLabel = caseBlock.getCaseLabel();
                        List<Statement> childStatementList = caseBlock.getChildStatement(StatementType.EXPRESSION);
                        childStatementList.forEach(
                                childStatement ->
                                {
                                    if (!(childStatement instanceof ExpressionStatement expressionStatement)) {
                                        throw new KrlParseException("解析出错，" + pProgramModule.getModuleName() + "模块中的CASE块中未找到表达式语句");
                                    }
                                    Expression expression = expressionStatement.getExpression();
                                    if (expression instanceof Invocation invocation) {
                                        String targetName = invocation.getTargetName();
                                        ProgramUnitType targetType = invocation.getTargetType();

                                        if (!invokerParseRule.isIgnore(targetName)) {
                                            // case 多标签场景，逐标签生成 minor 车型码分支。
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
        }

        existingNodes.put(id, pProgramNode);
        return pProgramNode;
    }

    /**
     * 构建车型代码节点（CAR_CODE）。
     *
     * @param majorIndexOfCar 主车型索引
     * @param minorIndexOfCar 次车型索引
     * @return 车型代码节点（已做去重）
     */
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

    /**
     * 解析车型程序节点及其轨迹调用。
     *
     * @param carProgramModule 车型程序模块
     * @param callableName 车型程序 callable 名称
     * @return 车型程序节点（包含子轨迹节点）
     */
    public CallNode parseCarProgram(KrlModule carProgramModule, String callableName) {

        ModuleParser moduleParser = new ModuleParser(carProgramModule);
        AstNode astNode = moduleParser.getSrcAst();
        //判断解析是否正确。如果不是KrlRoot节点(包括为NULL)，抛出异常。同时如果astNode是KrlRoot节点，将其转换为KrlRoot类型。
        if (!(astNode instanceof KrlRoot krlRoot)) {
            throw new KrlParseException("解析出错，" + carProgramModule.getModuleName() + "模块中不存在KrlRoot节点");
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
                .findFirst()
                .orElseThrow(() -> new KrlParseException("解析出错，" + carProgramModule.getModuleName()
                        + "模块中未找到程序单元: " + callableName));

        List<Invocation> invocationList = callProgramUnit.findNodesByType(Invocation.class);

        // 遍历车型程序中的调用表达式，筛选并生成轨迹节点。
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
                        KrlModule routeModule = moduleRepository.findByCallableName(targetName);
                        if (routeModule == null) {
                            throw new KrlParseException("解析出错，未找到轨迹程序对应模块: " + targetName);
                        }
                        this.setPropertyAboutFile(routeProcessNode, routeModule);
                        // 轨迹相关上下文信息来源于当前调用表达式所在根语句文本。
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
     * @return 调用关系树根节点
     */
    public CallNode analyze() {
        return analyzeCell();
    }

    /**
     * 设置节点对应文件元信息。
     * <p>
     * CAR_CODE 属于逻辑节点，不对应真实文件，写入虚构信息；
     * 其他节点写入 src 文件路径与时间信息。
     *
     * @param callNode 目标节点
     * @param krlModule 节点对应模块（CAR_CODE 可为空）
     */
    private void setPropertyAboutFile(CallNode callNode, KrlModule krlModule) {
        if (callNode instanceof CarCode carCode) {
            //设置补充信息。
            carCode.addProperty("srcFilePath", "虚构结点，不存在物理目录路径"); //文件路径
            carCode.addProperty("createTime", "虚构结点，不存在文件创建时间"); //创建时间
            carCode.addProperty("modifyTime", "虚构结点，不存在文件修改时间"); //修改时间
        } else {
            if (krlModule == null || krlModule.getModuleSrcFile() == null) {
                throw new KrlParseException("解析出错，节点缺少对应的模块文件信息: " + callNode.getValue());
            }
            //设置补充信息。
            callNode.addProperty("srcFilePath", krlModule.getModuleSrcFile().getPath()); //文件路径
            callNode.addProperty("createTime", krlModule.getModuleSrcFile().getCreateTime()); //创建时间
            callNode.addProperty("modifyTime", krlModule.getModuleSrcFile().getModifyTime()); //修改时间
        }
    }
}
