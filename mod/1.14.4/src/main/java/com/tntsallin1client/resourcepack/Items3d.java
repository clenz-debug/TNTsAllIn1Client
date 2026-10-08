package com.tntsallin1client.resourcepack;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tntsallin1client.config.ClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GrassColor;
import net.minecraft.world.level.ItemLike;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * "3D items in inventory & hand": with the 3D block models on, their items are 3D models too (the
 * pack brings them). With this switch off, the inventory and the hand show the item the game would
 * show without our pack; on the ground, in item frames and on the head it stays 3D - as in the
 * other versions.
 *
 * <p>A resource pack can't do that in this version: an item has one model for every place it is
 * shown in. So there is a second one per item. The pack lists the items it changes ({@link #LIST})
 * and carries, under names of ours, a copy of the model the game has for each; {@link #appendModels}
 * has the game load those as well, and its item model lookup hands them out while the inventory or
 * a hand is being drawn (see `ItemModelShaperMixin` and `ItemRendererMixin`).
 */
public final class Items3d {
	private static final Logger LOGGER = LogManager.getLogger("tntsallin1client");
	private static final String NAMESPACE = "tntsallin1client";
	private static final ResourceLocation LIST = new ResourceLocation(NAMESPACE, "flat_items.json");
	/** A model of ours in this folder is the copy of the game's model by the same name: `item/flat/ladder` is `item/ladder`. */
	private static final String FLAT_FOLDER = "flat/";
	/** What the game calls an item's model, as opposed to a block state's. */
	private static final String INVENTORY_VARIANT = "inventory";

	/** The items the active 3D pack changes, by their name. Empty while the 3D block models are off. */
	private static Set<String> items = Collections.emptySet();
	/** Those of them the inventory draws without its side light, like a flat item - see {@link #isFrontLit}. */
	private static Set<String> frontLit = Collections.emptySet();
	/** Those of them whose 3D model is tinted like grass, with the climate to take the color for: temperature, downfall. */
	private static Map<String, double[]> grassTinted = Collections.emptyMap();
	/** The second models found so far, by item name - forgotten whenever the game loads its models anew. */
	private static final Map<String, BakedModel> FLAT_MODELS = new HashMap<String, BakedModel>();
	/** Above zero while the inventory or a hand is being drawn. */
	private static int flatPlaces;

	private Items3d() {
	}

	/**
	 * Called whenever the game loads its models: reads the list anew from the packs now active and
	 * names the second model of every item on it, so the game loads those too.
	 */
	public static void appendModels(ResourceManager manager, Consumer<ModelResourceLocation> out) {
		Set<String> found = new HashSet<String>();
		Set<String> foundFrontLit = new HashSet<String>();
		Map<String, double[]> foundTinted = new HashMap<String, double[]>();
		if (manager.hasResource(LIST)) {
			try (Resource resource = manager.getResource(LIST);
					Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
				JsonObject list = new JsonParser().parse(reader).getAsJsonObject();
				for (JsonElement item : list.getAsJsonArray("items")) {
					found.add(item.getAsString());
				}
				if (list.has("grass_tinted")) {
					for (Map.Entry<String, JsonElement> tint : list.getAsJsonObject("grass_tinted").entrySet()) {
						JsonArray climate = tint.getValue().getAsJsonArray();
						foundTinted.put(tint.getKey(), new double[] {climate.get(0).getAsDouble(), climate.get(1).getAsDouble()});
					}
				}
				if (list.has("front_lit")) {
					for (JsonElement item : list.getAsJsonArray("front_lit")) {
						foundFrontLit.add(item.getAsString());
					}
				}
			} catch (IOException | RuntimeException e) {
				LOGGER.warn("Failed to read the list of 3D items.", e);
			}
		}
		items = found;
		frontLit = foundFrontLit;
		grassTinted = foundTinted;
		FLAT_MODELS.clear();
		for (String item : found) {
			out.accept(flatModelId(item));
		}
	}

	private static ModelResourceLocation flatModelId(String item) {
		return new ModelResourceLocation(new ResourceLocation(NAMESPACE, FLAT_FOLDER + item), INVENTORY_VARIANT);
	}

	/**
	 * Whether the inventory draws this item's 3D model the way it draws a flat item: without the light
	 * from the side a block gets there. The campfire's upright fire catches little of that light and
	 * looked dark (own user report, as in the newer versions, where the model itself can say so).
	 */
	public static boolean isFrontLit(ItemStack stack) {
		if (frontLit.isEmpty()) {
			return false;
		}
		ResourceLocation id = Registry.ITEM.getKey(stack.getItem());
		return "minecraft".equals(id.getNamespace()) && frontLit.contains(id.getPath());
	}

	/** The items {@link #tint} can color - the game wants them named when the mod starts, before any pack is read. */
	public static ItemLike[] tintableItems() {
		return new ItemLike[] {Items.SUGAR_CANE};
	}

	/**
	 * The color for a tinted face of this item's model, the way the game asks for it: the grass color
	 * the 3D pack names for the item while its 3D model is the one being drawn, no color (-1) in
	 * every other case - without the pack, and where the flat item is shown in its place.
	 */
	public static int tint(ItemStack stack, int tintIndex) {
		if (grassTinted.isEmpty() || (flatPlaces > 0 && !ClientConfig.get().blockModels3dItems)) {
			return -1;
		}
		ResourceLocation id = Registry.ITEM.getKey(stack.getItem());
		double[] climate = "minecraft".equals(id.getNamespace()) ? grassTinted.get(id.getPath()) : null;
		return climate == null ? -1 : GrassColor.get(climate[0], climate[1]);
	}

	/** Called before and after the inventory or a hand draws an item. */
	public static void beginFlatPlace() {
		flatPlaces++;
	}

	public static void endFlatPlace() {
		flatPlaces--;
	}

	/**
	 * The model to show for this item in place of `model`, the one the game found: the copy without
	 * our 3D model while the inventory or a hand is being drawn and the switch is off, `model` itself
	 * in every other case.
	 */
	public static BakedModel modelFor(ItemStack stack, BakedModel model) {
		if (flatPlaces <= 0 || items.isEmpty() || ClientConfig.get().blockModels3dItems) {
			return model;
		}
		ResourceLocation id = Registry.ITEM.getKey(stack.getItem());
		if (!"minecraft".equals(id.getNamespace()) || !items.contains(id.getPath())) {
			return model;
		}
		BakedModel flat = FLAT_MODELS.get(id.getPath());
		if (flat == null) {
			ModelManager models = Minecraft.getInstance().getModelManager();
			flat = models.getModel(flatModelId(id.getPath()));
			FLAT_MODELS.put(id.getPath(), flat);
		}
		// Not loaded for whatever reason: the 3D model is still better than the missing-model cube.
		return flat == Minecraft.getInstance().getModelManager().getMissingModel() ? model : flat;
	}
}
