package org.kveex.schedule.parser;

import org.kveex.schedule.LessonGroup;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public record ScheduleInfo(LocalDateTime editDateTime,
                           LocalDate scheduleDate,
                           List<LessonGroup> studentsSchedule,
                           List<LessonGroup> teachersSchedule,
                           List<String> groupsList,
                           Set<String> teachersList) {
}
