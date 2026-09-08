package com.animator70.itemprohibiteditems.config;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;

/**
 * 注册配置文件：物品
 * 用 Forge 的 COMMON 配置（写入 config 目录下：itemprohibiteditems-common.toml
 * 玩家可直接编辑文件中的：bannedItems 列表来维护黑名单，无需改代码或重启开服，保存并 /reload（或 Mods 菜单重新加载配置）即可生效。
 * 采用 live-read 模式：业务代码每次直接调用 .get() 读取最新值
 * 因此修改配置文件后即时反映，无需额外缓存与刷新事件。
 */
public final class ItemConfig {
    // 本模组唯一 Forge 配置规格
    public static final ForgeConfigSpec SPEC;

    // 本模组公共（双端）配置值
    public static final Common COMMON;

    // 是否启用
    public static boolean isEnabled = true;

    // 被禁物品 ID 黑名单（用 Set）
    public static Set<String> bannedListCache = Collections.emptySet();

    /**
     * 静态初始化块：构建配置规格与值对象
     */
    static {
        final Pair<Common, ForgeConfigSpec> pair = new ForgeConfigSpec.Builder().configure(Common::new);

        COMMON = pair.getLeft();
        SPEC = pair.getRight();
    }

    private ItemConfig() {
    }

    /**
     * 配置类
     */
    public static class Common {
        // 功能总开关
        public final ForgeConfigSpec.BooleanValue banEnabled;

        // 被禁止「使用」的物品 ID 黑名单。
        // 每项为注册表名，格式 "namespace:path"，如 "minecraft:diamond_sword"
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> bannedItems;

        // 尝试使用被禁物品时给玩家显示的提示文本
        public final ForgeConfigSpec.ConfigValue<String> banMessage;

        /**
         * 构造配置
         */
        public Common(ForgeConfigSpec.Builder builder) {
            builder.push("item_ban");

            // 总开关
            this.banEnabled = builder
                    .comment("Item Disable Master Switch.")
                    .define("banEnabled", true);

            // 黑名单
            this.bannedItems = builder
                    .comment(
                            "Write the ID of the item here as a blacklist, pay attention to",
                            "distinguishing between uppercase and lowercase letters as well",
                            "as spaces before and after. Enter the <mod_id> /reload in the game",
                            "to refresh the configuration.")
                    .defineListAllowEmpty(
                            List.of("bannedItems"),
                            List.of(),
                            obj -> obj instanceof String s && s.indexOf(':') >= 0);

            // 提示文本
            this.banMessage = builder
                    .comment(
                            "Message shown to a player who tries to use a banned item.")
                    .define("banMessage", "NoPermission");

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

    /**
     * 刷新配置值到缓存
     */
    public static void refresh() {
        // 更新开关
        isEnabled = COMMON.banEnabled.get();
        bannedListCache = Set.copyOf(COMMON.bannedItems.get());
    }
}
