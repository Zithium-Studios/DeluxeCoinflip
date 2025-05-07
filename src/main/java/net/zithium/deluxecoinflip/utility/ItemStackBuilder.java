/*
 * DeluxeCoinflip Plugin
 * Copyright (c) 2021 - 2025 Zithium Studios. All rights reserved.
 */

package net.zithium.deluxecoinflip.utility;

import net.zithium.deluxecoinflip.DeluxeCoinflipPlugin;
import net.zithium.library.utils.ColorUtil;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;

public class ItemStackBuilder {
    private static DeluxeCoinflipPlugin plugin;

    public static void setPlugin(DeluxeCoinflipPlugin pluginInstance) {
        plugin = pluginInstance;
    }

    private final ItemStack ITEM_STACK;

    public ItemStackBuilder(ItemStack item) {
        this.ITEM_STACK = item;
    }

    public ItemStackBuilder(Material material) {
        this.ITEM_STACK = new ItemStack(material);
    }

    public static ItemStackBuilder getItemStack(ConfigurationSection section) {
        final Material material = Material.matchMaterial(section.getString("material", "null").toUpperCase());

        ItemStack item = null;
        if (material != null) {
            item = new ItemStack(material);
        }
        if (item == null) {
            return new ItemStackBuilder(Material.BARRIER).withName("&cInvalid material");
        }

        if (item.getType() == Material.PLAYER_HEAD && section.contains("base64")) {
            item = Base64Util.getBaseHead(section.getString("base64")).clone();
        }

        ItemStackBuilder builder = new ItemStackBuilder(item);

        if (section.contains("amount")) {
            builder.withAmount(section.getInt("amount"));
        }

        if (section.contains("display_name")) {
            builder.withName(section.getString("display_name"));
        }

        if (section.contains("lore")) {
            builder.withLore(section.getStringList("lore"));
        }

        if (section.contains("custom_model_data")) {
            builder.withCustomModelData(section.getInt("custom_model_data"));
        }

        if (section.contains("glow") && section.getBoolean("glow")) {
            builder.withGlow();
        }

        if (section.contains("item_flags")) {
            List<ItemFlag> flags = new ArrayList<>();
            section.getStringList("item_flags").forEach(text -> {
                try {
                    ItemFlag flag = ItemFlag.valueOf(text);
                    flags.add(flag);
                } catch (IllegalArgumentException ignored) {
                }
            });
            builder.withFlags(flags.toArray(new ItemFlag[0]));
        }

        return builder;
    }

    public ItemStackBuilder withAmount(int amount) {
        ITEM_STACK.setAmount(amount);
        return this;
    }

    public ItemStackBuilder withFlags(ItemFlag... flags) {
        if (ITEM_STACK.getType() == Material.AIR) {
            return this;
        }
        ItemMeta meta = ITEM_STACK.getItemMeta();
        meta.addItemFlags(flags);
        ITEM_STACK.setItemMeta(meta);
        return this;
    }

    public ItemStackBuilder withName(String name) {
        if (ITEM_STACK.getType() == Material.AIR) {
            return this;
        }
        final ItemMeta meta = ITEM_STACK.getItemMeta();
        meta.setDisplayName(ColorUtil.color(name));
        ITEM_STACK.setItemMeta(meta);
        return this;
    }

    public ItemStackBuilder withCustomModelData(int data) {
        final ItemMeta meta = ITEM_STACK.getItemMeta();
        meta.setCustomModelData(data);
        ITEM_STACK.setItemMeta(meta);
        return this;
    }

    public ItemStackBuilder setSkullOwner(OfflinePlayer owner) {
        if (ITEM_STACK.getItemMeta() instanceof SkullMeta skullMeta) {
            skullMeta.setOwningPlayer(owner);
            ITEM_STACK.setItemMeta(skullMeta);
        }
        return this;
    }

    public ItemStackBuilder withLore(List<String> lore) {
        if (ITEM_STACK.getType() == Material.AIR) {
            return this;
        }
        final ItemMeta meta = ITEM_STACK.getItemMeta();
        List<String> coloredLore = new ArrayList<>();
        for (String s : lore) {
            coloredLore.add(ColorUtil.color(s));
        }
        meta.setLore(coloredLore);
        ITEM_STACK.setItemMeta(meta);
        return this;
    }

    public ItemStackBuilder withGlow() {
        final ItemMeta meta = ITEM_STACK.getItemMeta();
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        ITEM_STACK.setItemMeta(meta);
        ITEM_STACK.addUnsafeEnchantment(Enchantment.ARROW_INFINITE, 1);
        return this;
    }

    public ItemStack build() {
        if (plugin != null && ITEM_STACK != null && ITEM_STACK.getItemMeta() != null) {
            var meta = ITEM_STACK.getItemMeta();
            meta.getPersistentDataContainer().set(
                    plugin.getKey("dcf.dupeprotection"),
                    org.bukkit.persistence.PersistentDataType.BYTE,
                    (byte) 1
            );
            ITEM_STACK.setItemMeta(meta);
        }
        return ITEM_STACK;
    }
}
