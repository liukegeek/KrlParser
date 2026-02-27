package tech.waitforu.service;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import tech.waitforu.pojo.carcallgraph.CallNode;
import tech.waitforu.pojo.carcallgraph.CarReferenceNode;
import tech.waitforu.pojo.carcallgraph.NodeType;
import tech.waitforu.pojo.krl.RobotInfo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * 调用关系 Excel 导出服务。
 * <p>
 * 导出文件规则：
 * 1. 每个机器人一个 Sheet。
 * 2. 上半区为树形调用关系（Cell -> P/VIRTUAL -> 车型代码 -> 车型程序 -> 轨迹程序）。
 * 3. 下半区为调用矩阵（行=调用方，列=被调用方）。
 */
public class CallGraphExcelExportService {
    private static final String TREE_TITLE = "车型调用关系图(树形结构)";
    private static final String RELATION_TITLE = "调用关系表(横向代表调用关系，竖向代表被调用关系)";

    private static final int TREE_TITLE_ROW = 0;
    private static final int TREE_HEADER_ROW = 1;
    private static final int TREE_DATA_START_ROW = 2;

    private static final List<String> TREE_HEADERS = List.of("Cell程序", "P程序", "车型代码", "车型程序", "轨迹程序");

    /**
     * 树形区固定 5 列层级。
     * VIRTUAL 在树形区归属于 P 程序列，所以不单独占列。
     */
    private static final List<NodeType> TREE_LEVEL_ORDER = List.of(
            NodeType.CEll,
            NodeType.P_PROGRAM,
            NodeType.CAR_CODE,
            NodeType.CAR_PROGRAM,
            NodeType.ROUTE_PROCESS
    );

    /**
     * 调用矩阵排序层级。
     * VIRTUAL 需要单独可见并且排在 P_PROGRAM 后面。
     */
    private static final List<NodeType> MATRIX_LEVEL_ORDER = List.of(
            NodeType.CEll,
            NodeType.P_PROGRAM,
            NodeType.VIRTUAL,
            NodeType.CAR_CODE,
            NodeType.CAR_PROGRAM,
            NodeType.ROUTE_PROCESS
    );

