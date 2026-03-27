package org.gsn.complex.player;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.gsn.complex.Complex;
import org.gsn.complex.compat.AttributePlusCompat;
import org.gsn.complex.compat.BaikirutoCompat;
import org.gsn.complex.compat.MMOItemsCompat;
import org.gsn.complex.compat.NeigeItemsCompat;
import org.gsn.complex.compat.SertralineCompat;
import org.gsn.complex.compat.SXItemCompat;
import org.gsn.complex.compat.ZaphkielCompat;
import org.gsn.complex.slot.CustomSlot;
import org.gsn.complex.stat.ItemStat;
import org.gsn.complex.stat.ItemStatReader;
import org.gsn.complex.stat.PlayerStatMap;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 存储单个玩家的装备数据，并在装备/卸下时同步属性。
 *
 * <h3>属性后端选择</h3>
 * <ul>
 *   <li><b>AttributePlus 已安装</b>：通过 {@link AttributePlusCompat} 调用
 *       {@code AttributeAPI.addSourceAttribute / takeSourceAttribute}。
 *       Sertraline / Baikiruto / NeigeItems / SX-Item / Zaphkiel 物品会先 rebuild()
 *       刷新 lore，确保 AP 能正确解析动态属性数值；MMOItems 物品直接传入即可。</li>
 *   <li><b>AttributePlus 未安装</b>：回退到 {@link PlayerStatMap}（Bukkit 原生
 *       AttributeModifier + 内存自定义属性）。</li>
 * </ul>
 */
public class PlayerEquipment {

    private static final String KEY_PREFIX = "complex_";

    private final Player                    player;
    private final Complex                   plugin;
    private final Map<CustomSlot, ItemStack> equipped;
    private final ItemStatReader            statReader;
    private final PlayerStatMap             statMap;
    private final AttributePlusCompat       atPlus;
    private final SertralineCompat          sertraline;
    private final BaikirutoCompat           baikiruto;
    private final MMOItemsCompat            mmoItems;
    private final NeigeItemsCompat          neigeItems;
    private final SXItemCompat              sxItem;
    private final ZaphkielCompat            zaphkiel;

    public PlayerEquipment(Player player, Complex plugin,
                           AttributePlusCompat atPlus,
                           SertralineCompat sertraline,
                           BaikirutoCompat baikiruto,
                           MMOItemsCompat mmoItems,
                           NeigeItemsCompat neigeItems,
                           SXItemCompat sxItem,
                           ZaphkielCompat zaphkiel) {
        this.player     = player;
        this.plugin     = plugin;
        this.equipped   = new EnumMap<>(CustomSlot.class);
        this.statReader = new ItemStatReader();
        this.statMap    = new PlayerStatMap(player, plugin);
        this.atPlus     = atPlus;
        this.sertraline = sertraline;
        this.baikiruto  = baikiruto;
        this.mmoItems   = mmoItems;
        this.neigeItems = neigeItems;
        this.sxItem     = sxItem;
        this.zaphkiel   = zaphkiel;
    }

    // ── 公共 API ──────────────────────────────────────────────────────────────

    public Player    getPlayer()                     { return player; }
    public ItemStack getEquipped(CustomSlot slot)    { return equipped.get(slot); }
    public Map<CustomSlot, ItemStack> getEquipped()  { return equipped; }

    /**
     * 将 {@code item} 装备到 {@code slot}，自动卸下原有物品。
     * 传入 null 或 AIR 等同于清空该槽。
     */
    public void equip(CustomSlot slot, ItemStack item) {
        unequip(slot);
        if (item == null || item.getType().isAir()) return;
        equipped.put(slot, item);

        String sourceKey = KEY_PREFIX + slot.name().toLowerCase();

        if (atPlus.isEnabled()) {
            // ── AttributePlus 模式 ─────────────────────────────────────────
            // 各物品库按需 rebuild()，让 AP 能读取到动态属性的最终数值。
            atPlus.applyItem(player, sourceKey, effectiveItem(item));
        } else {
            // ── 回退：ItemStat → PlayerStatMap（Bukkit 原生）─────────────
            List<ItemStat> stats;
            if (sertraline.isSertralineItem(item)) {
                stats = sertraline.readStats(item, player,
                        plugin.getSertralineMappings(), statReader);
            } else if (baikiruto.isBaikirutoItem(item)) {
                stats = baikiruto.readStats(item, player,
                        plugin.getBaikirutoMappings(), statReader);
            } else if (mmoItems.isMMOItem(item)) {
                stats = mmoItems.readStats(item, player,
                        plugin.getMMOItemsMappings(), statReader);
            } else if (neigeItems.isNeigeItem(item)) {
                stats = neigeItems.readStats(item, player,
                        plugin.getNeigeItemsMappings(), statReader);
            } else if (sxItem.isSXItem(item)) {
                stats = sxItem.readStats(item, player, statReader);
            } else if (zaphkiel.isZaphkielItem(item)) {
                stats = zaphkiel.readStats(item, player, statReader);
            } else {
                stats = statReader.readStats(item);
            }

            // 按槽位白名单过滤（仅非 AP 模式支持）
            Set<String> allowed = plugin.getGUIConfig().getAllowedStats(slot);
            if (!allowed.isEmpty()) {
                stats = stats.stream()
                        .filter(s -> allowed.contains(s.getStatId()))
                        .collect(Collectors.toList());
            }

            statMap.apply(sourceKey, stats);
        }
    }

    /**
     * 卸下 {@code slot} 中的物品并返回，同时移除其属性。
     */
    public ItemStack unequip(CustomSlot slot) {
        String sourceKey = KEY_PREFIX + slot.name().toLowerCase();
        if (atPlus.isEnabled()) {
            atPlus.removeItem(player, sourceKey);
        } else {
            statMap.remove(sourceKey);
        }
        return equipped.remove(slot);
    }

    /**
     * 移除所有槽位的属性（玩家下线时调用）。
     */
    public void dispose() {
        if (atPlus.isEnabled()) {
            for (CustomSlot slot : equipped.keySet()) {
                atPlus.removeItem(player, KEY_PREFIX + slot.name().toLowerCase());
            }
        } else {
            statMap.clear();
        }
    }

    /**
     * 查询某项自定义属性在所有装备上的合计值（仅非 AP 模式有效）。
     * AP 模式下属性由 AttributePlus 独立管理，此方法通常返回 0。
     */
    public double getStat(String statId) {
        return statMap.getTotal(statId);
    }

    // ── 内部工具 ──────────────────────────────────────────────────────────────

    /**
     * 返回供 AttributePlus 读取的有效 ItemStack。
     * 各物品库通过 rebuild() 将动态属性写入 lore；
     * 普通物品及 MMOItems 保持原样（lore 已在生成时写入）。
     */
    private ItemStack effectiveItem(ItemStack item) {
        if (sertraline.isSertralineItem(item)) return sertraline.rebuild(item, player);
        if (baikiruto.isBaikirutoItem(item))   return baikiruto.rebuild(item, player);
        if (neigeItems.isNeigeItem(item))      return neigeItems.rebuild(item, player);
        if (sxItem.isSXItem(item))             return sxItem.rebuild(item, player);
        if (zaphkiel.isZaphkielItem(item))     return zaphkiel.rebuild(item, player);
        return item;
    }
}
