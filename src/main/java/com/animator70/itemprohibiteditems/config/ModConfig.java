package com.animator70.itemprohibiteditems.config;

import com.mojang.brigadier.context.CommandContext;

import net.minecraft.network.chat.Component;
import net.minecraft.commands.CommandSourceStack;

import com.animator70.itemprohibiteditems.ItemProhibitedItems;

public final class ModConfig {
    private ModConfig() {
    }

    /**
     * 重新加载配置文件
     */
    public static int reloadingConfig(CommandContext<CommandSourceStack> context) {
        // 在这里调用你之前写的缓存重建方法
        ItemConfig.refresh();
        WearableConfig.refresh();

        // 向发送指令的玩家发送成功提示
        context.getSource().sendSuccess(
                () -> Component.literal("§a[" + ItemProhibitedItems.MOD_ID + "] reload config success!"),
                true);

        return 1;
    }

    /**
     * 显示物品黑名单
     */
    public static int showItemBanList(CommandContext<CommandSourceStack> context) {
        // 遍历黑名单
        ItemConfig.bannedListCache.forEach(item -> {
            context.getSource().sendSuccess(
                    () -> Component.literal(item.toString()),
                    true);
        });

        // 显示数量
        context.getSource().sendSuccess(
                () -> Component.literal("item count:" + ItemConfig.bannedListCache.size()),
                true);

        return 1;
    }

    /**
     * 显示装备黑名单
     */
    public static int showWearableBanList(CommandContext<CommandSourceStack> context) {
        // 遍历黑名单
        WearableConfig.bannedListCache.forEach(item -> {
            context.getSource().sendSuccess(
                    () -> Component.literal(item.toString()),
                    true);
        });

        // 显示数量
        context.getSource().sendSuccess(
                () -> Component.literal("wearable count:" + ItemConfig.bannedListCache.size()),
                true);

        return 1;
    }
}
