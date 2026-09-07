package com.animator70.itemprohibiteditems;

import com.animator70.itemprohibiteditems.config.ModConfig;
import com.animator70.itemprohibiteditems.config.WearableConfig;

import net.minecraftforge.fml.common.Mod;

/**
 * 物品禁用机制 (ItemProhibitedItems) 主入口
 * 核心玩法意图：模拟 RLCraft「等级不足无法使用物品」的效果——玩家仍可持有
 * 携带被禁物品，但无法对它们进行「使用」（右键使用、食用、拉动弓箭、放置等）
 * 黑名单从 config 目录下的 itemprohibiteditems-common.toml 读取，玩家可直接编辑。
 */
@Mod(ItemProhibitedItems.MOD_ID)
public class ItemProhibitedItems {

    public static final String MOD_ID = "itemprohibiteditems";

    public ItemProhibitedItems() {
        // 注册配置文件：物品
        ModConfig.register();
        // 注册配置文件：可穿戴装备
        WearableConfig.register();
    }
}
