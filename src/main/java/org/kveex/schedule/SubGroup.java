package org.kveex.schedule;

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

    public static SubGroup toSubGroup(int subgroup) {
        return switch (subgroup) {
            case 1 -> FIRST;
            case 2 -> SECOND;
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