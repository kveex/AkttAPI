package org.kveex.schedule.type;

public record Pair<A, B>(A first, B second) {
    public A getFirst() { return first; }
    public B getSecond() { return second; }
}
