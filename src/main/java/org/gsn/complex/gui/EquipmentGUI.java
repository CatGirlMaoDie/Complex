package org.gsn.complex.gui;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.gsn.complex.Complex;
import org.gsn.complex.player.PlayerEquipment;
import org.gsn.complex.slot.CustomSlot;

public class EquipmentGUI implements InventoryHolder {

    private final Player          player;
    private final PlayerEquipment equipment;
    private       Inventory       inventory;

    public EquipmentGUI(Player player, PlayerEquipment equipment) {
        this.player    = player;
        this.equipment = equipment;
    }

    // ── 打开 ──────────────────────────────────────────────────────────────────

    public void open() {
        GUIConfig cfg = Complex.getInstance().getGUIConfig();
        inventory = Bukkit.createInventory(this, cfg.getSize(), cfg.getTitle());

        // 先用填充物占满所有格子
        ItemStack filler = cfg.getFiller();
        for (int i = 0; i < cfg.getSize(); i++) {
            inventory.setItem(i, filler);
        }

        // 装备槽：有物品则显示物品，否则显示占位符
        for (CustomSlot slot : CustomSlot.values()) {
            int pos = cfg.getGuiSlot(slot);
            if (pos < 0) continue;
            ItemStack item = equipment.getEquipped(slot);
            inventory.setItem(pos, item != null ? item : cfg.getPlaceholder(slot));
        }

        player.openInventory(inventory);
    }

    /** 装备或卸下某槽后刷新该格显示。 */
    public void refreshSlot(CustomSlot slot) {
        if (inventory == null) return;
        GUIConfig cfg = Complex.getInstance().getGUIConfig();
        int pos = cfg.getGuiSlot(slot);
        if (pos < 0) return;
        ItemStack item = equipment.getEquipped(slot);
        inventory.setItem(pos, item != null ? item : cfg.getPlaceholder(slot));
    }

    // ── Getter ────────────────────────────────────────────────────────────────

    @Override
    public Inventory       getInventory() { return inventory; }
    public Player          getPlayer()    { return player; }
    public PlayerEquipment getEquipment() { return equipment; }
}
