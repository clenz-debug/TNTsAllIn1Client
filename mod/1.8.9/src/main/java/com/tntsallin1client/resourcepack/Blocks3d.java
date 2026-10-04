package com.tntsallin1client.resourcepack;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.client.MinecraftClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * "3D Block Models": our own 3D models for ladders, rails, iron bars, vines, lily pads, sugar cane,
 * mushrooms, bookshelves, doors and trapdoors, as a switch in the mod menu - the same models as in
 * the Fabric versions, for the blocks this version has. Their items are 3D with it too; whether the
 * inventory and the hand show that is {@link Items3d}'s switch.
 *
 * <p>A 3D model follows the pixels of the picture it wears, and this version can show two sets of
 * pictures: its own, and the newer versions' through {@link NewTextures}. So there are two packs,
 * one shaped after each set (`resourcepacks/3d-blocks/legacy.py` builds them), and the switch puts
 * the one that fits into the game's list of active packs - swapping it when "New Textures" is
 * switched.
 *
 * <p>The packs hold only our own model files and travel inside the mod's jar; {@link #install}
 * copies them into `resourcepacks/`, where the game reads packs from.
 */
public final class Blocks3d {
	/** The pack for this version's own pictures, and the one for the "New Textures" pack's. */
	private static final String CLASSIC_PACK = "TNT 3D Blocks.zip";
	private static final String NEW_PACK = "TNT 3D Blocks - New Textures.zip";

	private static final Logger LOGGER = LogManager.getLogger("tntsallin1client");
	private static final String RESOURCES = "/assets/tntsallin1client/packs/";

	private Blocks3d() {
	}

	/**
	 * Called while the game starts, before it opens any resource pack: puts both packs into
	 * `resourcepacks/`, or replaces them where the mod's jar brings other ones (a file the game has
	 * open could no longer be replaced).
	 */
	public static void install() {
		File folder = new File(MinecraftClient.getInstance().runDirectory, "resourcepacks");
		copy("TNT-3D-Blocks-classic.zip", new File(folder, CLASSIC_PACK));
		copy("TNT-3D-Blocks-new.zip", new File(folder, NEW_PACK));
	}

	private static void copy(String resource, File target) {
		try (InputStream in = Blocks3d.class.getResourceAsStream(RESOURCES + resource)) {
			if (in == null) {
				LOGGER.warn("The mod's jar has no " + resource + " - no 3D block models.");
				return;
			}
			byte[] content = readAll(in);
			if (target.isFile() && target.length() == content.length && java.util.Arrays.equals(Files.readAllBytes(target.toPath()), content)) {
				return;
			}
			target.getParentFile().mkdirs();
			File temp = new File(target.getParentFile(), target.getName() + ".tmp");
			Files.write(temp.toPath(), content);
			Files.move(temp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException | RuntimeException e) {
			LOGGER.warn("Failed to put " + target.getName() + " into the resource packs folder.", e);
		}
	}

	static byte[] readAll(InputStream in) throws IOException {
		java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
		byte[] buffer = new byte[8192];
		for (int read = in.read(buffer); read >= 0; read = in.read(buffer)) {
			out.write(buffer, 0, read);
		}
		return out.toByteArray();
	}

	/** Whether a pack by that name is one of the two. */
	static boolean isOwnPack(String packName) {
		return CLASSIC_PACK.equals(packName) || NEW_PACK.equals(packName);
	}

	public static boolean isAvailable() {
		return LauncherPacks.isAvailable(CLASSIC_PACK) && LauncherPacks.isAvailable(NEW_PACK);
	}

	public static boolean isEnabled() {
		return LauncherPacks.isEnabled(CLASSIC_PACK) || LauncherPacks.isEnabled(NEW_PACK);
	}

	public static void setEnabled(boolean enabled) {
		LauncherPacks.apply(packsFor(enabled, NewTextures.isEnabled()));
	}

	/** Which of the two packs is on for the given switches - for {@link NewTextures}, which changes both at once. */
	static Map<String, Boolean> packsFor(boolean enabled, boolean newTextures) {
		Map<String, Boolean> packs = new LinkedHashMap<String, Boolean>();
		packs.put(CLASSIC_PACK, enabled && !newTextures);
		packs.put(NEW_PACK, enabled && newTextures);
		return packs;
	}
}
