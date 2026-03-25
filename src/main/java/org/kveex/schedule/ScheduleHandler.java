package org.kveex.schedule;

import org.kveex.AkttAPI;
import org.kveex.schedule.parser.HTMLScheduleParser;
import org.kveex.schedule.parser.ScheduleInfo;

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

    public synchronized void setInfo(ScheduleInfo newInfo) {
        boolean isTomorrow = newInfo.scheduleDate().isAfter(info.scheduleDate());
        boolean isYesterday = newInfo.scheduleDate().isBefore(info.scheduleDate());

        if (isYesterday) {
            AkttAPI.LOGGER.error("Расписание не было загружено, так как оно предназначено для дня, что уже прошёл");
            return;
        }
        if (!isTomorrow) {
            if (info.equals(newInfo)) {
                AkttAPI.LOGGER.error("Расписания одинаковые и не нуждаются в обновлении");
                return;
            }
        }

        info = newInfo;
        AkttAPI.LOGGER.info("Новое расписание было загружено");
    }

    private void startUpdateCycle(int repeatDelay) {
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                    info = htmlScheduleParser.parse();
            }
        },1000, repeatDelay);
    }

    public LocalDate getScheduleDate() {
        return info.scheduleDate();
    }

    public ScheduleGroup getStudentScheduleGroup(String groupName) throws IllegalArgumentException {
        if (info.groupsList().contains(groupName.toLowerCase())) {
            for (ScheduleGroup group : info.studentsSchedule()) {
                if (!group.groupName().equals(groupName.toLowerCase())) continue;
                return group;
            }
        }
        throw new IllegalArgumentException("Группа [%s] не найдена!".formatted(groupName));
    }

    public ScheduleGroup getStudentScheduleGroup(String group, SubGroup subGroup) {
        return getStudentScheduleGroup(group).getSubGroup(subGroup);
    }

    public List<ScheduleGroup> getStudentsSchedule() {
        return info.studentsSchedule();
    }

    public List<ScheduleGroup> getTeachersSchedule() {
        return info.teachersSchedule();
    }

    public List<String> getGroupsList() {
        return info.groupsList();
    }

    public ScheduleGroup getTeacherScheduleGroup(String teacherName) throws IllegalArgumentException {
        if (info.teachersList().contains(teacherName)) {
            for (ScheduleGroup group : info.teachersSchedule()) {
                System.out.println(group);
                if (!group.teacherName().equals(teacherName)) continue;
                System.out.println("found: " + group);
                return group;
            }
        }
        throw new IllegalArgumentException("Преподаватель [%s] не найден!".formatted(teacherName));
    }

    public List<String> getTeachersList() {
        return new ArrayList<>(info.teachersList());
    }
}
