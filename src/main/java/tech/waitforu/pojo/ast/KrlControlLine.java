package tech.waitforu.pojo.ast;

import tech.waitforu.pojo.krl.KrlFile;

import java.util.List;

/**
 * ClassName: KrlControlLine
 * Package: tech.waitforu.pojo.ast
 * Description:
 * Author: LiuKe
 * Create: 2025/12/12 14:04
 * Version 1.0
 */
public class KrlControlLine extends AbstractAstNode {

    public KrlControlLine(int startIndex, int stopIndex, KrlFile krlFile) {
        super(startIndex, stopIndex, krlFile);
    }
}
