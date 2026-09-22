package com.josuebrenes.toquedeathalert.mixin;

import com.josuebrenes.toquedeathalert.status.StatusMotd;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerMetadata;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Puts the live series information into the status reply.
 *
 * <p>Hooked here rather than through setMotd because that only stores the string:
 * the reply is built from the metadata, and a query handler is handed a fresh copy
 * of it for every incoming ping. Replacing the description at this point means the
 * Try and the day are current each time a player refreshes their server list.
 *
 * <p>Only the description is replaced; the player counts, the version, the favicon
 * and the secure chat flag are passed through untouched.
 */
@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {
    @Inject(method = "getServerMetadata", at = @At("RETURN"), cancellable = true)
    private void toque$describeSeries(CallbackInfoReturnable<ServerMetadata> cir) {
        ServerMetadata original = cir.getReturnValue();
        if (original == null) {
            return;
        }
        Text description = StatusMotd.build((MinecraftServer) (Object) this);
        if (description == null) {
            return;
        }
        cir.setReturnValue(new ServerMetadata(description, original.players(),
                original.version(), original.favicon(), original.secureChatEnforced()));
    }
}