    /**
     * 导出 Excel（二进制）。
     *
     * @param robotInfoList 机器人信息列表
     * @return xlsx 文件字节数组
     * @throws IOException 写入工作簿失败时抛出
     */
    public byte[] export(List<RobotInfo> robotInfoList) throws IOException {
        if (robotInfoList == null || robotInfoList.isEmpty()) {
            throw new IllegalArgumentException("导出失败：机器人列表为空");
        }

        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            WorkbookStyles styles = new WorkbookStyles(workbook);
            Set<String> usedSheetNames = new LinkedHashSet<>();

            for (int i = 0; i < robotInfoList.size(); i++) {
                RobotInfo robotInfo = robotInfoList.get(i);
                String sheetName = buildSheetName(robotInfo, i, usedSheetNames);
                Sheet sheet = workbook.createSheet(sheetName);
                renderRobotSheet(sheet, robotInfo, styles);
            }

            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    /**
     * 渲染单个机器人对应的 Sheet。
     *
     * @param sheet 目标工作表
     * @param robotInfo 机器人信息
     * @param styles 复用样式集合
     */
    private void renderRobotSheet(Sheet sheet, RobotInfo robotInfo, WorkbookStyles styles) {
        ExportModel exportModel = buildExportModel(robotInfo);
        List<TreeRow> treeRows = exportModel.treeRows();

        int treeRowCount = Math.max(treeRows.size(), 1);
        int treeDataEndRow = TREE_DATA_START_ROW + treeRowCount - 1;

        sheet.setDefaultColumnWidth(14);

        writeTreeHeader(sheet, styles);
        writeTreeRows(sheet, styles, treeRows, treeRowCount);
        mergeTreeColumns(sheet, TREE_DATA_START_ROW, treeDataEndRow);

        int relationTitleRow = treeDataEndRow + 3;
        int relationHeaderRow = relationTitleRow + 1;
        int matrixStartRow = relationHeaderRow + 1;

        List<NodeRef> orderedNodes = exportModel.orderedNodes();
        int matrixSize = Math.max(orderedNodes.size(), 1);
        int matrixEndRow = matrixStartRow + matrixSize - 1;

        writeRelationHeader(sheet, styles, relationTitleRow);
        writeRelationMatrix(sheet, styles, orderedNodes, exportModel.directEdges(), matrixStartRow);
        adjustColumnWidths(sheet, orderedNodes);

        for (int rowIndex = TREE_TITLE_ROW; rowIndex <= matrixEndRow + 1; rowIndex++) {
            Row row = getOrCreateRow(sheet, rowIndex);
            if (row.getHeightInPoints() < 19f) {
                row.setHeightInPoints(19f);
            }
        }
    }

    /**
     * 写入树形区标题与列表头。
     *
     * @param sheet 目标工作表
     * @param styles 样式集合
     */
    private void writeTreeHeader(Sheet sheet, WorkbookStyles styles) {
        Row titleRow = getOrCreateRow(sheet, TREE_TITLE_ROW);
        for (int col = 0; col < TREE_LEVEL_ORDER.size(); col++) {
            Cell cell = getOrCreateCell(titleRow, col);
            cell.setCellStyle(styles.treeTitleStyle());
        }
        getOrCreateCell(titleRow, 0).setCellValue(TREE_TITLE);
        sheet.addMergedRegion(new CellRangeAddress(TREE_TITLE_ROW, TREE_TITLE_ROW, 0, TREE_LEVEL_ORDER.size() - 1));

        Row headerRow = getOrCreateRow(sheet, TREE_HEADER_ROW);
        for (int col = 0; col < TREE_HEADERS.size(); col++) {
            Cell cell = getOrCreateCell(headerRow, col);
            cell.setCellStyle(styles.treeHeaderStyle());
            cell.setCellValue(TREE_HEADERS.get(col));
        }
    }

    /**
     * 写入树形区数据行。
     *
     * @param sheet 目标工作表
     * @param styles 样式集合
     * @param treeRows 树行数据
     * @param treeRowCount 输出行数
     */
    private void writeTreeRows(Sheet sheet, WorkbookStyles styles, List<TreeRow> treeRows, int treeRowCount) {
        for (int index = 0; index < treeRowCount; index++) {
            Row row = getOrCreateRow(sheet, TREE_DATA_START_ROW + index);
            TreeRow treeRow = index < treeRows.size() ? treeRows.get(index) : TreeRow.empty();

            for (int col = 0; col < TREE_LEVEL_ORDER.size(); col++) {
                Cell cell = getOrCreateCell(row, col);
                NodeRef nodeRef = treeRow.nodeAt(col);

                NodeType styleType = nodeRef != null ? nodeRef.displayType() : TREE_LEVEL_ORDER.get(col);
                cell.setCellStyle(styles.treeNodeStyle(styleType));

                if (nodeRef != null) {
                    cell.setCellValue(nodeRef.value());
                } else {
                    cell.setBlank();
                }
            }
        }
    }

    /**
     * 对树形区按列执行纵向合并。
     *
     * @param sheet 工作表
     * @param startRow 起始行
     * @param endRow 结束行
     */
    private void mergeTreeColumns(Sheet sheet, int startRow, int endRow) {
        for (int col = 0; col < TREE_LEVEL_ORDER.size(); col++) {
            int mergeStart = startRow;
            String previousValue = readCellText(sheet, startRow, col);

            for (int row = startRow + 1; row <= endRow; row++) {
                String currentValue = readCellText(sheet, row, col);
                if (Objects.equals(previousValue, currentValue)) {
                    continue;
                }

                mergeRangeIfNeeded(sheet, mergeStart, row - 1, col, previousValue);
                mergeStart = row;
                previousValue = currentValue;
            }
            mergeRangeIfNeeded(sheet, mergeStart, endRow, col, previousValue);
        }
    }

    /**
     * 条件合并单元格区域。
     *
     * @param sheet 工作表
     * @param startRow 起始行
     * @param endRow 结束行
     * @param col 列索引
     * @param value 该段文本值
     */
    private void mergeRangeIfNeeded(Sheet sheet, int startRow, int endRow, int col, String value) {
        if (startRow >= endRow) {
            return;
        }
        if (value == null || value.isBlank()) {
            return;
        }
        sheet.addMergedRegion(new CellRangeAddress(startRow, endRow, col, col));
    }

    /**
     * 写入调用矩阵标题。
     *
     * @param sheet 工作表
     * @param styles 样式集合
     * @param relationTitleRow 标题行号
     */
    private void writeRelationHeader(Sheet sheet, WorkbookStyles styles, int relationTitleRow) {
        Row titleRow = getOrCreateRow(sheet, relationTitleRow);
        for (int col = 0; col < TREE_LEVEL_ORDER.size(); col++) {
            Cell cell = getOrCreateCell(titleRow, col);
            cell.setCellStyle(styles.relationTitleStyle());
        }
        getOrCreateCell(titleRow, 0).setCellValue(RELATION_TITLE);
        sheet.addMergedRegion(new CellRangeAddress(relationTitleRow, relationTitleRow, 0, TREE_LEVEL_ORDER.size() - 1));

        Row headerRow = getOrCreateRow(sheet, relationTitleRow + 1);
        Cell corner = getOrCreateCell(headerRow, 0);
        corner.setCellStyle(styles.matrixCornerStyle());
        corner.setBlank();
    }

    /**
     * 写入调用矩阵主体。
     *
     * @param sheet 工作表
     * @param styles 样式集合
     * @param orderedNodes 节点排序
     * @param directEdges 直接调用边
     * @param matrixStartRow 矩阵起始行
     */
    private void writeRelationMatrix(
            Sheet sheet,
            WorkbookStyles styles,
            List<NodeRef> orderedNodes,
            Set<DirectEdge> directEdges,
            int matrixStartRow
    ) {
        if (orderedNodes.isEmpty()) {
            Row row = getOrCreateRow(sheet, matrixStartRow);
            Cell cell = getOrCreateCell(row, 0);
            cell.setCellStyle(styles.matrixBlankStyle());
            cell.setCellValue("--");
            return;
        }

        int relationHeaderRow = matrixStartRow - 1;

        Row headerRow = getOrCreateRow(sheet, relationHeaderRow);
        for (int index = 0; index < orderedNodes.size(); index++) {
            NodeRef callee = orderedNodes.get(index);
            Cell cell = getOrCreateCell(headerRow, index + 1);
            cell.setCellStyle(styles.matrixColumnHeaderStyle(callee.displayType()));
            cell.setCellValue(callee.value());
        }

        for (int rowOffset = 0; rowOffset < orderedNodes.size(); rowOffset++) {
            NodeRef caller = orderedNodes.get(rowOffset);
            Row row = getOrCreateRow(sheet, matrixStartRow + rowOffset);

            Cell rowHeaderCell = getOrCreateCell(row, 0);
            rowHeaderCell.setCellStyle(styles.matrixRowHeaderStyle(caller.displayType()));
            rowHeaderCell.setCellValue(caller.value());

            for (int colOffset = 0; colOffset < orderedNodes.size(); colOffset++) {
                Cell bodyCell = getOrCreateCell(row, colOffset + 1);
                bodyCell.setCellStyle(styles.matrixBlankStyle());
                bodyCell.setBlank();
            }
        }

        Map<String, Integer> nodeIndexMap = new HashMap<>();
        for (int index = 0; index < orderedNodes.size(); index++) {
            nodeIndexMap.put(orderedNodes.get(index).uniqueKey(), index);
        }

        Map<Integer, Integer> firstDirectCallerRowByColumn = new HashMap<>();
        for (DirectEdge edge : directEdges) {
            Integer callerIndex = nodeIndexMap.get(edge.from().uniqueKey());
            Integer calleeIndex = nodeIndexMap.get(edge.to().uniqueKey());
            if (callerIndex == null || calleeIndex == null) {
                continue;
            }

            int rowIndex = matrixStartRow + callerIndex;
            int colIndex = 1 + calleeIndex;

            Row row = getOrCreateRow(sheet, rowIndex);
            Cell cell = getOrCreateCell(row, colIndex);
            cell.setCellStyle(styles.matrixDirectStyle(edge.from().displayType()));
            cell.setCellValue(edge.from().value());

            firstDirectCallerRowByColumn.merge(colIndex, rowIndex, Math::min);
        }

        for (Map.Entry<Integer, Integer> entry : firstDirectCallerRowByColumn.entrySet()) {
            int colIndex = entry.getKey();
            int firstRow = entry.getValue();
            for (int rowIndex = matrixStartRow; rowIndex < firstRow; rowIndex++) {
                Row row = getOrCreateRow(sheet, rowIndex);
                Cell cell = getOrCreateCell(row, colIndex);
                if (cell.getCellType() == CellType.STRING && !cell.getStringCellValue().isBlank()) {
                    continue;
                }
                cell.setCellStyle(styles.matrixArrowStyle());
                cell.setCellValue("↑");
            }
        }
    }

    /**
     * 调整列宽。
     *
     * @param sheet 工作表
     * @param orderedNodes 矩阵节点列表
     */
    private void adjustColumnWidths(Sheet sheet, List<NodeRef> orderedNodes) {
        for (int col = 0; col < TREE_LEVEL_ORDER.size(); col++) {
            sheet.setColumnWidth(col, 14 * 256);
        }

        for (int index = 0; index < orderedNodes.size(); index++) {
            NodeRef nodeRef = orderedNodes.get(index);
            int col = index + 1;
            int width = Math.max(14, Math.min(28, nodeRef.value().length() + 4));
            sheet.setColumnWidth(col, width * 256);
        }
    }

    /**
     * 构建导出中间模型：
     * 1. 树形区行数据。
     * 2. 矩阵节点顺序。
     * 3. 直接调用边。
     *
     * @param robotInfo 机器人信息
     * @return 导出中间模型
     */
    private ExportModel buildExportModel(RobotInfo robotInfo) {
        CallNode root = robotInfo != null ? robotInfo.getCallGraphRoot() : null;
        if (root == null) {
            return ExportModel.empty();
        }

        GraphCollector collector = new GraphCollector();
        collectGraph(root, collector, new LinkedHashSet<>());

        List<TreeRow> treeRows = buildTreeRows(collector);
        if (treeRows.isEmpty()) {
            treeRows.add(TreeRow.empty());
        }

        return new ExportModel(treeRows, collector.orderedMatrixNodes(), collector.directEdges());
    }

    /**
     * 深度优先收集节点与边。
     *
     * @param node 当前节点
     * @param collector 收集器
     * @param recursionPath 当前递归路径（用于防环）
     */
    private void collectGraph(CallNode node, GraphCollector collector, Set<String> recursionPath) {
        if (node == null) {
            return;
        }

        NodeRef current = toNodeRef(node);
        if (current == null) {
            return;
        }

        collector.registerNode(current);

        if (!recursionPath.add(current.uniqueKey())) {
            return;
        }

        for (CallNode child : getCallChildren(node)) {
            NodeRef childRef = toNodeRef(child);
            if (childRef == null) {
                continue;
            }

            collector.registerNode(childRef);
            collector.registerDirectEdge(current, childRef);
            collector.registerTreeEdge(current, childRef);
            collectGraph(child, collector, recursionPath);
        }

        recursionPath.remove(current.uniqueKey());
    }

    /**
     * 构建树形区行数据。
     * <p>
     * 构建策略：
     * 1. 以 CAR_PROGRAM -> ROUTE_PROCESS 唯一边作为基础行。
     * 2. 再依次回填 CAR_CODE、P/VIRTUAL、Cell 列。
     * 3. 父子去重按父ID+子ID（缺失 ID 时回退 type+value）进行。
     *
     * @param collector 图收集器
     * @return 树形行列表
     */
    private List<TreeRow> buildTreeRows(GraphCollector collector) {
        List<TreeRow> rows = new ArrayList<>();

        List<NodeRef> orderedCarPrograms = collector.orderedTreeNodes(NodeType.CAR_PROGRAM);
        for (NodeRef carProgram : orderedCarPrograms) {
            List<NodeRef> routes = collector.childrenOf(collector.routeByProgramMap(), carProgram);
            if (routes.isEmpty()) {
                TreeRow row = TreeRow.empty();
                row.setCarProgram(carProgram);
                rows.add(row);
            } else {
                for (NodeRef route : routes) {
                    TreeRow row = TreeRow.empty();
                    row.setCarProgram(carProgram);
                    row.setRouteProgram(route);
                    rows.add(row);
                }
            }
        }

        if (rows.isEmpty()) {
            List<NodeRef> orderedCarCodes = collector.orderedTreeNodes(NodeType.CAR_CODE);
            for (NodeRef carCode : orderedCarCodes) {
                TreeRow row = TreeRow.empty();
                row.setCarCode(carCode);
                rows.add(row);
            }
        }

        if (rows.isEmpty()) {
            List<NodeRef> orderedPNodes = collector.orderedTreeNodes(NodeType.P_PROGRAM);
            for (NodeRef pNode : orderedPNodes) {
                TreeRow row = TreeRow.empty();
                row.setPProgram(pNode);
                rows.add(row);
            }
        }

        if (rows.isEmpty()) {
            List<NodeRef> orderedCells = collector.orderedTreeNodes(NodeType.CEll);
            for (NodeRef cellNode : orderedCells) {
                TreeRow row = TreeRow.empty();
                row.setCell(cellNode);
                rows.add(row);
            }
        }

        if (rows.isEmpty()) {
            return rows;
        }

        Map<String, List<NodeRef>> parentsByCarProgram = collector.parentsByChild(collector.programByCarCodeMap());
        attachParentValues(
                rows,
                orderedCarPrograms,
                TreeRow::getCarProgram,
                TreeRow::setCarProgram,
                TreeRow::setCarCode,
                parentsByCarProgram
        );

        List<NodeRef> orderedCarCodes = collector.orderedTreeNodes(NodeType.CAR_CODE);
        Map<String, List<NodeRef>> parentsByCarCode = collector.parentsByChild(collector.carCodeByPMap());
        attachParentValues(
                rows,
                orderedCarCodes,
                TreeRow::getCarCode,
                TreeRow::setCarCode,
                TreeRow::setPProgram,
                parentsByCarCode
        );

        List<NodeRef> orderedPNodes = collector.orderedTreeNodes(NodeType.P_PROGRAM);
        Map<String, List<NodeRef>> parentsByP = collector.parentsByChild(collector.pByCellMap());
        attachParentValues(
                rows,
                orderedPNodes,
                TreeRow::getPProgram,
                TreeRow::setPProgram,
                TreeRow::setCell,
                parentsByP
        );

        return rows;
    }

    /**
     * 将父节点按比例分配到已有子节点行。
     * <p>
     * 分配规则：
     * 1. 若同一子节点只有一个父节点，则父节点填满该子节点的全部行。
     * 2. 若父节点数量超过行数，则插入额外承载行（不复制右侧子节点）。
     * 3. 若父节点数量小于行数，则按顺序比例分配，保持块状连续。
     *
     * @param rows 目标行列表
     * @param orderedChildren 子节点顺序
     * @param childGetter 读取子节点
     * @param childSetter 写入子节点
     * @param parentSetter 写入父节点
     * @param parentsByChild 子->父映射
     */
    private void attachParentValues(
            List<TreeRow> rows,
            List<NodeRef> orderedChildren,
            Function<TreeRow, NodeRef> childGetter,
            BiConsumer<TreeRow, NodeRef> childSetter,
            BiConsumer<TreeRow, NodeRef> parentSetter,
            Map<String, List<NodeRef>> parentsByChild
    ) {
        for (NodeRef child : orderedChildren) {
            List<Integer> rowIndexes = findRowsByChild(rows, childGetter, child.uniqueKey());
            if (rowIndexes.isEmpty()) {
                TreeRow extra = TreeRow.empty();
                childSetter.accept(extra, child);
                rows.add(extra);
                rowIndexes = findRowsByChild(rows, childGetter, child.uniqueKey());
            }

            List<NodeRef> parents = parentsByChild.getOrDefault(child.uniqueKey(), List.of());
            if (parents.isEmpty()) {
                continue;
            }

            if (parents.size() > rowIndexes.size()) {
                int appendCount = parents.size() - rowIndexes.size();
                int insertAt = rowIndexes.get(rowIndexes.size() - 1) + 1;
                for (int i = 0; i < appendCount; i++) {
                    TreeRow extra = TreeRow.empty();
                    childSetter.accept(extra, child);
                    rows.add(insertAt + i, extra);
                }
                rowIndexes = findRowsByChild(rows, childGetter, child.uniqueKey());
            }

            if (parents.size() == 1) {
                NodeRef singleParent = parents.get(0);
                for (Integer rowIndex : rowIndexes) {
                    parentSetter.accept(rows.get(rowIndex), singleParent);
                }
                continue;
            }

            int rowCount = rowIndexes.size();
            int parentCount = parents.size();
            for (int localIndex = 0; localIndex < rowCount; localIndex++) {
                int parentIndex = Math.min((int) ((long) localIndex * parentCount / rowCount), parentCount - 1);
                parentSetter.accept(rows.get(rowIndexes.get(localIndex)), parents.get(parentIndex));
            }
        }
    }

    /**
     * 按子节点唯一键查找所在行。
     *
     * @param rows 行列表
     * @param childGetter 子节点读取器
     * @param childKey 子节点唯一键
     * @return 匹配行索引集合
     */
    private List<Integer> findRowsByChild(
            List<TreeRow> rows,
            Function<TreeRow, NodeRef> childGetter,
            String childKey
    ) {
        List<Integer> rowIndexes = new ArrayList<>();
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            TreeRow row = rows.get(rowIndex);
            NodeRef rowChild = childGetter.apply(row);
            if (rowChild == null) {
                continue;
            }
            if (Objects.equals(rowChild.uniqueKey(), childKey)) {
                rowIndexes.add(rowIndex);
            }
        }
        return rowIndexes;
    }

