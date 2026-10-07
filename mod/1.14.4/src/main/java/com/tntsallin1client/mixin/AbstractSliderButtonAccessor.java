package com.tntsallin1client.mixin;

import net.minecraft.client.gui.components.AbstractSliderButton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Client design: where a slider's knob is (0 to 1) - needed to draw the knob in the theme, see `AbstractWidgetThemeMixin`. */
@Mixin(AbstractSliderButton.class)
public interface AbstractSliderButtonAccessor {
	@Accessor("value")
	double tntsallin1client$getValue();
}
