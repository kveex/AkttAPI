package org.kveex;

import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

public class Main {
    public static final Logger LOGGER = Logger.getLogger("AKTT_API");

    public static void main(String[] args) {
        ScheduleHandler scheduleHandler;
        try {
            scheduleHandler = new ScheduleHandler();
        } catch (IOException e) {
            throw new RuntimeException("ScheduleHandler broken!\n" + e);
        }

        ScheduleGroup scheduleGroup = scheduleHandler.fillSchedule("23-36БУХ", 1);
        ScheduleGroup scheduleGroup3 = scheduleHandler.fillSchedule("25-40ТМ", 1);
        ScheduleGroup scheduleGroup1 = scheduleHandler.fillSchedule("25-53СВ", 1);
        ScheduleGroup scheduleGroup2 = scheduleHandler.fillSchedule("23-14ИС", 1);
        ScheduleHandler.printSchedule(scheduleGroup, scheduleGroup1, scheduleGroup2, scheduleGroup3);
    }
}