package org.kveex.schedule.parser;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.kveex.AkttAPI;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PDFScheduleParser extends ScheduleParser {
    private static final Pattern GROUP_PATTERN = Pattern.compile("(?<!\\S)\\d{2}-\\d{2}[A-Za-zА-Яа-яЁё\\d]{2,8}(?!\\S)");
    private static final Pattern TIME_PATTERN = Pattern.compile(
            "^(?:\\d,\\d|\\d{1,2}\\.\\d{2}|[Уу][Пп](?:\\.\\d+)?|[Пп][Пп](?:\\.\\d+)?|\\dп)\\b"
    );
    private static final Pattern TIME_SPLIT_PATTERN = Pattern.compile(
            "(?:(?<=^)|(?<=\\s{2,}))(?=\\d,\\d\\b|\\d{1,2}\\.\\d{2}\\b|[Уу][Пп](?:\\.\\d+)?\\b|[Пп][Пп](?:\\.\\d+)?\\b|\\dп\\b)"
    );

    private PDDocument document;
    private List<String> cachedLines;
    private List<Info> cachedInfoList;
    private Set<String> cachedGroups;

    public PDFScheduleParser(byte[] bytes) {
        updateDocument(bytes);
    }

    @Override
    public ScheduleInfo parse() {
        List<LessonInfo> lessons = buildLessonsList();
        return new ScheduleInfo(
                collectScheduleEditDate(),
                collectScheduleDate(),
                lessons,
                provideGroupsList(),
                collectAllTeachers()
        );
    }

    private void updateDocument(byte[] bytes) {
        try {
            document = Loader.loadPDF(bytes);
        } catch (IOException e) {
            AkttAPI.LOGGER.error("Ошибка при загрузке PDF: {}", e.toString());
            document = null;
        }
    }

    private List<String> getLines() {
        if (cachedLines != null) {
            return cachedLines;
        }
        if (document == null) {
            cachedLines = List.of();
            return cachedLines;
        }

        PDFTextStripper stripper = new PDFTextStripper();
        stripper.setSortByPosition(true);
        stripper.setLineSeparator("\n");
        stripper.setWordSeparator(" ");

        String text;
        try {
            text = stripper.getText(document);
        } catch (IOException e) {
            AkttAPI.LOGGER.error("Ошибка при получении текста из PDF: {}", e.toString());
            cachedLines = List.of();
            return cachedLines;
        }

        cachedLines = Arrays.asList(text.split("\n"));
        return cachedLines;
    }

    private void ensureScheduleParsed() {
        if (cachedInfoList != null && cachedGroups != null) {
            return;
        }

        LinkedHashMap<String, List<ScheduleEntryDraft>> drafts = new LinkedHashMap<>();
        ScheduleEntryDraft[] lastEntries = new ScheduleEntryDraft[3];
        String[] currentGroups = new String[3];

        for (String rawLine : getLines()) {
            String normalizedLine = normalize(rawLine);
            if (shouldSkipLine(normalizedLine)) {
                continue;
            }

            List<String> columnSegments = splitColumns(rawLine);
            if (columnSegments.size() > 1) {
                for (int columnIndex = 0; columnIndex < Math.min(3, columnSegments.size()); columnIndex++) {
                    handleSegment(columnSegments.get(columnIndex), columnIndex, currentGroups, lastEntries, drafts);
                }
                continue;
            }

            List<String> groupSegments = splitByGroupStarts(normalizedLine);
            if (groupSegments.size() > 1) {
                for (int columnIndex = 0; columnIndex < Math.min(3, groupSegments.size()); columnIndex++) {
                    handleSegment(groupSegments.get(columnIndex), columnIndex, currentGroups, lastEntries, drafts);
                }
                continue;
            }

            List<String> timeSegments = splitByTimeStarts(rawLine);
            if (timeSegments.size() > 1) {
                for (int columnIndex = 0; columnIndex < Math.min(3, timeSegments.size()); columnIndex++) {
                    handleSegment(timeSegments.get(columnIndex), columnIndex, currentGroups, lastEntries, drafts);
                }
                continue;
            }

            int continuationColumn = detectContinuationColumn(rawLine, currentGroups);
            handleSegment(rawLine, continuationColumn, currentGroups, lastEntries, drafts);
        }

        LinkedHashSet<String> groups = new LinkedHashSet<>();
        List<Info> infoList = new ArrayList<>();

        for (Map.Entry<String, List<ScheduleEntryDraft>> entry : drafts.entrySet()) {
            String groupName = entry.getKey();
            groups.add(groupName);

            for (ScheduleEntryDraft draft : entry.getValue()) {
                String info = normalizeInfo(draft.info.toString());
                if (draft.time.isBlank() || info.isBlank()) {
                    continue;
                }
                infoList.add(new Info(groupName, draft.time, info));
            }
        }

        cachedGroups = groups;
        cachedInfoList = infoList;
    }

    private void handleSegment(String rawSegment,
                               int columnIndex,
                               String[] currentGroups,
                               ScheduleEntryDraft[] lastEntries,
                               LinkedHashMap<String, List<ScheduleEntryDraft>> drafts) {
        if (columnIndex < 0 || columnIndex >= 3) {
            return;
        }

        ParsedSegment segment = parseSegment(rawSegment);
        if (segment.isEmpty()) {
            return;
        }

        if (segment.groupName != null) {
            currentGroups[columnIndex] = segment.groupName;
        }

        String currentGroup = currentGroups[columnIndex];
        if (currentGroup == null) {
            return;
        }

        if (segment.time != null) {
            String info = normalizeInfo(segment.info);
            if (info.isBlank()) {
                return;
            }

            ScheduleEntryDraft draft = new ScheduleEntryDraft(segment.time, info);
            drafts.computeIfAbsent(currentGroup, ignored -> new ArrayList<>()).add(draft);
            lastEntries[columnIndex] = draft;
            return;
        }

        if (segment.info == null || lastEntries[columnIndex] == null) {
            return;
        }

        lastEntries[columnIndex].append(segment.info);
    }

    private ParsedSegment parseSegment(String rawSegment) {
        String segment = normalize(rawSegment);
        if (segment.isBlank()) {
            return ParsedSegment.empty();
        }

        String groupName = null;
        Matcher groupMatcher = GROUP_PATTERN.matcher(segment);
        if (groupMatcher.find() && groupMatcher.start() == 0) {
            groupName = normalizeGroupName(groupMatcher.group());
            segment = normalize(segment.substring(groupMatcher.end()));
        }

        if (segment.isBlank()) {
            return new ParsedSegment(groupName, null, null);
        }

        Matcher timeMatcher = TIME_PATTERN.matcher(segment);
        if (timeMatcher.find()) {
            String time = timeMatcher.group();
            String info = normalize(segment.substring(timeMatcher.end()));
            return new ParsedSegment(groupName, time, info);
        }

        return new ParsedSegment(groupName, null, segment);
    }

    private List<String> splitColumns(String rawLine) {
        String withoutRightSpaces = Objects.toString(rawLine, "").replaceFirst("\\s+$", "");
        if (withoutRightSpaces.isBlank()) {
            return List.of();
        }

        String[] parts = withoutRightSpaces.split("\\s{2,}", -1);
        if (parts.length <= 1 || parts.length > 3) {
            return List.of(normalize(rawLine));
        }
        return Arrays.asList(parts);
    }

    private List<String> splitByGroupStarts(String line) {
        List<String> parts = new ArrayList<>();
        Matcher matcher = GROUP_PATTERN.matcher(line);
        int currentStart = -1;

        while (matcher.find()) {
            if (currentStart != -1) {
                parts.add(normalize(line.substring(currentStart, matcher.start())));
            }
            currentStart = matcher.start();
        }

        if (currentStart != -1) {
            parts.add(normalize(line.substring(currentStart)));
        }

        return parts;
    }

    private List<String> splitByTimeStarts(String line) {
        List<String> parts = new ArrayList<>();
        Matcher matcher = TIME_SPLIT_PATTERN.matcher(line);
        int currentStart = -1;

        while (matcher.find()) {
            if (currentStart != -1) {
                parts.add(normalize(line.substring(currentStart, matcher.start())));
            }
            currentStart = matcher.start();
        }

        if (currentStart != -1) {
            parts.add(normalize(line.substring(currentStart)));
        }

        return parts;
    }

    private int detectContinuationColumn(String rawLine, String[] currentGroups) {
        String line = Objects.toString(rawLine, "");
        String trimmedLeft = line.replaceFirst("^\\s+", "");
        int leadingSpaces = line.length() - trimmedLeft.length();

        if (leadingSpaces >= 60 && currentGroups[2] != null) {
            return 2;
        }
        if (leadingSpaces >= 20 && currentGroups[1] != null) {
            return 1;
        }
        return 0;
    }

    private boolean shouldSkipLine(String line) {
        if (line.isBlank()) {
            return true;
        }

        String lower = line.toLowerCase(Locale.ROOT);
        if (lower.startsWith("зам.")) {
            return true;
        }
        if (lower.startsWith("_______________")) {
            return true;
        }
        if (lower.startsWith("«")) {
            return true;
        }
        return lower.startsWith("группа");
    }

    private static String locateDateString(List<String> lines) {
        for (String line : lines) {
            String normalizedLine = Objects.toString(line, "").toLowerCase(Locale.ROOT).trim();
            if (normalizedLine.contains("расписание на")) {
                return normalizedLine;
            }
        }
        return "";
    }

    private String normalize(String text) {
        return Objects.toString(text, "")
                .replace('\u00A0', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String normalizeInfo(String text) {
        String info = normalize(text);
        if ("-".equals(info) || "–".equals(info)) {
            return "нет пары";
        }
        return info;
    }

    private String normalizeGroupName(String text) {
        return normalize(text)
                .replace("дистант", "")
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    @Override
    public boolean isWholeScheduleDistant() {
        String dateLine = locateDateString(getLines());
        return dateLine.contains("дист");
    }

    @Override
    public Set<String> provideGroupsList() {
        ensureScheduleParsed();
        return new LinkedHashSet<>(cachedGroups);
    }

    @Override
    public LocalDateTime collectScheduleEditDate() {
        if (document == null) {
            return null;
        }

        if (document.getDocumentInformation().getModificationDate() != null) {
            return document.getDocumentInformation().getModificationDate().toInstant()
                    .atZone(ZoneId.of("Europe/Moscow"))
                    .toLocalDateTime();
        }

        if (document.getDocumentInformation().getCreationDate() != null) {
            return document.getDocumentInformation().getCreationDate().toInstant()
                    .atZone(ZoneId.of("Europe/Moscow"))
                    .toLocalDateTime();
        }

        return null;
    }

    @Override
    public List<String> provideScheduleDateLines() {
        return getLines();
    }

    @Override
    public List<Info> provideTimeAndInfoForScheduleGroup() {
        ensureScheduleParsed();
        return new ArrayList<>(cachedInfoList);
    }

    private record ScheduleEntryDraft(String time, StringBuilder info) {
        private ScheduleEntryDraft(String time, String info) {
            this(time, new StringBuilder(info));
        }

        private void append(String extraInfo) {
            String normalizedExtra = Objects.toString(extraInfo, "").trim();
            if (normalizedExtra.isBlank()) {
                return;
            }
            if (!info.isEmpty()) {
                info.append(' ');
            }
            info.append(normalizedExtra);
        }
    }

    private record ParsedSegment(String groupName, String time, String info) {
        private static ParsedSegment empty() {
            return new ParsedSegment(null, null, null);
        }

        private boolean isEmpty() {
            return groupName == null && time == null && (info == null || info.isBlank());
        }
    }
}
