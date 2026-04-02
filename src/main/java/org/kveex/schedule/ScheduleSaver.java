package org.kveex.schedule;

import org.jooq.exception.DataAccessException;
import org.kveex.AkttAPI;
import org.kveex.database.DatabaseController;
import org.kveex.schedule.parser.HTMLScheduleParser;
import org.kveex.schedule.parser.PDFScheduleParser;
import org.kveex.schedule.parser.ScheduleInfo;

import java.sql.SQLDataException;
import java.sql.SQLException;
import java.util.Timer;
import java.util.TimerTask;

public class ScheduleSaver {
    private static HTMLScheduleParser htmlScheduleParser;

    public ScheduleSaver(int repeatDelayMills) {
        htmlScheduleParser = new HTMLScheduleParser();
        startUpdateCycle(repeatDelayMills);
    }

    private void startUpdateCycle(int repeatDelay) {
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    trySaveHTML();
                } catch (SQLException e) {
                    AkttAPI.LOGGER.error("Не удалось отправить расписание в базу данных! Причина: {}", e.getMessage());
                }
            }
        },1000, repeatDelay);
    }

    private void trySaveHTML() throws SQLException {
        ScheduleInfo info = htmlScheduleParser.parse();
        saveToDatabase(info);
    }

    public static void trySavePDF(byte[] bytes) throws SQLException {
        ScheduleInfo info = new PDFScheduleParser(bytes).parse();
        saveToDatabase(info);
    }

    private static void saveToDatabase(ScheduleInfo info) throws SQLException {
        try {
            DatabaseController.getInstance().insertSchedule(info);
        } catch (DataAccessException e) {
            SQLException sqlEx = (SQLException) e.getCause();
            String state = sqlEx != null ? sqlEx.getSQLState() : null;

            if ("23505".equals(state)) {
                throw new SQLDataException("Данное расписание [%s] уже есть в базе данных: %s".formatted(info.editDateTime(), e.getMessage()));
            } else {
                throw new SQLException("Ошибка БД (SQLState=%s): %s".formatted(state, e.getMessage()));
            }
        }

        AkttAPI.notifyAboutUpdate();
        AkttAPI.LOGGER.info("Новое расписание сохранено! Дата и время изменения: {}", info.editDateTime());
    }
}
