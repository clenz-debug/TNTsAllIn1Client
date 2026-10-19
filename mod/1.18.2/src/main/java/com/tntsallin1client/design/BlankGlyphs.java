package com.tntsallin1client.design;

import com.mojang.blaze3d.font.GlyphProvider;
import com.mojang.blaze3d.font.TrueTypeGlyphProvider;
import com.tntsallin1client.mixin.TrueTypeGlyphProviderAccessor;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.stb.STBTruetype;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;
import java.util.List;
import java.util.Map;

/**
 * Characters without ink in our own fonts, measured the way 1.19 does it (used by {@code FontSetMixin}).
 * 1.18.2's fonts know two things less, and both hit our fonts (decompiled 1.18.2 and 1.19.2 sources):
 * <ul>
 *   <li>{@code TrueTypeGlyphProvider#getGlyph} has no glyph for a character without ink - it returns null
 *   when the glyph's bitmap box is empty - and {@code FontSet} hard-codes the space (U+0020) to the pixel
 *   font's 4 px. From 1.19 on such a character is an empty glyph with the font's own advance. For the
 *   client font (Inter) every space would have been 4 px instead of 2.4 px - words further apart than in
 *   every other version - and a no-break space a missing-glyph box.</li>
 *   <li>There is no {@code "space"} glyph provider yet (1.19 brought it). {@code font/client_badge.json}
 *   uses one for the nametag logo's step back over the stacked layers and the gap before the name; here
 *   the json leaves it out (1.18.2 would only log "Invalid type: space" and skip it) and the advances
 *   are below - the same numbers {@code designs/branding/make_client_badge.py} writes.</li>
 * </ul>
 * Vanilla's fonts are left alone.
 */
public final class BlankGlyphs {
	private static final String NAMESPACE = "tntsallin1client";
	private static final ResourceLocation BADGE_FONT = new ResourceLocation(NAMESPACE, "client_badge");
	/** client_badge.json's space provider: back over one 9 px logo layer, then the gap before the name. */
	private static final Map<Integer, Float> BADGE_SPACES = Map.of(0xE006, -9.0F, 0xE007, 2.0F);

	private BlankGlyphs() {
	}

	/** The advance of {@code codepoint} in font {@code font} if it is one of ours and the character has no ink, else null. */
	public static @Nullable Float advance(ResourceLocation font, List<GlyphProvider> providers, int codepoint) {
		if (!NAMESPACE.equals(font.getNamespace())) {
			return null;
		}
		if (BADGE_FONT.equals(font)) {
			return BADGE_SPACES.get(codepoint);
		}
		if (!Character.isSpaceChar(codepoint)) {
			return null;
		}
		for (GlyphProvider provider : providers) {
			if (provider instanceof TrueTypeGlyphProvider) {
				Float advance = trueTypeAdvance((TrueTypeGlyphProviderAccessor) provider, codepoint);
				if (advance != null) {
					return advance;
				}
			}
		}
		return null;
	}

	/** What 1.19's {@code TrueTypeGlyphProvider#getGlyph} gives a glyph without ink: its horizontal advance. */
	private static @Nullable Float trueTypeAdvance(TrueTypeGlyphProviderAccessor provider, int codepoint) {
		if (provider.tntsallin1client$skip().contains(codepoint)) {
			return null;
		}
		int index = STBTruetype.stbtt_FindGlyphIndex(provider.tntsallin1client$font(), codepoint);
		if (index == 0) {
			return null;
		}
		try (MemoryStack stack = MemoryStack.stackPush()) {
			IntBuffer advanceWidth = stack.mallocInt(1);
			IntBuffer leftSideBearing = stack.mallocInt(1);
			STBTruetype.stbtt_GetGlyphHMetrics(provider.tntsallin1client$font(), index, advanceWidth, leftSideBearing);
			return advanceWidth.get(0) * provider.tntsallin1client$pointScale() / provider.tntsallin1client$oversample();
		}
	}
}
