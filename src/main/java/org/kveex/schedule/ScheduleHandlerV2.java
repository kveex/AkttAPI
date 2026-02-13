package org.kveex.schedule;

import org.kveex.schedule.parser.HTMLScheduleParser;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class ScheduleHandlerV2 {
    private final HTMLScheduleParser htmlScheduleParser;
    public ScheduleHandlerV2(int repeatDelay) throws IOException {
        htmlScheduleParser = new HTMLScheduleParser();
        startUpdateCycle(repeatDelay);
    }

    private void startUpdateCycle(int repeatDelay) {
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    htmlScheduleParser.updateDocument();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        },1000, repeatDelay);
    }

    public LocalDate getScheduleDate() {
        return htmlScheduleParser.getScheduleDate();
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
