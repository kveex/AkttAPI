package org.kveex.schedule;

public enum SubGroup {
    FIRST,
    SECOND,
    BOTH;

    public static SubGroup toSubGroup(int subgroup) {
        return switch (subgroup) {
            case 1 -> SubGroup.FIRST;
            case 2 -> SubGroup.SECOND;
            default -> SubGroup.BOTH;
        };
    }
}
