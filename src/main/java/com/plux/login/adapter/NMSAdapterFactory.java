package com.plux.login.adapter;

import java.util.logging.Logger;

public class NMSAdapterFactory {

    private static NMSAdapter instance;

    public static NMSAdapter createAdapter(Logger logger) {
        if (instance != null) {
            return instance;
        }

        VersionUtil.init();

        if (VersionUtil.isBelow_1_8()) {
            logger.info("[PluxLogin] Detected 1.7.x server, using NMS adapter for v1_7_R4");
            instance = new NMSAdapter_v1_7_R4();
        } else {
            logger.info("[PluxLogin] Detected 1.8+ server, using modern adapter");
            instance = new NMSAdapter_Modern();
        }

        return instance;
    }

    public static NMSAdapter getAdapter() {
        return instance;
    }
}