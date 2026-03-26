package org.gsn.complex;

import com.mojang.brigadier.Command;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.gsn.complex.compat.AttributePlusCompat;
import org.gsn.complex.compat.BaikirutoCompat;
import org.gsn.complex.compat.BaikirutoStatMapping;
import org.gsn.complex.compat.MMOItemsCompat;
import org.gsn.complex.compat.MMOItemsStatMapping;
import org.gsn.complex.compat.NeigeItemsCompat;
import org.gsn.complex.compat.NeigeItemsStatMapping;
import org.gsn.complex.compat.SertralineCompat;
import org.gsn.complex.compat.SertralineStatMapping;
import org.gsn.complex.compat.SXItemCompat;
import org.gsn.complex.compat.ZaphkielCompat;
import org.gsn.complex.gui.EquipmentGUI;
import org.gsn.complex.gui.GUIConfig;
import org.gsn.complex.listener.InventoryListener;
import org.gsn.complex.listener.PlayerListener;
import org.gsn.complex.player.PlayerManager;

import java.util.List;

public final class Complex extends JavaPlugin {

    private static Complex instance;
    private AttributePlusCompat          atPlusCompat;
    private SertralineCompat             sertralineCompat;
    private List<SertralineStatMapping>  sertralineMappings;
    private BaikirutoCompat              baikirutoCompat;
    private List<BaikirutoStatMapping>   baikirutoMappings;
    private MMOItemsCompat               mmoItemsCompat;
    private List<MMOItemsStatMapping>    mmoItemsMappings;
    private NeigeItemsCompat             neigeItemsCompat;
    private List<NeigeItemsStatMapping>  neigeItemsMappings;
    private SXItemCompat                 sxItemCompat;
    private ZaphkielCompat               zaphkielCompat;
    private PlayerManager                playerManager;
    private GUIConfig                    guiConfig;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        guiConfig          = GUIConfig.load(getConfig());
        sertralineMappings = SertralineStatMapping.loadAll(getConfig());
        baikirutoMappings  = BaikirutoStatMapping.loadAll(getConfig());
        mmoItemsMappings   = MMOItemsStatMapping.loadAll(getConfig());
        neigeItemsMappings = NeigeItemsStatMapping.loadAll(getConfig());

        atPlusCompat = new AttributePlusCompat();
        atPlusCompat.init(getLogger());

        sertralineCompat = new SertralineCompat();
        sertralineCompat.init(getLogger());

        baikirutoCompat = new BaikirutoCompat();
        baikirutoCompat.init(getLogger());

        mmoItemsCompat = new MMOItemsCompat();
        mmoItemsCompat.init(getLogger());

        neigeItemsCompat = new NeigeItemsCompat();
        neigeItemsCompat.init(getLogger());

        sxItemCompat = new SXItemCompat();
        sxItemCompat.init(getLogger());

        zaphkielCompat = new ZaphkielCompat();
        zaphkielCompat.init(getLogger());

        playerManager = new PlayerManager(this, atPlusCompat, sertralineCompat, baikirutoCompat,
                mmoItemsCompat, neigeItemsCompat, sxItemCompat, zaphkielCompat);

        getServer().getPluginManager().registerEvents(new InventoryListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this),    this);

        // Paper 插件必须通过 LifecycleManager 以 Brigadier 方式注册命令
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            event.registrar().register(
                    Commands.literal("rpginv")
                            .executes(ctx -> {
                                if (!(ctx.getSource().getSender() instanceof Player player)) {
                                    ctx.getSource().getSender().sendMessage("§c只有玩家才能使用此指令。");
                                    return 0;
                                }
                                new EquipmentGUI(player, playerManager.getOrCreate(player)).open();
                                return Command.SINGLE_SUCCESS;
                            })
                            .build(),
                    "打开 RPG 装备界面",
                    List.of("rpginventory", "equipment")
            );
        });

        getLogger().info("Complex 已启用。");
    }

    @Override
    public void onDisable() {
        if (playerManager != null) playerManager.saveAll();
        getLogger().info("Complex 已禁用。");
    }

    public static Complex               getInstance()              { return instance; }
    public PlayerManager                getPlayerManager()         { return playerManager; }
    public AttributePlusCompat          getAtPlusCompat()          { return atPlusCompat; }
    public SertralineCompat             getSertralineCompat()      { return sertralineCompat; }
    public List<SertralineStatMapping>  getSertralineMappings()    { return sertralineMappings; }
    public BaikirutoCompat              getBaikirutoCompat()       { return baikirutoCompat; }
    public List<BaikirutoStatMapping>   getBaikirutoMappings()     { return baikirutoMappings; }
    public MMOItemsCompat               getMMOItemsCompat()        { return mmoItemsCompat; }
    public List<MMOItemsStatMapping>    getMMOItemsMappings()      { return mmoItemsMappings; }
    public NeigeItemsCompat             getNeigeItemsCompat()      { return neigeItemsCompat; }
    public List<NeigeItemsStatMapping>  getNeigeItemsMappings()    { return neigeItemsMappings; }
    public SXItemCompat                 getSXItemCompat()          { return sxItemCompat; }
    public ZaphkielCompat               getZaphkielCompat()        { return zaphkielCompat; }
    public GUIConfig                    getGUIConfig()             { return guiConfig; }
}
