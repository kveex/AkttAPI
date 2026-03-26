package org.kveex.schedule;

import java.util.ArrayList;
import java.util.List;

public record LessonGroup(String scheduleDate, String groupName, String teacherName, List<Lesson> lessons) {
    public LessonGroup(String scheduleDate, String groupName, String teacherName) {
        this(scheduleDate, groupName, teacherName, new ArrayList<>());
    }

    public void add(Lesson lesson) {
        this.lessons.add(lesson);
    }

    public void addAll(List<Lesson> lessons) {
        this.lessons.addAll(lessons);
    }

    public void replaceScheduleItems(List<Lesson> lessons) {
        if (this.lessons == lessons) return;

        this.lessons.clear();
        this.lessons.addAll(lessons);
    }

    public LessonGroup getSubGroup(SubGroup subGroup) {
        LessonGroup newLessonGroup = new LessonGroup(this.scheduleDate, this.groupName, this.teacherName);
        if (subGroup == SubGroup.BOTH) return this;
        for (Lesson lesson : this.lessons) {
            if (lesson.subGroup() == subGroup || lesson.subGroup() == SubGroup.BOTH) {
                newLessonGroup.add(lesson);
            }
        }
        return newLessonGroup;
    }
}
