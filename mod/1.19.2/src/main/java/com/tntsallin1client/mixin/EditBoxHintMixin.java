package com.tntsallin1client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tntsallin1client.design.ClientFont;
import com.tntsallin1client.design.ThemedUi;
import com.tntsallin1client.gui.EditBoxAccess;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * {@link EditBoxAccess} on 1.19.2's text field: the hint is drawn the way 1.19.4's {@code renderWidget}
 * does it - in the text colour, where the text would start, while the field is empty and not focused.
 * On themed screens in the client font, like the rest of the field's text ({@link EditBoxThemeMixin}).
 */
@Mixin(EditBox.class)
public abstract class EditBoxHintMixin extends AbstractWidget implements EditBoxAccess {
	@Shadow
	@Final
	private Font font;

	@Shadow
	private String value;

	@Shadow
	private boolean bordered;

	@Shadow
	private boolean isEditable;

	@Shadow
	private int textColor;

	@Shadow
	private int textColorUneditable;

	@Unique
	@Nullable
	private Component tntsallin1client$hint;

	protected EditBoxHintMixin(int x, int y, int width, int height, Component message) {
		super(x, y, width, height, message);
	}

	@Shadow
	public abstract boolean isVisible();

	@Override
	public void setHint(Component hint) {
		this.tntsallin1client$hint = hint;
	}

	@Inject(method = "renderButton", at = @At("TAIL"))
	private void tntsallin1client$renderHint(PoseStack poseStack, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
		if (this.tntsallin1client$hint == null || !this.isVisible() || !this.value.isEmpty() || this.isFocused()) {
			return;
		}
		int x = this.bordered ? this.x + 4 : this.x;
		int y = this.bordered ? this.y + (this.height - 8) / 2 : this.y;
		if (ThemedUi.active()) {
			// Like the text of a themed field (EditBoxThemeMixin): client font, dimmed, without shadow.
			this.font.draw(poseStack, ClientFont.of(this.tntsallin1client$hint.getString()), x, y, ThemedUi.textColor(0xFFA0A0A0));
			return;
		}
		this.font.drawShadow(poseStack, this.tntsallin1client$hint, x, y, this.isEditable ? this.textColor : this.textColorUneditable);
	}
}
