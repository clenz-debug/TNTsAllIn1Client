package com.tntsallin1client.mixin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.tntsallin1client.resourcepack.Items3d;
import net.minecraft.client.render.model.ModelLoader;
import net.minecraft.client.render.model.json.BlockModel;
import net.minecraft.item.Item;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * "3D items in inventory & hand", see {@link Items3d}: has the game load a second model for every
 * item our 3D pack changes - the one it would show without the pack.
 *
 * <p>The game loads one model per name in an item's list of names (the method filling those lists
 * and the one reading them have no name in this version's name table); ours is added to the list.
 */
@Mixin(ModelLoader.class)
public abstract class ModelLoaderMixin {
	@Shadow
	@Final
	private ResourceManager resourceManager;

	@Shadow
	private Map<Item, List<String>> modelVariantNames;

	@Shadow
	private List<String> method_10392(Item item) {
		throw new AssertionError();
	}

	@Inject(method = "method_10402()V", at = @At("TAIL"))
	private void tnt$addFlatModels(CallbackInfo ci) {
		Set<String> items = Items3d.reload(this.resourceManager);
		if (items.isEmpty()) {
			return;
		}
		for (Item item : Item.REGISTRY) {
			List<String> names = this.method_10392(item);
			List<String> all = null;
			for (String name : names) {
				Identifier id = new Identifier(name);
				if ("minecraft".equals(id.getNamespace()) && items.contains(id.getPath())) {
					if (all == null) {
						all = new ArrayList<String>(names);
					}
					all.add(Items3d.flatVariant(id.getPath()));
				}
			}
			if (all != null) {
				this.modelVariantNames.put(item, all);
			}
		}
	}

	@Inject(method = "getModel(Lnet/minecraft/util/Identifier;)Lnet/minecraft/client/render/model/json/BlockModel;", at = @At("HEAD"), cancellable = true)
	private void tnt$loadFlatModel(Identifier id, CallbackInfoReturnable<BlockModel> cir) throws IOException {
		if (Items3d.isFlatModel(id)) {
			cir.setReturnValue(Items3d.loadFlat(this.resourceManager, id));
		}
	}
}
