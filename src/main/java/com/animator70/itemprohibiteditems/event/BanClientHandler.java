package com.animator70.itemprohibiteditems.event;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;

import com.animator70.itemprohibiteditems.ItemProhibitedItems;
import com.animator70.itemprohibiteditems.config.ItemConfig;
import com.animator70.itemprohibiteditems.config.WearableConfig;

/**
 * 客户端侧拦截（仅 Dist.CLIENT 加载，挂在 FORGE bus）。
 *
 * 目的：让被禁物品的「使用 / 攻击」动作，从一开始就不发生：
 * 不播拉弓、吃东西、挥臂动画、不发无意义的服务端包，而非等动画开始后由服务端强行回滚。
 * 
 * 分两类处理：
 * 
 * 右键使用、吃、喝、拉弓、投掷、对被禁方块的放置、去皮等：
 * 由客户端自身的：{@link PlayerInteractEvent.RightClickItem}
 * 取消、{@link PlayerInteractEvent.RightClickBlock}
 * 
 * 将物品使用置为 {@link Event.Result#DENY}。这两个事件在 {@code item.use / useOn} 之前触发，
 * 取消后本地不会先行预测（正是那把「先把原木变去皮又变回来」的闪烁的成因）。
 * 只 DENY 物品自身、保留目标方块交互（开箱等照常）。
 * 
 * 左键攻击、挖掘，由：
 * {@link InputEvent.InteractionKeyMappingTriggered}（仅攻击键）取消，主手被禁物品时挥臂与伤害都不发生。
 * 
 * 提示信息在 {@link RenderGuiEvent.Post} 里以屏幕中间的红字短暂渲染。
 * 服务端权威兜底见 {@link ItemBanEventHandler}（客户端即便直发包也会被服务端再次拦截）
 */
@Mod.EventBusSubscriber(modid = ItemProhibitedItems.MOD_ID, value = Dist.CLIENT)
public final class BanClientHandler {
    // 屏幕中间红字提示停留时长（毫秒）。每次尝试被拦都会刷新计时
    private static final long DISPLAY_MS = 1500L;

    // 当前待显示的提示文本（null 表示不显示）
    private static String pendingText;

    // 提示消失的时间戳（毫秒）
    private static long showUntilMs;

    // 屏幕中间红字的 ARGB 颜色
    private static final int RED_COLOR = 0xFFFF0000;

    // 兜底提示文本（普通禁用与可穿戴禁用各自配置项为空时的默认值）
    private static final String DEFAULT_MSG = "\u4f60\u65e0\u6cd5\u4f7f\u7528\u6b64\u7269\u54c1!";
    private static final String DEFAULT_WEARABLE_MSG = "\u4f60\u65e0\u6cd5\u7a7f\u6234\u6b64\u7269\u54c1!";

    private BanClientHandler() {
    }

    /**
     * 右键「使用物品」（对空气 / 对生物，走 item.use）：取消即不进入吃东西 / 拉弓等动画。
     */
    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        // 仅客户端加载
        if (event.getSide() != LogicalSide.CLIENT) {
            return;
        }

        ItemStack stack = event.getItemStack();

        if (!stack.isEmpty() && event.isCancelable()) {
            if (ItemBanEventHandler.isBannedItem(stack)) {
                // 被禁物品：右键即试图使用，取消。
                event.setCanceled(true);
                armOverlayDefault();
            } else if (WearableBanHandler.isBannedEquipable(stack)) {
                // 被禁可穿戴物：右键即试图穿上，取消（走 ArmorItem.use 的换装不会发生）。
                event.setCanceled(true);
                armOverlayWearable();
            }
        }
    }

    /**
     * 右键「对目标方块使用物品」（放置 / 去皮等）：只 DENY 物品自身，保留方块交互。
     * 客户端在本地先行执行 useOn 前就被挡下，杜绝「去皮后闪回原木」的预测抖动。
     */
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        // 仅客户端加载
        if (event.getSide() != LogicalSide.CLIENT) {
            return;
        }

        ItemStack stack = event.getItemStack();

        // 被禁物品：取消使用，不播放动画
        if (!stack.isEmpty() && ItemBanEventHandler.isBannedItem(stack) && event.getUseItem() != Event.Result.DENY) {
            event.setUseItem(Event.Result.DENY);
            armOverlayDefault();
        }
    }

    /**
     * 左键攻击 / 挖掘（isAttack）：主手持被禁武器时取消整个动作且不挥臂。
     */
    @SubscribeEvent
    public static void onKeyInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isAttack()) {
            return;
        }

        Player player = Minecraft.getInstance().player;

        if (player == null) {
            return;
        }

        ItemStack stack = player.getMainHandItem();

        if (stack.isEmpty() || !ItemBanEventHandler.isBannedItem(stack)) {
            return;
        }

        event.setCanceled(true);
        event.setSwingHand(false);
        armOverlayDefault();
    }

    /**
     * 把「屏幕中间红字」提示武装起来（普通「使用」禁用消息）。
     */
    private static void armOverlayDefault() {
        String text = ItemConfig.COMMON.banMessage.get();

        if (text == null || text.isEmpty()) {
            text = DEFAULT_MSG;
        }

        armOverlayRaw(text);
    }

    /**
     * 把「屏幕中间红字」提示武装起来（可穿戴禁用消息）。
     */
    private static void armOverlayWearable() {
        String text = WearableConfig.WEARABLE.wearableBanMessage.get();

        if (text == null || text.isEmpty()) {
            text = DEFAULT_WEARABLE_MSG;
        }

        armOverlayRaw(text);
    }

    /**
     * 设置待显示文本并刷新显示窗口
     */
    private static void armOverlayRaw(String text) {
        pendingText = text;
        showUntilMs = System.currentTimeMillis() + DISPLAY_MS;
    }

    /**
     * 在 HUD 之上以屏幕中间的红字渲染提示（短暂显示）
     */
    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (pendingText == null || System.currentTimeMillis() > showUntilMs) {
            return;
        }

        Font font = Minecraft.getInstance().font;

        if (font == null) {
            return;
        }

        GuiGraphics graphics = event.getGuiGraphics();
        int width = event.getWindow().getGuiScaledWidth();
        int height = event.getWindow().getGuiScaledHeight();

        // 横向居中、纵向靠近正中偏上（避开准星），落在「屏幕中间」区域。
        graphics.drawCenteredString(font, pendingText, width / 2, height / 2 - font.lineHeight, RED_COLOR);
    }
}
