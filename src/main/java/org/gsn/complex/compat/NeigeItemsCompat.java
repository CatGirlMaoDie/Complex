package org.gsn.complex.compat;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.gsn.complex.stat.ItemStat;
import org.gsn.complex.stat.ItemStatReader;
import pers.neige.neigeitems.item.ItemInfo;
import pers.neige.neigeitems.manager.ItemManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

/**
 * NeigeItems 物品库的轻量兼容层。
 *
 * 属性读取策略（{@link #readStats}）：
 *  1. 若 config.yml 中配置了 neigeitems.stat-mapping，优先通过
 *     {@link ItemInfo#getData()} 直接从物品 data 键读取数值；
 *  2. 将读到的值与 rebuild() 后 lore 解析到的值合并（去重），
 *     保证两种方式都能覆盖到。
 *
 * rebuild() 通过 {@link ItemManager#rebuild(ItemStack, OfflinePlayer, java.util.Map)}
 * （@JvmStatic member extension）原地刷新物品的 lore；
 * 实际调用时传入克隆，不影响存储槽中的原始物品。
 */
public class NeigeItemsCompat {

    private boolean enabled = false;

    // ── 初始化 ────────────────────────────────────────────────────────────────

    public void init(Logger logger) {
        if (Bukkit.getPluginManager().getPlugin("NeigeItems") == null) {
            logger.info("[Complex] 未检测到 NeigeItems，跳过物品兼容。");
            return;
        }
        try {
            // 调用 isNiItem 验证 API 可访问
            ItemManager.INSTANCE.isNiItem(null);
            enabled = true;
            logger.info("[Complex] 检测到 NeigeItems，已启用物品兼容。");
        } catch (Exception e) {
            logger.warning("[Complex] NeigeItems 兼容初始化失败: " + e.getMessage());
        }
    }

    // ── 公共 API ──────────────────────────────────────────────────────────────

    public boolean isEnabled() { return enabled; }

    /** 判断物品是否由 NeigeItems 生成。 */
    public boolean isNeigeItem(ItemStack item) {
        if (!enabled || item == null) return false;
        try { return ItemManager.INSTANCE.isNiItem(item) != null; }
        catch (Exception e) { return false; }
    }

    /** 获取 NeigeItems 物品 ID，非 NeigeItems 物品返回 null。 */
    public String getId(ItemStack item) {
        if (!enabled || item == null) return null;
        try { return ItemManager.INSTANCE.getItemId(item); }
        catch (Exception e) { return null; }
    }

    /**
     * 为指定玩家重建物品 lore（刷新动态属性显示）。
     * 操作在克隆上进行，返回克隆；失败时返回原物品克隆。
     */
    public ItemStack rebuild(ItemStack item, Player player) {
        if (!enabled || item == null) return item;
        try {
            ItemStack clone = item.clone();
            ItemManager.rebuild(clone, player, new HashMap<>());
            return clone;
        } catch (Exception e) { return item.clone(); }
    }

    /**
     * 从 NeigeItems 物品中读取属性列表，整合两种来源：
     * <ol>
     *   <li>通过 {@code mappings} 中配置的 data 键直接读取数值（精确）</li>
     *   <li>通过 {@code rebuild()} 刷新 lore 后使用 {@code loreReader} 解析（兼容）</li>
     * </ol>
     * 两者合并，以 data 键来源优先（相同 statId+type 组合不重复计入）。
     *
     * @param item       待读取的 ItemStack
     * @param player     玩家上下文（用于 rebuild）
     * @param mappings   config.yml 中加载的 stat-mapping 列表（可为空列表）
     * @param loreReader 共享的 lore 解析器（用于 fallback）
     */
    public List<ItemStat> readStats(ItemStack item, Player player,
                                    List<NeigeItemsStatMapping> mappings,
                                    ItemStatReader loreReader) {
        List<ItemStat> result = new ArrayList<>();

        // ── 1. 从 data 键读取 ────────────────────────────────────────────
        if (!mappings.isEmpty()) {
            try {
                ItemInfo info = ItemManager.INSTANCE.isNiItem(item, true);
                if (info != null) {
                    HashMap<String, String> data = info.getData();
                    if (data != null) {
                        for (NeigeItemsStatMapping m : mappings) {
                            String raw = data.get(m.getPath());
                            if (raw == null) continue;
                            double value = toDouble(raw);
                            if (value == 0) continue;
                            result.add(new ItemStat(m.getStatId(), value, m.getType()));
                        }
                    }
                }
            } catch (Exception ignored) {}
        }

        // ── 2. Lore 解析（rebuild 后），补充 data 键未覆盖的属性 ──────────
        ItemStack rebuilt = rebuild(item, player);
        List<ItemStat> loreStats = loreReader.readStats(rebuilt);
        for (ItemStat ls : loreStats) {
            boolean duplicate = false;
            for (ItemStat existing : result) {
                if (existing.getStatId().equals(ls.getStatId())
                        && existing.getType() == ls.getType()) {
                    duplicate = true;
                    break;
                }
            }
            if (!duplicate) result.add(ls);
        }

        return result;
    }

    // ── 内部工具 ──────────────────────────────────────────────────────────────

    private static double toDouble(Object obj) {
        if (obj instanceof Number) return ((Number) obj).doubleValue();
        try { return Double.parseDouble(obj.toString().trim()); }
        catch (NumberFormatException e) { return 0; }
    }
}
