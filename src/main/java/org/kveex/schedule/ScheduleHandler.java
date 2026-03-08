package org.kveex.schedule;

import org.kveex.AkttAPI;
import org.kveex.schedule.parser.HTMLScheduleParser;
import org.kveex.schedule.parser.ScheduleInfo;
import org.kveex.schedule.parser.ScheduleParser;

import java.time.LocalDate;
import java.util.*;

public class ScheduleHandler {
    private static ScheduleHandler INSTANCE = null;
    private final HTMLScheduleParser htmlScheduleParser;
    private volatile ScheduleInfo info;

    private ScheduleHandler(int repeatDelay) {
        htmlScheduleParser = new HTMLScheduleParser();
        startUpdateCycle(repeatDelay);
    }

    public static synchronized void initialize(int repeatDelay) {
        if (INSTANCE == null) {
            INSTANCE = new ScheduleHandler(repeatDelay);
        } else {
            AkttAPI.LOGGER.warn("ScheduleHandler уже инициализирован!");
        }
    }

    public static synchronized ScheduleHandler getInstance() {
        if (INSTANCE == null) throw new RuntimeException("ScheduleHandler не был инициализирован!");
        return INSTANCE;
    }

    private void startUpdateCycle(int repeatDelay) {
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                    info = htmlScheduleParser.parse();
                    info.setTeachersList(collectAllTeachers());
            }
        },1000, repeatDelay);
    }

    public LocalDate getScheduleDate() {
        return info.scheduleDate();
    }

    public ScheduleGroup getStudentScheduleGroup(String groupName) {
        if (info.groupsList().contains(groupName.toLowerCase())) {
            for (ScheduleGroup group : info.fullSchedule()) {
                if (!group.groupName().equals(groupName.toLowerCase())) continue;
                return group;
            }
        }
        throw new IllegalArgumentException("Группа [%s] не найдена!".formatted(groupName));
    }

    public ScheduleGroup getStudentScheduleGroup(String group, SubGroup subGroup) {
        return getStudentScheduleGroup(group).getSubGroup(subGroup);
    }

    public List<ScheduleGroup> getSchedule() {
        return info.fullSchedule();
    }

    public List<String> getGroupsList() {
        return info.groupsList();
    }

    public ScheduleGroup getTeacherScheduleGroup(String teacherName) {
        ScheduleGroup teacherScheduleGroup = new ScheduleGroup(info.scheduleDate().toString(), null, teacherName);

        if (!info.teachersList().contains(teacherName)) return teacherScheduleGroup;

        for (ScheduleGroup group : info.fullSchedule()) {
            for (ScheduleItem item : group.scheduleItems()) {
                List<String> teachers = item.teacherNames();
                if (teachers == null || !teachers.contains(teacherName)) continue;
                ScheduleItem newItem = new ScheduleItem(item.time(), item.subjectName(), group.groupName(), teacherName, item.roomNumber(), item.subGroup(), ScheduleItemState.OK, info.scheduleDate());
                teacherScheduleGroup.add(newItem);
            }
        }

        var teacherScheduleItems = ScheduleParser.sortScheduleItems(teacherScheduleGroup);

        teacherScheduleGroup.replaceScheduleItems(teacherScheduleItems);

        return teacherScheduleGroup;
    }

    /**
     * Собирает имена преподавателей из всех групп
     * @return Список
     */
    private LinkedHashSet<String> collectAllTeachers() {
        LinkedHashSet<String> teacherNames = new LinkedHashSet<>();

        for (ScheduleGroup scheduleGroup : info.fullSchedule()) {
            for (ScheduleItem scheduleItem : scheduleGroup.scheduleItems()) {
                List<String> teacherName = scheduleItem.teacherNames();
                if (teacherName == null) continue;
                for (String name : teacherName) {
                    if (name.isBlank()) continue;
                    if (name.contains("указан")) continue;
                    teacherNames.add(name);
                }
            }
        }
        return teacherNames;
    }

    public List<String> getTeachersList() {
        return new ArrayList<>(info.teachersList());
    }
}
