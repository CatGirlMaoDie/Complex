package org.gsn.complex.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.gsn.complex.slot.CustomSlot;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 从 config.yml 解析 GUI 布局与槽位配置。
 *
 * 布局规则：
 *  - layout 为一组字符串，每行恰好 9 个字符
 *  - 每个字符对应 characters 节点下的一个 key
 *  - key 带 filler:true → 填充物格子
 *  - key 带 slot:XXX   → 装备槽（XXX 为 CustomSlot 枚举名，不区分大小写）
 *  - 未定义的字符渲染为填充物
 */
public class GUIConfig {

    private static final LegacyComponentSerializer LEGACY =
            LegacyComponentSerializer.legacyAmpersand();

    private final Component title;
    private final int       rows;

    /** GUI 格子序号 → 装备槽（仅装备槽位有值） */
    private final Map<Integer, CustomSlot> guiToSlot  = new HashMap<>();
    /** 装备槽 → GUI 格子序号 */
    private final Map<CustomSlot, Integer> slotToGui  = new EnumMap<>(CustomSlot.class);
    /** 装备槽 → 空槽占位物品 */
    private final Map<CustomSlot, ItemStack> placeholder = new EnumMap<>(CustomSlot.class);
    /**
     * 装备槽 → 允许生效的属性 ID 集合。
     * 集合为空表示该槽允许所有属性生效（默认行为）。
     */
    private final Map<CustomSlot, Set<String>> allowedStats = new EnumMap<>(CustomSlot.class);
    /** 所有非装备格子统一使用的填充物 */
    private ItemStack filler;

    private GUIConfig(Component title, int rows) {
        this.title = title;
        this.rows  = rows;
    }

    // ── 工厂方法 ──────────────────────────────────────────────────────────────

    public static GUIConfig load(FileConfiguration cfg) {
        Component title = color(cfg.getString("gui.title", "&8【&6RPG 装备&8】"));
        int       rows  = Math.max(1, Math.min(6, cfg.getInt("gui.rows", 6)));
        GUIConfig g     = new GUIConfig(title, rows);

        // 1. 解析 characters 节点
        Map<Character, CharDef> charMap = parseCharacters(cfg);

        // 2. 确定默认填充物
        g.filler = createFallbackFiller();
        for (CharDef def : charMap.values()) {
            if (def.isFiller) { g.filler = def.item; break; }
        }

        // 3. 解析 layout，建立双向映射
        List<String> layout = cfg.getStringList("gui.layout");
        for (int row = 0; row < Math.min(layout.size(), rows); row++) {
            String line = layout.get(row);
            for (int col = 0; col < Math.min(line.length(), 9); col++) {
                char c       = line.charAt(col);
                int  guiSlot = row * 9 + col;
                CharDef def  = charMap.get(c);
                if (def == null || def.isFiller || def.slot == null) continue;
                g.guiToSlot.put(guiSlot, def.slot);
                g.slotToGui.put(def.slot, guiSlot);
                g.placeholder.put(def.slot, def.item);
                g.allowedStats.put(def.slot, def.allowedStats);
            }
        }
        return g;
    }

    // ── 公共 API ──────────────────────────────────────────────────────────────

    public Component getTitle()                       { return title; }
    public int       getSize()                        { return rows * 9; }
    public ItemStack getFiller()                      { return filler.clone(); }

    /** 返回该 GUI 格子对应的装备槽，非装备格子返回 null。 */
    public CustomSlot getEquipSlot(int guiSlot)       { return guiToSlot.get(guiSlot); }

    /** 返回装备槽在 GUI 中的位置，未配置时返回 -1。 */
    public int        getGuiSlot(CustomSlot slot)     { return slotToGui.getOrDefault(slot, -1); }

    /** 返回装备槽的空槽占位物品。 */
    public ItemStack  getPlaceholder(CustomSlot slot) {
        return placeholder.getOrDefault(slot, createFallbackFiller());
    }

    /**
     * 返回指定槽位允许生效的属性 ID 集合（大写）。
     * 集合为空表示允许所有属性生效。
     */
    public Set<String> getAllowedStats(CustomSlot slot) {
        return allowedStats.getOrDefault(slot, Collections.emptySet());
    }

    // ── 内部解析 ──────────────────────────────────────────────────────────────

    private static Map<Character, CharDef> parseCharacters(FileConfiguration cfg) {
        Map<Character, CharDef> map = new HashMap<>();
        ConfigurationSection sec = cfg.getConfigurationSection("gui.characters");
        if (sec == null) return map;
        for (String key : sec.getKeys(false)) {
            if (key.length() != 1) continue;
            ConfigurationSection cs = sec.getConfigurationSection(key);
            if (cs == null) continue;
            map.put(key.charAt(0), new CharDef(cs));
        }
        return map;
    }

    private static ItemStack createFallbackFiller() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.space());
            item.setItemMeta(meta);
        }
        return item;
    }

    /** 将 config.yml 中的 {@code &} 颜色代码字符串转为 Adventure Component。 */
    static Component color(String s) {
        return s == null ? Component.empty() : LEGACY.deserialize(s);
    }

    // ── 内部类 ────────────────────────────────────────────────────────────────

    private static class CharDef {
        final boolean    isFiller;
        final CustomSlot slot;         // null when isFiller == true
        final ItemStack  item;
        /** 允许生效的属性 ID 集合（大写）；空集合 = 全部生效 */
        final Set<String> allowedStats;

        CharDef(ConfigurationSection cs) {
            this.isFiller = cs.getBoolean("filler", false);

            CustomSlot s = null;
            if (!isFiller) {
                String name = cs.getString("slot", "");
                try { s = CustomSlot.valueOf(name.toUpperCase()); }
                catch (IllegalArgumentException ignored) {}
            }
            this.slot = s;
            this.item = buildItem(cs);

            // 解析 allowed-stats 列表；列表为空或不存在表示全部生效
            List<String> raw = cs.getStringList("allowed-stats");
            if (raw.isEmpty()) {
                this.allowedStats = Collections.emptySet();
            } else {
                this.allowedStats = raw.stream()
                        .map(String::toUpperCase)
                        .collect(Collectors.toCollection(LinkedHashSet::new));
            }
        }

        private static ItemStack buildItem(ConfigurationSection cs) {
            String matName = cs.getString("material", "GRAY_STAINED_GLASS_PANE").toUpperCase();
            Material mat;
            try { mat = Material.valueOf(matName); }
            catch (IllegalArgumentException e) { mat = Material.GRAY_STAINED_GLASS_PANE; }

            ItemStack item = new ItemStack(mat);
            ItemMeta  meta = item.getItemMeta();
            if (meta != null) {
                meta.displayName(color(cs.getString("name", " ")));
                List<String> rawLore = cs.getStringList("lore");
                if (!rawLore.isEmpty()) {
                    List<Component> lore = rawLore.stream()
                            .map(GUIConfig::color)
                            .collect(Collectors.toList());
                    meta.lore(lore);
                }
                item.setItemMeta(meta);
            }
            return item;
        }
    }
}
