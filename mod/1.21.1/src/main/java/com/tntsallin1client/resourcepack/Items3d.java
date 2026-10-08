package com.tntsallin1client.resourcepack;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.tntsallin1client.TNTsAllIn1ClientMod;
import com.tntsallin1client.config.ClientConfig;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.PreparableModelLoadingPlugin;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.FastColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GrassColor;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * "3D items in inventory & hand" for the 3D blocks pack. The newer versions do this with a second
 * pack of item definitions that say "flat in the inventory and the hand, 3D elsewhere"; 1.21.1 has no
 * item definitions yet (they came with 1.21.4) - an item has one model for every place it is shown in.
 * So the switch is ours, as in the 1.14.4 version: the pack lists the items it changes in
 * {@code assets/tntsallin1client/flat_items.json} and carries a copy of the model the game would show
 * for each without the pack ({@code resourcepacks/3d-blocks/early.py}); while the switch is off, those
 * copies are handed out in the inventory and the hand ({@code ItemModelShaperMixin}, with
 * {@code GuiGraphicsMixin} and {@code ItemInHandRendererMixin} marking those places).
 *
 * <p>The same file names the items tinted like grass (the sugar cane) - the newer versions say that
 * in the item definition, here it is a color provider. Only the 3D model has a tinted layer.
 */
public final class Items3d {
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final ResourceLocation LIST = ResourceLocation.fromNamespaceAndPath(TNTsAllIn1ClientMod.MOD_ID, "flat_items.json");
	private static final String FLAT_FOLDER = "item/flat/";

	/** What the pack's list says - empty without the pack. */
	private record Listed(Set<String> items, Map<String, double[]> grassTinted) {
		static final Listed NONE = new Listed(Set.of(), Map.of());
	}

	private static Listed listed = Listed.NONE;
	/** How many "flat places" (inventory slot, hand) are being drawn right now, nested. */
	private static int flatPlaces;

	private Items3d() {
	}

	public static void register() {
		PreparableModelLoadingPlugin.register(Items3d::read, Items3d::addFlatModels);
		ColorProviderRegistry.ITEM.register(Items3d::tint, Items.SUGAR_CANE);
	}

	private static CompletableFuture<Listed> read(ResourceManager manager, Executor executor) {
		return CompletableFuture.supplyAsync(() -> {
			Optional<Resource> resource = manager.getResource(LIST);
			if (resource.isEmpty()) {
				return Listed.NONE;
			}
			try (Reader reader = resource.get().openAsReader()) {
				JsonObject list = JsonParser.parseReader(reader).getAsJsonObject();
				Set<String> items = new HashSet<>();
				for (JsonElement item : list.getAsJsonArray("items")) {
					items.add(item.getAsString());
				}
				Map<String, double[]> grassTinted = new HashMap<>();
				if (list.has("grass_tinted")) {
					for (Map.Entry<String, JsonElement> tint : list.getAsJsonObject("grass_tinted").entrySet()) {
						JsonArray climate = tint.getValue().getAsJsonArray();
						grassTinted.put(tint.getKey(), new double[] {climate.get(0).getAsDouble(), climate.get(1).getAsDouble()});
					}
				}
				return new Listed(items, grassTinted);
			} catch (IOException | RuntimeException e) {
				LOGGER.warn("Failed to read the list of 3D items.", e);
				return Listed.NONE;
			}
		}, executor);
	}

	/** Has the game load the flat copies along with its own models. */
	private static void addFlatModels(Listed found, ModelLoadingPlugin.Context context) {
		listed = found;
		for (String item : found.items) {
			context.addModels(flatModelId(item));
		}
	}

	private static ResourceLocation flatModelId(String item) {
		return ResourceLocation.fromNamespaceAndPath(TNTsAllIn1ClientMod.MOD_ID, FLAT_FOLDER + item);
	}

	public static void beginFlatPlace() {
		flatPlaces++;
	}

	public static void endFlatPlace() {
		flatPlaces--;
	}

	/** The model to show for {@code stack}: its flat copy in a flat place while the switch is off, else {@code model}. */
	public static BakedModel modelFor(ItemStack stack, BakedModel model) {
		if (flatPlaces <= 0 || listed.items.isEmpty() || ClientConfig.get().blockModels3dItems) {
			return model;
		}
		ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
		if (!ResourceLocation.DEFAULT_NAMESPACE.equals(id.getNamespace()) || !listed.items.contains(id.getPath())) {
			return model;
		}
		// Asked for every time: what the model manager holds changes with each resource reload.
		ModelManager models = Minecraft.getInstance().getModelManager();
		BakedModel flat = models.getModel(flatModelId(id.getPath()));
		return flat == null || flat == models.getMissingModel() ? model : flat;
	}

	private static int tint(ItemStack stack, int tintIndex) {
		if (listed.grassTinted.isEmpty() || (flatPlaces > 0 && !ClientConfig.get().blockModels3dItems)) {
			return -1;
		}
		ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
		double[] climate = ResourceLocation.DEFAULT_NAMESPACE.equals(id.getNamespace()) ? listed.grassTinted.get(id.getPath()) : null;
		return climate == null ? -1 : FastColor.ARGB32.opaque(GrassColor.get(climate[0], climate[1]));
	}
}
