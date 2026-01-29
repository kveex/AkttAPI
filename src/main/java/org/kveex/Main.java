package org.kveex;

import java.io.IOException;
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

        ScheduleGroup scheduleGroup2 = scheduleHandler.createSchedule("23-14ИС", 0);
        ScheduleHandler.printSchedule(scheduleGroup2);
    }
}