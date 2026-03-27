package org.gsn.complex.compat;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.serverct.ersha.api.AttributeAPI;
import org.serverct.ersha.attribute.data.AttributeData;

import java.util.logging.Logger;

/**
 * AttributePlus 属性插件的兼容层。
 *
 * 当 AttributePlus 已安装时，Complex 自定义装备槽的属性将通过
 * AttributeAPI 而非 Bukkit 原生 AttributeModifier 来管理：
 * <ul>
 *   <li>装备时：{@link #applyItem} → AttributeAPI.addSourceAttribute</li>
 *   <li>卸下时：{@link #removeItem} → AttributeAPI.takeSourceAttribute</li>
 * </ul>
 *
 * 对于 Sertraline / Baikiruto 物品，调用方需先执行 rebuild()
 * 刷新 lore（确保动态属性写入物品文字），再传入此类。
 */
public class AttributePlusCompat {

    private boolean enabled = false;

    // ── 初始化 ────────────────────────────────────────────────────────────────

    public void init(Logger logger) {
        if (Bukkit.getPluginManager().getPlugin("AttributePlus") == null) {
            logger.info("[Complex] 未检测到 AttributePlus，使用 Bukkit 原生属性系统。");
            return;
        }
        try {
            AttributeAPI.allServerKey(); // 验证 API 可正常访问
            enabled = true;
            logger.info("[Complex] 检测到 AttributePlus，装备属性将通过 AP 管理。");
        } catch (Exception e) {
            logger.warning("[Complex] AttributePlus 兼容初始化失败: " + e.getMessage());
        }
    }

    // ── 公共 API ──────────────────────────────────────────────────────────────

    public boolean isEnabled() { return enabled; }

    /**
     * 将物品的 AP 属性以 {@code sourceKey} 为来源键添加到玩家。
     * <p>
     * {@code effectiveItem} 应为最终展示状态的 ItemStack（Sertraline / Baikiruto
     * 物品需在调用前通过各自的 rebuild() 刷新 lore）。
     */
    public void applyItem(Player player, String sourceKey, ItemStack effectiveItem) {
        if (!enabled || effectiveItem == null) return;
        try {
            AttributeData data = AttributeAPI.getAttrData(player);
            if (data == null) return;
            AttributeAPI.addSourceAttribute(data, sourceKey, effectiveItem);
        } catch (Exception ignored) {}
    }

    /**
     * 移除玩家身上以 {@code sourceKey} 标识的 AP 属性来源。
     */
    public void removeItem(Player player, String sourceKey) {
        if (!enabled) return;
        try {
            AttributeData data = AttributeAPI.getAttrData(player);
            if (data == null) return;
            AttributeAPI.takeSourceAttribute(data, sourceKey);
        } catch (Exception ignored) {}
    }
}
