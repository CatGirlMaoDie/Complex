package org.gsn.complex.listener;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;
import org.gsn.complex.Complex;
import org.gsn.complex.gui.EquipmentGUI;
import org.gsn.complex.gui.GUIConfig;
import org.gsn.complex.player.PlayerEquipment;
import org.gsn.complex.slot.CustomSlot;

public class InventoryListener implements Listener {

    private final Complex plugin;

    public InventoryListener(Complex plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getInventory().getHolder() instanceof EquipmentGUI gui)) return;

        GUIConfig cfg  = plugin.getGUIConfig();
        int       size = cfg.getSize();
        int    rawSlot = event.getRawSlot();

        // ── Click inside the equipment GUI (top inventory) ──────────────────
        if (rawSlot >= 0 && rawSlot < size) {
            // Always cancel movement within the top pane
            event.setCancelled(true);

            CustomSlot target = cfg.getEquipSlot(rawSlot);
            if (target == null) return; // filler slot — ignore

            handleEquipmentClick(player, gui, target);
            return;
        }

        // ── Click in the player's own inventory (bottom pane) ───────────────
        // Normal clicks are NOT cancelled — the player can freely pick up items
        // and place them on the cursor, then click an equipment slot.
        // Only shift-click is intercepted for auto-equip.
        if (event.isShiftClick() && event.getCurrentItem() != null
                && !event.getCurrentItem().getType().isAir()) {
            event.setCancelled(true);
            autoEquip(player, gui, event.getCurrentItem(), event.getSlot());
        }
    }

    /** Cancel drag events that touch any slot inside the GUI pane. */
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getInventory().getHolder() instanceof EquipmentGUI)) return;
        int size = plugin.getGUIConfig().getSize();
        for (int slot : event.getRawSlots()) {
            if (slot < size) {
                event.setCancelled(true);
                return;
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Handles a click on an equipment slot:
     *  • cursor has item  → equip it, put old item on cursor
     *  • cursor empty     → unequip to cursor
     */
    private void handleEquipmentClick(Player player, EquipmentGUI gui, CustomSlot slot) {
        PlayerEquipment equipment = gui.getEquipment();
        ItemStack cursor  = player.getItemOnCursor();
        ItemStack current = equipment.getEquipped(slot);

        if (cursor != null && !cursor.getType().isAir()) {
            equipment.equip(slot, cursor.clone());
            player.setItemOnCursor(current); // null → clears cursor
        } else if (current != null) {
            equipment.unequip(slot);
            player.setItemOnCursor(current);
        }
        gui.refreshSlot(slot);
    }

    /**
     * Tries to auto-equip a shift-clicked item from the player inventory.
     * Detects the best slot by item type; does nothing if no suitable slot exists.
     */
    private void autoEquip(Player player, EquipmentGUI gui,
                           ItemStack item, int playerInvSlot) {
        CustomSlot target = detectSlot(item, gui.getEquipment());
        if (target == null) return;

        ItemStack old = gui.getEquipment().getEquipped(target);
        gui.getEquipment().equip(target, item.clone());
        player.getInventory().setItem(playerInvSlot,
                (old != null) ? old : new ItemStack(Material.AIR));
        gui.refreshSlot(target);
    }

    /** Heuristically picks the most fitting slot for the given item type. */
    private CustomSlot detectSlot(ItemStack item, PlayerEquipment equipment) {
        String name = item.getType().name();

        if (endsWith(name, "_HELMET", "_SKULL", "_CAP"))          return CustomSlot.HELMET;
        if (endsWith(name, "_CHESTPLATE", "_TUNIC", "_ELYTRA"))   return CustomSlot.CHESTPLATE;
        if (endsWith(name, "_LEGGINGS", "_PANTS"))                 return CustomSlot.LEGGINGS;
        if (endsWith(name, "_BOOTS"))                              return CustomSlot.BOOTS;
        if (endsWith(name, "_SWORD", "_AXE", "_TRIDENT",
                "_BOW", "_CROSSBOW"))                              return CustomSlot.WEAPON;
        if (item.getType() == Material.SHIELD)                     return CustomSlot.OFFHAND;

        // Accessory slots — pick first empty one
        for (CustomSlot acc : new CustomSlot[]{
                CustomSlot.RING_1, CustomSlot.RING_2,
                CustomSlot.NECKLACE, CustomSlot.BELT}) {
            if (equipment.getEquipped(acc) == null) return acc;
        }
        return null;
    }

    private boolean endsWith(String value, String... suffixes) {
        for (String s : suffixes) if (value.endsWith(s)) return true;
        return false;
    }
}
