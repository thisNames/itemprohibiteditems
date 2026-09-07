package com.animator70.itemprohibiteditems.event;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

import com.animator70.itemprohibiteditems.ItemProhibitedItems;
import com.animator70.itemprohibiteditems.config.ModConfig;

/**
 * 物品禁用机制的运行期事件处理（挂在 FORGE bus）。
 *
 * 目标：禁止「使用」黑名单物品，但允许持有。本类是服务端权威兜底，防止绕过
 * 
 * 客户端输入级拦截的直发包客户端真正生效：
 * {@link PlayerInteractEvent.RightClickItem}：右键使用（吃/喝/拉弓/投掷/放置于空气）→ 取消
 * {@link PlayerInteractEvent.RightClickBlock}：右键方块时本应触发物品 {@code useOn}
 * （如放置方块）→ 把 useItem 置为 {@code DENY}，阻止物品使用但保留目标方块的交互。
 * 
 * {@link AttackEntityEvent}：用被禁武器左键攻击生物 → 取消，阻止造成伤害。
 * {@link TickEvent.PlayerTickEvent}：兜底——若玩家正在使用的物品恰好被列入黑名单（例如使用途中配置被改），强制终止该次使用。
 * 
 * 正常客户端在发起「使用 / 攻击」动作之前就被 {@link BanClientHandler}（客户端输入级拦截）
 * 挡下，动画不会开始、也不发包到本端；此处仅兜底绕过拦截的直发包客户端。
 * 提示信息（屏幕中间红字）由 {@code BanClientHandler} 在客户端渲染，本类不再发消息。
 */
@Mod.EventBusSubscriber(modid = ItemProhibitedItems.MOD_ID)
public final class BanEventHandler {

    private BanEventHandler() {
    }

    /**
     * 尝试「使用」（右键空气等）被禁物品：服务端权威取消该次使用。
     */
    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getSide() != LogicalSide.SERVER) {
            return;
        }

        ItemStack stack = event.getItemStack();

        if (!stack.isEmpty() && isBannedItem(stack) && event.isCancelable()) {
            event.setCanceled(true);
        }
    }

    /**
     * 对目标方块使用物品（例如用被禁方块右键方块面放置）：
     * 仅 DENY 物品自身使用，不影响被右键方块本身的交互（如打开箱子）。
     */
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getSide() != LogicalSide.SERVER) {
            return;
        }

        ItemStack stack = event.getItemStack();

        if (!stack.isEmpty() && isBannedItem(stack) && event.getUseItem() != Event.Result.DENY) {
            event.setUseItem(Event.Result.DENY);
        }
    }

    /**
     * 用被禁武器（主手）左键攻击实体：服务端权威取消，阻止任何伤害。
     * 正常客户端已被输入级拦截不会走到这里；此处仅为拦截直接发包的客户端。
     */
    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        Player player = event.getEntity();

        if (player.level().isClientSide) {
            return;
        }

        ItemStack stack = player.getMainHandItem();

        if (!stack.isEmpty() && isBannedItem(stack) && event.isCancelable()) {
            event.setCanceled(true);
        }
    }

    /**
     * 兜底：若玩家正在使用的物品已进入黑名单（使用途中配置变更等），强制终止使用。
     */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.side != LogicalSide.SERVER || event.phase != TickEvent.Phase.END) {
            return;
        }

        Player player = event.player;

        if (player.isUsingItem()) {
            ItemStack using = player.getUseItem();

            if (!using.isEmpty() && isBannedItem(using)) {
                player.stopUsingItem();
            }
        }
    }

    /** 判断物品是否处于禁用状态且功能开启。供本类及 {@link BanClientHandler} 复用。 */
    static boolean isBannedItem(ItemStack stack) {
        if (!ModConfig.COMMON.banEnabled.get()) {
            return false;
        }

        List<? extends String> banned = ModConfig.COMMON.bannedItems.get();

        if (banned == null || banned.isEmpty()) {
            return false;
        }

        Item item = stack.getItem();

        ResourceLocation key = ForgeRegistries.ITEMS.getKey(item);

        if (key == null) {
            return false;
        }

        String full = key.toString();

        for (String entry : banned) {
            if (entry == null) {
                continue;
            }

            String e = entry.trim();

            if (e.equals(full) || e.equalsIgnoreCase("minecraft:" + key.getPath())) {
                return true;
            }
        }

        return false;
    }
}
