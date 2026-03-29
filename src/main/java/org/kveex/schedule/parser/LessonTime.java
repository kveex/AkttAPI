package org.kveex.schedule.parser;

import org.jetbrains.annotations.NotNull;
import org.jooq.EnumType;

import java.time.DayOfWeek;
import java.time.LocalDate;

public enum LessonTime implements EnumType {
    FIRST("FIRST"),
    FIRST_SHORT("FIRST_SHORT"),
    SECOND("SECOND"),
    SECOND_FULL("SECOND_FULL"),
    SECOND_SHORT("SECOND_SHORT"),
    THIRD("THIRD"),
    THIRD_SHORT("THIRD_SHORT"),
    FOURTH("FOURTH"),
    FOURTH_SHORT("FOURTH_SHORT"),
    FIFTH("FIFTH"),
    FIFTH_SHORT("FIFTH_SHORT"),
    PRODUCTION_PRACTICE("PRODUCTION_PRACTICE"),
    LEARNING_PRACTICE("LEARNING_PRACTICE"),
    CUSTOM("");

    private String literal;

    LessonTime(String literal) {
        this.literal = literal;
    }

    @Override
    public @NotNull String getLiteral() {
        return literal;
    }

    private LessonTime setLiteral(String literal) {
        this.literal = literal;
        return this;
    }

    public static LessonTime convertFromString(String time, LocalDate scheduleDate) {
        boolean todayIsSaturday = scheduleDate.getDayOfWeek() == DayOfWeek.SATURDAY;
        return switch (time) {
            case "1,2" -> !todayIsSaturday ? LessonTime.FIRST : LessonTime.FIRST_SHORT;
            case "3,4" -> {
                if (todayIsSaturday) {
                    yield LessonTime.SECOND_SHORT;
                }
                yield !isInSecondCampus ? LessonTime.SECOND : LessonTime.SECOND_FULL;
            }
            case "5,6" -> !todayIsSaturday ? LessonTime.THIRD : LessonTime.THIRD_SHORT;
            case "7,8" -> !todayIsSaturday ? LessonTime.FOURTH : LessonTime.FOURTH_SHORT;
            default -> CUSTOM.setLiteral(time);
        };
    }

    @Override
    public @NotNull String getName() {
        return "lesson_time";
    }
}
