package org.kveex.schedule.parser;

import org.kveex.schedule.LessonState;
import org.kveex.schedule.SubGroup;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record LessonInfo(String groupName,
                         List<String> teacherNames,
                         LessonTime time,
                         String subjectName,
                         List<String> room,
                         SubGroup subGroup,
                         LessonState state,
                         int position) {
}
