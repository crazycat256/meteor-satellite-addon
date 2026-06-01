/*
 * This file is part of Meteor Satellite Addon (https://github.com/crazycat256/meteor-satellite-addon).
 * Copyright (c) crazycat256.
 */

package fr.crazycat256.satellite.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

public class FrameDrop extends Command {
    public FrameDrop() {
        super("framedrop", "Drops all items from item frames around you.");
    }

    @Override
    public void build(LiteralArgumentBuilder<ClientSuggestionProvider> builder) {
        builder.executes(context -> {
            if (mc.player == null || mc.level == null || mc.gameMode == null) return 0;
            List<ItemFrame> itemFrames = new ArrayList<>();
            for (Entity entity : mc.level.entitiesForRendering()) {
                if (entity instanceof ItemFrame itemFrame && itemFrame.distanceToSqr(mc.player) <= 16 && itemFrame.getItem().getItem() != Items.AIR) {
                    itemFrames.add(itemFrame);
                }
            }
            for (ItemFrame itemFrame : itemFrames) {
                mc.gameMode.attack(mc.player, itemFrame);
            }
            info("Dropped " + itemFrames.size() + " items.");
            return SINGLE_SUCCESS;
        });
    }
}
