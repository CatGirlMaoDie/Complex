package org.gsn.complex.compat;

import github.saukiya.sxitem.SXItem;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.gsn.complex.stat.ItemStat;
import org.gsn.complex.stat.ItemStatReader;

import java.util.List;
import java.util.logging.Logger;

/**
 * SX-Item 物品库的轻量兼容层。
 *
 * SX-Item 物品属性通过 lore 展示，无独立数据 Map，
 * 因此属性读取策略为：
 *  1. 通过 {@link github.saukiya.sxitem.data.item.ItemManager#updateItem(Player, ItemStack)}
 *     在克隆上刷新 lore（相当于 rebuild）；
 *  2. 用 {@code loreReader} 解析刷新后的 lore。
 */
public class SXItemCompat {

    private boolean enabled = false;

    // ── 初始化 ────────────────────────────────────────────────────────────────

    public void init(Logger logger) {
        if (Bukkit.getPluginManager().getPlugin("SX-Item") == null) {
            logger.info("[Complex] 未检测到 SX-Item，跳过物品兼容。");
            return;
        }
        try {
            // 调用 getItemManager() 验证 API 可访问
            SXItem.getItemManager();
            enabled = true;
            logger.info("[Complex] 检测到 SX-Item，已启用物品兼容。");
        } catch (Exception e) {
            logger.warning("[Complex] SX-Item 兼容初始化失败: " + e.getMessage());
        }
    }

    // ── 公共 API ──────────────────────────────────────────────────────────────

    public boolean isEnabled() { return enabled; }

    /** 判断物品是否由 SX-Item 生成。 */
    public boolean isSXItem(ItemStack item) {
        if (!enabled || item == null) return false;
        try { return SXItem.getItemManager().getItemKey(item) != null; }
        catch (Exception e) { return false; }
    }

    /** 获取 SX-Item 物品 ID，非 SX-Item 物品返回 null。 */
    public String getId(ItemStack item) {
        if (!enabled || item == null) return null;
        try { return SXItem.getItemManager().getItemKey(item); }
        catch (Exception e) { return null; }
    }

    /**
     * 为指定玩家刷新物品 lore（更新动态属性显示）。
     * 操作在克隆上进行，返回克隆；失败时返回原物品克隆。
     */
    public ItemStack rebuild(ItemStack item, Player player) {
        if (!enabled || item == null) return item;
        try {
            ItemStack clone = item.clone();
            SXItem.getItemManager().updateItem(player, clone);
            return clone;
        } catch (Exception e) { return item.clone(); }
    }

    /**
     * 从 SX-Item 物品中读取属性列表。
     * 通过 {@link #rebuild} 刷新 lore 后使用 {@code loreReader} 解析。
     *
     * @param item       待读取的 ItemStack
     * @param player     玩家上下文（用于 rebuild）
     * @param loreReader 共享的 lore 解析器
     */
    public List<ItemStat> readStats(ItemStack item, Player player, ItemStatReader loreReader) {
        ItemStack rebuilt = rebuild(item, player);
        return loreReader.readStats(rebuilt);
    }
}
