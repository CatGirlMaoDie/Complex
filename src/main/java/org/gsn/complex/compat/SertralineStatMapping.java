package org.gsn.complex.compat;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.gsn.complex.stat.ModifierType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 单条 Sertraline 数据路径 → Complex 属性 ID 映射规则。
 *
 * 对应 config.yml 中 sertraline.stat-mapping 列表里的每一项：
 * <pre>
 *   - path: "data.physical_damage"
 *     stat: PHYSICAL_DAMAGE
 *     type: FLAT
 * </pre>
 */
public class SertralineStatMapping {

    private final String       path;
    private final String       statId;
    private final ModifierType type;

    public SertralineStatMapping(String path, String statId, ModifierType type) {
        this.path   = path;
        this.statId = statId;
        this.type   = type;
    }

    public String       getPath()   { return path; }
    public String       getStatId() { return statId; }
    public ModifierType getType()   { return type; }

    // ── 工厂：从 config.yml 批量加载 ─────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public static List<SertralineStatMapping> loadAll(FileConfiguration cfg) {
        List<SertralineStatMapping> result = new ArrayList<>();
        List<?> raw = cfg.getList("sertraline.stat-mapping");
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

            result.add(new SertralineStatMapping(path.trim(), stat.trim().toUpperCase(), type));
        }
        return result;
    }
}
