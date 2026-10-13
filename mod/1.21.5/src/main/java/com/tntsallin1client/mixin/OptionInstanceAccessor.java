package com.tntsallin1client.mixin;

import net.minecraft.client.OptionInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Phase 5j: direct write access to {@code OptionInstance}'s private backing
 * field, bypassing {@code OptionInstance#set(T)}'s call into
 * {@code ValueSet#validateValue(T)}. Needed for fullbright: gamma's ValueSet
 * ({@code OptionInstance.UnitDouble}) hard-rejects anything outside [0.0, 1.0]
 * and silently falls back to the 0.5 default, so the usual "just call set()
 * with a big number" trick (which works for options without a clamped
 * ValueSet) doesn't apply here.
 */
@Mixin(OptionInstance.class)
public interface OptionInstanceAccessor<T> {
	@Accessor("value")
	void tntsallin1client$setValue(T value);
}
