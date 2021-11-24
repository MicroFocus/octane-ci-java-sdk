package com.hp.octane.integrations.uft.ufttestresults.schema;

import java.io.Serializable;
import java.util.Objects;
import java.util.StringJoiner;

public class UftResultStepParameter implements Serializable {

    private String name;

    private String value;

    private String type;

    public UftResultStepParameter(String name, String value, String type) {
        this.name = name;
        this.value = value;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public String getType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UftResultStepParameter that = (UftResultStepParameter) o;
        return Objects.equals(name, that.name) && Objects.equals(value, that.value) && Objects.equals(type, that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, value, type);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", UftResultStepParameter.class.getSimpleName() + "[", "]")
                .add("name='" + name + "'")
                .add("value='" + value + "'")
                .add("type='" + type + "'")
                .toString();
    }
}
