package org.kveex;

public record ScheduleItem(String time, String subject, String teacherName, String roomNumber) {
    /**
     * Возвращает строку с уроками, как промежуток времени, занимаемый учебной парой
     * @return Строку с промежутком времени соответствующим урокам
     */
    public String goodTime() {
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
}
