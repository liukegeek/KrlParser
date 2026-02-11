package tech.waitforu.krlweb.service;

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
import org.springframework.stereotype.Service;
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

/**
 * 生成调用关系 Excel 文件：
 * 1. 每个机器人一个 sheet。
 * 2. 上半区：树形调用关系（5 列，按值纵向合并）。
 * 3. 下半区：调用关系表（行=调用方，列=被调用方；直调填调用方，顶部箭头提示调用方向）。
 */
@Service
public class CallGraphExcelExportService {
    private static final String TREE_TITLE = "车型调用关系图(树形结构)";
    private static final String RELATION_TITLE = "调用关系表(横向代表调用关系，竖向代表被调用关系)";

    private static final int TREE_TITLE_ROW = 0;
    private static final int TREE_HEADER_ROW = 1;
    private static final int TREE_DATA_START_ROW = 2;

    private static final List<String> TREE_HEADERS = List.of("Cell程序", "P程序", "车型代码", "车型程序", "轨迹程序");
    private static final List<NodeType> LEVEL_ORDER = List.of(
            NodeType.CEll,
            NodeType.P_PROGRAM,
            NodeType.CAR_CODE,
            NodeType.CAR_PROGRAM,
            NodeType.ROUTE_PROCESS
    );

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

    private void renderRobotSheet(Sheet sheet, RobotInfo robotInfo, WorkbookStyles styles) {
        TreeBuildResult treeBuildResult = buildTreeModel(robotInfo);
        List<TreePathRow> treeRows = treeBuildResult.treeRows();

        int treeRowCount = Math.max(treeRows.size(), 1);
        int treeDataEndRow = TREE_DATA_START_ROW + treeRowCount - 1;

        sheet.setDefaultColumnWidth(14);

        writeTreeHeader(sheet, styles);
        writeTreeRows(sheet, styles, treeRows, treeRowCount);
        mergeTreeColumns(sheet, TREE_DATA_START_ROW, treeDataEndRow);

        int relationTitleRow = treeDataEndRow + 3;
        int relationHeaderRow = relationTitleRow + 1;
        int matrixStartRow = relationHeaderRow + 1;

        List<NodeKey> orderedNodes = treeBuildResult.orderedNodes();
        int matrixSize = Math.max(orderedNodes.size(), 1);
        int matrixEndRow = matrixStartRow + matrixSize - 1;

        writeRelationHeader(sheet, styles, relationTitleRow, relationHeaderRow);
        writeRelationMatrix(sheet, styles, orderedNodes, treeBuildResult.directEdges(), matrixStartRow);

        adjustColumnWidths(sheet, orderedNodes);

        // 保持和模板一致的可视范围（顶部树 + 关系矩阵）
        for (int rowIndex = TREE_TITLE_ROW; rowIndex <= matrixEndRow + 1; rowIndex++) {
            Row row = getOrCreateRow(sheet, rowIndex);
            if (row.getHeightInPoints() < 19f) {
                row.setHeightInPoints(19f);
            }
        }
    }

    private void writeTreeHeader(Sheet sheet, WorkbookStyles styles) {
        Row titleRow = getOrCreateRow(sheet, TREE_TITLE_ROW);
        for (int col = 0; col < 5; col++) {
            Cell cell = getOrCreateCell(titleRow, col);
            cell.setCellStyle(styles.treeTitleStyle());
        }
        getOrCreateCell(titleRow, 0).setCellValue(TREE_TITLE);
        sheet.addMergedRegion(new CellRangeAddress(TREE_TITLE_ROW, TREE_TITLE_ROW, 0, 4));

        Row headerRow = getOrCreateRow(sheet, TREE_HEADER_ROW);
        for (int col = 0; col < TREE_HEADERS.size(); col++) {
            Cell cell = getOrCreateCell(headerRow, col);
            cell.setCellStyle(styles.treeHeaderStyle());
            cell.setCellValue(TREE_HEADERS.get(col));
        }
    }

