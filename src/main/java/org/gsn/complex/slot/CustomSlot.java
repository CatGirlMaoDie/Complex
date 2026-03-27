package org.gsn.complex.slot;

/**
 * 所有自定义装备槽的枚举定义。
 * GUI 位置和占位物品已完全移至 config.yml，由 GUIConfig 管理。
 */
public enum CustomSlot {

    HELMET    ("头盔"),
    WEAPON    ("武器"),
    CHESTPLATE("胸甲"),
    RING_1    ("戒指 I"),
    OFFHAND   ("副手"),
    LEGGINGS  ("护腿"),
    RING_2    ("戒指 II"),
    BELT      ("腰带"),
    BOOTS     ("靴子"),
    NECKLACE  ("项链");

    private final String displayName;

    CustomSlot(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() { return displayName; }
}
