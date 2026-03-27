package org.gsn.complex.compat;

import io.github.zzzyyylllty.sertraline.api.SertralineAPI;
import io.github.zzzyyylllty.sertraline.api.SertralineAPIImpl;
import io.github.zzzyyylllty.sertraline.data.ModernSItem;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.gsn.complex.stat.ItemStat;
import org.gsn.complex.stat.ItemStatReader;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Sertraline 物品库的轻量兼容层。
 *
 * 属性读取策略（{@link #readStats}）：
 *  1. 若 config.yml 中配置了 sertraline.stat-mapping，优先通过
 *     {@link ModernSItem#getDeepData(String)} 直接读取数据路径的数值；
 *  2. 将读到的值与 rebuild() 后 lore 解析到的值合并（去重），
 *     保证两种方式都能覆盖到。
 */
public class SertralineCompat {

    private SertralineAPI api;
    private boolean       enabled = false;

    // ── 初始化 ────────────────────────────────────────────────────────────────

    public void init(Logger logger) {
        if (Bukkit.getPluginManager().getPlugin("Sertraline") == null) {
            logger.info("[Complex] 未检测到 Sertraline，跳过物品兼容。");
            return;
        }
        try {
            api = Bukkit.getServicesManager().load(SertralineAPI.class);
            if (api == null) api = new SertralineAPIImpl();
            enabled = true;
            logger.info("[Complex] 检测到 Sertraline，已启用物品兼容。");
        } catch (Exception e) {
            logger.warning("[Complex] Sertraline 兼容初始化失败: " + e.getMessage());
        }
    }

    // ── 公共 API ──────────────────────────────────────────────────────────────

    public boolean isEnabled() { return enabled; }

    /** 判断物品是否由 Sertraline 生成。 */
    public boolean isSertralineItem(ItemStack item) {
        if (!enabled || item == null) return false;
        try { return api.isVaildItem(item); }
        catch (Exception e) { return false; }
    }

    /** 获取 Sertraline 物品 ID，非 Sertraline 物品返回 null。 */
    public String getId(ItemStack item) {
        if (!enabled || item == null) return null;
        try { return api.getId(item); }
        catch (Exception e) { return null; }
    }

    /**
     * 为指定玩家重建物品 lore（刷新动态属性显示）。
     * 返回刷新后的 ItemStack；失败时返回原物品。
     */
    public ItemStack rebuild(ItemStack item, Player player) {
        if (!enabled || item == null) return item;
        try { return api.rebuild(item, player); }
        catch (Exception e) { return item; }
    }

    /**
     * 获取物品对应的 {@link ModernSItem} 定义，用于直接读取 data 节点数值。
     * 非 Sertraline 物品或 API 不可用时返回 null。
     */
    public ModernSItem getModernSItem(ItemStack item) {
        if (!enabled || item == null) return null;
        try {
            String id = api.getId(item);
            if (id == null) return null;
            return api.getItem(id);
        } catch (Exception e) { return null; }
    }

    /**
     * 从 Sertraline 物品中读取属性列表，整合两种来源：
     * <ol>
     *   <li>通过 {@code mappings} 中配置的 data 路径直接读取数值（精确）</li>
     *   <li>通过 {@code rebuild()} 刷新 lore 后使用 {@code loreReader} 解析（兼容）</li>
     * </ol>
     * 两者合并，以 data-path 来源优先（相同 statId+type 组合不重复计入）。
     *
     * @param item       待读取的 ItemStack（可以是原始或已重建的）
     * @param player     玩家上下文（用于 rebuild）
     * @param mappings   config.yml 中加载的 stat-mapping 列表（可为空列表）
     * @param loreReader 共享的 lore 解析器（用于 fallback）
     */
    public List<ItemStat> readStats(ItemStack item, Player player,
                                    List<SertralineStatMapping> mappings,
                                    ItemStatReader loreReader) {
        List<ItemStat> result = new ArrayList<>();

        // ── 1. 从 data 路径读取 ─────────────────────────────────────────────
        if (!mappings.isEmpty()) {
            ModernSItem sItem = getModernSItem(item);
            if (sItem != null) {
                for (SertralineStatMapping m : mappings) {
                    Object raw = null;
                    try { raw = sItem.getDeepData(m.getPath()); }
                    catch (Exception ignored) {}
                    if (raw == null) continue;

                    double value = toDouble(raw);
                    if (value == 0) continue;
                    result.add(new ItemStat(m.getStatId(), value, m.getType()));
                }
            }
        }

        // ── 2. Lore 解析（rebuild 后），补充 data-path 未覆盖的属性 ──────────
        ItemStack rebuilt = rebuild(item, player);
        List<ItemStat> loreStats = loreReader.readStats(rebuilt);
        for (ItemStat ls : loreStats) {
            // 已经由 data-path 提供了同一属性+类型则跳过，避免重复叠加
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
