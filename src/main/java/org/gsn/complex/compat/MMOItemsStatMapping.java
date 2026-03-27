package org.gsn.complex.compat;

import org.bukkit.configuration.file.FileConfiguration;
import org.gsn.complex.stat.ModifierType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 单条 MMOItems 属性 ID → Complex 属性 ID 映射规则。
 *
 * 对应 config.yml 中 mmoitems.stat-mapping 列表里的每一项：
 * <pre>
 *   - mmo-stat: "ATTACK_DAMAGE"
 *     stat: PHYSICAL_DAMAGE
 *     type: FLAT
 * </pre>
 * mmo-stat 为 MMOItems 中 ItemStat 的 ID（大小写不敏感），
 * stat 为 Complex 属性 ID。
 */
public class MMOItemsStatMapping {

    private final String       mmoStatId;   // MMOItems ItemStat ID（大写）
    private final String       statId;      // Complex 属性 ID（大写）
    private final ModifierType type;

    public MMOItemsStatMapping(String mmoStatId, String statId, ModifierType type) {
        this.mmoStatId = mmoStatId;
        this.statId    = statId;
        this.type      = type;
    }

    public String       getMmoStatId() { return mmoStatId; }
    public String       getStatId()    { return statId; }
    public ModifierType getType()      { return type; }

    @SuppressWarnings("unchecked")
    public static List<MMOItemsStatMapping> loadAll(FileConfiguration cfg) {
        List<MMOItemsStatMapping> result = new ArrayList<>();
        List<?> raw = cfg.getList("mmoitems.stat-mapping");
        if (raw == null || raw.isEmpty()) return result;

        for (Object entry : raw) {
            if (!(entry instanceof Map<?, ?> map)) continue;
            String mmoStat = (String) map.get("mmo-stat");
            String stat    = (String) map.get("stat");
            if (mmoStat == null || mmoStat.isBlank() || stat == null || stat.isBlank()) continue;

            String typeStr = map.containsKey("type") ? ((String) map.get("type")) : "FLAT";
            ModifierType type;
            try { type = ModifierType.valueOf(typeStr.toUpperCase()); }
            catch (IllegalArgumentException e) { type = ModifierType.FLAT; }

            result.add(new MMOItemsStatMapping(
                    mmoStat.trim().toUpperCase(),
                    stat.trim().toUpperCase(),
                    type));
        }
        return result;
    }
}
