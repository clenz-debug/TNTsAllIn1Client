package com.tntsallin1client.mixin;

import com.google.common.collect.ImmutableList;
import com.tntsallin1client.gui.OptionsAccess;
import com.tntsallin1client.resourcepack.BundledResourcePacks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

/**
 * {@link OptionsAccess} on 1.18.2's options - 1.19.4's {@code updateResourcePacks}, with the rule the
 * newer versions add to it through {@code OptionsResourcePacksMixin}: our pinned packs are saved too
 * (see {@link OptionsScreenPacksMixin}).
 */
@Mixin(Options.class)
public abstract class OptionsAccessMixin implements OptionsAccess {
	@Shadow
	public List<String> resourcePacks;

	@Shadow
	public List<String> incompatibleResourcePacks;

	@Shadow
	protected Minecraft minecraft;

	@Shadow
	public abstract void save();

	@Override
	public void updateResourcePacks(PackRepository repository) {
		List<String> before = ImmutableList.copyOf(this.resourcePacks);
		this.resourcePacks.clear();
		this.incompatibleResourcePacks.clear();
		for (Pack pack : repository.getSelectedPacks()) {
			if (!pack.isFixedPosition() || BundledResourcePacks.isPinned(pack)) {
				this.resourcePacks.add(pack.getId());
				if (!pack.getCompatibility().isCompatible()) {
					this.incompatibleResourcePacks.add(pack.getId());
				}
			}
		}
		this.save();
		if (!ImmutableList.copyOf(this.resourcePacks).equals(before)) {
			this.minecraft.reloadResourcePacks();
		}
	}
}
