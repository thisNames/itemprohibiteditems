package com.animator70.itemprohibiteditems.config;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;

import com.animator70.itemprohibiteditems.ItemProhibitedItems;

/**
 * 注册配置文件：可穿戴装备
 * 「可穿戴物品」禁用配置（独立于 {@link ItemConfig} 的单独配置文件）。
 *
 * 用于禁用：盔甲可穿戴装备、饰品；被列入 bannedWearables 的
 * 物品将无法被穿戴（玩家仍可持有/携带，与主配置一致），尝试装备时会被拦下并
 * 提示；若因 GUI 拖拽、其他模组装备等途径已穿戴上，服务端会在下一个 tick 权威地把它们
 * 卸下并放回背包。
 *
 * 「可穿戴」按原版语义判定：物品能对应到某 ARMOR 装备槽 {@link EquipmentSlot.Type#ARMOR}
 * 即 {@link LivingEntity#getEquipmentSlotForItem} 返回 HEAD/CHEST/LEGS/FEET）。
 * ArmorItem 及其所有子类（含第三方模组的盔甲）、鞘翅等均属此类；因此本功能天然兼容其他模组的盔甲。
 * Curios 独立槽位（非原版 ARMOR 槽）需在 Curios 环境内另配，详见 {@code PROJECT_STATE.md}。
 *
 * 采用 live-read：业务代码每次调用 {@code .get()} 读取最新值，改完配置文件保存并 /reload）即生效，无需重启。
 *
 * 本类为独立配置规格，注册为单独 TOML 文件（见 {@link #register()}），不触碰原 {@link ItemConfig}。
 */
public final class WearableConfig {
    // 可穿戴禁用配置的唯一 Forge 配置规格
    public static final ForgeConfigSpec SPEC;

    // 可穿戴禁用配置值。
    public static final Common WEARABLE;

    // 是否启用
    public static boolean isEnabled = true;

    // 被禁物品 ID 黑名单（用 Set）
    public static Set<String> bannedListCache = Collections.emptySet();

    // 静态初始化：注册配置文件、读取配置值
    static {
        final Pair<Common, ForgeConfigSpec> pair = new ForgeConfigSpec.Builder().configure(Common::new);

        WEARABLE = pair.getLeft();
        SPEC = pair.getRight();
    }

    private WearableConfig() {
    }

    public static class Common {
        // 可穿戴禁用功能总开关。
        public final ForgeConfigSpec.BooleanValue wearableBanEnabled;

        // 被禁止「穿戴」的物品 ID 黑名单
        // 每项为注册表名，格式 "namespace:path"，如
        // "minecraft:diamond_chestplate"、"minecraft:elytra"，默认空
        // 被禁物品仍可持有，但无法穿上。
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> bannedWearables;

        // 尝试穿戴被禁装备时给玩家显示的提示文本（客户端的屏幕红字用）
        public final ForgeConfigSpec.ConfigValue<String> wearableBanMessage;

        /**
         * 构造配置
         */
        public Common(ForgeConfigSpec.Builder builder) {
            builder.push("wearable_ban");

            this.wearableBanEnabled = builder
                    .comment("Item Disable Master Switch.")
                    .define("wearableBanEnabled", true);

            this.bannedWearables = builder
                    .comment(
                            "Write the ID of the item here as a blacklist, pay attention to",
                            "distinguishing between uppercase and lowercase letters as well",
                            "as spaces before and after. Enter the <mod_id> /reload in the game",
                            "to refresh the configuration.")
                    .defineListAllowEmpty(
                            List.of("bannedWearables"),
                            List.of(),
                            obj -> obj instanceof String s && s.indexOf(':') >= 0);

            this.wearableBanMessage = builder
                    .comment(
                            "Message shown to a player who tries to equip a banned wearable.")
                    .define("wearableBanMessage", "NoPermission");

            builder.pop();
        }
    }

    /**
     * 注册为独立的 COMMON 配置文件（config 目录下单独一个 TOML）
     */
    public static void register() {
        ModLoadingContext.get().registerConfig(
                net.minecraftforge.fml.config.ModConfig.Type.COMMON,
                SPEC,
                ItemProhibitedItems.MOD_ID + "-wearable.toml");
    }

    /**
     * 刷新配置值到缓存
     */
    public static void refresh() {
        isEnabled = WEARABLE.wearableBanEnabled.get();
        bannedListCache = Set.copyOf(WEARABLE.bannedWearables.get());
    }
}
