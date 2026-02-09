package org.kveex.schedule;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

//TODO: Разделить преподавателя и группу в два разных значения
public record ScheduleItem(String time, String subjectName, String teacherOrGroupName, String roomNumber, SubGroup subGroup) {
    public ScheduleItem(String time, String subjectName, String teacherOrGroupName, String roomNumber, SubGroup subGroup) {
        this.subjectName = subjectName;
        this.teacherOrGroupName = teacherOrGroupName;
        this.roomNumber = roomNumber;
        this.subGroup = subGroup;
        this.time = setGoodTime(time);
    }

    private String setGoodTime(String time) {
        String goodTime = time;
        switch (time) {
            case "1,2": {
                goodTime = "8:30 - 10:00";
                break;
            }
            case "3,4": {
                boolean isInSecondCampus = true;
                try {
                    int roomNum = Integer.parseInt(roomNumber.replace("а", "").replace("б", ""));
                    isInSecondCampus = roomNum > 40 && roomNum < 100;
                } catch (NumberFormatException ignored) {}
                goodTime = isInSecondCampus ? "10:10 - 11:40" : "[10:10 - 10:45 () 11:15 - 12:00]";
                break;
            }
            case "5,6": {
                goodTime = "12:10 - 13:40";
                break;
            }
            case "7,8": {
                goodTime = "13:50 - 15:20";
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
        int teacherNamePartsAmount = this.teacherOrGroupName.split(" ").length;
        String[] teacherNames = this.teacherOrGroupName.split(",");
        List<String> finalTeacherNames = new ArrayList<>();
        for (String teacherName : teacherNames) {
            finalTeacherNames.add(teacherName.strip());
        }
        return teacherNamePartsAmount >= 2 ? finalTeacherNames : null;
    }

    public static int toInt(@NotNull SubGroup subGroup) {
        return switch (subGroup) {
            case FIRST -> 1;
            case SECOND -> 2;
            default -> 0;
        };
    }

    public int timeToInt() {
        return switch (time) {
            case "10:10 - 11:40", "[10:10 - 10:45 () 11:15 - 12:00]" -> 1;
            case "12:10 - 13:40" -> 2;
            case "13:50 - 15:20" -> 3;
            default -> 0;
        };
    }
}
