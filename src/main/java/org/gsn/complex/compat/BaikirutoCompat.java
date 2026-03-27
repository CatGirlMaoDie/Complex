package org.gsn.complex.compat;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.gsn.complex.stat.ItemStat;
import org.gsn.complex.stat.ItemStatReader;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Baikiruto 物品库的轻量兼容层（纯反射实现，无需编译期 JAR）。
 *
 * 属性读取策略（{@link #readStats}）：
 *  1. 若 config.yml 中配置了 baikiruto.stat-mapping，优先通过
 *     ItemStream.runtimeData（存储于 NBT baikiruto.data 节点）直接读取数值；
 *  2. 将读到的值与 rebuildToItemStack 后的 lore 解析结果合并（去重）。
 *
 * 整个 readStats() 只调用一次 readItem()（单次 NBT 读取），
 * stream 对象同时用于 data-map 读取和 lore rebuild。
 */
public class BaikirutoCompat {

    private boolean enabled = false;

    // 缓存的反射对象
    private Object api;                    // BaikirutoAPI 实例
    private Method methodGetItemId;        // BaikirutoAPI.getItemId(ItemStack) : String?
    private Method methodReadItem;         // BaikirutoAPI.readItem(ItemStack) : ItemStream?
    private Method methodGetRuntimeData;   // ItemStream.getRuntimeData() : Map<String, Any?>
    private Method methodRebuildToStack;   // ItemStream.rebuildToItemStack(Player?) : ItemStack

    // ── 初始化 ────────────────────────────────────────────────────────────────

    public void init(Logger logger) {
        if (Bukkit.getPluginManager().getPlugin("Baikiruto") == null) {
            logger.info("[Complex] 未检测到 Baikiruto，跳过物品兼容。");
            return;
        }
        try {
            // Kotlin object 单例通过 INSTANCE 字段访问
            Class<?> baikirutoClass = Class.forName("org.tabooproject.baikiruto.core.Baikiruto");
            Object   baikirutoInst  = baikirutoClass.getField("INSTANCE").get(null);
            Method   apiMethod      = baikirutoClass.getMethod("api");
            api = apiMethod.invoke(baikirutoInst);  // BaikirutoAPI 实例

            // BaikirutoAPI 上的方法
            methodGetItemId = api.getClass().getMethod("getItemId", ItemStack.class);
            methodReadItem  = api.getClass().getMethod("readItem",  ItemStack.class);

            // ItemStream 上的方法（Kotlin property getter + 方法）
            Class<?> streamClass    = Class.forName("org.tabooproject.baikiruto.core.item.ItemStream");
            methodGetRuntimeData    = streamClass.getMethod("getRuntimeData");
            methodRebuildToStack    = streamClass.getMethod("rebuildToItemStack", Player.class);

            enabled = true;
            logger.info("[Complex] 检测到 Baikiruto，已启用物品兼容。");
        } catch (Exception e) {
            logger.warning("[Complex] Baikiruto 兼容初始化失败: " + e.getMessage());
        }
    }

    // ── 公共 API ──────────────────────────────────────────────────────────────

    public boolean isEnabled() { return enabled; }

    /** 判断物品是否由 Baikiruto 生成。 */
    public boolean isBaikirutoItem(ItemStack item) {
        if (!enabled || item == null) return false;
        try { return methodGetItemId.invoke(api, item) != null; }
        catch (Exception e) { return false; }
    }

    /** 获取 Baikiruto 物品 ID，非 Baikiruto 物品返回 null。 */
    public String getId(ItemStack item) {
        if (!enabled || item == null) return null;
        try {
            Object id = methodGetItemId.invoke(api, item);
            return id != null ? id.toString() : null;
        } catch (Exception e) { return null; }
    }

    /**
     * 为指定玩家重建物品 lore（刷新动态属性显示）。
     * 返回刷新后的 ItemStack；失败时返回原物品。
     */
    public ItemStack rebuild(ItemStack item, Player player) {
        if (!enabled || item == null) return item;
        try {
            Object stream = methodReadItem.invoke(api, item);
            if (stream == null) return item;
            Object result = methodRebuildToStack.invoke(stream, player);
            return result instanceof ItemStack ? (ItemStack) result : item;
        } catch (Exception e) { return item; }
    }

    /**
     * 从 Baikiruto 物品中读取属性列表，整合两种来源：
     * <ol>
     *   <li>通过 {@code mappings} 中配置的 data key 读取 runtimeData 数值（精确）</li>
     *   <li>通过 {@code rebuildToItemStack()} 刷新 lore 后使用 {@code loreReader} 解析（兼容）</li>
     * </ol>
     * 两者合并，以 data-path 来源优先（相同 statId+type 组合不重复计入）。
     * 整个方法只执行一次 NBT 读取（readItem），stream 对象被复用。
     */
    public List<ItemStat> readStats(ItemStack item, Player player,
                                    List<BaikirutoStatMapping> mappings,
                                    ItemStatReader loreReader) {
        List<ItemStat> result = new ArrayList<>();

        // 只读一次 NBT，stream 同时用于 data-map 读取和 rebuild
        Object stream = null;
        try { stream = methodReadItem.invoke(api, item); } catch (Exception ignored) {}

        // ── 1. 从 stream.runtimeData 读取 data-path 属性 ─────────────────────
        if (!mappings.isEmpty() && stream != null) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) methodGetRuntimeData.invoke(stream);
                if (data != null) {
                    for (BaikirutoStatMapping m : mappings) {
                        Object raw = getDeep(data, m.getPath());
                        if (raw == null) continue;
                        double value = toDouble(raw);
                        if (value == 0) continue;
                        result.add(new ItemStat(m.getStatId(), value, m.getType()));
                    }
                }
            } catch (Exception ignored) {}
        }

        // ── 2. Lore 解析（rebuildToItemStack 复用同一 stream），补充未覆盖属性 ──
        ItemStack rebuilt = item;
        if (stream != null) {
            try {
                Object r = methodRebuildToStack.invoke(stream, player);
                if (r instanceof ItemStack) rebuilt = (ItemStack) r;
            } catch (Exception ignored) {}
        }
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

    /** 支持点分隔路径从嵌套 Map 中取值，例如 "stats.atk"。 */
    @SuppressWarnings("unchecked")
    private static Object getDeep(Map<String, Object> map, String path) {
        String[] parts = path.split("\\.", 2);
        Object val = map.get(parts[0]);
        if (parts.length == 1 || val == null) return val;
        if (val instanceof Map<?, ?>) return getDeep((Map<String, Object>) val, parts[1]);
        return null;
    }

    private static double toDouble(Object obj) {
        if (obj instanceof Number) return ((Number) obj).doubleValue();
        try { return Double.parseDouble(obj.toString().trim()); }
        catch (NumberFormatException e) { return 0; }
    }
}
