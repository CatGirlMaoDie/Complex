package org.gsn.complex.stat;

/** 从物品上解析出的单条属性修改器。 */
public class ItemStat {

    private final String statId;
    private final double value;
    private final ModifierType type;

    public ItemStat(String statId, double value, ModifierType type) {
        this.statId = statId;
        this.value  = value;
        this.type   = type;
    }

    public String       getStatId() { return statId; }
    public double       getValue()  { return value; }
    public ModifierType getType()   { return type; }
}
