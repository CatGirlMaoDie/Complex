package org.gsn.complex.compat;

import org.bukkit.configuration.file.FileConfiguration;
import org.gsn.complex.stat.ModifierType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 单条 Baikiruto data 路径 → Complex 属性 ID 映射规则。
 *
 * 对应 config.yml 中 baikiruto.stat-mapping 列表里的每一项：
 * <pre>
 *   - path: "atk"
 *     stat: PHYSICAL_DAMAGE
 *     type: FLAT
 * </pre>
 * path 为 {@code BaikirutoAPI.getItemData(item)} 返回的 Map 的 key（支持点分隔嵌套）。
 */
public class BaikirutoStatMapping {

    private final String       path;
    private final String       statId;
    private final ModifierType type;

    public BaikirutoStatMapping(String path, String statId, ModifierType type) {
        this.path   = path;
        this.statId = statId;
        this.type   = type;
    }

    public String       getPath()   { return path; }
    public String       getStatId() { return statId; }
    public ModifierType getType()   { return type; }

    @SuppressWarnings("unchecked")
    public static List<BaikirutoStatMapping> loadAll(FileConfiguration cfg) {
        List<BaikirutoStatMapping> result = new ArrayList<>();
        List<?> raw = cfg.getList("baikiruto.stat-mapping");
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

            result.add(new BaikirutoStatMapping(path.trim(), stat.trim().toUpperCase(), type));
        }
        return result;
    }
}
