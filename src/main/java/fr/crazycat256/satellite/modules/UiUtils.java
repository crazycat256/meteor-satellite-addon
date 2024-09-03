/*
 * This file is part of Meteor Satellite Addon (https://github.com/crazycat256/meteor-satellite-addon).
 * Copyright (c) crazycat256.
 */

package fr.crazycat256.satellite.modules;

import fr.crazycat256.satellite.Addon;
import meteordevelopment.meteorclient.systems.modules.Module;

import java.lang.reflect.Field;


public class UiUtils extends Module {

    public static final Class<?> uiUtilsClass;
    private Field enabled;

    static {
        Class<?> klass;
        try {
            klass = Class.forName("org.uiutils.SharedVariables");
        } catch (ClassNotFoundException e) {
            klass = null;
        }
        uiUtilsClass = klass;
    }

    public UiUtils() {
        super(Addon.CATEGORY, "ui-utils", "Implementation of UI-TPUtils mod.");
        // https://github.com/Coderx-Gamer/ui-utils

        try {
            enabled = uiUtilsClass.getDeclaredField("enabled");
            enabled.setAccessible(true);
        } catch (NoSuchFieldException e) {
            // Empty catch block
        }

        set(isActive());
    }

    @Override
    public void onActivate() {
        set(true);
    }

    @Override
    public void onDeactivate() {
        set(false);
    }


    private void set(boolean value) {
        try {
            enabled.set(null, value);
        } catch (IllegalAccessException e) {
            // Empty catch block
        }
    }
}
