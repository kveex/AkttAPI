package me.kveex.akttapispringed.service;

import me.kveex.akttapispringed.service.impl.ScheduleParserServiceImpl;

import java.time.LocalDateTime;
import java.util.List;

public interface ScheduleParserService {
    void parse(LocalDateTime editTimeStamp, List<String> scheduleDateLines, List<ScheduleParserServiceImpl.Info> timeAndInfoForScheduleGroup, boolean isWholeScheduleDistant);
}
