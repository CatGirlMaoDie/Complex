package org.gsn.complex.compat;

import org.bukkit.configuration.file.FileConfiguration;
import org.gsn.complex.stat.ModifierType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 单条 NeigeItems data 键 → Complex 属性 ID 映射规则。
 *
 * 对应 config.yml 中 neigeitems.stat-mapping 列表里的每一项：
 * <pre>
 *   - path: "atk"
 *     stat: PHYSICAL_DAMAGE
 *     type: FLAT
 * </pre>
 * path 为 {@code ItemInfo.getData()} 返回的 HashMap 的 key，
 * 对应物品 YAML 配置中 data 节点下定义的字段名。
 */
public class NeigeItemsStatMapping {

    private final String       path;    // NeigeItems data 键
    private final String       statId;  // Complex 属性 ID（大写）
    private final ModifierType type;

    public NeigeItemsStatMapping(String path, String statId, ModifierType type) {
        this.path   = path;
        this.statId = statId;
        this.type   = type;
    }

    public String       getPath()   { return path; }
    public String       getStatId() { return statId; }
    public ModifierType getType()   { return type; }

    @SuppressWarnings("unchecked")
    public static List<NeigeItemsStatMapping> loadAll(FileConfiguration cfg) {
        List<NeigeItemsStatMapping> result = new ArrayList<>();
        List<?> raw = cfg.getList("neigeitems.stat-mapping");
        if (raw == null || raw.isEmpty()) return result;

        for (Object entry : raw) {
            if (!(entry instanceof Map<?, ?> map)) continue;
            String path = (String) map.get("path");
            String stat = (String) map.get("stat");
            if (path == null || path.isBlank() || stat == null || stat.isBlank()) continue;

            String typeStr = map.containsKey("type") ? ((String) map.get("type")) : "FLAT";
            ModifierType type;
            try { type = ModifierType.valueOf(typeStr.toUpperCase()); }
            catch (IllegalArgumentException e) { type = ModifierType.FLAT; }

            result.add(new NeigeItemsStatMapping(path.trim(), stat.trim().toUpperCase(), type));
        }
        return result;
    }
}
