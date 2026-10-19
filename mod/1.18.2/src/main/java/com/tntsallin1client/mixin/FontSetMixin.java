package com.tntsallin1client.mixin;

import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.blaze3d.font.GlyphProvider;
import com.tntsallin1client.design.BlankGlyphs;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.gui.font.FontSet;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.client.gui.font.glyphs.EmptyGlyph;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Optional;

/**
 * Characters without ink in our own fonts get an empty glyph with their real advance, as from 1.19 on -
 * see {@link BlankGlyphs}. {@code getGlyphInfoForSpace} is where 1.18.2 already does exactly that for
 * the space in every font (with the pixel font's 4 px); {@code getGlyph} is what draws, which would
 * otherwise bake the missing glyph for these. Both run per drawn character, so the answer is kept per
 * font set until its next reload. 1.18.2's own {@code glyphInfos}/{@code glyphs} caches are emptied at
 * the start of {@code reload} and filled lazily afterwards, so they only ever see the finished providers.
 */
@Mixin(FontSet.class)
public abstract class FontSetMixin {
	@Unique
	private static final EmptyGlyph TNTSALLIN1CLIENT$EMPTY = new EmptyGlyph();

	@Shadow
	@Final
	private ResourceLocation name;

	@Shadow
	@Final
	private List<GlyphProvider> providers;

	@Unique
	private final Int2ObjectMap<Optional<Float>> tntsallin1client$blankAdvances = new Int2ObjectOpenHashMap<>();

	@Unique
	private Optional<Float> tntsallin1client$blankAdvance(int codepoint) {
		return this.tntsallin1client$blankAdvances.computeIfAbsent(codepoint,
				c -> Optional.ofNullable(BlankGlyphs.advance(this.name, this.providers, c)));
	}

	/**
	 * At the end, not the start: {@code reload} empties {@code providers} first and only fills it again
	 * after asking {@code getGlyphInfoForSpace} about every character - answers from in between would
	 * have been kept as "no blank glyph".
	 */
	@Inject(method = "reload", at = @At("RETURN"))
	private void tntsallin1client$forgetBlankAdvances(List<GlyphProvider> providers, CallbackInfo ci) {
		this.tntsallin1client$blankAdvances.clear();
	}

	@Inject(method = "getGlyphInfoForSpace", at = @At("HEAD"), cancellable = true)
	private void tntsallin1client$blankGlyphInfo(int codepoint, CallbackInfoReturnable<GlyphInfo> cir) {
		Optional<Float> advance = this.tntsallin1client$blankAdvance(codepoint);
		if (advance.isPresent()) {
			float value = advance.get();
			cir.setReturnValue(() -> value);
		}
	}

	@Inject(method = "getGlyph", at = @At("HEAD"), cancellable = true)
	private void tntsallin1client$blankGlyph(int codepoint, CallbackInfoReturnable<BakedGlyph> cir) {
		if (this.tntsallin1client$blankAdvance(codepoint).isPresent()) {
			cir.setReturnValue(TNTSALLIN1CLIENT$EMPTY);
		}
	}
}
