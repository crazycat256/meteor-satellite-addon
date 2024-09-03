/*
 * This file is part of Meteor Satellite Addon (https://github.com/crazycat256/meteor-satellite-addon).
 * Copyright (c) crazycat256.
 */

package fr.crazycat256.satellite.modules.seedmap;

import com.google.gson.JsonObject;
import fr.crazycat256.satellite.Addon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.Util;
import net.minecraft.util.math.Vec3d;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class SeedMap extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> serverPort = sgGeneral.add(new IntSetting.Builder()
        .name("port")
        .description("The port used for connections, your minecraft instance must be able to accept incoming connections on this port.")
        .defaultValue(8080)
        .range(1, 65535)
        .noSlider()
        .build()
    );

    private final Setting<Boolean> onlyLocal = sgGeneral.add(new BoolSetting.Builder()
        .name("only-local")
        .description("Only allow local connections (keep enabled unless you know what you're doing).")
        .defaultValue(true)
        .onChanged(b -> {
            if (this.webSocket != null) {
                this.webSocket.setOnlyLocal(b);
            }
        })
        .build()
    );

    private final Setting<Boolean> debugMessages = sgGeneral.add(new BoolSetting.Builder()
        .name("debug-messages")
        .description("Show debug messages in chat.")
        .defaultValue(false)
        .onChanged(b -> {
            if (this.webSocket != null) {
                this.webSocket.setDebugMessages(b);
            }
        })
        .build()
    );


    private final String script;

    private SeedMapWebSocket webSocket;
    private int lastX = 0;
    private int lastZ = 0;
    private String lastDimension = null;

    public SeedMap() {
        super(Addon.CATEGORY, "seed-map", "Show dynamicly your position on Chunkbase's Seed Map");

        try (InputStream inputStream = SeedMap.class.getResourceAsStream("/assets/satellite/seedmap-script.js");
             InputStreamReader streamReader = new InputStreamReader(inputStream);
             BufferedReader reader = new BufferedReader(streamReader)) {

            StringBuilder content = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }

            script = content.toString();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onActivate() {
        close();
        webSocket = new SeedMapWebSocket(serverPort.get(), onlyLocal.get(), debugMessages.get());
        Thread thread = new Thread(webSocket);
        thread.start();
    }

    @Override
    public WWidget getWidget(GuiTheme theme) {
        WVerticalList list = theme.verticalList();

        WHorizontalList b = list.add(theme.horizontalList()).expandX().widget();

        WButton copyScript = b.add(theme.button("Copy script")).expandX().widget();
        copyScript.action = () -> mc.keyboard.setClipboard(String.format(script, serverPort.get()));

        WButton site = list.add(theme.button("Open Seed Map")).expandX().widget();
        site.action = () -> Util.getOperatingSystem().open("https://www.chunkbase.com/apps/seed-map");

        return list;
    }

    @Override
    public void onDeactivate() {
        close();
    }

    public void close() {
        try {
            if (webSocket != null) {
                webSocket.stop();
                webSocket = null;
            }
        } catch (Exception ignored) {}
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (webSocket == null || mc.player == null || mc.world == null) return;

        Vec3d pos = mc.player.getPos();
        int x = (int) pos.x;
        int z = (int) pos.z;

        String dimension = mc.world.getRegistryKey().getValue().toString().substring(10); // Remove the "minecraft:"


        JsonObject packet = new JsonObject();
        if (x != lastX)
            packet.addProperty("x", x);
        if (z != lastZ)
            packet.addProperty("z", z);
        if (!dimension.equals(lastDimension)) {
            packet.addProperty("dimension", dimension);
        }

        if (!packet.isEmpty()) {
            webSocket.broadcast(packet.toString());
        }
        lastX = x;
        lastZ = z;
        lastDimension = dimension;
    }
}
