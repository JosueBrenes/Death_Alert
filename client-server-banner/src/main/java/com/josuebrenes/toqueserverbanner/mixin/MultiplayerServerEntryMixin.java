package com.josuebrenes.toqueserverbanner.mixin;

import com.josuebrenes.toqueserverbanner.ToqueServerBannerRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerServerListWidget;
import net.minecraft.client.network.ServerInfo;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiplayerServerListWidget.ServerEntry.class)
public abstract class MultiplayerServerEntryMixin {
    @Shadow @Final private ServerInfo server;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void toque$render(
            DrawContext context,
            int index,
            int y,
            int x,
            int entryWidth,
            int entryHeight,
            int mouseX,
            int mouseY,
            boolean hovered,
            float tickDelta,
            CallbackInfo ci
    ) {
        if (!isToqueServer(server)) {
            return;
        }

        ToqueServerBannerRenderer.render(
                context,
                server,
                x,
                y,
                entryWidth,
                mouseX,
                mouseY,
                hovered
        );
        ci.cancel();
    }

    private static boolean isToqueServer(ServerInfo server) {
        String name = server.name == null ? "" : server.name.toLowerCase();
        String address = server.address == null ? "" : server.address.toLowerCase();

        return name.contains("toque")
                || address.contains("toque")
                || address.contains("ivan-fda.tun.ply.gg");
    }
}
