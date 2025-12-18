package tech.waitforu.pojo.ast.programunit;

import tech.waitforu.pojo.ast.declaration.Declaration;
import tech.waitforu.pojo.ast.statements.Statement;
import tech.waitforu.pojo.krl.KrlFile;

import java.util.List;

/**
 * ClassName: ProcedureUnit
 * Package: tech.waitforu.pojo.ast.programunit
 * Description:
 * Author: LiuKe
 * Create: 2025/12/15 09:11
 * Version 1.0
 */
public class ProcedureUnit extends AbstractProgramUnit implements ProgramUnit, Callable {
    public ProcedureUnit(int startIndex, int stopIndex, KrlFile krlFile) {
        super(startIndex, stopIndex, krlFile);
    }
}
