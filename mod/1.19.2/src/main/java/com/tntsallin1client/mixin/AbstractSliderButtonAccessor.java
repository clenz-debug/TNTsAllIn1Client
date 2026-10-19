package com.tntsallin1client.mixin;

import net.minecraft.client.gui.components.AbstractSliderButton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The slider position (0 to 1), for the themed handle drawn in {@code AbstractButtonThemeMixin}. */
@Mixin(AbstractSliderButton.class)
public interface AbstractSliderButtonAccessor {
	@Accessor("value")
	double tntsallin1client$getValue();
}
