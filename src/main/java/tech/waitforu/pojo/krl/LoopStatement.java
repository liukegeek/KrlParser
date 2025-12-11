package tech.waitforu.pojo.krl;

import java.util.Collections;
import java.util.List;

/**
 * Represents a KRL {@code LOOP ... ENDLOOP} block.
 */
public class LoopStatement implements KrlStatement {
    private final List<KrlStatement> body;

    public LoopStatement(List<KrlStatement> body) {
        this.body = body == null ? Collections.emptyList() : List.copyOf(body);
    }

    public List<KrlStatement> getBody() {
        return body;
    }
}
