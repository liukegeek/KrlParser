package tech.waitforu.pojo.krl;

import java.util.Collections;
import java.util.List;

/**
 * Represents a direct callable invocation like {@code P00()} or {@code p90()}.
 */
public class CallStatement implements KrlStatement {
    private final String name;
    private final List<String> arguments;

    public CallStatement(String name, List<String> arguments) {
        this.name = name;
        this.arguments = arguments == null ? Collections.emptyList() : List.copyOf(arguments);
    }

    public String getName() {
        return name;
    }

    public List<String> getArguments() {
        return arguments;
    }

    @Override
    public String toString() {
        return "CallStatement{" +
                "name='" + name + '\'' +
                ", arguments=" + arguments +
                '}';
    }
}
