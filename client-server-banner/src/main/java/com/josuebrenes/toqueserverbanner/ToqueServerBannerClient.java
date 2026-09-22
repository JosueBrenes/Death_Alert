package com.josuebrenes.toqueserverbanner;

import net.fabricmc.api.ClientModInitializer;

public final class ToqueServerBannerClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ToqueServerBannerConfig.load();
        System.out.println("[TOQUE] Server Banner loaded.");
    }
}
