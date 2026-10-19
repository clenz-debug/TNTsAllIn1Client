package com.tntsallin1client.mixin;

import com.tntsallin1client.resourcepack.BundledResourcePacks;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Gives our launcher-bundled resource packs vanilla's "Default"-pack placement (fixed, bottom) -
 * see {@link BundledResourcePacks}. In 1.18.2 every pack - folder packs and the built-in packs Fabric
 * registers for a mod alike - ends in this one constructor (the other one delegates to it), so the
 * placement is set right after it, the same three values the newer versions pass to {@code create}.
 */
@Mixin(Pack.class)
public class PackMixin {
	@Shadow
	@Final
	private String id;

	@Shadow
	@Final
	private PackSource packSource;

	@Shadow
	@Final
	@Mutable
	private boolean required;

	@Shadow
	@Final
	@Mutable
	private Pack.Position defaultPosition;

	@Shadow
	@Final
	@Mutable
	private boolean fixedPosition;

	@Inject(method = "<init>(Ljava/lang/String;ZLjava/util/function/Supplier;Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/Component;Lnet/minecraft/server/packs/repository/PackCompatibility;Lnet/minecraft/server/packs/repository/Pack$Position;ZLnet/minecraft/server/packs/repository/PackSource;)V",
			at = @At("TAIL"))
	private void tntsallin1client$pinToBottom(CallbackInfo ci) {
		if (BundledResourcePacks.pinsAboveDefault(this.id, this.packSource)) {
			// Like "Default", but optional - the user can still turn ours off.
			this.required = false;
			this.defaultPosition = Pack.Position.BOTTOM;
			this.fixedPosition = true;
		}
	}
}
