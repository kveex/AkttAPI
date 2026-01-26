package org.kveex;

import java.util.ArrayList;
import java.util.List;

public class ScheduleGroup {
    private final String groupName;
    private final List<ScheduleItem> scheduleItems;

    public ScheduleGroup(String group) {
        this.groupName = group;
        this.scheduleItems = new ArrayList<>();
    }

    public ScheduleGroup(String group, List<ScheduleItem> scheduleItems) {
        this.groupName = group;
        this.scheduleItems = scheduleItems;
    }

    public void add(ScheduleItem scheduleItem) {
        this.scheduleItems.add(scheduleItem);
    }

    public List<ScheduleItem> getScheduleItems() {
        return this.scheduleItems;
    }

    public String getGroupName() {
        return groupName;
    }

    public boolean isEmpty() {
        return scheduleItems.isEmpty();
    }
}
