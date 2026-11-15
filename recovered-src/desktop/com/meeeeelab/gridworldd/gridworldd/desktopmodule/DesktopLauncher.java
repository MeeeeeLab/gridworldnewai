/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.jme3.system.AppSettings
 *  com.meeeeelab.gridworldd.gridworldd.game.GridWorldD
 */
package com.meeeeelab.gridworldd.gridworldd.desktopmodule;

import com.jme3.system.AppSettings;
import com.meeeeelab.gridworldd.gridworldd.game.GridWorldD;

public class DesktopLauncher {
    public static void main(String[] args) {
        GridWorldD game = new GridWorldD();
        AppSettings appSettings = new AppSettings(true);
        game.setSettings(appSettings);
        game.setShowSettings(false);
        game.start();
    }
}

