package tech.waitforu.pojo.ast.programunit;

import tech.waitforu.pojo.krl.KrlFile;

/**
 * DATA 程序单元节点。
 */
public class DataUnit extends AbstractProgramUnit {

   /**
    * 构建 DATA 单元。
    *
    * @param startIndex 起始索引
    * @param stopIndex 结束索引
    * @param krlFile 所属文件
    */
   public DataUnit(int startIndex, int stopIndex, KrlFile krlFile) {
        super(startIndex, stopIndex, krlFile);
    }
}
