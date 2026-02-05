package org.kveex.schedule;

import java.util.ArrayList;
import java.util.List;

public record ScheduleGroup(String scheduleDate, String groupName, List<ScheduleItem> scheduleItems) {
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

    public ScheduleGroup getSubGroup(ScheduleItem.SubGroup subGroup) {
        ScheduleGroup newScheduleGroup = new ScheduleGroup(this.scheduleDate, this.groupName);
        for (ScheduleItem scheduleItem : this.scheduleItems) {
            if (scheduleItem.subGroup() == subGroup || scheduleItem.subGroup() == ScheduleItem.SubGroup.BOTH) {
                newScheduleGroup.add(scheduleItem);
            }
        }
        return newScheduleGroup;
    }
}
