package tech.waitforu.parser;

import org.junit.jupiter.api.Test;
import tech.waitforu.pojo.ast.KrlRoot;
import tech.waitforu.pojo.ast.expression.Invocation;
import tech.waitforu.pojo.ast.programunit.ProgramUnit;
import tech.waitforu.pojo.ast.statements.ForStatement;
import tech.waitforu.pojo.ast.statements.IfStatement;
import tech.waitforu.pojo.ast.statements.RepeatStatement;
import tech.waitforu.pojo.ast.statements.Statement;
import tech.waitforu.pojo.ast.statements.WhileStatement;
import tech.waitforu.pojo.krl.KrlFile;
import tech.waitforu.pojo.krl.KrlModule;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AST 构建回归测试。
 */
class AstBuilderVisitorTest {

    /**
     * 控制语句中的调用表达式应被保留在业务 AST 中。
     */
    @Test
    void shouldKeepInvocationsInsideControlStatements() {
        KrlRoot root = parse("""
                &ACCESS RVP2
                &REL 13
                DEF CONTROL_FLOW()
                IF FLAG THEN
                CALL_IF()
                ELSE
                CALL_ELSE()
                ENDIF
                FOR IDX = 1 TO 3 STEP 1
                CALL_FOR()
                ENDFOR
                WHILE FLAG
                CALL_WHILE()
                ENDWHILE
                REPEAT
                CALL_REPEAT()
                UNTIL FLAG
                END
                """);

        ProgramUnit programUnit = root.getBody().getMainProgramUnit();

        Set<String> invocations = programUnit.findNodesByType(Invocation.class).stream()
                .map(Invocation::getTargetName)
                .collect(Collectors.toSet());

        assertTrue(invocations.contains("CALL_IF"));
        assertTrue(invocations.contains("CALL_ELSE"));
        assertTrue(invocations.contains("CALL_FOR"));
        assertTrue(invocations.contains("CALL_WHILE"));
        assertTrue(invocations.contains("CALL_REPEAT"));

        Statement ifStatement = programUnit.getStatementList().get(0);
        Statement forStatement = programUnit.getStatementList().get(1);
        Statement whileStatement = programUnit.getStatementList().get(2);
        Statement repeatStatement = programUnit.getStatementList().get(3);

        assertInstanceOf(IfStatement.class, ifStatement);
        assertInstanceOf(ForStatement.class, forStatement);
        assertInstanceOf(WhileStatement.class, whileStatement);
        assertInstanceOf(RepeatStatement.class, repeatStatement);
    }

    private KrlRoot parse(String content) {
        KrlFile krlFile = new KrlFile("/tmp/control_flow.src", "", "", 0L, content);
        KrlModule module = new KrlModule("control_flow");
        module.setModuleSrcFile(krlFile);
        return (KrlRoot) new ModuleParser(module).getSrcAst();
    }
}
