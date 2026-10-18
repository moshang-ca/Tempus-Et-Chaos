package org.moshang.tempusetchaos.api;

import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Multipliers a machine's upgrades apply to its base numbers. {@link #NONE} is the machine without upgrades; the curve
 * that turns upgrade items into these factors belongs to the upgrade system, not to the machine.
 */
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public record EntropyModifiers(float gasRate, float chrononCost, float compensationRate) {
    public static final EntropyModifiers NONE = new EntropyModifiers(1f, 1f, 1f);

    public EntropyModifiers {
        gasRate = Math.max(0f, gasRate);
        chrononCost = Math.max(0f, chrononCost);
        compensationRate = Math.max(0f, compensationRate);
    }
}
