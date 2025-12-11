package tech.waitforu.pojo.krl;

import java.util.Collections;
import java.util.List;

/**
 * Represents a {@code SWITCH} statement with its {@code CASE} branches and optional default section.
 */
public class SwitchStatement implements KrlStatement {
    private final String expression;
    private final List<SwitchCase> cases;
    private final List<KrlStatement> defaultStatements;

    public SwitchStatement(String expression, List<SwitchCase> cases, List<KrlStatement> defaultStatements) {
        this.expression = expression;
        this.cases = cases == null ? Collections.emptyList() : List.copyOf(cases);
        this.defaultStatements = defaultStatements == null ? Collections.emptyList() : List.copyOf(defaultStatements);
    }

    public String getExpression() {
        return expression;
    }

    public List<SwitchCase> getCases() {
        return cases;
    }

    public List<KrlStatement> getDefaultStatements() {
        return defaultStatements;
    }
}
