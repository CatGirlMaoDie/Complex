package org.gsn.complex.compat;

import io.lumine.mythic.lib.api.item.NBTItem;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.item.mmoitem.VolatileMMOItem;
import net.Indyuce.mmoitems.stat.data.DoubleData;
import net.Indyuce.mmoitems.stat.data.type.StatData;
import net.Indyuce.mmoitems.stat.type.ItemStat;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.gsn.complex.stat.ItemStatReader;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * MMOItems 物品库的轻量兼容层。
 *
 * 属性读取策略（{@link #readStats}）：
 *  1. 若 config.yml 中配置了 mmoitems.stat-mapping，优先通过
 *     {@link ItemStat#getData(NBTItem)} 直接从 NBT 读取 MMOItems 属性数值；
 *  2. 将读到的值与 lore 解析到的值合并（去重），
 *     保证两种方式都能覆盖到。
 *
 * MMOItems 物品 lore 已在物品生成时写入，无需 rebuild()，
 * 因此在 AttributePlus 模式下可直接将原始物品传给 AP。
 */
public class MMOItemsCompat {

    private boolean enabled = false;

    // ── 初始化 ────────────────────────────────────────────────────────────────

    public void init(Logger logger) {
        if (Bukkit.getPluginManager().getPlugin("MMOItems") == null) {
            logger.info("[Complex] 未检测到 MMOItems，跳过物品兼容。");
            return;
        }
        try {
            // 调用 getStats() 验证 MMOItems API 可访问
            MMOItems.plugin.getStats();
            enabled = true;
            logger.info("[Complex] 检测到 MMOItems，已启用物品兼容。");
        } catch (Exception e) {
            logger.warning("[Complex] MMOItems 兼容初始化失败: " + e.getMessage());
        }
    }

    // ── 公共 API ──────────────────────────────────────────────────────────────

    public boolean isEnabled() { return enabled; }

    /** 判断物品是否由 MMOItems 生成。 */
    public boolean isMMOItem(ItemStack item) {
        if (!enabled || item == null) return false;
        try { return NBTItem.get(item).hasType(); }
        catch (Exception e) { return false; }
    }

    /** 获取 MMOItems 物品 ID，非 MMOItems 物品返回 null。 */
    public String getId(ItemStack item) {
        if (!enabled || item == null) return null;
        try {
            NBTItem nbt = NBTItem.get(item);
            if (!nbt.hasType()) return null;
            return nbt.getString("MMOITEMS_ITEM_ID");
        } catch (Exception e) { return null; }
    }

    /**
     * 从 MMOItems 物品中读取属性列表，整合两种来源：
     * <ol>
     *   <li>通过 {@code mappings} 中配置的 MMOItems 属性 ID 直接从 NBT 读取数值（精确）</li>
     *   <li>通过 lore 文本解析（兼容）</li>
     * </ol>
     * 两者合并，以 mmo-stat 来源优先（相同 statId+type 组合不重复计入）。
     *
     * @param item       待读取的 ItemStack
     * @param player     玩家上下文（保留参数，与其他 compat 类签名保持一致；MMOItems 不需要）
     * @param mappings   config.yml 中加载的 mmoitems.stat-mapping 列表（可为空列表）
     * @param loreReader 共享的 lore 解析器（用于 fallback）
     */
    @SuppressWarnings("rawtypes")
    public List<org.gsn.complex.stat.ItemStat> readStats(ItemStack item, Player player,
                                                          List<MMOItemsStatMapping> mappings,
                                                          ItemStatReader loreReader) {
        List<org.gsn.complex.stat.ItemStat> result = new ArrayList<>();

        // ── 1. 从 MMOItems NBT 属性直接读取 ───────────────────────────────
        if (!mappings.isEmpty()) {
            try {
                VolatileMMOItem mmoItem = new VolatileMMOItem(NBTItem.get(item));
                for (MMOItemsStatMapping m : mappings) {
                    ItemStat mmoStat = MMOItems.plugin.getStats().get(m.getMmoStatId());
                    if (mmoStat == null) continue;
                    StatData data = mmoItem.getData(mmoStat);
                    if (!(data instanceof DoubleData)) continue;
                    double value = ((DoubleData) data).getValue();
                    if (value == 0) continue;
                    result.add(new org.gsn.complex.stat.ItemStat(m.getStatId(), value, m.getType()));
                }
            } catch (Exception ignored) {}
        }

        // ── 2. Lore 解析，补充 mmo-stat 未覆盖的属性 ─────────────────────
        List<org.gsn.complex.stat.ItemStat> loreStats = loreReader.readStats(item);
        for (org.gsn.complex.stat.ItemStat ls : loreStats) {
            boolean duplicate = false;
            for (org.gsn.complex.stat.ItemStat existing : result) {
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
}
