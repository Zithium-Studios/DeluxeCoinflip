/*
 * DeluxeCoinflip Plugin
 * Copyright (c) 2021 - 2022 Lewis D (ItsLewizzz). All rights reserved.
 */

package fun.lewisdev.deluxecoinflip.menu;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import fun.lewisdev.deluxecoinflip.utility.universal.XMaterial;
import org.apache.commons.lang.StringUtils;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Base64Util {

    private static final Map<String, ItemStack> cache = new HashMap<>();

    public static ItemStack getBaseHead(String data) {
        if (cache.containsKey(data)) return cache.get(data);

        ItemStack head = XMaterial.PLAYER_HEAD.parseItem();
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        GameProfile profile = new GameProfile(UUID.randomUUID(), "");
        profile.getProperties().put("textures", new Property("textures", data));
        Field profileField;
        try {
            profileField = meta.getClass().getDeclaredField("profile");
            profileField.setAccessible(true);
            profileField.set(meta, profile);
        } catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
            e.printStackTrace();
        }
        head.setItemMeta(meta);
        cache.put(data, head);
        return head;
    }

    private static String getUrl(final String u) {
        String uuid;
        uuid = StringUtils.replace(u, "-", "");
        URL url = null;

        try {
            url = new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid + "?unsigned=false");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        try {
            InputStreamReader reader = new InputStreamReader(url.openStream());
            JsonObject json = new JsonParser().parse(reader).getAsJsonObject().get("properties").getAsJsonArray().get(0).getAsJsonObject();
            return json.get("value").getAsString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static GameProfile getGameProfile(String url) {
        GameProfile profile = new GameProfile(UUID.randomUUID(), null);
        profile.getProperties().put("textures", new Property("textures", url));
        return profile;
    }

    public static ItemStack getSkull(UUID uuid) {
        if(cache.containsKey(uuid.toString())) return cache.get(uuid.toString());

        String skinUrl = getUrl(uuid.toString());
        ItemStack head = XMaterial.PLAYER_HEAD.parseItem();
        if (skinUrl.isEmpty()) return head;

        SkullMeta headMeta = (SkullMeta) head.getItemMeta();
        GameProfile profile = getGameProfile(skinUrl);
        Field profileField;
        try {
            profileField = headMeta.getClass().getDeclaredField("profile");
            profileField.setAccessible(true);
            profileField.set(headMeta, profile);
        } catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException e1) {
            e1.printStackTrace();
        }
        head.setItemMeta(headMeta);
        cache.put(uuid.toString(), head);
        return head;
    }

}