    private void writeTreeRows(Sheet sheet, WorkbookStyles styles, List<TreePathRow> treeRows, int treeRowCount) {
        for (int index = 0; index < treeRowCount; index++) {
            Row row = getOrCreateRow(sheet, TREE_DATA_START_ROW + index);
            TreePathRow treePathRow = index < treeRows.size() ? treeRows.get(index) : TreePathRow.empty();

            for (int col = 0; col < LEVEL_ORDER.size(); col++) {
                Cell cell = getOrCreateCell(row, col);
                NodeType type = LEVEL_ORDER.get(col);
                cell.setCellStyle(styles.treeNodeStyle(type));

                NodeKey nodeKey = treePathRow.nodeAt(col);
                if (nodeKey != null) {
                    cell.setCellValue(nodeKey.value());
                } else {
                    cell.setBlank();
                }
            }
        }
    }

    private void mergeTreeColumns(Sheet sheet, int startRow, int endRow) {
        for (int col = 0; col < LEVEL_ORDER.size(); col++) {
            int mergeStart = startRow;
            String previousValue = readCellText(sheet, startRow, col);

            for (int row = startRow + 1; row <= endRow; row++) {
                String currentValue = readCellText(sheet, row, col);
                boolean sameValue = Objects.equals(previousValue, currentValue);
                if (sameValue) {
                    continue;
                }

                mergeRangeIfNeeded(sheet, mergeStart, row - 1, col, previousValue);
                mergeStart = row;
                previousValue = currentValue;
            }

            mergeRangeIfNeeded(sheet, mergeStart, endRow, col, previousValue);
        }
    }

    private void mergeRangeIfNeeded(Sheet sheet, int startRow, int endRow, int col, String value) {
        if (startRow >= endRow) {
            return;
        }
        if (value == null || value.isBlank()) {
            return;
        }
        sheet.addMergedRegion(new CellRangeAddress(startRow, endRow, col, col));
    }

    private void writeRelationHeader(Sheet sheet, WorkbookStyles styles, int relationTitleRow, int relationHeaderRow) {
        Row titleRow = getOrCreateRow(sheet, relationTitleRow);
        for (int col = 0; col < 5; col++) {
            Cell cell = getOrCreateCell(titleRow, col);
            cell.setCellStyle(styles.relationTitleStyle());
        }
        getOrCreateCell(titleRow, 0).setCellValue(RELATION_TITLE);
        sheet.addMergedRegion(new CellRangeAddress(relationTitleRow, relationTitleRow, 0, 4));

        Row headerRow = getOrCreateRow(sheet, relationHeaderRow);
        Cell corner = getOrCreateCell(headerRow, 0);
        corner.setCellStyle(styles.matrixCornerStyle());
        corner.setBlank();
    }

