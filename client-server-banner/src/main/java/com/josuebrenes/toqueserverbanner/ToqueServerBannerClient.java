package com.josuebrenes.toqueserverbanner;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ToqueServerBannerClient implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("TOQUE");

    @Override
    public void onInitializeClient() {
        ToqueServerBannerConfig.load();
        LOGGER.info("[TOQUE] Server Banner loaded.");
    }
}
