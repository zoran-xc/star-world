package top.xcyyds.starworld.forge.debug.console.client;

import top.xcyyds.starworld.forge.debug.console.StarWorldConsoleLogEntry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class StarWorldClientConsoleState {
    private static volatile List<StarWorldConsoleLogEntry> snapshot = List.of();

    private StarWorldClientConsoleState() {
    }

    public static List<StarWorldConsoleLogEntry> getSnapshot() {
        return snapshot;
    }

    public static void setSnapshot(List<StarWorldConsoleLogEntry> entries) {
        snapshot = Collections.unmodifiableList(new ArrayList<>(entries));
    }
}
