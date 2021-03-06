package mcjty.rftools.blocks.environmental.modules;

import mcjty.rftools.PlayerBuff;
import mcjty.rftools.blocks.environmental.EnvironmentalConfiguration;

public class BlindnessEModule extends PotionEffectModule {

    public BlindnessEModule() {
        super("blindness", 0);
    }

    @Override
    public float getRfPerTick() {
        return EnvironmentalConfiguration.BLINDNESS_RFPERTICK;
    }

    @Override
    protected PlayerBuff getBuff() {
        return PlayerBuff.BUFF_BLINDNESS;
    }

    @Override
    protected boolean allowedForPlayers() {
        return EnvironmentalConfiguration.blindnessAvailable;
    }
}
