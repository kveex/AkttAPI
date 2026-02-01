package org.kveex.schedule;

import org.jetbrains.annotations.NotNull;

public record ScheduleItem(String time, String subject, String teacherName, String roomNumber, SubGroup subGroup) {
    public enum SubGroup {
        FIRST,
        SECOND,
        BOTH
    }

    public ScheduleItem(String time, String subject, String teacherName, String roomNumber, SubGroup subGroup) {
        this.subject = subject;
        this.teacherName = teacherName;
        this.roomNumber = roomNumber;
        this.subGroup = subGroup;
        this.time = setGoodTime(time);
    }

    public String setGoodTime(String time) {
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

    public static int toInt(@NotNull SubGroup subGroup) {
        int result;
        switch (subGroup) {
            case FIRST -> result = 1;
            case SECOND -> result = 2;
            default -> result = 0;
        }
        return result;
    }
}
