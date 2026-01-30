package org.kveex.api;

import org.apache.commons.cli.*;
import org.kveex.AkttAPI;

public class ArgsParser {
    private int port = 8080;
    private static final int minimumRepeatDelayMs = 30 * 60 * 1000;
    private int repeatDelayMs = minimumRepeatDelayMs;

    public void parse(String[] args) {
        Options options = new Options();

        options.addOption("p", "port", true, "Порт на котором будет запущен REST API (По умолчанию: 8080)");
        options.addOption("r", "repeat-delay", true, "Время задержки между обновлениями документа с расписанием (По умолчанию: 30m)");
        options.addOption("h", "help", false, "Показать справку");

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();

        try {
            CommandLine commandLine = parser.parse(options, args);
            if (commandLine.hasOption("h")) {
                formatter.printHelp(AkttAPI.ID, options);
                System.exit(0);
            }

            if (commandLine.hasOption("p")) {
                port = Integer.parseInt(commandLine.getOptionValue("p"));
            }

            if (commandLine.hasOption("r")) {
                String timeStr = commandLine.getOptionValue("r");
                repeatDelayMs = parseTimeToMillis(timeStr);

                if (repeatDelayMs < minimumRepeatDelayMs) {
                    AkttAPI.LOGGER.warn("Интервал не может быть меньше 30 минут!");
                    repeatDelayMs = minimumRepeatDelayMs;
                }
            }
        } catch (ParseException e) {
            AkttAPI.LOGGER.error("Ошибка парсинга аргументов: {}", e.getMessage());
            formatter.printHelp(AkttAPI.ID, options);
            System.exit(1);
        }
    }

    private int parseTimeToMillis(String timeStr) {
        timeStr = timeStr.toLowerCase().replace(" ", "");
        int totalMs = 0;

        int hIndex = timeStr.indexOf('h');
        if (hIndex != -1) {
            int hours = Integer.parseInt(timeStr.substring(0, hIndex));
            totalMs += hours * 60 * 60 * 1000;
            timeStr = timeStr.substring(hIndex + 1);
        }

        int mIndex = timeStr.indexOf('m');
        if (mIndex != -1) {
            int minutes = Integer.parseInt(timeStr.substring(0, mIndex));
            totalMs += minutes * 60 * 1000;
            timeStr = timeStr.substring(mIndex + 1);
        }

        int sIndex = timeStr.indexOf('s');
        if (sIndex != -1) {
            int seconds = Integer.parseInt(timeStr.substring(0, sIndex));
            totalMs += seconds * 1000;
        }

        return totalMs;
    }

    public int getPort() { return port; }
    public int getRepeatDelayMs() { return repeatDelayMs; }
}
