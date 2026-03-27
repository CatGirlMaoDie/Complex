package org.gsn.complex.stat;

public enum ModifierType {
    /** 固定加成，直接加到属性值上（例如 +10 物理伤害）*/
    FLAT,
    /** 百分比加成，乘以基础值（例如 +5% 暴击率）*/
    RELATIVE
}
