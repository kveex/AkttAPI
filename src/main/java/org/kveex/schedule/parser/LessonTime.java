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
    PRODUCTION_PRACTICE("PRODUCTION_PRACTICE"),
    LEARNING_PRACTICE("LEARNING_PRACTICE"),
    PRE_DIPLOMA_PRACTICE("PRE_DIPLOMA_PRACTICE"),
    CUSTOM("CUSTOM");

    private final String literal;
    private String customTime = null;

    LessonTime(String literal) {
        this.literal = literal;
    }

    @Override
    public @NotNull String getLiteral() {
        return literal;
    }

    private LessonTime setCustomTime(String time) {
        customTime = time;
        return this;
    }

    public String getCustomTime() {
        return customTime;
    }

    public static LessonTime convertFromString(String time, LocalDate scheduleDate/*, boolean isInSecondCampus*/) {
        boolean todayIsSaturday = scheduleDate.getDayOfWeek() == DayOfWeek.SATURDAY;
        return switch (time) {
            case "1,2" -> !todayIsSaturday ? FIRST : FIRST_SHORT;
//            case "3,4" -> {
//                if (todayIsSaturday) {
//                    yield SECOND_SHORT;
//                }
//                yield !isInSecondCampus ? SECOND : SECOND_FULL;
//            }
            case "3,4" -> !todayIsSaturday ? SECOND : SECOND_SHORT;
            case "5,6" -> !todayIsSaturday ? THIRD : THIRD_SHORT;
            case "7,8" -> !todayIsSaturday ? FOURTH : FOURTH_SHORT;
            case "УП" -> LEARNING_PRACTICE;
            case "ПП" -> PRODUCTION_PRACTICE;
            case "ПДП" -> PRE_DIPLOMA_PRACTICE;
            default -> CUSTOM.setCustomTime(time);
        };
    }

    @Override
    public @NotNull String getName() {
        return "lesson_time";
    }
}
