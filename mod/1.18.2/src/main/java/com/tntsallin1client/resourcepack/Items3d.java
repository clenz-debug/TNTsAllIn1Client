package com.tntsallin1client.resourcepack;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.tntsallin1client.TNTsAllIn1ClientMod;
import com.tntsallin1client.config.ClientConfig;
import net.fabricmc.fabric.api.client.model.BakedModelManagerHelper;
import net.fabricmc.fabric.api.client.model.ModelLoadingRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GrassColor;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * "3D items in inventory & hand" for the 3D blocks pack. The newer versions do this with a second
 * pack of item definitions that say "flat in the inventory and the hand, 3D elsewhere"; 1.18.2 has no
 * item definitions yet (they came with 1.21.4) - an item has one model for every place it is shown in.
 * So the switch is ours, as in the 1.14.4 version: the pack lists the items it changes in
 * {@code assets/tntsallin1client/flat_items.json} and carries a copy of the model the game would show
 * for each without the pack ({@code resourcepacks/3d-blocks/early.py}); while the switch is off, those
 * copies are handed out in the inventory and the hand ({@code ItemModelShaperMixin}, with
 * {@code ItemRendererGuiMixin} and {@code ItemInHandRendererMixin} marking those places).
 *
 * <p>The same file names the items tinted like grass (the sugar cane) - the newer versions say that
 * in the item definition, here it is a color provider. Only the 3D model has a tinted layer. And it
 * names the items the inventory draws unlit ({@link #isFrontLit}).
 */
public final class Items3d {
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final ResourceLocation LIST = new ResourceLocation(TNTsAllIn1ClientMod.MOD_ID, "flat_items.json");
	private static final String FLAT_FOLDER = "item/flat/";

	/** What the pack's list says - empty without the pack. */
	private record Listed(Set<String> items, Map<String, double[]> grassTinted, Set<String> frontLit) {
		static final Listed NONE = new Listed(Set.of(), Map.of(), Set.of());
	}

	private static Listed listed = Listed.NONE;
	/** How many "flat places" (inventory slot, hand) are being drawn right now, nested. */
	private static int flatPlaces;

	private Items3d() {
	}

	public static void register() {
		// Fabric API for 1.18.2 has no preparable model loading plugin yet - the list is read when the
		// game asks for extra models, on its model loading thread.
		ModelLoadingRegistry.INSTANCE.registerModelProvider((manager, out) -> addFlatModels(read(manager), out));
		ColorProviderRegistry.ITEM.register(Items3d::tint, Items.SUGAR_CANE);
	}

	private static Listed read(ResourceManager manager) {
		{
			if (!manager.hasResource(LIST)) {
				return Listed.NONE;
			}
			try (Resource resource = manager.getResource(LIST);
					Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
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
				Set<String> frontLit = new HashSet<>();
				if (list.has("front_lit")) {
					for (JsonElement item : list.getAsJsonArray("front_lit")) {
						frontLit.add(item.getAsString());
					}
				}
				return new Listed(items, grassTinted, frontLit);
			} catch (IOException | RuntimeException e) {
				LOGGER.warn("Failed to read the list of 3D items.", e);
				return Listed.NONE;
			}
		}
	}

	/** Has the game load the flat copies along with its own models. */
	private static void addFlatModels(Listed found, Consumer<ResourceLocation> out) {
		listed = found;
		for (String item : found.items) {
			out.accept(flatModelId(item));
		}
	}

	private static ResourceLocation flatModelId(String item) {
		return new ResourceLocation(TNTsAllIn1ClientMod.MOD_ID, FLAT_FOLDER + item);
	}

	/**
	 * Whether the inventory draws this item without any light shading, like 1.14.4 does: the pack lists
	 * the campfires, whose upright fire looked dark in the inventory. The pack's model asks for front
	 * light ({@code gui_light}), which suits the newest versions; on this version's tilted 3D icon it
	 * left the campfire very dark (own user report) - see {@code ItemRendererGuiMixin}.
	 */
	public static boolean isFrontLit(ItemStack stack) {
		if (listed.frontLit.isEmpty()) {
			return false;
		}
		ResourceLocation id = Registry.ITEM.getKey(stack.getItem());
		return ResourceLocation.DEFAULT_NAMESPACE.equals(id.getNamespace()) && listed.frontLit.contains(id.getPath());
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
		ResourceLocation id = Registry.ITEM.getKey(stack.getItem());
		if (!ResourceLocation.DEFAULT_NAMESPACE.equals(id.getNamespace()) || !listed.items.contains(id.getPath())) {
			return model;
		}
		// Asked for every time: what the model manager holds changes with each resource reload.
		ModelManager models = Minecraft.getInstance().getModelManager();
		BakedModel flat = BakedModelManagerHelper.getModel(models, flatModelId(id.getPath()));
		return flat == null || flat == models.getMissingModel() ? model : flat;
	}

	private static int tint(ItemStack stack, int tintIndex) {
		if (listed.grassTinted.isEmpty() || (flatPlaces > 0 && !ClientConfig.get().blockModels3dItems)) {
			return -1;
		}
		ResourceLocation id = Registry.ITEM.getKey(stack.getItem());
		double[] climate = ResourceLocation.DEFAULT_NAMESPACE.equals(id.getNamespace()) ? listed.grassTinted.get(id.getPath()) : null;
		return climate == null ? -1 : 0xFF000000 | GrassColor.get(climate[0], climate[1]);
	}
}
