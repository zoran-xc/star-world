package top.xcyyds.starworld.forge;

import org.slf4j.Logger;
import top.xcyyds.starworld.common.StarWorldPlatform;

public final class ForgePlatform implements StarWorldPlatform {
    private final Logger logger;

    public ForgePlatform(Logger logger) {
        this.logger = logger;
    }

    @Override
    public String platformName() {
        return "Forge";
    }

    @Override
    public void logInfo(String message) {
        logger.info(message);
    }
}
