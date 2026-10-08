package com.tntsallin1client.menu;

import com.tntsallin1client.config.ClientConfig;
import com.tntsallin1client.resourcepack.Blocks3d;
import net.minecraft.client.gui.screens.Screen;

/**
 * Options of the "3D Block Models" row, as in the other versions: whether the inventory and the
 * hand show the blocks' items in 3D too (see {@link com.tntsallin1client.resourcepack.Items3d}).
 * That takes effect at once, nothing is loaded anew. And whether the third-party Bushy Vegetation
 * pack is switched along with ours - only with that pack in the instance. The newer versions'
 * further row, the 3D bushes, has nothing to switch here - this version has no such blocks.
 */
public class BlockModels3dOptionsScreen extends FeatureOptionsScreen {
	public BlockModels3dOptionsScreen(Screen parent) {
		super(parent, "gui.tntsallin1client.block_models_3d_options.title");
		final ClientConfig config = ClientConfig.get();
		addToggle("gui.tntsallin1client.menu.items_3d_inventory", () -> config.blockModels3dItems, value -> config.blockModels3dItems = value);
		if (Blocks3d.isBushyVegetationAvailable()) {
			addToggle("gui.tntsallin1client.menu.bushy_vegetation", () -> config.blockModels3dBushyVegetation, Blocks3d::setBushyVegetation);
		}
	}
}
