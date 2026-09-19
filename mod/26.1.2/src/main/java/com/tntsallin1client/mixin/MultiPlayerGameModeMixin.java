package com.tntsallin1client.mixin;

import com.tntsallin1client.freecam.FreecamHandler;
import com.tntsallin1client.inventory.ContainerClickPacingHandler;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Defensive: some Spigot/Paper servers run a packet-rate/anti-dupe limiter that
 * kicks with "Too many suspicious packets" after a burst of
 * ServerboundContainerClickPacket sends arrives too close together - ordinary
 * fast clicking (villager trading, moving/consolidating stacks) can trip it on
 * servers this mod doesn't control. Redirecting {@code this.connection.send(...)}
 * here - which fires AFTER {@code menu.clicked(...)} has already run client-side
 * prediction earlier in {@code handleContainerInput} - lets local/visual state
 * stay instant while the actual network send gets queued and paced out by
 * {@link ContainerClickPacingHandler} instead of firing immediately.
 *
 * <p>Known limitation: {@code CreativeModeInventoryScreen} doesn't route
 * creative-mode item placement through this method (it sends
 * {@code ServerboundSetCreativeModeSlotPacket} via a different path) - out of
 * scope here, not what triggered the reported kicks.
 *
 * <p><b>Freecam interaction lock:</b> {@link FreecamHandler} detaches the render camera from the
 * real (frozen) player - since it's not verified here which position vanilla's own hit-testing
 * actually targets while that's active, every attack/mine/place/use entry point on this class
 * (which already mediates essentially all player-world interaction) is cancelled outright while
 * freecam is active, per explicit user decision: better to block interaction entirely than risk
 * an "attack/mine through the wall from the detached camera" exploit.
 */
@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeMixin {
	@Redirect(method = "handleContainerInput", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/multiplayer/ClientPacketListener;send(Lnet/minecraft/network/protocol/Packet;)V"))
	private void tntsallin1client$paceContainerClickSend(ClientPacketListener connection, Packet<?> packet) {
		ContainerClickPacingHandler.enqueue(connection, packet);
	}

	@Inject(method = "startDestroyBlock", at = @At("HEAD"), cancellable = true)
	private void tntsallin1client$blockStartDestroy(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
		if (FreecamHandler.isActive()) {
			cir.setReturnValue(false);
		}
	}

	@Inject(method = "continueDestroyBlock", at = @At("HEAD"), cancellable = true)
	private void tntsallin1client$blockContinueDestroy(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
		if (FreecamHandler.isActive()) {
			cir.setReturnValue(false);
		}
	}

	@Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
	private void tntsallin1client$blockUseItemOn(LocalPlayer player, InteractionHand hand, BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
		if (FreecamHandler.isActive()) {
			cir.setReturnValue(InteractionResult.PASS);
		}
	}

	@Inject(method = "useItem", at = @At("HEAD"), cancellable = true)
	private void tntsallin1client$blockUseItem(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
		if (FreecamHandler.isActive()) {
			cir.setReturnValue(InteractionResult.PASS);
		}
	}

	@Inject(method = "attack", at = @At("HEAD"), cancellable = true)
	private void tntsallin1client$blockAttack(Player player, Entity target, CallbackInfo ci) {
		if (FreecamHandler.isActive()) {
			ci.cancel();
		}
	}

	@Inject(method = "interact", at = @At("HEAD"), cancellable = true)
	private void tntsallin1client$blockInteract(Player player, Entity target, EntityHitResult hitResult, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
		if (FreecamHandler.isActive()) {
			cir.setReturnValue(InteractionResult.PASS);
		}
	}
}
