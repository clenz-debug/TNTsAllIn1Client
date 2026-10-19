package com.tntsallin1client.mixin;

import com.mojang.blaze3d.font.TrueTypeGlyphProvider;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.lwjgl.stb.STBTTFontinfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** What {@link com.tntsallin1client.design.BlankGlyphs} needs to measure a character the provider has no glyph for. */
@Mixin(TrueTypeGlyphProvider.class)
public interface TrueTypeGlyphProviderAccessor {
	@Accessor("font")
	STBTTFontinfo tntsallin1client$font();

	@Accessor("oversample")
	float tntsallin1client$oversample();

	@Accessor("pointScale")
	float tntsallin1client$pointScale();

	@Accessor("skip")
	IntSet tntsallin1client$skip();
}