    /**
     * 将 CallNode 转为导出节点引用。
     *
     * @param node 原始调用节点
     * @return 导出节点引用；无法构建时返回 null
     */
    private NodeRef toNodeRef(CallNode node) {
        if (node == null) {
            return null;
        }

        NodeType displayType = node.getNodeType();
        if (displayType == null) {
            return null;
        }

        NodeType treeLevelType = toTreeLevelType(displayType);
        String value = safeText(node.getValue());
        if (value.isBlank()) {
            value = safeText(node.getId());
        }
        if (value.isBlank()) {
            return null;
        }

        String id = safeText(node.getId());
        String uniqueKey = !id.isBlank()
                ? id.toLowerCase(Locale.ROOT)
                : (displayType + "::" + value.toLowerCase(Locale.ROOT));

        return new NodeRef(uniqueKey, id, value, displayType, treeLevelType);
    }

    /**
     * 将显示类型映射为树形层级类型。
     *
     * @param displayType 显示类型
     * @return 树形层级类型
     */
    private NodeType toTreeLevelType(NodeType displayType) {
        if (displayType == null) {
            return null;
        }
        if (displayType == NodeType.VIRTUAL) {
            return NodeType.P_PROGRAM;
        }
        return TREE_LEVEL_ORDER.contains(displayType) ? displayType : null;
    }

