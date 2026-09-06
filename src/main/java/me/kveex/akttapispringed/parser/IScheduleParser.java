package me.kveex.akttapispringed.parser;

import me.kveex.akttapispringed.service.impl.ScheduleParserServiceImpl;

import java.time.LocalDateTime;
import java.util.List;

public interface IScheduleParser {
    void parse();
    LocalDateTime scheduleEditDate();
    List<String> scheduleDateLines();
    List<ScheduleParserServiceImpl.Info> timeAndInfoForScheduleGroup();
    boolean isWholeScheduleDistant();
}
