package org.kveex;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

public class Main {

    public static final Logger LOGGER = Logger.getLogger("AKTT_API");

    public static void main(String[] args) {
        Document document = null;
        try {
            document = Jsoup.connect("https://aktt.org/raspisaniya/izmenenie-v-raspisanii-dnevnogo-otdeleniya.html").get();
        } catch (IOException e) {
            LOGGER.severe("Something not working!");
        }
        if (document == null) return;
        ScheduleGroup scheduleGroup = ScheduleHandler.fillSchedule(document, "23-14ИС", 1);
        ScheduleHandler.printSchedule(scheduleGroup);
    }
}