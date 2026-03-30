package org.kveex.schedule.parser;

import org.kveex.schedule.LessonState;
import org.kveex.schedule.SubGroup;

import java.util.List;

public record LessonInfo(String groupName,
                         List<String> teacherNames,
                         LessonTime time,
                         String subjectName,
                         String room,
                         SubGroup subGroup,
                         LessonState state) {
}
