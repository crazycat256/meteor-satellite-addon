/*
 * This file is part of Meteor Satellite Addon (https://github.com/crazycat256/meteor-satellite-addon).
 * Copyright (c) crazycat256.
 */

package fr.crazycat256.satellite.utils;

import com.google.common.base.Strings;
import net.minecraft.nbt.*;

@SuppressWarnings("UnnecessaryUnicodeEscape")
public class NBTUtils {

    /**
     * Format an NBT tag compound into a readable json-like string
     * @param compound The NBT tag compound to format
     * @param indentationLevel The number of spaces to indent each level
     * @param colors Whether to colorize the output
     * @return The formatted NBT tag compound
     */
    public static String formatNBT(NbtCompound compound, int indentationLevel, boolean colors) {
        return formatNBT(compound, indentationLevel, indentationLevel, colors);
    }

    /**
     * Recursive function used in {@link #formatNBT(NbtCompound, int, boolean)}
     */
    public static String formatNBT(NbtCompound compound, int currentIndentation, int indentationLevel, boolean colors) {
        String keyColor = "\u00a7b";
        String bracketColor = "\u00a7f";
        String resetColor = "\u00a7r";
        if (!colors) {
            keyColor = "";
            bracketColor = "";
            resetColor = "";
        }
        StringBuilder result = new StringBuilder(bracketColor + "{" + resetColor);

        String indentation = Strings.repeat(" ", currentIndentation);

        for (String key : compound.getKeys()) {
            NbtElement tag = compound.get(key);
            result.append("\n").append(indentation).append(keyColor).append(key).append(resetColor).append(": ");

            handleTag(currentIndentation, indentationLevel, colors, result, tag);
        }

        if (result.charAt(result.length() - 1) == ',') {
            result.setCharAt(result.length() - 1, ' ');
        }

        for (int i = result.length(); i < 0; i++) {
            if (result.charAt(i) == ' ' || result.charAt(i) == '\n') {
                continue;
            }
            if (result.charAt(i) == '{') {
                break;
            }
            break;
        }
        if (!compound.getKeys().isEmpty()) {
            result.append("\n").append(Strings.repeat(" ", Math.max(0, currentIndentation - indentationLevel)));
        }
        result.append(bracketColor).append("}").append(resetColor);

        return result.toString();
    }

    private static void handleTag(int currentIndentation, int indentationLevel, boolean colors, StringBuilder result, NbtElement tag) {
        if (tag instanceof NbtCompound) {
            result.append(formatNBT((NbtCompound) tag, currentIndentation + indentationLevel, indentationLevel, colors));
        } else if (tag instanceof NbtList) {
            result.append(formatNBT((NbtList) tag, currentIndentation + indentationLevel, indentationLevel, colors));
        } else {
            result.append(colors ? colorTag(tag) : tag.toString());
        }

        result.append(",");
    }

    /**
     * Recursive function used in {@link #formatNBT(NbtCompound, int, boolean)}
     */
    public static String formatNBT(NbtList list, int currentIndentation, int indentationLevel, boolean colors) {
        String bracketColor = "\u00a7f";
        String resetColor = "\u00a7r";
        if (!colors) {
            bracketColor = "";
            resetColor = "";
        }
        StringBuilder result = new StringBuilder(bracketColor + "[" + resetColor);

        String indentation = Strings.repeat(" ", currentIndentation);

        for (NbtElement tag : list) {
            result.append("\n").append(indentation);

            handleTag(currentIndentation, indentationLevel, colors, result, tag);
        }

        if (result.charAt(result.length() - 1) == ',') {
            result.setCharAt(result.length() - 1, ' ');
        }

        if (!list.isEmpty()) {
            result.append("\n").append(Strings.repeat(" ", Math.max(0, currentIndentation - indentationLevel)));
        }
        result.append(bracketColor).append("]").append(resetColor);

        return result.toString();
    }

    /**
     * Colorize an NBT tag
     * @param tag The NBT tag to colorize
     * @return The colorized NBT tag
     */
    @SuppressWarnings("unused")
    public static String colorTag(NbtElement tag) {
        return switch (tag) {
            case NbtByte      n -> color(tag, '2');
            case NbtShort     n -> color(tag, '2');
            case NbtInt       n -> color(tag, '2');
            case NbtLong      n -> color(tag, '2');
            case NbtFloat     n -> color(tag, '2');
            case NbtDouble    n -> color(tag, '2');
            case NbtByteArray n -> color(tag, '9');
            case NbtIntArray  n -> color(tag, '9');
            case NbtString    n -> color(tag, '6');
            default             -> color(tag, 'f');
        };
    }

    /**
     * Colorize a string
     * @param string The string to colorize
     * @param color The color to use
     * @return The colorized string
     */
    private static String color(Object string, char color) {
        return "\u00a7" + color + string.toString() + "\u00a7r";
    }
}
