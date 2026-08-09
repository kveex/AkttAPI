package me.kveex.akttapispringed.schedule.parser;

import me.kveex.akttapispringed.service.ScheduleParserService;

import java.time.LocalDateTime;
import java.util.List;

public interface IScheduleParser {
    void parse();
    LocalDateTime scheduleEditDate();
    List<String> scheduleDateLines();
    List<ScheduleParserService.Info> timeAndInfoForScheduleGroup();
    boolean isWholeScheduleDistant();
}
