package org.kveex.schedule.type;

import org.jetbrains.annotations.NotNull;
import org.jooq.EnumType;

public enum LessonState implements EnumType {
    OK("OK"),
    DISTANT("DISTANT"),
    PRACTICE("PRACTICE"),
    EMPTY("EMPTY");

    private final String literal;

    LessonState(String literal) {
        this.literal = literal;
    }

    @Override
    public @NotNull String getLiteral() {
        return literal;
    }

    @Override
    public @NotNull String getName() {
        return "lesson_state";
    }
}
