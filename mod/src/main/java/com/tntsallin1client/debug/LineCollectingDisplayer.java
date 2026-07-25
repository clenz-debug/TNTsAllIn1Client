package com.tntsallin1client.debug;

import net.minecraft.client.gui.components.debug.DebugScreenDisplayer;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Minimal {@link DebugScreenDisplayer} that just flattens whatever a vanilla
 * {@code DebugScreenEntry} would normally hand to the F3 renderer into a plain
 * list of lines, so entries can be reused for a differently-positioned display
 * (see {@link SystemInfoOverlay}) without duplicating their data-gathering logic.
 */
final class LineCollectingDisplayer implements DebugScreenDisplayer {
	final List<String> lines = new ArrayList<>();

	@Override
	public void addPriorityLine(String string) {
		lines.add(string);
	}

	@Override
	public void addLine(String string) {
		lines.add(string);
	}

	@Override
	public void addToGroup(Identifier identifier, Collection<String> collection) {
		lines.addAll(collection);
	}

	@Override
	public void addToGroup(Identifier identifier, String string) {
		lines.add(string);
	}
}
