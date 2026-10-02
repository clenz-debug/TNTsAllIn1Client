package com.tntsallin1client.mixin;

import java.util.List;
import java.util.Map;

import com.tntsallin1client.text.ClientTranslations;
import net.minecraft.client.resource.language.TranslationStorage;
import net.minecraft.resource.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds our texts to the game's translation table every time it is (re)built - see {@link ClientTranslations}. */
@Mixin(TranslationStorage.class)
public abstract class TranslationStorageMixin {
	@Shadow
	Map<String, String> translations;

	// `languages` is English first, then the selected language - later entries win, as in vanilla.
	@Inject(method = "load(Lnet/minecraft/resource/ResourceManager;Ljava/util/List;)V", at = @At("RETURN"))
	private void tnt$addClientTranslations(ResourceManager resourceManager, List<String> languages, CallbackInfo ci) {
		for (String language : languages) {
			ClientTranslations.addTo(this.translations, language);
		}
	}
}
