package com.tntsallin1client.tour;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.jspecify.annotations.Nullable;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * One step of the in-game tour. Its texts are {@code gui.tntsallin1client.tour.<id>.title/.text}.
 *
 * @param screen    the screen the step belongs to ({@code null} = no screen open, drawn on the HUD)
 * @param target    what gets highlighted there, or null for a centered explanation
 * @param onNext    INFO/TRY: what "Weiter" does before moving on (open a menu, switch the design, ...)
 * @param done      ACTION/WAIT: the step moves on by itself once this is true
 * @param available whether the step applies at all right now (e.g. friends only with an online launch)
 */
public record TourStep(String id, Kind kind, Predicate<@Nullable Screen> screen, Target target,
		@Nullable Consumer<Minecraft> onNext, @Nullable Predicate<Minecraft> done, BooleanSupplier available) {

	public enum Kind {
		/** Everything is blocked except the explanation's own buttons - look, then "Weiter". */
		INFO,
		/** Like INFO, but the highlighted part can be clicked too (trying a switch). */
		TRY,
		/** The highlighted part has to be clicked; the step moves on by itself ({@link #done}). */
		ACTION,
		/** Only a small hint in the corner, nothing blocked - the player does something on their own (create a world, ...). */
		WAIT
	}

	@FunctionalInterface
	public interface Target {
		Target NONE = screen -> null;

		@Nullable TourRect resolve(@Nullable Screen screen);
	}

	/** Whether "Weiter" exists for this step. */
	public boolean hasNextButton() {
		return this.kind == Kind.INFO || this.kind == Kind.TRY;
	}

	/** Whether clicks inside the highlighted part reach the screen. */
	public boolean targetClickable() {
		return this.kind == Kind.TRY || this.kind == Kind.ACTION;
	}
}
