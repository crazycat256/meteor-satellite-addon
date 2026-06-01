/*
 * This file is part of Meteor Satellite Addon (https://github.com/crazycat256/meteor-satellite-addon).
 * Copyright (c) crazycat256.
 */

package fr.crazycat256.satellite.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import fr.crazycat256.satellite.utils.TPUtils;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.commands.arguments.PlayerArgumentType;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.world.entity.player.Player;

public class TPCommand extends Command {
    public TPCommand() {
        super("tp", "Teleports you to a player.");
    }

    @Override
    public void build(LiteralArgumentBuilder<ClientSuggestionProvider> builder) {

        builder.then(argument("player", PlayerArgumentType.create())
            .executes(ctx -> {

                Player player = PlayerArgumentType.get(ctx);

                if (player == mc.player) {
                    error("You can't teleport to yourself.");
                    return 0;
                } else if (player == mc.getCameraEntity()) {
                    error("Camera entity doesn't exist.");
                    return 0;
                }

                TPUtils.PaperTP(player.position());
                mc.player.setPos(player.getX(), player.getY(), player.getZ());

                return SINGLE_SUCCESS;
        }));
    }
}
