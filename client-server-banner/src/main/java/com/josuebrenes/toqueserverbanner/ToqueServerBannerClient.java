package com.josuebrenes.toqueserverbanner;

import net.fabricmc.api.ClientModInitializer;

public final class ToqueServerBannerClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        System.out.println("[TOQUE] Server Banner loaded.");
    }
}
