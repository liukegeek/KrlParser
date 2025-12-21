package tech.waitforu.pojo.ast;

import tech.waitforu.pojo.ast.programunit.ProgramUnit;
import tech.waitforu.pojo.krl.KrlFile;

import java.util.ArrayList;
import java.util.List;

/**
 * ClassName: ast.pojo.tech.waitforu.KrlBody
 * Package: tech.waitforu.pojo.ast
 * Description:
 * Author: LiuKe
 * Create: 2025/12/12 14:04
 * Version 1.0
 */
public class KrlBody extends tech.waitforu.pojo.ast.AbstractAstNode implements AstNode {
    private final List<ProgramUnit> programUnitList = new ArrayList<>();

    public KrlBody(int startIndex, int stopIndex, KrlFile krlFile) {
        super(startIndex, stopIndex, krlFile);
    }


    public List<ProgramUnit> getProgramUnitList() {
        return new ArrayList<>(programUnitList);
    }

    public ProgramUnit getMainProgramUnit() {
        if (programUnitList.isEmpty()) {
            throw new RuntimeException("KRL文件中没有程序单元");
        }
        // 主程序单元通常是第一个程序单元
        return programUnitList.getFirst();
    }

    public void addProgramUnit(ProgramUnit programUnit) {
        programUnitList.add(programUnit);
        addChild(programUnit);
    }

    public ProgramUnit getProgramUnit(int index) {
        return programUnitList.get(index);
    }

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
