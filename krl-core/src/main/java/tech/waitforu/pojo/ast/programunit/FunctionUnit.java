package tech.waitforu.pojo.ast.programunit;

import tech.waitforu.pojo.krl.KrlFile;

/**
 * FUNCTION 程序单元节点。
 */
public class FunctionUnit extends AbstractProgramUnit implements ProgramUnit, Callable {

    /**
     * 构建 FUNCTION 单元。
     *
     * @param startIndex 起始索引
     * @param stopIndex 结束索引
     * @param krlFile 所属文件
     */
    public FunctionUnit(int startIndex, int stopIndex, KrlFile krlFile) {
        super(startIndex, stopIndex, krlFile);
    }
}
