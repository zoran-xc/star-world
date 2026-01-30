package top.xcyyds.starworld.forge;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import top.xcyyds.starworld.common.StarWorldCommon;

@Mod(StarWorldForgeMod.MOD_ID)
public final class StarWorldForgeMod {
    public static final String MOD_ID = StarWorldCommon.MOD_ID;

    private static final Logger LOGGER = LogUtils.getLogger();

    public StarWorldForgeMod() {
        StarWorldCommon.init(new ForgePlatform(LOGGER));
    }
}
