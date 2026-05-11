package org.kveex.schedule.type;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public record ScheduleInfo(LocalDateTime editDateTime,
                           LocalDate scheduleDate,
                           List<LessonInfo> lessons,
                           Set<String> groupsList,
                           Set<String> teachersList) {
}
