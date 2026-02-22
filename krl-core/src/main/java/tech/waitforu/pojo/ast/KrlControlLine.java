package tech.waitforu.pojo.ast;

import tech.waitforu.pojo.krl.KrlFile;

/**
 * KRL 控制行节点（如 &ACCESS、&REL 等头部控制指令）。
 */
public class KrlControlLine extends tech.waitforu.pojo.ast.AbstractAstNode {

    /**
     * 构建控制行节点。
     *
     * @param startIndex 起始索引
     * @param stopIndex 结束索引
     * @param krlFile 所属文件
     */
    public KrlControlLine(int startIndex, int stopIndex, KrlFile krlFile) {
        super(startIndex, stopIndex, krlFile);
    }
}
