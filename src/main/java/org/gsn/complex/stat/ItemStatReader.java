package org.gsn.complex.stat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 从物品 lore 中解析属性修改器。
 *
 * 支持格式（颜色格式自动剥离）：
 *   +10 Physical Damage        → FLAT   固定加成
 *   +5% Critical Strike Chance → RELATIVE 百分比加成
 *
 * 同时支持英文和中文属性名称。
 */
public class ItemStatReader {

    /** 匹配 "+<数值>[%] <属性名>" */
    private static final Pattern STAT_PATTERN =
            Pattern.compile("\\+([\\d.]+)(%?)\\s+(.+)");

    private static final PlainTextComponentSerializer PLAIN =
            PlainTextComponentSerializer.plainText();

    private static final Map<String, String> ALIASES = new HashMap<>();

    static {
        // ── 英文 ──────────────────────────────────────────────────────────────
        ALIASES.put("physical damage",        "PHYSICAL_DAMAGE");
        ALIASES.put("magical damage",         "MAGICAL_DAMAGE");
        ALIASES.put("max health",             "MAX_HEALTH");
        ALIASES.put("physical defense",       "PHYSICAL_DEFENSE");
        ALIASES.put("magical defense",        "MAGICAL_DEFENSE");
        ALIASES.put("movement speed",         "MOVEMENT_SPEED");
        ALIASES.put("attack speed",           "ATTACK_SPEED");
        ALIASES.put("attack damage",          "ATTACK_DAMAGE");
        ALIASES.put("critical strike chance", "CRITICAL_STRIKE_CHANCE");
        ALIASES.put("critical strike power",  "CRITICAL_STRIKE_POWER");
        ALIASES.put("armor",                  "ARMOR");
        ALIASES.put("armor toughness",        "ARMOR_TOUGHNESS");

        // ── 中文 ──────────────────────────────────────────────────────────────
        ALIASES.put("物理伤害",  "PHYSICAL_DAMAGE");
        ALIASES.put("魔法伤害",  "MAGICAL_DAMAGE");
        ALIASES.put("最大生命值","MAX_HEALTH");
        ALIASES.put("物理防御",  "PHYSICAL_DEFENSE");
        ALIASES.put("魔法防御",  "MAGICAL_DEFENSE");
        ALIASES.put("移动速度",  "MOVEMENT_SPEED");
        ALIASES.put("攻击速度",  "ATTACK_SPEED");
        ALIASES.put("攻击伤害",  "ATTACK_DAMAGE");
        ALIASES.put("暴击率",    "CRITICAL_STRIKE_CHANCE");
        ALIASES.put("暴击伤害",  "CRITICAL_STRIKE_POWER");
        ALIASES.put("护甲",     "ARMOR");
        ALIASES.put("护甲韧性",  "ARMOR_TOUGHNESS");
    }

    public List<ItemStat> readStats(ItemStack item) {
        List<ItemStat> result = new ArrayList<>();
        if (item == null || !item.hasItemMeta()) return result;

        List<Component> lore = item.getItemMeta().lore();
        if (lore == null) return result;

        for (Component line : lore) {
            String clean = PLAIN.serialize(line).trim();
            Matcher m = STAT_PATTERN.matcher(clean);
            if (!m.find()) continue;

            double  value     = Double.parseDouble(m.group(1));
            boolean isPercent = !m.group(2).isEmpty();
            String  statName  = m.group(3).trim().toLowerCase();
            String  statId    = ALIASES.get(statName);

            if (statId != null) {
                result.add(new ItemStat(statId, value,
                        isPercent ? ModifierType.RELATIVE : ModifierType.FLAT));
            }
        }
        return result;
    }
}
