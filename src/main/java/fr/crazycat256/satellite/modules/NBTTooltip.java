/*
 * This file is part of Meteor Satellite Addon (https://github.com/crazycat256/meteor-satellite-addon).
 * Copyright (c) crazycat256.
 */

package fr.crazycat256.satellite.modules;

import com.mojang.serialization.DataResult;
import fr.crazycat256.satellite.Addon;
import fr.crazycat256.satellite.utils.NBTUtils;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.NbtOps;
import org.lwjgl.glfw.GLFW;

public class NBTTooltip extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> indentationLevel = sgGeneral.add(new IntSetting.Builder()
        .name("indentation-level")
        .description("The level of indentation for NBT data.")
        .min(0)
        .sliderMax(4)
        .defaultValue(2)
        .build()
    );

    private final Setting<Boolean> color = sgGeneral.add(new BoolSetting.Builder()
        .name("color")
        .description("Colorize NBT data.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> onlyOnKey = sgGeneral.add(new BoolSetting.Builder()
        .name("only-on-key")
        .description("Only show NBT data when CTRL is held.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Keybind> displayKey = sgGeneral.add(new KeybindSetting.Builder()
        .name("display-key")
        .description("The key to press to display the nbt tooltip.")
        .defaultValue(Keybind.fromKey(GLFW.GLFW_KEY_LEFT_CONTROL))
        .visible(onlyOnKey::get)
        .build()
    );

    private final Setting<Keybind> copyKey = sgGeneral.add(new KeybindSetting.Builder()
        .name("copy-key")
        .description("The key to press to copy the NBT into the clipboard.")
        .defaultValue(Keybind.fromKeys(67, 2)) // Ctrl + C
        .build()
    );

    public NBTTooltip() {
        super(Addon.CATEGORY, "nbt-tooltip", "Shows NBT data in item tooltips.");
    }



    public String getTooltip(ItemStack stack, Item.TooltipContext ctx) {

        if (isActive() && (!onlyOnKey.get() || displayKey.get().isPressed())) {
            DataResult<Tag> result = DataComponentPatch.CODEC.encodeStart(ctx.registries().createSerializationContext(NbtOps.INSTANCE), stack.getComponentsPatch());
            result.ifError(e->{});
            Tag nbtElement = result.getOrThrow();
            CompoundTag compound = (CompoundTag) nbtElement;

            if (copyKey.get().isPressed()) {
                mc.keyboardHandler.setClipboard(NBTUtils.formatNBT(compound, indentationLevel.get(), false));
            }

            return NBTUtils.formatNBT(compound, indentationLevel.get(), color.get());
        }

        return null;
    }
}
