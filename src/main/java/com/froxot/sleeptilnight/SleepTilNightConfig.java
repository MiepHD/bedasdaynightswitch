package com.froxot.sleeptilnight;

import com.hypixel.hytale.codec.Codec; // Careful to not use other Codec imports
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

public class SleepTilNightConfig {
    public static final BuilderCodec<SleepTilNightConfig> CODEC = BuilderCodec.builder(SleepTilNightConfig.class, SleepTilNightConfig::new)
            .append(new KeyedCodec<Float>("AfternoonWakeUpHour", Codec.FLOAT),
                    (config, value) -> config.afternoonWakeUpHour = value, // Setter
                    (config) -> config.afternoonWakeUpHour).add() // Getter
            .build();


    private float afternoonWakeUpHour = 19.5F;

    public SleepTilNightConfig() {
    }

    public float getAfternoonWakeUpHour() {
        return afternoonWakeUpHour;
    }

    public void setAfternoonWakeUpHour(float afternoonWakeUpHour) {
        this.afternoonWakeUpHour = afternoonWakeUpHour;
    }
}