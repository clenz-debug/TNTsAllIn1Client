package com.tntsallin1client.tour;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Predicate;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/** One step of the in-game tour. Its texts are `gui.tntsallin1client.tour.<id>.title/.text`. */
public final class TourStep {
	public enum Kind {
		/** Everything is blocked except the explanation's own buttons - look, then "Next". */
		INFO,
		/** Like INFO, but the highlighted part can be clicked too (trying a switch). */
		TRY,
		/** The highlighted part has to be clicked; the step moves on by itself ({@link #done}). */
		ACTION,
		/** Only a small hint in the corner, nothing blocked - the player does something on their own (create a world, ...). */
		WAIT
	}

	public interface Target {
		Target NONE = screen -> null;

		/** `screen` may be null: no screen open. */
		TourRect resolve(Screen screen);
	}

	public final String id;
	public final Kind kind;
	/** The screen the step belongs to (it is asked about null, too: no screen open, drawn on the HUD). */
	public final Predicate<Screen> screen;
	/** What gets highlighted there; {@link Target#NONE} for a centered explanation. */
	public final Target target;
	/** INFO/TRY: what "Next" does before moving on (open a menu, switch the design, ...), or null. */
	public final Consumer<Minecraft> onNext;
	/** ACTION/WAIT: the step moves on by itself once this is true; null otherwise. */
	public final Predicate<Minecraft> done;
	/** Whether the step applies at all right now (e.g. friends only with an online launch). */
	public final BooleanSupplier available;

	public TourStep(String id, Kind kind, Predicate<Screen> screen, Target target, Consumer<Minecraft> onNext,
			Predicate<Minecraft> done, BooleanSupplier available) {
		this.id = id;
		this.kind = kind;
		this.screen = screen;
		this.target = target;
		this.onNext = onNext;
		this.done = done;
		this.available = available;
	}

	/** Whether "Next" exists for this step. */
	public boolean hasNextButton() {
		return this.kind == Kind.INFO || this.kind == Kind.TRY;
	}

	/** Whether clicks inside the highlighted part reach the screen. */
	public boolean targetClickable() {
		return this.kind == Kind.TRY || this.kind == Kind.ACTION;
	}
}
