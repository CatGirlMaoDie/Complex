package org.gsn.complex.compat;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.gsn.complex.stat.ItemStat;
import org.gsn.complex.stat.ItemStatReader;

import java.lang.reflect.Method;
import java.util.List;
import java.util.logging.Logger;

/**
 * Zaphkiel 物品库的轻量兼容层（纯反射实现，无需编译期 JAR）。
 *
 * Zaphkiel 物品属性通过 lore 展示，属性读取策略为：
 *  1. 通过反射调用 {@code ItemStream.rebuildToItemStack(Player)} 刷新物品 lore；
 *  2. 用 {@code loreReader} 解析刷新后的 lore。
 *
 * 注意：使用本兼容层需要将 Zaphkiel JAR 放入服务器 plugins/ 目录。
 * Zaphkiel-Plus 源码位于 {@code libs/Zaphkiel-Plus-main}，
 * 执行 {@code gradle build} 后将 plugin 子模块输出 JAR 复制到服务器即可。
 */
public class ZaphkielCompat {

    private boolean enabled = false;

    // 缓存的反射对象
    private Object  itemHandler;          // ZaphkielItemHandler 实例
    private Method  methodGetItemId;      // getItemId(ItemStack) : String
    private Method  methodRead;           // read(ItemStack) : ItemStream
    private Method  methodIsVanilla;      // ItemStream.isVanilla() : boolean
    private Method  methodRebuild;        // ItemStream.rebuildToItemStack(Player) : ItemStack

    // ── 初始化 ────────────────────────────────────────────────────────────────

    public void init(Logger logger) {
        if (Bukkit.getPluginManager().getPlugin("Zaphkiel") == null) {
            logger.info("[Complex] 未检测到 Zaphkiel，跳过物品兼容。");
            return;
        }
        try {
            // Kotlin object 单例通过 INSTANCE 字段访问
            Class<?> zaphkielClass = Class.forName("ink.ptms.zaphkiel.Zaphkiel");
            Object   zaphkielInst  = zaphkielClass.getField("INSTANCE").get(null);

            // Zaphkiel.api() → ZaphkielAPI
            Method   apiMethod     = zaphkielClass.getMethod("api");
            Object   api           = apiMethod.invoke(zaphkielInst);

            // ZaphkielAPI.getItemHandler() → ZaphkielItemHandler
            Method   getHandler    = api.getClass().getMethod("getItemHandler");
            itemHandler            = getHandler.invoke(api);

            // ZaphkielItemHandler 上的方法
            methodGetItemId = itemHandler.getClass().getMethod("getItemId", ItemStack.class);
            methodRead      = itemHandler.getClass().getMethod("read",      ItemStack.class);

            // ItemStream 上的方法
            Class<?> streamClass = Class.forName("ink.ptms.zaphkiel.api.ItemStream");
            methodIsVanilla      = streamClass.getMethod("isVanilla");
            methodRebuild        = streamClass.getMethod("rebuildToItemStack", Player.class);

            enabled = true;
            logger.info("[Complex] 检测到 Zaphkiel，已启用物品兼容。");
        } catch (Exception e) {
            logger.warning("[Complex] Zaphkiel 兼容初始化失败: " + e.getMessage());
        }
    }

    // ── 公共 API ──────────────────────────────────────────────────────────────

    public boolean isEnabled() { return enabled; }

    /** 判断物品是否由 Zaphkiel 生成。 */
    public boolean isZaphkielItem(ItemStack item) {
        if (!enabled || item == null) return false;
        try { return methodGetItemId.invoke(itemHandler, item) != null; }
        catch (Exception e) { return false; }
    }

    /** 获取 Zaphkiel 物品 ID，非 Zaphkiel 物品返回 null。 */
    public String getId(ItemStack item) {
        if (!enabled || item == null) return null;
        try {
            Object id = methodGetItemId.invoke(itemHandler, item);
            return id != null ? id.toString() : null;
        } catch (Exception e) { return null; }
    }

    /**
     * 为指定玩家重建物品 lore（刷新动态属性显示）。
     * 返回重建后的新 ItemStack；失败时返回原物品。
     */
    public ItemStack rebuild(ItemStack item, Player player) {
        if (!enabled || item == null) return item;
        try {
            Object stream = methodRead.invoke(itemHandler, item);
            if (stream == null) return item;
            if ((boolean) methodIsVanilla.invoke(stream)) return item;
            Object result = methodRebuild.invoke(stream, player);
            return result instanceof ItemStack ? (ItemStack) result : item;
        } catch (Exception e) { return item; }
    }

    /**
     * 从 Zaphkiel 物品中读取属性列表。
     * 通过 {@link #rebuild} 刷新 lore 后使用 {@code loreReader} 解析。
     *
     * @param item       待读取的 ItemStack
     * @param player     玩家上下文（用于 rebuild）
     * @param loreReader 共享的 lore 解析器
     */
    public List<ItemStat> readStats(ItemStack item, Player player, ItemStatReader loreReader) {
        ItemStack rebuilt = rebuild(item, player);
        return loreReader.readStats(rebuilt);
    }
}
