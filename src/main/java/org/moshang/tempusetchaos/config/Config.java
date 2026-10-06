package org.moshang.tempusetchaos.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class Config {
    public static final Config CONFIG;
    public static final ModConfigSpec SPEC;

    public final ModConfigSpec.BooleanValue disableAnimations;

    static {
        Pair<Config, ModConfigSpec> pair = new ModConfigSpec.Builder().configure(Config::new);
        CONFIG = pair.getLeft();
        SPEC = pair.getRight();
    }

    private Config(ModConfigSpec.Builder builder) {
        builder.comment("Client settings").push("client");
        disableAnimations = builder
                .comment("Turn off every ui animation. Meant for machines that cannot keep up with them.")
                .define("disableAnimations", false);

        builder.pop();
    }
}
