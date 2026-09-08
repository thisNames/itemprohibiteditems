package com.animator70.itemprohibiteditems.event;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import com.animator70.itemprohibiteditems.ItemProhibitedItems;
import com.animator70.itemprohibiteditems.config.WearableConfig;

/**
 * 「可穿戴物品禁用」的服务器权威事件处理（挂在 FORGE bus）。
 *
 * 独立于 {@link ItemBanEventHandler}（普通「使用」禁用），只针对「穿戴」：
 * 
 * {@link PlayerInteractEvent.RightClickItem}（SERVER）：右键「使用」被禁可穿戴物
 * 品（原版盔甲/鞘翅的穿戴走 {@code item.use}，见 {@code ArmorItem.use}）→ 取消，
 * 阻止通过右键把它穿上，客户端输入级已先拦下并渲染红字，此处兜底绕过拦截的直发包。
 * 
 * {@link TickEvent.PlayerTickEvent}（SERVER END）：权威兜底——玩家任意装备槽（头/
 * 胸/腿/脚）上出现被禁可穿戴物（GUI 拖拽、命令发放、其他模组装备等途径都可能绕开
 * 右键拦截）→ 卸下并放回背包；放不下则原地丢出，并给玩家一条提示。
 * 
 * 只在服务端逻辑端判定，杜绝客户端直发包真正生效。
 */
@Mod.EventBusSubscriber(modid = ItemProhibitedItems.MOD_ID)
public final class WearableBanHandler {
    // 兜底提示文本（配置项为空时的默认值）
    private static final String DEFAULT_MSG = "NoPermission";

    private WearableBanHandler() {
    }

    /**
     * 右键「使用」被禁可穿戴物品（即试图右键穿上）→ 服务端权威取消。
     */
    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getSide() != LogicalSide.SERVER) {
            return;
        }

        ItemStack stack = event.getItemStack();

        if (!stack.isEmpty() && WearableBanHandler.isBannedEquipable(stack) && event.isCancelable()) {
            event.setCanceled(true);
        }
    }

    /**
     * 服务端权威兜底：逐 tick 检查玩家是否穿上了被禁可穿戴物，有则卸下放回背包。
     *
     * 仅当功能开启且名单非空才进入 {@code getItemBySlot} 扫描，空名单时开销几乎为零。
     */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.side != LogicalSide.SERVER || event.phase != TickEvent.Phase.END) {
            return;
        }

        if (!WearableConfig.isEnabled || WearableConfig.bannedListCache.isEmpty()) {
            return;
        }

        Player player = event.player;

        // 展开为 4 次独立调用，消除循环开销，同时保持代码整洁
        checkAndUnequip(player, EquipmentSlot.HEAD);
        checkAndUnequip(player, EquipmentSlot.CHEST);
        checkAndUnequip(player, EquipmentSlot.LEGS);
        checkAndUnequip(player, EquipmentSlot.FEET);
    }

    /**
     * 封装单槽位的检查与卸下逻辑
     */
    private static void checkAndUnequip(Player player, EquipmentSlot slot) {
        ItemStack worn = player.getItemBySlot(slot);

        // 快速跳过空物品，然后进行 O(1) 的 HashSet 查找
        if (!worn.isEmpty() && WearableBanHandler.isBannedWearable(worn)) {
            // 卸下来
            unequip(player, slot, worn);
        }
    }

    /**
     * 把指定装备槽的穿戴物卸下，放回背包（背包满则原地丢出），并提示玩家
     */
    private static void unequip(Player player, EquipmentSlot slot, ItemStack worn) {
        player.setItemSlot(slot, ItemStack.EMPTY);

        if (!player.getInventory().add(worn)) {
            player.drop(worn, false);
        }

        String text = WearableConfig.WEARABLE.wearableBanMessage.get();

        if (text == null || text.isEmpty()) {
            text = DEFAULT_MSG;
        }

        player.displayClientMessage(Component.literal(text).withStyle(ChatFormatting.RED), true);
    }

    /**
     * 判断某物品是否「可穿戴」
     * 能否对应到一个 ARMOR 装备槽（头/胸/腿/脚）
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
        // 功能总开关关闭时，不拦任何物品
        if (!WearableConfig.isEnabled) {
            return false;
        }

        // 空物品不算被列入黑名单
        if (stack == null || stack.isEmpty()) {
            return false;
        }

        // 可穿戴黑名单为空时，不拦任何物品
        if (WearableConfig.bannedListCache.isEmpty()) {
            return false;
        }

        // 获取物品注册表名
        Item item = stack.getItem();
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(item);

        // 空注册表名不算「被列入黑名单」
        if (key == null) {
            return false;
        }

        // 如果物品在黑名单中，则返回 true
        return WearableConfig.bannedListCache.contains(key.toString());
    }

    /**
     * 是否为「会被拦下的可穿戴」
     * 既在黑名单里，又确实可穿戴
     * 用于拦右键「使用/装备」：避免把误加入名单的非穿戴物品的正常使用（吃/放/用）一并拦掉
     */
    public static boolean isBannedEquipable(ItemStack stack) {
        return isWearableItem(stack) && isBannedWearable(stack);
    }
}
