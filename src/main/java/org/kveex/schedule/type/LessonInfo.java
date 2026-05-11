package org.kveex.schedule.type;

import java.util.List;

public record LessonInfo(String groupName,
                         List<String> teacherNames,
                         LessonTime time,
                         String subjectName,
                         String room,
                         SubGroup subGroup,
                         LessonState state,
                         String customTime) {
}
