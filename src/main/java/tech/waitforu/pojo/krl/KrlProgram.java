package tech.waitforu.pojo.krl;

import java.util.Collections;
import java.util.List;

/**
 * Root model for a parsed KRL source file.
 */
public class KrlProgram {
    private final String moduleName;
    private final List<KrlStatement> statements;

    public KrlProgram(String moduleName, List<KrlStatement> statements) {
        this.moduleName = moduleName;
        this.statements = statements == null ? Collections.emptyList() : List.copyOf(statements);
    }

    public String getModuleName() {
        return moduleName;
    }

    public List<KrlStatement> getStatements() {
        return statements;
    }
}
