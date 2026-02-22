package tech.waitforu.pojo.ast;

import tech.waitforu.pojo.ast.programunit.ProgramUnit;
import tech.waitforu.pojo.krl.KrlFile;

import java.util.ArrayList;
import java.util.List;

/**
 * KRL 文件主体节点。
 * <p>
 * 包含主程序单元和可选子程序单元列表。
 */
public class KrlBody extends tech.waitforu.pojo.ast.AbstractAstNode implements AstNode {
    /** 程序单元列表（第一个通常为主程序）。 */
    private final List<ProgramUnit> programUnitList = new ArrayList<>();

    /**
     * 构建 KrlBody 节点。
     *
     * @param startIndex 起始索引
     * @param stopIndex 结束索引
     * @param krlFile 所属文件
     */
    public KrlBody(int startIndex, int stopIndex, KrlFile krlFile) {
        super(startIndex, stopIndex, krlFile);
    }

    /**
     * 获取程序单元列表。
     *
     * @return 程序单元列表拷贝
     */
    public List<ProgramUnit> getProgramUnitList() {
        return new ArrayList<>(programUnitList);
    }

    /**
     * 获取主程序单元（列表首项）。
     *
     * @return 主程序单元
     */
    public ProgramUnit getMainProgramUnit() {
        if (programUnitList.isEmpty()) {
            throw new RuntimeException("KRL文件中没有程序单元");
        }
        // 主程序单元通常是第一个程序单元
        return programUnitList.getFirst();
    }

    /**
     * 添加程序单元并建立 AST 子节点关系。
     *
     * @param programUnit 程序单元
     */
    public void addProgramUnit(ProgramUnit programUnit) {
        programUnitList.add(programUnit);
        addChild(programUnit);
    }

    /**
     * 获取指定索引程序单元。
     *
     * @param index 索引
     * @return 程序单元
     */
    public ProgramUnit getProgramUnit(int index) {
        return programUnitList.get(index);
    }

    /**
     * 获取模块名（来自主程序单元名称）。
     *
     * @return 模块名
     */
    public String getModuleName() {
        return getMainProgramUnit().getName();
    }

    /**
     * 设置模块名，实际上是设置主程序单元的名称
     * @param moduleName 模块名
     */
    public void setModuleName(String moduleName) {
        getMainProgramUnit().setName(moduleName);
    }
}
