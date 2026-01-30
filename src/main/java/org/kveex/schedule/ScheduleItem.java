package org.kveex.schedule;

public class ScheduleItem {
    private final String time;
    private final String subject;
    private final String teacherName;
    private final String roomNumber;
    private final int subGroup;

    public ScheduleItem(String time, String subject, String teacherName, String roomNumber, int subGroup) {
        this.time = setGoodTime(time);
        this.subject = subject;
        this.teacherName = teacherName;
        this.roomNumber = roomNumber;
        this.subGroup = subGroup;
    }

    public String setGoodTime(String time) {
        String goodTime = time;
        switch (time) {
            case "1,2": {
                goodTime = "8:30 - 10:00";
                break;
            }
            case "3,4": {
                if (roomNumber == null) break;
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

    public String getTime() {
        return time;
    }

    public String getSubject() {
        return subject;
    }

    public String getTeacherName() {
        return teacherName;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public int getSubGroup() {
        return subGroup;
    }
}
