package me.kveex.akttapispringed.domain.entity;

public enum Subgroup {
    FIRST,
    SECOND,
    BOTH;

    public static Subgroup fromString(String subgroup) {
        return switch (subgroup) {
            case "1", "1п" -> FIRST;
            case "2", "2п" -> SECOND;
            default -> BOTH;
        };
    }
}
