package com.animator70.itemprohibiteditems.event;

import com.animator70.itemprohibiteditems.ItemProhibitedItems;
import com.animator70.itemprohibiteditems.config.WearableConfig;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;

/**
 * 「可穿戴物品禁用」的服务器权威事件处理（挂在 FORGE bus）。
 *
 * 独立于 {@link BanEventHandler}（普通「使用」禁用），只针对「穿戴」：
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

    /**
     * 扫描的四个原版 ARMOR 装备槽
     */
    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET
    };

    /**
     * 兜底提示文本（配置项为空时的默认值）
     */
    private static final String DEFAULT_MSG = "\u4f60\u65e0\u6cd5\u7a7f\u6234\u6b64\u7269\u54c1!";

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

        if (!stack.isEmpty() && WearableConfig.isBannedEquipable(stack) && event.isCancelable()) {
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

        if (!WearableConfig.WEARABLE.wearableBanEnabled.get()
                || WearableConfig.WEARABLE.bannedWearables.get().isEmpty()) {
            return;
        }

        Player player = event.player;

        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack worn = player.getItemBySlot(slot);

            if (worn.isEmpty() || !WearableConfig.isBannedWearable(worn)) {
                continue;
            }

            unequip(player, slot, worn);
        }
    }

    /** 把指定装备槽的穿戴物卸下，放回背包（背包满则原地丢出），并提示玩家。 */
    private static void unequip(Player player, EquipmentSlot slot, ItemStack worn) {
        player.setItemSlot(slot, ItemStack.EMPTY);

        if (!worn.isEmpty() && !player.getInventory().add(worn)) {
            player.drop(worn, false);
        }

        String text = WearableConfig.WEARABLE.wearableBanMessage.get();

        if (text == null || text.isEmpty()) {
            text = DEFAULT_MSG;
        }

        player.displayClientMessage(Component.literal(text).withStyle(ChatFormatting.RED), true);
    }
}
