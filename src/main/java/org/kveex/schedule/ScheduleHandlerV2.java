package org.kveex.schedule;

import org.kveex.schedule.parser.HTMLScheduleParser;

import java.io.IOException;
import java.util.List;

public class ScheduleHandlerV2 {
    private final HTMLScheduleParser htmlScheduleParser;
    public ScheduleHandlerV2() throws IOException {
        htmlScheduleParser = new HTMLScheduleParser();
    }

    public ScheduleGroup getScheduleGroup(String group) {
        return htmlScheduleParser.getScheduleGroup(group);
    }

    public ScheduleGroup getScheduleGroup(String group, ScheduleItem.SubGroup subGroup) {
        return htmlScheduleParser.getScheduleGroup(group).getSubGroup(subGroup);
    }

    public List<ScheduleGroup> getSchedule() {
        return htmlScheduleParser.getSchedule();
    }

    public List<String> getGroupsList() {
        return htmlScheduleParser.getAllGroups();
    }
}
