package org.kveex;

import java.util.ArrayList;
import java.util.List;

public record ScheduleGroup(String groupName, List<ScheduleItem> scheduleItems) {
    public ScheduleGroup(String group) {
        this(group, new ArrayList<>());
    }

    public ScheduleGroup add(ScheduleItem scheduleItem) {
        this.scheduleItems.add(scheduleItem);
        return this;
    }

    public boolean isEmpty() {
        return scheduleItems.isEmpty();
    }
}
