/*
 * DeluxeCoinflip Plugin
 * Copyright (c) 2021 - 2022 Lewis D (ItsLewizzz). All rights reserved.
 */

package net.zithium.deluxecoinflip.utility;

import net.zithium.deluxecoinflip.utility.universal.XMaterial;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ItemStackBuilder {

    private final ItemStack ITEM_STACK;

    public ItemStackBuilder(ItemStack item) {
        this.ITEM_STACK = item;
    }

    public static ItemStackBuilder getItemStack(ConfigurationSection section) {
        final Optional<XMaterial> xMaterial = XMaterial.matchXMaterial(section.getString("material", "null").toUpperCase());

        ItemStack item;
        if (xMaterial.isPresent()) {
            item = xMaterial.get().parseItem();
        } else {
            return new ItemStackBuilder(XMaterial.BARRIER.parseItem()).withName("&cInvalid material");
        }

        if (item.getType() == XMaterial.PLAYER_HEAD.parseMaterial() && section.contains("base64")) {
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
        ItemMeta meta = ITEM_STACK.getItemMeta();
        meta.addItemFlags(flags);
        ITEM_STACK.setItemMeta(meta);
        return this;
    }

    public ItemStackBuilder withName(String name) {
        final ItemMeta meta = ITEM_STACK.getItemMeta();
        meta.setDisplayName(TextUtil.color(name));
        ITEM_STACK.setItemMeta(meta);
        return this;
    }

    public ItemStackBuilder withCustomModelData(int data) {
        final ItemMeta meta = ITEM_STACK.getItemMeta();
        meta.setCustomModelData(data);
        ITEM_STACK.setItemMeta(meta);
        return this;
    }

    @SuppressWarnings("deprecation") // Suppressing im.setOwner(owner.getName();
    public ItemStackBuilder setSkullOwner(OfflinePlayer owner) {

        SkullMeta im = (SkullMeta) ITEM_STACK.getItemMeta();
        try {
            im.setOwningPlayer(owner);
        } catch (ClassCastException | NoSuchMethodError ex) {
            im.setOwner(owner.getName());
        }

        ITEM_STACK.setItemMeta(im);
        return this;
    }

    public ItemStackBuilder withLore(List<String> lore) {
        final ItemMeta meta = ITEM_STACK.getItemMeta();
        List<String> coloredLore = new ArrayList<String>();
        for (String s : lore) {
            coloredLore.add(TextUtil.color(s));
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
        return ITEM_STACK;
    }
}

