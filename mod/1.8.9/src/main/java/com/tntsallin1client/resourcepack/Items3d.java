package com.tntsallin1client.resourcepack;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tntsallin1client.config.ClientConfig;
import net.minecraft.client.render.model.json.BlockModel;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * "3D items in inventory & hand": with the 3D block models on, their items are 3D models too (the
 * pack brings them, see `resourcepacks/3d-blocks/legacy.py`). With this switch off, the inventory
 * and the hand show the item the game would show without our pack; on the ground, in item frames
 * and on the head it stays 3D - as in the Fabric versions.
 *
 * <p>A resource pack can't do that in this version: an item has one model for every place it is
 * shown in. So the mod keeps a second one per item. The active pack lists the items it changes
 * ({@link #LIST}); for each, the game loads one more model under a name of ours, which
 * {@link #loadFlat} reads from the highest pack that isn't ours. The game's item model lookup hands
 * that one out while the inventory or a hand is being drawn (see the mixins on `ModelLoader`,
 * `ItemModels`, `ItemRenderer` and `HeldItemRenderer`).
 */
public final class Items3d {
	private static final Logger LOGGER = LogManager.getLogger("tntsallin1client");
	private static final String NAMESPACE = "tntsallin1client";
	private static final Identifier LIST = new Identifier(NAMESPACE, "flat_items.json");
	/** Marks a model of ours as the copy of the model by the same name without it: `item/flat/ladder` is `item/ladder`. */
	private static final String FLAT_FOLDER = "flat/";
	private static final String BUILTIN_PREFIX = "builtin/";
	private static final String ENTITY_PICTURES = "entity/";

	/** The items the active 3D pack changes, by their model's name. Empty while the 3D block models are off. */
	private static Set<String> items = Collections.emptySet();
	/** Above zero while the inventory or a hand is being drawn. */
	private static int flatPlaces;

	private Items3d() {
	}

	/** Reads the list anew from the packs now active. Called whenever the game loads its models. */
	public static Set<String> reload(ResourceManager manager) {
		Set<String> found = new HashSet<String>();
		try {
			Resource resource = manager.getResource(LIST);
			try (Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
				for (JsonElement item : new JsonParser().parse(reader).getAsJsonObject().getAsJsonArray("items")) {
					found.add(item.getAsString());
				}
			}
		} catch (IOException e) {
			// No pack of ours is on.
		} catch (RuntimeException e) {
			LOGGER.warn("Failed to read the list of 3D items.", e);
		}
		items = found;
		return found;
	}

	public static Set<String> items() {
		return items;
	}

	/** The name the game loads and keeps the item's second model under. */
	public static String flatVariant(String item) {
		return NAMESPACE + ":" + FLAT_FOLDER + item;
	}

	public static boolean isFlatModel(Identifier id) {
		return NAMESPACE.equals(id.getNamespace()) && id.getPath().contains("/" + FLAT_FOLDER);
	}

	/**
	 * The model the game would load by that name if our packs weren't there. A model it builds on is
	 * read the same way, so a trapdoor - the block's model in this version - gets the game's block.
	 */
	public static BlockModel loadFlat(ResourceManager manager, Identifier id) throws IOException {
		String path = id.getPath().replaceFirst("/" + FLAT_FOLDER, "/");
		JsonObject json = new JsonParser().parse(readBelowOurPacks(manager, new Identifier("models/" + path + ".json"))).getAsJsonObject();
		if (json.has("parent")) {
			String parent = json.get("parent").getAsString();
			int folderEnd = parent.indexOf('/');
			if (!parent.startsWith(BUILTIN_PREFIX) && folderEnd >= 0) {
				json.addProperty("parent", NAMESPACE + ":" + parent.substring(0, folderEnd + 1) + FLAT_FOLDER + parent.substring(folderEnd + 1));
			}
		}
		BlockModel model = BlockModel.create(json.toString());
		// The model's name, as the game sets it for every model it loads.
		model.field_10928 = id.toString();
		return model;
	}

	private static String readBelowOurPacks(ResourceManager manager, Identifier file) throws IOException {
		// Lowest pack first.
		List<Resource> resources = manager.getAllResources(file);
		String content = null;
		for (Resource resource : resources) {
			try (InputStream in = resource.getInputStream()) {
				if (!Blocks3d.isOwnPack(resource.getResourcePackName())) {
					content = new String(Blocks3d.readAll(in), StandardCharsets.UTF_8);
				}
			}
		}
		if (content == null) {
			throw new IOException("No pack but ours has " + file);
		}
		return content;
	}

	/**
	 * An entity's picture as the game can take it for a model: square, the picture in its top left
	 * corner and nothing around it. Any other picture comes back as it is. The 3D items shaped like
	 * their entities wear these (`legacy.py` counts their pixels on the square).
	 */
	public static BufferedImage squared(String spriteName, BufferedImage image) {
		if (image.getWidth() == image.getHeight() || !new Identifier(spriteName).getPath().startsWith(ENTITY_PICTURES)) {
			return image;
		}
		int size = Math.max(image.getWidth(), image.getHeight());
		BufferedImage square = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = square.createGraphics();
		graphics.drawImage(image, 0, 0, null);
		graphics.dispose();
		return square;
	}

	/** Called before and after the inventory or a hand draws an item. */
	public static void beginFlatPlace() {
		flatPlaces++;
	}

	public static void endFlatPlace() {
		flatPlaces--;
	}

	/** Whether the item being looked up right now is to be the one without our 3D model. */
	public static boolean showsFlat() {
		return flatPlaces > 0 && !ClientConfig.get().blockModels3dItems;
	}
}
