package com.animator70.itemprohibiteditems.config;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

/**
 * 注册配置文件：可穿戴装备
 * 「可穿戴物品」禁用配置（独立于 {@link ModConfig} 的单独配置文件）。
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
 * 本类为独立配置规格，注册为单独 TOML 文件（见 {@link #register()}），不触碰原 {@link ModConfig}。
 */
public final class WearableConfig {

    // 可穿戴禁用配置的唯一 Forge 配置规格
    public static final ForgeConfigSpec SPEC;
    // 可穿戴禁用配置值。
    public static final Common WEARABLE;

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

        public Common(ForgeConfigSpec.Builder builder) {
            builder.push("wearable_ban");

            this.wearableBanEnabled = builder
                    .comment(
                            "Whether banning wearable equipment (armor / elytra etc.) is active.",
                            "When false the wearable blacklist is ignored entirely.",
                            "Non-wearable items put in bannedWearables have no effect.")
                    .define("wearableBanEnabled", true);

            this.bannedWearables = builder
                    .comment(
                            "Blacklist of wearable item IDs that cannot be EQUIPPED.",
                            "Each entry is a registry name in the form 'namespace:path',",
                            "e.g. 'minecraft:diamond_chestplate' or 'minecraft:elytra'.",
                            "Items can still be held / carried, but wearing them is blocked",
                            "and, if they are currently worn, they are taken off and returned",
                            "to the inventory.",
                            "Add or remove entries here and the change applies on next reload.")
                    .defineListAllowEmpty(
                            List.of("bannedWearables"),
                            List.of(),
                            obj -> obj instanceof String s && s.indexOf(':') >= 0);

            this.wearableBanMessage = builder
                    .comment(
                            "Message shown to a player who tries to equip a banned wearable.")
                    .define("wearableBanMessage", "\u4f60\u65e0\u6cd5\u7a7f\u6234\u6b64\u7269\u54c1!");

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
                "itemprohibiteditems-wearable.toml");
    }

    /**
     * 判断某物品是否「可穿戴」——能否对应到一个 ARMOR 装备槽（头/胸/腿/脚）
     * 兼容原版及一切返回装备槽的第三方盔甲/鞘翅
     * {@code instanceof} 判断不够，鞘翅并非 {@code ArmorItem} 子类，故用原版
     * {@code getEquipmentSlotForItem} 判定更通用）
     */
    public static boolean isWearableItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }

        EquipmentSlot slot = LivingEntity.getEquipmentSlotForItem(stack);

        return slot != null && slot.getType() == EquipmentSlot.Type.ARMOR;
    }

    /**
     * 判断某物品是否被列入「可穿戴黑名单」（只看名单，不看是否可穿戴）
     * 供服务端装备槽扫描与成员判定复用。
     */
    public static boolean isBannedWearable(ItemStack stack) {
        // 空物品不算「被列入黑名单」
        if (stack == null || stack.isEmpty()) {
            return false;
        }

        // 「可穿戴禁用」功能总开关关闭时，不拦任何物品
        if (!WEARABLE.wearableBanEnabled.get()) {
            return false;
        }

        // 「可穿戴黑名单」为空时，不拦任何物品
        List<? extends String> banned = WEARABLE.bannedWearables.get();

        // 「可穿戴黑名单」为空时，不拦任何物品
        if (banned == null || banned.isEmpty()) {
            return false;
        }

        // 获取物品注册表名
        Item item = stack.getItem();
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(item);

        // 空注册表名不算「被列入黑名单」
        if (key == null) {
            return false;
        }

        // 格式化为「namespace:path」
        String full = key.toString();

        // 逐个检查名单项，匹配时返回 true
        for (String entry : banned) {
            if (entry == null) {
                continue;
            }

            String e = entry.trim();

            if (e.equals(full) || e.equalsIgnoreCase("minecraft:" + key.getPath())) {
                return true;
            }
        }

        // 未匹配到名单项，返回 false
        return false;
    }

    /**
     * 是否为「会被拦下的可穿戴」——既在黑名单里，又确实可穿戴。
     * 用于拦右键「使用/装备」：避免把误加入名单的非穿戴物品的正常使用（吃/放/用）一并拦掉。
     */
    public static boolean isBannedEquipable(ItemStack stack) {
        return isWearableItem(stack) && isBannedWearable(stack);
    }
}
