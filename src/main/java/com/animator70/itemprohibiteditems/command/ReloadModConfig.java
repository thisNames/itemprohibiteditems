package com.animator70.itemprohibiteditems.command;

import com.mojang.brigadier.CommandDispatcher;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import com.animator70.itemprohibiteditems.ItemProhibitedItems;
import com.animator70.itemprohibiteditems.config.ModConfig;

/**
 * 重载配置文件
 */
@Mod.EventBusSubscriber(modid = ItemProhibitedItems.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ReloadModConfig {
    private ReloadModConfig() {
    }

    /**
     * 注册命令
     */
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        // 重新加载配置
        dispatcher.register(Commands
                // 主指令名称
                .literal(ItemProhibitedItems.MOD_ID)
                // 子指令
                .then(Commands
                        .literal("reload")
                        // 设置权限
                        .requires(source -> source.hasPermission(2))
                        // 执行逻辑
                        .executes(context -> {
                            return ModConfig.reloadingConfig(context);
                        })));

        // 显示物品黑名单
        dispatcher.register(Commands
                // 主指令名称
                .literal(ItemProhibitedItems.MOD_ID)
                // 子指令
                .then(Commands
                        .literal("items")
                        // 设置权限
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> {
                            return ModConfig.showItemBanList(context);
                        })));

        // 显示装备黑名单
        dispatcher.register(Commands
                // 主指令名称
                .literal(ItemProhibitedItems.MOD_ID)
                // 子指令
                .then(Commands
                        .literal("wearables")
                        // 设置权限
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> {
                            return ModConfig.showWearableBanList(context);
                        })));
    }
}
