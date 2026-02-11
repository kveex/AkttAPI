package org.kveex.schedule;

import java.util.ArrayList;
import java.util.List;

//TODO: Разделить преподавателя и группу в два разных значения
public record ScheduleGroup(String scheduleDate, String groupOrTeacherName, List<ScheduleItem> scheduleItems) {
    public ScheduleGroup(String scheduleDate, String groupOrTeacherName) {
        this(scheduleDate, groupOrTeacherName, new ArrayList<>());
    }

    public void add(ScheduleItem scheduleItem) {
        this.scheduleItems.add(scheduleItem);
    }

    public void replaceScheduleItems(List<ScheduleItem> scheduleItems) {
        for (int i = 0; i < scheduleItems.size(); i++) {
            this.scheduleItems.set(i, scheduleItems.get(i));
        }
    }

    public void combine(ScheduleGroup scheduleGroup) {
        this.scheduleItems.addAll(scheduleGroup.scheduleItems);
    }

    public boolean contains(ScheduleItem scheduleItem) {
        return scheduleItems.contains(scheduleItem);
    }

    public ScheduleGroup getSubGroup(SubGroup subGroup) {
        ScheduleGroup newScheduleGroup = new ScheduleGroup(this.scheduleDate, this.groupOrTeacherName);
        if (subGroup == SubGroup.BOTH) return this;
        for (ScheduleItem scheduleItem : this.scheduleItems) {
            if (scheduleItem.subGroup() == subGroup || scheduleItem.subGroup() == SubGroup.BOTH) {
                newScheduleGroup.add(scheduleItem);
            }
        }
        return newScheduleGroup;
    }
}
