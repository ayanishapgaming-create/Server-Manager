package com.example.servermanager.mixin;

import com.example.servermanager.client.ServerSearchManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerServerListWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.option.ServerList;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiplayerScreen.class)
public abstract class MultiplayerScreenMixin extends Screen {

    @Shadow
    protected MultiplayerServerListWidget serverListWidget;

    @Shadow
    private ServerList serverList;

    @Unique
    private TextFieldWidget searchField;

    protected MultiplayerScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        // Clear query on init
        ServerSearchManager.searchQuery = "";

        // Create the search bar. Positioned right below the title.
        // Width: 200px, Height: 16px
        int x = this.width / 2 - 100;
        int y = 8;
        this.searchField = new TextFieldWidget(this.textRenderer, x, y, 200, 16, Text.literal("Search Server Name/IP..."));
        this.searchField.setMaxLength(64);
        this.searchField.setPlaceholder(Text.literal("Search..."));
        
        // Listener to refresh search and filter entries dynamically
        this.searchField.setChangedListener(query -> {
            ServerSearchManager.searchQuery = query;
            if (this.serverListWidget != null && this.serverList != null) {
                // Force-updates the list using our mixed-in filters
                this.serverListWidget.setServers(this.serverList);
            }
        });

        this.addSelectableChild(this.searchField);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (this.searchField != null) {
            // Draw the search field widget
            this.searchField.render(context, mouseX, mouseY, delta);
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void onTick(CallbackInfo ci) {
        if (this.searchField != null) {
            this.searchField.tick();
        }
    }

    @Inject(method = "removed", at = @At("TAIL"))
    private void onRemoved(CallbackInfo ci) {
        // Reset query on screen exit to avoid leakages
        ServerSearchManager.searchQuery = "";
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (this.searchField != null) {
            if (this.searchField.mouseClicked(mouseX, mouseY, button)) {
                this.searchField.setFocused(true);
                this.setFocused(this.searchField);
                cir.setReturnValue(true);
            } else {
                this.searchField.setFocused(false);
            }
        }
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void onKeyPressed(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        if (this.searchField != null && this.searchField.isFocused()) {
            if (keyCode == 256) { // ESC key
                this.searchField.setFocused(false);
            } else {
                this.searchField.keyPressed(keyCode, scanCode, modifiers);
            }
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
    private void onCharTyped(char chr, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        if (this.searchField != null && this.searchField.isFocused()) {
            this.searchField.charTyped(chr, modifiers);
            cir.setReturnValue(true);
        }
    }
}
