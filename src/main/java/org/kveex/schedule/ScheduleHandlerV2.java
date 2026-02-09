package org.kveex.schedule;

import org.kveex.schedule.parser.HTMLScheduleParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ScheduleHandlerV2 {
    private final HTMLScheduleParser htmlScheduleParser;
    public ScheduleHandlerV2() throws IOException {
        htmlScheduleParser = new HTMLScheduleParser();
    }

    public ScheduleGroup getStudentScheduleGroup(String group) {
        return htmlScheduleParser.getStudentScheduleGroup(group);
    }

    public ScheduleGroup getStudentScheduleGroup(String group, SubGroup subGroup) {
        return htmlScheduleParser.getStudentScheduleGroup(group).getSubGroup(subGroup);
    }

    public List<ScheduleGroup> getSchedule() {
        return htmlScheduleParser.getSchedule();
    }

    public List<String> getGroupsList() {
        return htmlScheduleParser.getAllGroups();
    }

    public ScheduleGroup getTeacherScheduleGroup(String teacherName) {
        return htmlScheduleParser.getTeacherScheduleGroup(teacherName);
    }

    public List<String> getTeachersList() {
        return new ArrayList<>(htmlScheduleParser.getAllTeachers());
    }
}