    /**
     * 读取调用节点的 CallNode 类型子节点。
     *
     * @param node 当前节点
     * @return 子节点列表
     */
    private List<CallNode> getCallChildren(CallNode node) {
        if (node == null || node.getChildren() == null || node.getChildren().isEmpty()) {
            return Collections.emptyList();
        }

        List<CallNode> children = new ArrayList<>();
        for (CarReferenceNode child : node.getChildren()) {
            if (child instanceof CallNode callNode) {
                children.add(callNode);
            }
        }
        return children;
    }

    /**
     * 构建合法且唯一的 Sheet 名称。
     *
     * @param robotInfo 机器人信息
     * @param index 索引
     * @param usedSheetNames 已占用名称集合
     * @return Sheet 名称
     */
    private String buildSheetName(RobotInfo robotInfo, int index, Set<String> usedSheetNames) {
        String rawName = robotInfo != null ? safeText(robotInfo.getRobotName()) : "";
        if (rawName.isBlank()) {
            rawName = "Robot_" + (index + 1);
        }

        String safeBaseName = WorkbookUtil.createSafeSheetName(rawName);
        if (safeBaseName == null || safeBaseName.isBlank()) {
            safeBaseName = "Robot_" + (index + 1);
        }

        safeBaseName = truncate(safeBaseName, 31);
        String candidate = safeBaseName;
        int suffix = 2;
        while (usedSheetNames.contains(candidate)) {
            String suffixText = "_" + suffix;
            candidate = truncate(safeBaseName, 31 - suffixText.length()) + suffixText;
            suffix++;
        }

        usedSheetNames.add(candidate);
        return candidate;
    }

