/*
 * This file is part of Meteor Satellite Addon (https://github.com/crazycat256/meteor-satellite-addon).
 * Copyright (c) crazycat256.
 */

package fr.crazycat256.satellite;

import fr.crazycat256.satellite.commands.*;
import fr.crazycat256.satellite.modules.*;
import com.mojang.logging.LogUtils;
import fr.crazycat256.satellite.modules.seedmap.SeedMap;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.commands.Commands;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.slf4j.Logger;

public class Addon extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();
    public static final Category CATEGORY = new Category("Satellite");

    @Override
    public void onInitialize() {
        LOG.info("Initializing Satellite");

        // Modules
        Modules.get().add(new EventlessFly());
        Modules.get().add(new Phase());
        Modules.get().add(new InfiniteClickTP());
        Modules.get().add(new InfiniteInteract());
        Modules.get().add(new KnockbackTweaks());
        Modules.get().add(new SeedMap());
        Modules.get().add(new AutoFrameDupe());
        Modules.get().add(new SpeedBypass());
        Modules.get().add(new NBTTooltip());
        Modules.get().add(new VecFly());

        // Require UI TPUtils
        if (UiUtils.uiUtilsClass != null) {
            Modules.get().add(new UiUtils());
        }

        // Commands
        Commands.add(new FrameDrop());
        Commands.add(new TPCamCommand());
        Commands.add(new TPCommand());

    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(CATEGORY);
    }

    @Override
    public String getPackage() {
        return "fr.crazycat256.satellite";
    }
}
