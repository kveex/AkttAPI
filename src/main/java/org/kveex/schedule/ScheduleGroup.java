package org.kveex.schedule;

import java.util.ArrayList;
import java.util.List;

public record ScheduleGroup(String scheduleDate, String groupName, String teacherName, List<ScheduleItem> scheduleItems) {
    public ScheduleGroup(String scheduleDate, String groupName, String teacherName) {
        this(scheduleDate, groupName, teacherName, new ArrayList<>());
    }

    public void add(ScheduleItem scheduleItem) {
        this.scheduleItems.add(scheduleItem);
    }

    public void addAll(List<ScheduleItem> scheduleItems) {
        this.scheduleItems.addAll(scheduleItems);
    }

    public void replaceScheduleItems(List<ScheduleItem> scheduleItems) {
        if (this.scheduleItems == scheduleItems) return;

        this.scheduleItems.clear();
        this.scheduleItems.addAll(scheduleItems);
    }

    public ScheduleGroup getSubGroup(SubGroup subGroup) {
        ScheduleGroup newScheduleGroup = new ScheduleGroup(this.scheduleDate, this.groupName, this.teacherName);
        if (subGroup == SubGroup.BOTH) return this;
        for (ScheduleItem scheduleItem : this.scheduleItems) {
            if (scheduleItem.subGroup() == subGroup || scheduleItem.subGroup() == SubGroup.BOTH) {
                newScheduleGroup.add(scheduleItem);
            }
        }
        return newScheduleGroup;
    }
}
