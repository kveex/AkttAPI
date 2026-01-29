package org.kveex;

import java.util.ArrayList;
import java.util.List;

public record ScheduleGroup(String groupName, List<ScheduleItem> scheduleItems) {
    public ScheduleGroup(String group) {
        this(group, new ArrayList<>());
    }

    public void add(ScheduleItem scheduleItem) {
        this.scheduleItems.add(scheduleItem);
    }

    public void combine(ScheduleGroup scheduleGroup) {
        this.scheduleItems.addAll(scheduleGroup.scheduleItems);
    }

    public boolean isEmpty() {
        return scheduleItems.isEmpty();
    }

    public boolean contains(ScheduleItem scheduleItem) {
        return scheduleItems.contains(scheduleItem);
    }
}
