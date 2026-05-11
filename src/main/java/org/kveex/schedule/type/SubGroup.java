package org.kveex.schedule.type;

import org.jetbrains.annotations.NotNull;
import org.jooq.EnumType;

public enum SubGroup implements EnumType {
    FIRST("FIRST"),
    SECOND("SECOND"),
    BOTH("BOTH");

    private final String literal;

    SubGroup(String literal) {
        this.literal = literal;
    }

    public static SubGroup toSubGroup(String subgroup) {
        return switch (subgroup) {
            case "1", "1п" -> FIRST;
            case "2", "2п" -> SECOND;
            default -> BOTH;
        };
    }

    @Override
    public @NotNull String getLiteral() {
        return literal; // значение ENUM в БД
    }

    @Override
    public @NotNull String getName() {
        return "subgroup_type"; // имя типа в PostgreSQL
    }
}