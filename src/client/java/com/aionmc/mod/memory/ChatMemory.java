package com.aionmc.mod.memory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class ChatMemory {

    public record Turn(Role role, String content) {
        public enum Role {
            PLAYER,
            ASSISTANT,
            EVENT
        }
    }

    private static final int MAX_TURNS = 10;

    private final Deque<Turn> turns = new ArrayDeque<>();

    public synchronized void addPlayerMessage(String message) {
        add(new Turn(Turn.Role.PLAYER, message));
    }

    public synchronized void addAssistantMessage(String message) {
        add(new Turn(Turn.Role.ASSISTANT, message));
    }

    public synchronized void addEvent(String description) {
        add(new Turn(Turn.Role.EVENT, description));
    }

    private void add(Turn turn) {
        turns.addLast(turn);
        while (turns.size() > MAX_TURNS) {
            turns.removeFirst();
        }
    }

    public synchronized List<Turn> snapshot() {
        return new ArrayList<>(turns);
    }

    public synchronized void clear() {
        turns.clear();
    }

    public synchronized boolean isEmpty() {
        return turns.isEmpty();
    }
}
