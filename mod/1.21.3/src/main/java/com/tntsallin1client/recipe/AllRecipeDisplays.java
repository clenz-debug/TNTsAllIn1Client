package com.tntsallin1client.recipe;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.tntsallin1client.TNTsAllIn1ClientMod;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.VanillaPackResources;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Every crafting recipe display in the game, not just the player's unlocked ones - the fallback
 * {@link PinnedRecipeManager#findSubIngredients} searches when the client's own recipe book has no
 * recipe for a sub-ingredient.
 *
 * <p>Needed because the server only ever sends the client the recipes the player has already
 * unlocked ({@code ServerRecipeBook#sendInitialRecipeBook} iterates its {@code known} set), and many
 * sub-ingredient recipes unlock late: e.g. red dye's four recipes each unlock only once the player
 * has held that specific flower, while red concrete powder already unlocks with sand or gravel - so
 * pinning the powder found no dye recipe at all.
 *
 * <p>Singleplayer reads the integrated server's full recipe manager (includes data packs).
 * Multiplayer has no such access - there it parses the vanilla recipe files bundled in the game jar
 * once per connection (cached against the connection's {@link RegistryAccess}), which covers every
 * vanilla recipe but not a server's custom ones (those still show once unlocked, via the recipe book).
 */
final class AllRecipeDisplays {
	private static @Nullable RegistryAccess cachedFor;
	private static List<RecipeDisplay> cachedVanilla = List.of();

	private AllRecipeDisplays() {
	}

	static List<RecipeDisplay> get(Minecraft client) {
		MinecraftServer server = client.getSingleplayerServer();
		if (server != null) {
			List<RecipeDisplay> displays = new ArrayList<>();
			for (RecipeHolder<?> holder : server.getRecipeManager().getRecipes()) {
				displays.addAll(holder.value().display());
			}
			return displays;
		}

		if (client.level == null) {
			return List.of();
		}
		RegistryAccess registryAccess = client.level.registryAccess();
		if (registryAccess != cachedFor) {
			cachedVanilla = parseVanillaRecipes(registryAccess);
			cachedFor = registryAccess;
		}
		return cachedVanilla;
	}

	private static List<RecipeDisplay> parseVanillaRecipes(RegistryAccess registryAccess) {
		RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, registryAccess);
		List<RecipeDisplay> displays = new ArrayList<>();
		try (VanillaPackResources vanilla = ServerPacksSource.createVanillaPackSource()) {
			vanilla.listResources(PackType.SERVER_DATA, "minecraft", "recipe", (id, resource) -> {
				try (InputStream in = resource.get(); Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
					// Recipes that don't parse against this connection's registries (e.g. items behind a
					// disabled feature flag) are simply skipped.
					Recipe.CODEC.parse(ops, JsonParser.parseReader(reader)).result()
							.ifPresent(recipe -> displays.addAll(recipe.display()));
				} catch (Exception e) {
					TNTsAllIn1ClientMod.LOGGER.debug("[{}] Skipping vanilla recipe {}.", TNTsAllIn1ClientMod.MOD_ID, id, e);
				}
			});
		}
		return displays;
	}
}
