/*
 * This file is part of Meteor Satellite Addon (https://github.com/crazycat256/meteor-satellite-addon).
 * Copyright (c) crazycat256.
 */

package fr.crazycat256.satellite.mixin;

import fr.crazycat256.satellite.modules.NBTTooltip;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
    @Inject(method = "getTooltipLines", at = @At("RETURN"))
    private void getTooltip(Item.TooltipContext tooltipContext, @Nullable Player entity, TooltipFlag tooltipType, CallbackInfoReturnable<List<Component>> info) {
        String nbtStr = Modules.get().get(NBTTooltip.class).getTooltip((ItemStack) (Object) this, tooltipContext);
        if (nbtStr != null) {
            List<Component> tooltip = info.getReturnValue();
            for (String line : nbtStr.split("\n")) {
                tooltip.add(Component.literal(line));
            }
        }
    }
}
