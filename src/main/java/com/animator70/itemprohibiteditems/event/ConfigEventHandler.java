package com.animator70.itemprohibiteditems.event;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

import com.animator70.itemprohibiteditems.ItemProhibitedItems;
import com.animator70.itemprohibiteditems.config.WearableConfig;
import com.animator70.itemprohibiteditems.config.ItemConfig;

/**
 * 首次启动时加载配置到缓存
 */
@Mod.EventBusSubscriber(modid = ItemProhibitedItems.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ConfigEventHandler {
    /**
     * 配置首次加载时触发（游戏启动、toml 文件读取完毕后）
     */
    @SubscribeEvent
    public static void onConfigLoad(ModConfigEvent.Loading event) {
        // 物品禁用配置
        if (event.getConfig().getSpec() == ItemConfig.SPEC) {
            ItemConfig.refresh();
        }

        // 可穿戴装备配置
        if (event.getConfig().getSpec() == WearableConfig.SPEC) {
            WearableConfig.refresh();
        }
    }
}
