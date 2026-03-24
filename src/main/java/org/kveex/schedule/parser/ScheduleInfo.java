package org.kveex.schedule.parser;

import org.kveex.schedule.ScheduleGroup;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public record ScheduleInfo(LocalDateTime editDateTime,
                           LocalDate scheduleDate,
                           List<ScheduleGroup> studentsSchedule,
                           List<ScheduleGroup> teachersSchedule,
                           List<String> groupsList,
                           Set<String> teachersList) {
}
