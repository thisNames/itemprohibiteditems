package com.animator70.itemprohibiteditems.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

/**
 * 注册配置文件：物品
 * 用 Forge 的 COMMON 配置（写入 config 目录下：itemprohibiteditems-common.toml
 * 玩家可直接编辑文件中的：bannedItems 列表来维护黑名单，无需改代码或重启开服，保存并 /reload（或 Mods 菜单重新加载配置）即可生效。
 * 采用 live-read 模式：业务代码每次直接调用 .get() 读取最新值
 * 因此修改配置文件后即时反映，无需额外缓存与刷新事件。
 */
public final class ModConfig {

    // 本模组唯一 Forge 配置规格
    public static final ForgeConfigSpec SPEC;
    // 本模组公共（双端）配置值
    public static final Common COMMON;

    /**
     * 静态初始化块：构建配置规格与值对象
     */
    static {
        final Pair<Common, ForgeConfigSpec> pair = new ForgeConfigSpec.Builder().configure(Common::new);

        COMMON = pair.getLeft();
        SPEC = pair.getRight();
    }

    private ModConfig() {
    }

    public static class Common {

        // 功能总开关
        public final ForgeConfigSpec.BooleanValue banEnabled;

        // 被禁止「使用」的物品 ID 黑名单。
        // 每项为注册表名，格式 "namespace:path"，如 "minecraft:diamond_sword"
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> bannedItems;

        // 尝试使用被禁物品时给玩家显示的提示文本
        public final ForgeConfigSpec.ConfigValue<String> banMessage;

        public Common(ForgeConfigSpec.Builder builder) {
            builder.push("item_ban");

            this.banEnabled = builder
                    .comment(
                            "Whether the item-banning mechanic is active.",
                            "When false the blacklist is ignored entirely.")
                    .define("banEnabled", true);

            this.bannedItems = builder
                    .comment(
                            "Blacklist of item IDs whose USE is disallowed.",
                            "Each entry is a registry name in the form 'namespace:path',",
                            "e.g. 'minecraft:diamond_sword' or 'minecraft:apple'.",
                            "Banned items can still be held / carried by players;",
                            "they simply cannot be used (similar to RLCraft level-gating).",
                            "Add or remove entries here and the change applies on next reload.")
                    .defineListAllowEmpty(
                            List.of("bannedItems"),
                            List.of(),
                            obj -> obj instanceof String s && s.indexOf(':') >= 0);

            this.banMessage = builder
                    .comment(
                            "Message shown to a player who tries to use a banned item.")
                    .define("banMessage", "\u4f60\u65e0\u6cd5\u4f7f\u7528\u6b64\u7269\u54c1!");

            builder.pop();
        }
    }

    /**
     * 把本配置注册为 COMMON 类型（路径在 config 目录）
     */
    public static void register() {

        // 全限定名 net.minecraftforge.fml.config.ModConfig.Type.COMMON，
        // 避免与本文件同名类 ModConfig 冲突。
        ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.COMMON, SPEC);
    }
}
