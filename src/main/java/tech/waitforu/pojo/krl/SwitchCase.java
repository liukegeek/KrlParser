package tech.waitforu.pojo.krl;

import java.util.Collections;
import java.util.List;

/**
 * Represents a single {@code CASE} branch inside a {@code SWITCH}.
 */
public class SwitchCase {
    private final List<String> labels;
    private final List<KrlStatement> statements;

    public SwitchCase(List<String> labels, List<KrlStatement> statements) {
        this.labels = labels == null ? Collections.emptyList() : List.copyOf(labels);
        this.statements = statements == null ? Collections.emptyList() : List.copyOf(statements);
    }

    public List<String> getLabels() {
        return labels;
    }

    public List<KrlStatement> getStatements() {
        return statements;
    }
}
