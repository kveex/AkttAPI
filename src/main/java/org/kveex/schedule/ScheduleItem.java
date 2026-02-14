package org.kveex.schedule;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

//TODO: Разделить преподавателя и группу в два разных значения
//TODO: Добавить Enum который будет указывать тип итогового предмета:
// OK - всё в порядке, информация есть,
// EMPTY - нет пары или ничего не указано
public record ScheduleItem(String time, String subjectName, String groupName, String teacherName, String roomNumber, SubGroup subGroup, ScheduleItemState state) {
    public ScheduleItem(String time, String subjectName, String groupName, String teacherName, String roomNumber, SubGroup subGroup, ScheduleItemState state) {
        this.subjectName = subjectName;
        this.groupName = groupName;
        this.teacherName = teacherName;
        this.roomNumber = roomNumber;
        this.subGroup = subGroup;
        this.time = setGoodTime(time);
        this.state = state;
    }

    private String setGoodTime(String time) {
        String goodTime = time;
        boolean todayIsSaturday = LocalDate.now().getDayOfWeek() == DayOfWeek.SATURDAY;
        switch (time) {
            case "1,2": {
                goodTime = !todayIsSaturday ? "8:30 - 10:00" : "8:00 - 9:10";
                break;
            }
            case "3,4": {
                boolean isInSecondCampus = true;
                try {
                    int roomNum = Integer.parseInt(roomNumber.replace("а", "").replace("б", ""));
                    isInSecondCampus = roomNum > 35 && roomNum < 85;
                } catch (NumberFormatException ignored) {}
                if (todayIsSaturday) {
                    goodTime = "9:20 - 10:30";
                    break;
                }
                goodTime = isInSecondCampus ? "10:10 - 11:40" : "[10:10 - 10:45 () 11:15 - 12:00]";
                break;
            }
            case "5,6": {
                goodTime = !todayIsSaturday ? "12:10 - 13:40" : "10:40 - 11:50";
                break;
            }
            case "7,8": {
                goodTime = !todayIsSaturday ? "13:50 - 15:20" : "12:00 - 13:10";
                break;
            }
        }
        return goodTime;
    }

    /**
     * Метод для возвращения имени и инициалов преподавателя
     * @return пара строк с фамилией и инициалами преподавателя
     */
    public List<String> teacherNames() {
        int teacherNamePartsAmount = this.teacherName.split(" ").length;
        String[] teacherNames = this.teacherName.split(",");
        List<String> finalTeacherNames = new ArrayList<>();
        for (String teacherName : teacherNames) {
            finalTeacherNames.add(teacherName.strip());
        }
        return teacherNamePartsAmount >= 2 ? finalTeacherNames : null;
    }

    public int timeToInt() {
        return switch (time) {
            case "10:10 - 11:40", "[10:10 - 10:45 () 11:15 - 12:00]", "9:20 - 10:30" -> 1;
            case "12:10 - 13:40", "10:40 - 11:50" -> 2;
            case "13:50 - 15:20", "12:00 - 13:10" -> 3;
            default -> 0;
        };
    }
}