    /**
     * 文本截断。
     *
     * @param text 文本
     * @param maxLength 最大长度
     * @return 截断后的文本
     */
    private String truncate(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, Math.max(0, maxLength));
    }

    /**
     * 安全文本化。
     *
     * @param value 原文本
     * @return 去空白后的文本
     */
    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }

    /**
     * 读取单元格文本内容。
     *
     * @param sheet 工作表
     * @param rowIndex 行索引
     * @param colIndex 列索引
     * @return 文本值
     */
    private String readCellText(Sheet sheet, int rowIndex, int colIndex) {
        Row row = sheet.getRow(rowIndex);
        if (row == null) {
            return "";
        }

        Cell cell = row.getCell(colIndex);
        if (cell == null) {
            return "";
        }

        return switch (cell.getCellType()) {
            case STRING -> safeText(cell.getStringCellValue());
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            default -> "";
        };
    }

    /**
     * 获取或创建行。
     *
     * @param sheet 工作表
     * @param rowIndex 行索引
     * @return 行对象
     */
    private Row getOrCreateRow(Sheet sheet, int rowIndex) {
        Row row = sheet.getRow(rowIndex);
        return row != null ? row : sheet.createRow(rowIndex);
    }

    /**
     * 获取或创建单元格。
     *
     * @param row 行对象
     * @param colIndex 列索引
     * @return 单元格对象
     */
    private Cell getOrCreateCell(Row row, int colIndex) {
        Cell cell = row.getCell(colIndex);
        return cell != null ? cell : row.createCell(colIndex);
    }

    /**
     * 导出中间模型。
     *
     * @param treeRows 树形区行
     * @param orderedNodes 矩阵节点顺序
     * @param directEdges 直接调用边
     */
    private record ExportModel(List<TreeRow> treeRows, List<NodeRef> orderedNodes, Set<DirectEdge> directEdges) {
        private static ExportModel empty() {
            return new ExportModel(List.of(), List.of(), Set.of());
        }
    }

    /**
     * 树形区单行。
     */
    private static final class TreeRow {
        private NodeRef cell;
        private NodeRef pProgram;
        private NodeRef carCode;
        private NodeRef carProgram;
        private NodeRef routeProgram;

        private static TreeRow empty() {
            return new TreeRow();
        }

        private NodeRef nodeAt(int colIndex) {
            return switch (colIndex) {
                case 0 -> cell;
                case 1 -> pProgram;
                case 2 -> carCode;
                case 3 -> carProgram;
                case 4 -> routeProgram;
                default -> null;
            };
        }

        private NodeRef getCell() {
            return cell;
        }

        private void setCell(NodeRef cell) {
            this.cell = cell;
        }

        private NodeRef getPProgram() {
            return pProgram;
        }

        private void setPProgram(NodeRef pProgram) {
            this.pProgram = pProgram;
        }

        private NodeRef getCarCode() {
            return carCode;
        }

        private void setCarCode(NodeRef carCode) {
            this.carCode = carCode;
        }

        private NodeRef getCarProgram() {
            return carProgram;
        }

        private void setCarProgram(NodeRef carProgram) {
            this.carProgram = carProgram;
        }

        private NodeRef getRouteProgram() {
            return routeProgram;
        }

        private void setRouteProgram(NodeRef routeProgram) {
            this.routeProgram = routeProgram;
        }
    }

    /**
     * 导出节点引用。
     *
     * @param uniqueKey 去重唯一键（优先使用 ID）
     * @param id 原始 ID
     * @param value 显示文本
     * @param displayType 显示类型
     * @param treeLevelType 树形层级类型
     */
    private record NodeRef(
            String uniqueKey,
            String id,
            String value,
            NodeType displayType,
            NodeType treeLevelType
    ) {
    }

    /**
     * 直接调用边。
     *
     * @param from 调用方
     * @param to 被调用方
     */
    private record DirectEdge(NodeRef from, NodeRef to) {
    }

    /**
     * 调用图收集器。
     */
    private static final class GraphCollector {
        private final Map<String, NodeRef> nodesByKey = new LinkedHashMap<>();

        /** Cell -> P/VIRTUAL */
        private final Map<String, LinkedHashSet<String>> pByCell = new LinkedHashMap<>();
        /** P/VIRTUAL -> CarCode */
        private final Map<String, LinkedHashSet<String>> carCodeByP = new LinkedHashMap<>();
        /** CarCode -> CarProgram */
        private final Map<String, LinkedHashSet<String>> programByCarCode = new LinkedHashMap<>();
        /** CarProgram -> Route */
        private final Map<String, LinkedHashSet<String>> routeByProgram = new LinkedHashMap<>();

        private final Set<DirectEdge> directEdges = new LinkedHashSet<>();

        private final Map<NodeType, LinkedHashMap<String, NodeRef>> orderedTreeNodesByLevel = new EnumMap<>(NodeType.class);
        private final Map<NodeType, LinkedHashMap<String, NodeRef>> orderedMatrixNodesByType = new EnumMap<>(NodeType.class);

        private GraphCollector() {
            for (NodeType levelType : TREE_LEVEL_ORDER) {
                orderedTreeNodesByLevel.put(levelType, new LinkedHashMap<>());
            }
            for (NodeType displayType : MATRIX_LEVEL_ORDER) {
                orderedMatrixNodesByType.put(displayType, new LinkedHashMap<>());
            }
        }

        /**
         * 注册节点并返回规范化节点对象。
         *
         * @param nodeRef 待注册节点
         * @return 规范化节点对象
         */
        private NodeRef registerNode(NodeRef nodeRef) {
            NodeRef existing = nodesByKey.get(nodeRef.uniqueKey());
            if (existing != null) {
                return existing;
            }

            nodesByKey.put(nodeRef.uniqueKey(), nodeRef);

            if (nodeRef.treeLevelType() != null) {
                orderedTreeNodesByLevel.get(nodeRef.treeLevelType()).putIfAbsent(nodeRef.uniqueKey(), nodeRef);
            }
            if (orderedMatrixNodesByType.containsKey(nodeRef.displayType())) {
                orderedMatrixNodesByType.get(nodeRef.displayType()).putIfAbsent(nodeRef.uniqueKey(), nodeRef);
            }

            return nodeRef;
        }

        /**
         * 注册直接调用边。
         *
         * @param from 调用方
         * @param to 被调用方
         */
        private void registerDirectEdge(NodeRef from, NodeRef to) {
            NodeRef canonicalFrom = registerNode(from);
            NodeRef canonicalTo = registerNode(to);
            if (canonicalFrom.uniqueKey().equals(canonicalTo.uniqueKey())) {
                return;
            }
            directEdges.add(new DirectEdge(canonicalFrom, canonicalTo));
        }

        /**
         * 注册树形层级边。
         *
         * @param from 父节点
         * @param to 子节点
         */
        private void registerTreeEdge(NodeRef from, NodeRef to) {
            NodeRef canonicalFrom = registerNode(from);
            NodeRef canonicalTo = registerNode(to);

            NodeType fromLevel = canonicalFrom.treeLevelType();
            NodeType toLevel = canonicalTo.treeLevelType();
            if (fromLevel == null || toLevel == null) {
                return;
            }

            if (fromLevel == NodeType.CEll && toLevel == NodeType.P_PROGRAM) {
                putEdge(pByCell, canonicalFrom.uniqueKey(), canonicalTo.uniqueKey());
                return;
            }
            if (fromLevel == NodeType.P_PROGRAM && toLevel == NodeType.CAR_CODE) {
                putEdge(carCodeByP, canonicalFrom.uniqueKey(), canonicalTo.uniqueKey());
                return;
            }
            if (fromLevel == NodeType.CAR_CODE && toLevel == NodeType.CAR_PROGRAM) {
                putEdge(programByCarCode, canonicalFrom.uniqueKey(), canonicalTo.uniqueKey());
                return;
            }
            if (fromLevel == NodeType.CAR_PROGRAM && toLevel == NodeType.ROUTE_PROCESS) {
                putEdge(routeByProgram, canonicalFrom.uniqueKey(), canonicalTo.uniqueKey());
            }
        }

        /**
         * 添加唯一边。
         *
         * @param edgeMap 边映射
         * @param fromKey 父节点键
         * @param toKey 子节点键
         */
        private void putEdge(Map<String, LinkedHashSet<String>> edgeMap, String fromKey, String toKey) {
            edgeMap.computeIfAbsent(fromKey, key -> new LinkedHashSet<>()).add(toKey);
        }

        /**
         * 获取某层级有序节点列表。
         *
         * @param levelType 树形层级类型
         * @return 有序节点列表
         */
        private List<NodeRef> orderedTreeNodes(NodeType levelType) {
            LinkedHashMap<String, NodeRef> nodeMap = orderedTreeNodesByLevel.get(levelType);
            if (nodeMap == null || nodeMap.isEmpty()) {
                return List.of();
            }
            return new ArrayList<>(nodeMap.values());
        }

        /**
         * 构建矩阵节点排序列表。
         *
         * @return 矩阵节点列表
         */
        private List<NodeRef> orderedMatrixNodes() {
            List<NodeRef> ordered = new ArrayList<>();
            for (NodeType displayType : MATRIX_LEVEL_ORDER) {
                LinkedHashMap<String, NodeRef> nodeMap = orderedMatrixNodesByType.get(displayType);
                if (nodeMap == null || nodeMap.isEmpty()) {
                    continue;
                }
                ordered.addAll(nodeMap.values());
            }
            return ordered;
        }

        /**
         * 从 parent->children 边表获取某 parent 的子节点列表。
         *
         * @param edgeMap 边映射
         * @param parent 父节点
         * @return 子节点列表
         */
        private List<NodeRef> childrenOf(Map<String, LinkedHashSet<String>> edgeMap, NodeRef parent) {
            LinkedHashSet<String> childKeys = edgeMap.get(parent.uniqueKey());
            if (childKeys == null || childKeys.isEmpty()) {
                return List.of();
            }

            List<NodeRef> children = new ArrayList<>(childKeys.size());
            for (String childKey : childKeys) {
                NodeRef child = nodesByKey.get(childKey);
                if (child != null) {
                    children.add(child);
                }
            }
            return children;
        }

        /**
         * 将 parent->children 映射反转为 child->parents。
         *
         * @param parentToChildren 父到子的边映射
         * @return 子到父的有序映射
         */
        private Map<String, List<NodeRef>> parentsByChild(Map<String, LinkedHashSet<String>> parentToChildren) {
            Map<String, LinkedHashMap<String, NodeRef>> childParents = new LinkedHashMap<>();

            for (Map.Entry<String, LinkedHashSet<String>> entry : parentToChildren.entrySet()) {
                NodeRef parent = nodesByKey.get(entry.getKey());
                if (parent == null) {
                    continue;
                }

                for (String childKey : entry.getValue()) {
                    childParents
                            .computeIfAbsent(childKey, key -> new LinkedHashMap<>())
                            .putIfAbsent(parent.uniqueKey(), parent);
                }
            }

            Map<String, List<NodeRef>> result = new LinkedHashMap<>();
            for (Map.Entry<String, LinkedHashMap<String, NodeRef>> entry : childParents.entrySet()) {
                result.put(entry.getKey(), new ArrayList<>(entry.getValue().values()));
            }
            return result;
        }

        /** @return Cell->P/VIRTUAL 边映射 */
        private Map<String, LinkedHashSet<String>> pByCellMap() {
            return pByCell;
        }

        /** @return P/VIRTUAL->CarCode 边映射 */
        private Map<String, LinkedHashSet<String>> carCodeByPMap() {
            return carCodeByP;
        }

        /** @return CarCode->CarProgram 边映射 */
        private Map<String, LinkedHashSet<String>> programByCarCodeMap() {
            return programByCarCode;
        }

        /** @return CarProgram->Route 边映射 */
        private Map<String, LinkedHashSet<String>> routeByProgramMap() {
            return routeByProgram;
        }

        /** @return 直接调用边集合 */
        private Set<DirectEdge> directEdges() {
            return directEdges;
        }
    }

    /**
     * 工作簿样式集合。
     */
    private static final class WorkbookStyles {
        private static final String COLOR_CELL = "F54A45";
        private static final String COLOR_P_PROGRAM = "B3D600";
        private static final String COLOR_CAR_CODE = "FFC60A";
        private static final String COLOR_CAR_PROGRAM = "ECE2FE";
        private static final String COLOR_ROUTE = "D9F3FD";
        private static final String COLOR_ARROW = "049FD7";

        private final CellStyle treeTitleStyle;
        private final CellStyle treeHeaderStyle;
        private final CellStyle relationTitleStyle;
        private final CellStyle matrixCornerStyle;
        private final CellStyle matrixBlankStyle;
        private final CellStyle matrixArrowStyle;

        private final Map<NodeType, CellStyle> treeNodeStyles;
        private final Map<NodeType, CellStyle> matrixColumnHeaderStyles;
        private final Map<NodeType, CellStyle> matrixRowHeaderStyles;
        private final Map<NodeType, CellStyle> matrixDirectStyles;

        /**
         * 初始化样式集合。
         *
         * @param workbook 工作簿
         */
        private WorkbookStyles(XSSFWorkbook workbook) {
            XSSFFont normalFont = createFont(workbook, "000000", 11);
            XSSFFont titleFont = createFont(workbook, COLOR_CELL, 11);
            XSSFFont arrowFont = createFont(workbook, COLOR_ARROW, 11);

            this.treeTitleStyle = createStyle(workbook, titleFont, null, BorderStyle.THIN, HorizontalAlignment.CENTER, VerticalAlignment.CENTER, false);
            this.treeHeaderStyle = createStyle(workbook, normalFont, null, BorderStyle.THIN, HorizontalAlignment.CENTER, VerticalAlignment.CENTER, false);
            this.relationTitleStyle = createStyle(workbook, titleFont, null, null, HorizontalAlignment.CENTER, VerticalAlignment.CENTER, false);
            this.matrixCornerStyle = createStyle(workbook, normalFont, null, BorderStyle.THIN, HorizontalAlignment.CENTER, VerticalAlignment.CENTER, false);
            this.matrixBlankStyle = createStyle(workbook, normalFont, null, null, HorizontalAlignment.CENTER, VerticalAlignment.CENTER, false);
            this.matrixArrowStyle = createStyle(workbook, arrowFont, null, null, HorizontalAlignment.CENTER, VerticalAlignment.CENTER, false);

            Map<NodeType, String> colorByType = new EnumMap<>(NodeType.class);
            colorByType.put(NodeType.CEll, COLOR_CELL);
            colorByType.put(NodeType.P_PROGRAM, COLOR_P_PROGRAM);
            colorByType.put(NodeType.CAR_CODE, COLOR_CAR_CODE);
            colorByType.put(NodeType.CAR_PROGRAM, COLOR_CAR_PROGRAM);
            colorByType.put(NodeType.ROUTE_PROCESS, COLOR_ROUTE);

            this.treeNodeStyles = new EnumMap<>(NodeType.class);
            this.matrixColumnHeaderStyles = new EnumMap<>(NodeType.class);
            this.matrixRowHeaderStyles = new EnumMap<>(NodeType.class);
            this.matrixDirectStyles = new EnumMap<>(NodeType.class);

            for (NodeType nodeType : MATRIX_LEVEL_ORDER) {
                String fillColor = colorByType.get(nodeType);
                BorderStyle outline = nodeType == NodeType.VIRTUAL ? BorderStyle.DASHED : BorderStyle.THIN;

                treeNodeStyles.put(
                        nodeType,
                        createStyle(workbook, normalFont, fillColor, outline, HorizontalAlignment.CENTER, VerticalAlignment.CENTER, false)
                );
                matrixColumnHeaderStyles.put(
                        nodeType,
                        createStyle(workbook, normalFont, fillColor, outline, HorizontalAlignment.CENTER, VerticalAlignment.CENTER, false)
                );
                matrixRowHeaderStyles.put(
                        nodeType,
                        createStyle(workbook, normalFont, fillColor, outline, HorizontalAlignment.CENTER, VerticalAlignment.CENTER, false)
                );
                matrixDirectStyles.put(
                        nodeType,
                        createStyle(
                                workbook,
                                normalFont,
                                fillColor,
                                nodeType == NodeType.VIRTUAL ? BorderStyle.DASHED : null,
                                HorizontalAlignment.CENTER,
                                VerticalAlignment.CENTER,
                                false
                        )
                );
            }
        }

        /** @return 树形标题样式 */
        private CellStyle treeTitleStyle() {
            return treeTitleStyle;
        }

        /** @return 树形表头样式 */
        private CellStyle treeHeaderStyle() {
            return treeHeaderStyle;
        }

        /**
         * 获取树形节点样式。
         *
         * @param nodeType 节点类型
         * @return 对应样式
         */
        private CellStyle treeNodeStyle(NodeType nodeType) {
            return treeNodeStyles.getOrDefault(nodeType, matrixBlankStyle);
        }

        /** @return 关系标题样式 */
        private CellStyle relationTitleStyle() {
            return relationTitleStyle;
        }

        /** @return 矩阵左上角样式 */
        private CellStyle matrixCornerStyle() {
            return matrixCornerStyle;
        }

        /**
         * 获取矩阵列表头样式。
         *
         * @param nodeType 节点类型
         * @return 对应样式
         */
        private CellStyle matrixColumnHeaderStyle(NodeType nodeType) {
            return matrixColumnHeaderStyles.getOrDefault(nodeType, matrixCornerStyle);
        }

        /**
         * 获取矩阵行表头样式。
         *
         * @param nodeType 节点类型
         * @return 对应样式
         */
        private CellStyle matrixRowHeaderStyle(NodeType nodeType) {
            return matrixRowHeaderStyles.getOrDefault(nodeType, matrixCornerStyle);
        }

        /**
         * 获取矩阵直调单元格样式。
         *
         * @param callerType 调用方类型
         * @return 对应样式
         */
        private CellStyle matrixDirectStyle(NodeType callerType) {
            return matrixDirectStyles.getOrDefault(callerType, matrixBlankStyle);
        }

        /** @return 矩阵空白样式 */
        private CellStyle matrixBlankStyle() {
            return matrixBlankStyle;
        }

        /** @return 箭头样式 */
        private CellStyle matrixArrowStyle() {
            return matrixArrowStyle;
        }

        /**
         * 创建字体。
         *
         * @param workbook 工作簿
         * @param colorHex 颜色
         * @param size 字号
         * @return 字体
         */
        private XSSFFont createFont(XSSFWorkbook workbook, String colorHex, int size) {
            XSSFFont font = workbook.createFont();
            font.setFontName("Calibri");
            font.setFontHeightInPoints((short) size);
            if (colorHex != null) {
                font.setColor(parseColor(colorHex));
            }
            return font;
        }

        /**
         * 创建样式。
         *
         * @param workbook 工作簿
         * @param font 字体
         * @param fillColor 填充色
         * @param borderStyle 边框样式（null 表示无边框）
         * @param horizontalAlignment 水平对齐
         * @param verticalAlignment 垂直对齐
         * @param wrapText 自动换行
         * @return 样式对象
         */
        private CellStyle createStyle(
                XSSFWorkbook workbook,
                XSSFFont font,
                String fillColor,
                BorderStyle borderStyle,
                HorizontalAlignment horizontalAlignment,
                VerticalAlignment verticalAlignment,
                boolean wrapText
        ) {
            XSSFCellStyle style = workbook.createCellStyle();
            style.setFont(font);
            style.setAlignment(horizontalAlignment);
            style.setVerticalAlignment(verticalAlignment);
            style.setWrapText(wrapText);

            if (fillColor != null) {
                style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                style.setFillForegroundColor(parseColor(fillColor));
            }

            if (borderStyle != null) {
                style.setBorderLeft(borderStyle);
                style.setBorderRight(borderStyle);
                style.setBorderTop(borderStyle);
                style.setBorderBottom(borderStyle);
            }
            return style;
        }

        /**
         * 解析颜色。
         *
         * @param hex 十六进制颜色字符串
         * @return XSSFColor
         */
        private XSSFColor parseColor(String hex) {
            String normalized = hex.replace("#", "");
            if (normalized.length() == 8) {
                normalized = normalized.substring(2);
            }
            if (normalized.length() != 6) {
                return new XSSFColor(new byte[]{0, 0, 0}, null);
            }
            return new XSSFColor(hexToBytes(normalized), null);
        }

        /**
         * 十六进制转 RGB 字节数组。
         *
         * @param hex RRGGBB
         * @return RGB 字节
         */
        private byte[] hexToBytes(String hex) {
            byte[] bytes = new byte[3];
            for (int i = 0; i < 3; i++) {
                int start = i * 2;
                bytes[i] = (byte) Integer.parseInt(hex.substring(start, start + 2), 16);
            }
            return bytes;
        }
    }
}
