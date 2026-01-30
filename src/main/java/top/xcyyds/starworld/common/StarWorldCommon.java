package top.xcyyds.starworld.common;

public final class StarWorldCommon {
    public static final String MOD_ID = "starworld";

    private static volatile boolean initialized = false;

    private StarWorldCommon() {
    }

    public static void init(StarWorldPlatform platform) {
        if (initialized) {
            return;
        }
        initialized = true;

        platform.logInfo("[" + MOD_ID + "] common init, platform=" + platform.platformName());
    }
}
