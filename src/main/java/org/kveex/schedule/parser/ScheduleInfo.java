package org.kveex.schedule.parser;

import org.kveex.schedule.ScheduleGroup;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public record ScheduleInfo(LocalDateTime editDateTime,
                           LocalDate scheduleDate,
                           List<ScheduleGroup> fullSchedule,
                           List<String> groupsList,
                           Set<String> teachersList) {

    public ScheduleInfo(LocalDateTime editDateTime,
                        LocalDate scheduleDate,
                        List<ScheduleGroup> fullSchedule,
                        List<String> groupsList) {
        this(editDateTime, scheduleDate, fullSchedule, groupsList, new HashSet<>());
    }

    public void setTeachersList(Set<String> newTeachersList) {
        teachersList.clear();
        teachersList.addAll(newTeachersList);
    }
}
