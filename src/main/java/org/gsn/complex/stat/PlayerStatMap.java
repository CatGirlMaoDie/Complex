package org.gsn.complex.stat;

import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.*;

/**
 * 管理单个玩家所有装备槽的属性修改器。
 *
 * 对于有 Bukkit 原生对应项的属性（如 MAX_HEALTH、ARMOR 等），
 * 会通过 AttributeModifier 实时同步给玩家。
 * 其余自定义属性（如 PHYSICAL_DAMAGE）仅存储在内存中，
 * 可通过 {@link #getTotal(String)} 查询。
 *
 * 计算公式：最终值 = Σ(FLAT) × (1 + Σ(RELATIVE) / 100)
 */
public class PlayerStatMap {

    // 自定义属性 ID → Bukkit Attribute 映射
    private static final Map<String, Attribute> BUKKIT_MAP = new HashMap<>();

    static {
        BUKKIT_MAP.put("MAX_HEALTH",          Attribute.MAX_HEALTH);
        BUKKIT_MAP.put("MOVEMENT_SPEED",      Attribute.MOVEMENT_SPEED);
        BUKKIT_MAP.put("ARMOR",               Attribute.ARMOR);
        BUKKIT_MAP.put("ARMOR_TOUGHNESS",     Attribute.ARMOR_TOUGHNESS);
        BUKKIT_MAP.put("ATTACK_SPEED",        Attribute.ATTACK_SPEED);
        BUKKIT_MAP.put("ATTACK_DAMAGE",       Attribute.ATTACK_DAMAGE);
        BUKKIT_MAP.put("KNOCKBACK_RESISTANCE",Attribute.KNOCKBACK_RESISTANCE);
    }

    private final Player player;
    private final Plugin plugin;

    // slotKey → 该槽位的所有属性修改器列表
    private final Map<String, List<ItemStat>> slotStats = new HashMap<>();

    public PlayerStatMap(Player player, Plugin plugin) {
        this.player = player;
        this.plugin = plugin;
    }

    // ── 公共 API ──────────────────────────────────────────────────────────────

    /** 为指定槽位应用一组属性修改器（会先清除该槽的旧修改器）。 */
    public void apply(String slotKey, List<ItemStat> stats) {
        remove(slotKey);
        if (stats == null || stats.isEmpty()) return;
        slotStats.put(slotKey, new ArrayList<>(stats));
        syncToAttributes(slotKey, stats);
    }

    /** 移除指定槽位的所有属性修改器。 */
    public void remove(String slotKey) {
        List<ItemStat> old = slotStats.remove(slotKey);
        if (old == null) return;
        // 收集该槽涉及的所有 statId，逐一从 Bukkit 属性上移除
        Set<String> affected = new HashSet<>();
        for (ItemStat s : old) affected.add(s.getStatId());
        for (String statId : affected) removeFromAttribute(slotKey, statId);
    }

    /** 移除所有槽位的属性修改器（玩家下线时调用）。 */
    public void clear() {
        for (String slotKey : new HashSet<>(slotStats.keySet())) remove(slotKey);
    }

    /**
     * 获取某项属性在所有装备槽上的最终合计值。
     * 最终值 = Σ(FLAT) × (1 + Σ(RELATIVE) / 100)
     */
    public double getTotal(String statId) {
        double flat = 0, relative = 0;
        for (List<ItemStat> stats : slotStats.values()) {
            for (ItemStat s : stats) {
                if (!s.getStatId().equals(statId)) continue;
                if (s.getType() == ModifierType.FLAT) flat     += s.getValue();
                else                                  relative += s.getValue();
            }
        }
        return flat * (1 + relative / 100.0);
    }

    // ── 内部：Bukkit 属性同步 ─────────────────────────────────────────────────

    private void syncToAttributes(String slotKey, List<ItemStat> stats) {
        // 将同一属性的 FLAT / RELATIVE 分别汇总
        Map<String, double[]> totals = new HashMap<>(); // statId → [flat, relative]
        for (ItemStat s : stats) {
            double[] arr = totals.computeIfAbsent(s.getStatId(), k -> new double[2]);
            if (s.getType() == ModifierType.FLAT) arr[0] += s.getValue();
            else                                  arr[1] += s.getValue();
        }
        for (Map.Entry<String, double[]> entry : totals.entrySet()) {
            applyToAttribute(slotKey, entry.getKey(), entry.getValue()[0], entry.getValue()[1]);
        }
    }

    private void applyToAttribute(String slotKey, String statId, double flat, double relative) {
        Attribute attr = BUKKIT_MAP.get(statId);
        if (attr == null) return;
        AttributeInstance instance = player.getAttribute(attr);
        if (instance == null) return;

        if (flat != 0) {
            NamespacedKey key = nsKey(slotKey, statId, "flat");
            removeByKey(instance, key);
            instance.addModifier(new AttributeModifier(key, flat,
                    AttributeModifier.Operation.ADD_NUMBER));
        }
        if (relative != 0) {
            NamespacedKey key = nsKey(slotKey, statId, "rel");
            removeByKey(instance, key);
            instance.addModifier(new AttributeModifier(key, relative / 100.0,
                    AttributeModifier.Operation.ADD_SCALAR));
        }
    }

    private void removeFromAttribute(String slotKey, String statId) {
        Attribute attr = BUKKIT_MAP.get(statId);
        if (attr == null) return;
        AttributeInstance instance = player.getAttribute(attr);
        if (instance == null) return;
        removeByKey(instance, nsKey(slotKey, statId, "flat"));
        removeByKey(instance, nsKey(slotKey, statId, "rel"));
    }

    private void removeByKey(AttributeInstance instance, NamespacedKey key) {
        for (AttributeModifier mod : new HashSet<>(instance.getModifiers())) {
            if (mod.getKey().equals(key)) {
                instance.removeModifier(mod);
                break;
            }
        }
    }

    /** NamespacedKey 格式：complex:<slotKey>_<statId>_<suffix>（全小写，连字符转下划线）*/
    private NamespacedKey nsKey(String slotKey, String statId, String suffix) {
        String key = (slotKey + "_" + statId + "_" + suffix)
                .toLowerCase().replace("-", "_");
        return new NamespacedKey(plugin, key);
    }
}
