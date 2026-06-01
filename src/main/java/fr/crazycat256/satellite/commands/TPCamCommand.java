/*
 * This file is part of Meteor Satellite Addon (https://github.com/crazycat256/meteor-satellite-addon).
 * Copyright (c) crazycat256.
 */

package fr.crazycat256.satellite.commands;


import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import fr.crazycat256.satellite.utils.TPUtils;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.world.phys.Vec3;

public class TPCamCommand extends Command {
    public TPCamCommand() {
        super("tpcam", "Teleports you to the camera entity.");
    }

    @Override
    public void build(LiteralArgumentBuilder<ClientSuggestionProvider> builder) {

        builder.executes(context -> {
            if (mc.getCameraEntity() == null) {
                error("Camera entity doesn't exist.");
                return 0;
            }
            Vec3 pos = mc.gameRenderer.getMainCamera().position();
            TPUtils.PaperTP(pos);
            mc.player.setPos(pos.x, pos.y, pos.z);
            return SINGLE_SUCCESS;
        });
    }
}
