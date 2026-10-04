package com.tntsallin1client.mixin;

import java.util.HashMap;
import java.util.Map;

import com.tntsallin1client.resourcepack.Items3d;
import net.minecraft.client.render.item.ItemModels;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedModelManager;
import net.minecraft.client.util.ModelIdentifier;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * "3D items in inventory & hand", see {@link Items3d}: the game's lookup of an item's model hands
 * out the one without our 3D pack while the inventory or a hand is being drawn and the switch is
 * off. Everything deciding how an item is drawn there - its model, whether it counts as a block -
 * asks this lookup, so all of it follows.
 */
@Mixin(ItemModels.class)
public abstract class ItemModelsMixin {
	private static final String INVENTORY_VARIANT = "inventory";

	@Shadow
	@Final
	private Map<Integer, ModelIdentifier> modelIds;

	@Shadow
	@Final
	private BakedModelManager modelManager;

	/** Keyed like the game's own models: by item and variant. */
	@Unique
	private final Map<Integer, BakedModel> tnt$flatModels = new HashMap<Integer, BakedModel>();

	@Shadow
	protected abstract int getMetadata(ItemStack stack);

	@Shadow
	private int pack(Item item, int metadata) {
		throw new AssertionError();
	}

	@Inject(method = "reloadModels()V", at = @At("TAIL"))
	private void tnt$collectFlatModels(CallbackInfo ci) {
		this.tnt$flatModels.clear();
		BakedModel missing = this.modelManager.getBakedModel();
		for (Map.Entry<Integer, ModelIdentifier> entry : this.modelIds.entrySet()) {
			ModelIdentifier id = entry.getValue();
			if (!"minecraft".equals(id.getNamespace()) || !Items3d.items().contains(id.getPath())) {
				continue;
			}
			BakedModel flat = this.modelManager.getByIdentifier(new ModelIdentifier(Items3d.flatVariant(id.getPath()), INVENTORY_VARIANT));
			if (flat != missing) {
				this.tnt$flatModels.put(entry.getKey(), flat);
			}
		}
	}

	@Inject(method = "getModel(Lnet/minecraft/item/ItemStack;)Lnet/minecraft/client/render/model/BakedModel;", at = @At("HEAD"), cancellable = true)
	private void tnt$flatModel(ItemStack stack, CallbackInfoReturnable<BakedModel> cir) {
		if (this.tnt$flatModels.isEmpty() || !Items3d.showsFlat() || stack.getItem() == null) {
			return;
		}
		BakedModel flat = this.tnt$flatModels.get(this.pack(stack.getItem(), this.getMetadata(stack)));
		if (flat != null) {
			cir.setReturnValue(flat);
		}
	}
}
