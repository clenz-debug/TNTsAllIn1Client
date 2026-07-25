package com.tntsallin1client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tntsallin1client.hud.CoordinatesHud;
import com.tntsallin1client.hud.MaterialCounterHud;

public class TNTsAllIn1ClientMod implements ClientModInitializer {
	public static final String MOD_ID = "tntsallin1client";

	// This logger is used to write text to the console and the log file.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitializeClient() {
		LOGGER.info("[{}] Hello Fabric world!", MOD_ID);

		// Phase 1 "done" check: a log line and a chat message when a world is joined.
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			LOGGER.info("[{}] Joined a world.", MOD_ID);
			client.gui.getChat().addMessage(Component.nullToEmpty("TNT's All-In-1 Client loaded."));
		});

		// Phase 5a: coordinates + compass HUD.
		HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(MOD_ID, "coordinates_hud"), new CoordinatesHud());

		// Phase 5b: material counter HUD.
		HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(MOD_ID, "material_counter_hud"), new MaterialCounterHud());
	}
}