    private void writeRelationMatrix(
            Sheet sheet,
            WorkbookStyles styles,
            List<NodeKey> orderedNodes,
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

        // 写顶部列标题（被调用方）
        Row headerRow = getOrCreateRow(sheet, relationHeaderRow);
        for (int index = 0; index < orderedNodes.size(); index++) {
            NodeKey callee = orderedNodes.get(index);
            Cell cell = getOrCreateCell(headerRow, index + 1);
            cell.setCellStyle(styles.matrixColumnHeaderStyle(callee.type()));
            cell.setCellValue(callee.value());
        }

        // 写左侧行标题（调用方）+ 初始化矩阵体
        for (int rowOffset = 0; rowOffset < orderedNodes.size(); rowOffset++) {
            NodeKey caller = orderedNodes.get(rowOffset);
            Row row = getOrCreateRow(sheet, matrixStartRow + rowOffset);

            Cell rowHeaderCell = getOrCreateCell(row, 0);
            rowHeaderCell.setCellStyle(styles.matrixRowHeaderStyle(caller.type()));
            rowHeaderCell.setCellValue(caller.value());

            for (int colOffset = 0; colOffset < orderedNodes.size(); colOffset++) {
                Cell bodyCell = getOrCreateCell(row, colOffset + 1);
                bodyCell.setCellStyle(styles.matrixBlankStyle());
                bodyCell.setBlank();
            }
        }

        Map<NodeKey, Integer> nodeIndexMap = new HashMap<>();
        for (int i = 0; i < orderedNodes.size(); i++) {
            nodeIndexMap.put(orderedNodes.get(i), i);
        }

        Map<Integer, Integer> firstDirectCallerRowByColumn = new HashMap<>();

        // 直调关系：行=调用方，列=被调用方，填入调用方名称
        for (DirectEdge edge : directEdges) {
            Integer callerIndex = nodeIndexMap.get(edge.from());
            Integer calleeIndex = nodeIndexMap.get(edge.to());
            if (callerIndex == null || calleeIndex == null) {
                continue;
            }

            int rowIndex = matrixStartRow + callerIndex;
            int colIndex = 1 + calleeIndex;

            Row row = getOrCreateRow(sheet, rowIndex);
            Cell cell = getOrCreateCell(row, colIndex);
            cell.setCellStyle(styles.matrixDirectStyle(edge.from().type()));
            cell.setCellValue(edge.from().value());

            firstDirectCallerRowByColumn.merge(colIndex, rowIndex, Math::min);
        }

        // 箭头提示：从最早的直调关系向上填充到矩阵起始行
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

    private void adjustColumnWidths(Sheet sheet, List<NodeKey> orderedNodes) {
        // 左半区固定 5 列
        for (int col = 0; col < 5; col++) {
            sheet.setColumnWidth(col, 14 * 256);
        }

        // 矩阵区域列宽：根据标题长度动态放大
        for (int index = 0; index < orderedNodes.size(); index++) {
            NodeKey key = orderedNodes.get(index);
            int col = index + 1;
            int width = Math.max(14, Math.min(24, key.value().length() + 4));
            sheet.setColumnWidth(col, width * 256);
        }
    }

    private TreeBuildResult buildTreeModel(RobotInfo robotInfo) {
        CallNode root = robotInfo != null ? robotInfo.getCallGraphRoot() : null;
        if (root == null) {
            return TreeBuildResult.empty();
        }

        List<TreePathRow> treeRows = new ArrayList<>();
        Map<NodeType, LinkedHashMap<String, NodeKey>> orderedByType = new EnumMap<>(NodeType.class);
        for (NodeType level : LEVEL_ORDER) {
            orderedByType.put(level, new LinkedHashMap<>());
        }

        walkTree(root, new EnumMap<>(NodeType.class), treeRows, orderedByType);
        if (treeRows.isEmpty()) {
            treeRows.add(TreePathRow.empty());
        }

        List<NodeKey> orderedNodes = new ArrayList<>();
        for (NodeType level : LEVEL_ORDER) {
            orderedNodes.addAll(orderedByType.getOrDefault(level, new LinkedHashMap<>()).values());
        }

        Set<DirectEdge> directEdges = collectDirectEdges(treeRows);
        return new TreeBuildResult(treeRows, orderedNodes, directEdges);
    }

    private void walkTree(
            CallNode node,
            EnumMap<NodeType, NodeKey> path,
            List<TreePathRow> rows,
            Map<NodeType, LinkedHashMap<String, NodeKey>> orderedByType
    ) {
        if (node == null) {
            return;
        }

        NodeType normalizedType = normalizeType(node.getNodeType());
        NodeKey currentNodeKey = toNodeKey(node, normalizedType);

        EnumMap<NodeType, NodeKey> currentPath = new EnumMap<>(path);
        if (normalizedType != null && currentNodeKey != null && LEVEL_ORDER.contains(normalizedType)) {
            currentPath.put(normalizedType, currentNodeKey);
            orderedByType.get(normalizedType).putIfAbsent(currentNodeKey.uniqueKey(), currentNodeKey);
        }

        List<CallNode> children = getCallChildren(node);
        if (children.isEmpty()) {
            rows.add(TreePathRow.fromPath(currentPath));
            return;
        }

        for (CallNode child : children) {
            walkTree(child, currentPath, rows, orderedByType);
        }
    }

    private Set<DirectEdge> collectDirectEdges(List<TreePathRow> rows) {
        Set<DirectEdge> directEdges = new LinkedHashSet<>();
        for (TreePathRow row : rows) {
            addEdgeIfPresent(directEdges, row.cell(), row.pProgram());
            addEdgeIfPresent(directEdges, row.pProgram(), row.carCode());
            addEdgeIfPresent(directEdges, row.carCode(), row.carProgram());
            addEdgeIfPresent(directEdges, row.carProgram(), row.routeProgram());
        }
        return directEdges;
    }

    private void addEdgeIfPresent(Set<DirectEdge> directEdges, NodeKey from, NodeKey to) {
        if (from == null || to == null) {
            return;
        }
        if (from.equals(to)) {
            return;
        }
        directEdges.add(new DirectEdge(from, to));
    }

    private NodeType normalizeType(NodeType nodeType) {
        if (nodeType == null) {
            return null;
        }
        if (nodeType == NodeType.VIRTUAL) {
            return NodeType.P_PROGRAM;
        }
        return nodeType;
    }

    private NodeKey toNodeKey(CallNode node, NodeType normalizedType) {
        if (node == null || normalizedType == null) {
            return null;
        }
        String value = safeText(node.getValue());
        if (value.isBlank()) {
            value = safeText(node.getId());
        }
        if (value.isBlank()) {
            return null;
        }
        return new NodeKey(normalizedType, value);
    }

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
            String end = "_" + suffix;
            candidate = truncate(safeBaseName, 31 - end.length()) + end;
            suffix++;
        }
        usedSheetNames.add(candidate);
        return candidate;
    }

    private String truncate(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, Math.max(maxLength, 0));
    }

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

    private Row getOrCreateRow(Sheet sheet, int rowIndex) {
        Row row = sheet.getRow(rowIndex);
        return row != null ? row : sheet.createRow(rowIndex);
    }

    private Cell getOrCreateCell(Row row, int colIndex) {
        Cell cell = row.getCell(colIndex);
        return cell != null ? cell : row.createCell(colIndex);
    }

    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }

    private record TreeBuildResult(List<TreePathRow> treeRows, List<NodeKey> orderedNodes, Set<DirectEdge> directEdges) {
        private static TreeBuildResult empty() {
            return new TreeBuildResult(List.of(), List.of(), Set.of());
        }
    }

    private record TreePathRow(NodeKey cell, NodeKey pProgram, NodeKey carCode, NodeKey carProgram, NodeKey routeProgram) {
        private static TreePathRow fromPath(Map<NodeType, NodeKey> path) {
            return new TreePathRow(
                    path.get(NodeType.CEll),
                    path.get(NodeType.P_PROGRAM),
                    path.get(NodeType.CAR_CODE),
                    path.get(NodeType.CAR_PROGRAM),
                    path.get(NodeType.ROUTE_PROCESS)
            );
        }

        private static TreePathRow empty() {
            return new TreePathRow(null, null, null, null, null);
        }

        private NodeKey nodeAt(int colIndex) {
            return switch (colIndex) {
                case 0 -> cell;
                case 1 -> pProgram;
                case 2 -> carCode;
                case 3 -> carProgram;
                case 4 -> routeProgram;
                default -> null;
            };
        }
    }

    private record NodeKey(NodeType type, String value) {
        private String uniqueKey() {
            return type + "::" + value.toLowerCase(Locale.ROOT);
        }
    }

    private record DirectEdge(NodeKey from, NodeKey to) {
    }

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

        private WorkbookStyles(XSSFWorkbook workbook) {
            XSSFFont normalFont = createFont(workbook, "000000", 11);
            XSSFFont titleFont = createFont(workbook, COLOR_CELL, 11);
            XSSFFont arrowFont = createFont(workbook, COLOR_ARROW, 11);

            this.treeTitleStyle = createStyle(workbook, titleFont, null, true, HorizontalAlignment.CENTER, VerticalAlignment.CENTER, false);
            this.treeHeaderStyle = createStyle(workbook, normalFont, null, true, HorizontalAlignment.CENTER, VerticalAlignment.CENTER, false);
            this.relationTitleStyle = createStyle(workbook, titleFont, null, false, HorizontalAlignment.CENTER, VerticalAlignment.CENTER, false);
            this.matrixCornerStyle = createStyle(workbook, normalFont, null, true, HorizontalAlignment.CENTER, VerticalAlignment.CENTER, false);
            this.matrixBlankStyle = createStyle(workbook, normalFont, null, false, HorizontalAlignment.CENTER, VerticalAlignment.CENTER, false);
            this.matrixArrowStyle = createStyle(workbook, arrowFont, null, false, HorizontalAlignment.CENTER, VerticalAlignment.CENTER, false);

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

            for (NodeType nodeType : LEVEL_ORDER) {
                String fillColor = colorByType.get(nodeType);
                treeNodeStyles.put(nodeType, createStyle(workbook, normalFont, fillColor, true, HorizontalAlignment.CENTER, VerticalAlignment.CENTER, false));
                matrixColumnHeaderStyles.put(nodeType, createStyle(workbook, normalFont, fillColor, true, HorizontalAlignment.CENTER, VerticalAlignment.CENTER, false));
                matrixRowHeaderStyles.put(nodeType, createStyle(workbook, normalFont, fillColor, true, HorizontalAlignment.CENTER, VerticalAlignment.CENTER, false));
                matrixDirectStyles.put(nodeType, createStyle(workbook, normalFont, fillColor, false, HorizontalAlignment.CENTER, VerticalAlignment.CENTER, false));
            }
        }

        private CellStyle treeTitleStyle() {
            return treeTitleStyle;
        }

        private CellStyle treeHeaderStyle() {
            return treeHeaderStyle;
        }

        private CellStyle treeNodeStyle(NodeType type) {
            return treeNodeStyles.getOrDefault(type, matrixBlankStyle);
        }

        private CellStyle relationTitleStyle() {
            return relationTitleStyle;
        }

        private CellStyle matrixCornerStyle() {
            return matrixCornerStyle;
        }

        private CellStyle matrixColumnHeaderStyle(NodeType type) {
            return matrixColumnHeaderStyles.getOrDefault(type, matrixCornerStyle);
        }

        private CellStyle matrixRowHeaderStyle(NodeType type) {
            return matrixRowHeaderStyles.getOrDefault(type, matrixCornerStyle);
        }

        private CellStyle matrixDirectStyle(NodeType callerType) {
            return matrixDirectStyles.getOrDefault(callerType, matrixBlankStyle);
        }

        private CellStyle matrixBlankStyle() {
            return matrixBlankStyle;
        }

        private CellStyle matrixArrowStyle() {
            return matrixArrowStyle;
        }

        private XSSFFont createFont(XSSFWorkbook workbook, String colorHex, int size) {
            XSSFFont font = workbook.createFont();
            font.setFontName("Calibri");
            font.setFontHeightInPoints((short) size);
            if (colorHex != null) {
                font.setColor(parseColor(colorHex));
            }
            return font;
        }

        private CellStyle createStyle(
                XSSFWorkbook workbook,
                XSSFFont font,
                String fillColor,
                boolean withBorder,
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

            if (withBorder) {
                style.setBorderLeft(BorderStyle.THIN);
                style.setBorderRight(BorderStyle.THIN);
                style.setBorderTop(BorderStyle.THIN);
                style.setBorderBottom(BorderStyle.THIN);
            }
            return style;
        }

        private XSSFColor parseColor(String hex) {
            String normalized = hex.replace("#", "");
            if (normalized.length() == 8) {
                normalized = normalized.substring(2);
            }
            if (normalized.length() != 6) {
                return new XSSFColor(new byte[]{0, 0, 0}, null);
            }
            byte[] rgb = hexToBytes(normalized);
            return new XSSFColor(rgb, null);
        }

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
