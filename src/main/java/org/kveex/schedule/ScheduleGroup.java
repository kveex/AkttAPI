package org.kveex.schedule;

import java.util.ArrayList;
import java.util.List;

public record ScheduleGroup(String scheduleDate, String groupOrTeacherName, List<ScheduleItem> scheduleItems) {
    public ScheduleGroup(String scheduleDate, String group) {
        this(scheduleDate, group, new ArrayList<>());
    }

    public void add(ScheduleItem scheduleItem) {
        this.scheduleItems.add(scheduleItem);
    }

    public void combine(ScheduleGroup scheduleGroup) {
        this.scheduleItems.addAll(scheduleGroup.scheduleItems);
    }

    public boolean contains(ScheduleItem scheduleItem) {
        return scheduleItems.contains(scheduleItem);
    }

    public ScheduleGroup getSubGroup(SubGroup subGroup) {
        ScheduleGroup newScheduleGroup = new ScheduleGroup(this.scheduleDate, this.groupOrTeacherName);
        for (ScheduleItem scheduleItem : this.scheduleItems) {
            if (scheduleItem.subGroup() == subGroup || scheduleItem.subGroup() == SubGroup.BOTH) {
                newScheduleGroup.add(scheduleItem);
            }
        }
        return newScheduleGroup;
    }
}
