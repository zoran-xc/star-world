package top.xcyyds.starworld.forge.debug.console;

import net.minecraft.server.level.ServerLevel;

public final class StarWorldConsole {
    private StarWorldConsole() {
    }

    public static void log(ServerLevel level, String source, String message) {
        StarWorldConsoleSavedData data = StarWorldConsoleSavedData.getOrCreate(level);
        data.append(level.getGameTime(), source, message);
    }
}
