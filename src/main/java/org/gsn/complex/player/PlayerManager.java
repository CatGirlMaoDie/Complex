package org.gsn.complex.player;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.gsn.complex.Complex;
import org.gsn.complex.compat.AttributePlusCompat;
import org.gsn.complex.compat.BaikirutoCompat;
import org.gsn.complex.compat.MMOItemsCompat;
import org.gsn.complex.compat.NeigeItemsCompat;
import org.gsn.complex.compat.SertralineCompat;
import org.gsn.complex.compat.SXItemCompat;
import org.gsn.complex.compat.ZaphkielCompat;
import org.gsn.complex.slot.CustomSlot;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** 管理所有在线玩家的 {@link PlayerEquipment} 实例。 */
public class PlayerManager {

    private final Complex             plugin;
    private final AttributePlusCompat atPlus;
    private final SertralineCompat    sertraline;
    private final BaikirutoCompat     baikiruto;
    private final MMOItemsCompat      mmoItems;
    private final NeigeItemsCompat    neigeItems;
    private final SXItemCompat        sxItem;
    private final ZaphkielCompat      zaphkiel;
    private final Map<UUID, PlayerEquipment> cache = new HashMap<>();

    public PlayerManager(Complex plugin, AttributePlusCompat atPlus,
                         SertralineCompat sertraline, BaikirutoCompat baikiruto,
                         MMOItemsCompat mmoItems, NeigeItemsCompat neigeItems,
                         SXItemCompat sxItem, ZaphkielCompat zaphkiel) {
        this.plugin     = plugin;
        this.atPlus     = atPlus;
        this.sertraline = sertraline;
        this.baikiruto  = baikiruto;
        this.mmoItems   = mmoItems;
        this.neigeItems = neigeItems;
        this.sxItem     = sxItem;
        this.zaphkiel   = zaphkiel;
    }

    // ── 生命周期 ──────────────────────────────────────────────────────────────

    /** 获取（或新建）玩家的装备数据，并加载存档。 */
    public PlayerEquipment getOrCreate(Player player) {
        return cache.computeIfAbsent(player.getUniqueId(), uuid -> {
            PlayerEquipment eq = new PlayerEquipment(player, plugin,
                    atPlus, sertraline, baikiruto, mmoItems, neigeItems, sxItem, zaphkiel);
            load(eq);
            return eq;
        });
    }

    /** 返回缓存的装备数据，若玩家不在线则返回 null。 */
    public PlayerEquipment get(Player player) {
        return cache.get(player.getUniqueId());
    }

    /** 玩家下线时：清除 Bukkit 属性修改器、保存存档、移出缓存。 */
    public void remove(Player player) {
        PlayerEquipment eq = cache.remove(player.getUniqueId());
        if (eq == null) return;
        eq.dispose();
        save(eq);
    }

    /** 插件关闭时保存所有在线玩家数据。 */
    public void saveAll() {
        cache.values().forEach(eq -> {
            eq.dispose();
            save(eq);
        });
    }

    // ── 持久化 ────────────────────────────────────────────────────────────────

    public void save(PlayerEquipment eq) {
        File file = dataFile(eq.getPlayer().getUniqueId());
        YamlConfiguration cfg = new YamlConfiguration();
        for (Map.Entry<CustomSlot, ItemStack> entry : eq.getEquipped().entrySet()) {
            if (entry.getValue() != null) {
                cfg.set(entry.getKey().name(), entry.getValue());
            }
        }
        try {
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("保存装备数据失败（"
                    + eq.getPlayer().getName() + "）: " + e.getMessage());
        }
    }

    private void load(PlayerEquipment eq) {
        File file = dataFile(eq.getPlayer().getUniqueId());
        if (!file.exists()) return;
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        for (CustomSlot slot : CustomSlot.values()) {
            ItemStack item = cfg.getItemStack(slot.name());
            if (item != null) eq.equip(slot, item);
        }
    }

    private File dataFile(UUID uuid) {
        File dir = new File(plugin.getDataFolder(), "players");
        //noinspection ResultOfMethodCallIgnored
        dir.mkdirs();
        return new File(dir, uuid + ".yml");
    }
}
