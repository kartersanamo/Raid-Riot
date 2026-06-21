package com.kartersanamo.raidriot;

import com.kartersanamo.raidriot.base.BaseDifficultyStore;
import com.kartersanamo.raidriot.base.BasePlacementService;
import com.kartersanamo.raidriot.breach.BreachService;
import com.kartersanamo.raidriot.chat.ClickableMessageService;
import com.kartersanamo.raidriot.combat.NakedPatchEnforcer;
import com.kartersanamo.raidriot.combat.PredefinedKitService;
import com.kartersanamo.raidriot.combat.RespawnQueue;
import com.kartersanamo.raidriot.combat.VirtualDeathService;
import com.kartersanamo.raidriot.command.RaidRiotCommand;
import com.kartersanamo.raidriot.config.RaidRiotConfig;
import com.kartersanamo.raidriot.faction.ClaimBaseProvider;
import com.kartersanamo.raidriot.faction.EventFactionService;
import com.kartersanamo.raidriot.faction.EventTeamAccessService;
import com.kartersanamo.raidriot.faction.FactionBaseClaimProvider;
import com.kartersanamo.raidriot.faction.FactionsBridge;
import com.kartersanamo.raidriot.listener.BlockBreakListener;
import com.kartersanamo.raidriot.listener.BlockPlaceListener;
import com.kartersanamo.raidriot.listener.DeathListener;
import com.kartersanamo.raidriot.listener.EventFactionAccessListener;
import com.kartersanamo.raidriot.listener.ExplosionBreachListener;
import com.kartersanamo.raidriot.listener.FactionCommandListener;
import com.kartersanamo.raidriot.listener.MatchLockNotifier;
import com.kartersanamo.raidriot.listener.TntAttributionTracker;
import com.kartersanamo.raidriot.listener.TntDispenseListener;
import com.kartersanamo.raidriot.listener.TntSpawnListener;
import com.kartersanamo.raidriot.listener.VirtualCombatListener;
import com.kartersanamo.raidriot.match.EventManager;
import com.kartersanamo.raidriot.message.MessageService;
import com.kartersanamo.raidriot.queue.QueueManager;
import com.kartersanamo.raidriot.ui.RaidRiotGuiListener;
import com.kartersanamo.raidriot.ui.RaidRiotGuiService;
import com.kartersanamo.raidriot.vote.VoteManager;
import com.kartersanamo.raidriot.world.EventWorldBorderService;
import com.kartersanamo.raidriot.world.SchematicService;
import com.kartersanamo.raidriot.world.WorldResetService;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class RaidRiotPlugin extends JavaPlugin {

    private static RaidRiotPlugin instance;

    private RaidRiotConfig raidRiotConfig;
    private MessageService messages;
    private FactionsBridge factionsBridge;
    private BaseDifficultyStore baseDifficultyStore;
    private EventManager eventManager;
    private RespawnQueue respawnQueue;
    private VirtualDeathService virtualDeathService;
    private EventFactionService eventFactionService;
    private EventTeamAccessService eventTeamAccessService;
    private EventWorldBorderService eventWorldBorderService;
    private BreachService breachService;
    private NakedPatchEnforcer nakedPatchEnforcer;
    private WorldResetService worldResetService;
    private RaidRiotGuiService guiService;

    public static RaidRiotPlugin getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }
        File schemDir = new File(getDataFolder(), "schematics");
        if (!schemDir.exists()) {
            schemDir.mkdirs();
        }

        saveDefaultConfig();
        saveResource("messages.yml", false);
        saveResource("bases.yml", false);

        raidRiotConfig = new RaidRiotConfig(this);
        raidRiotConfig.reload();
        messages = new MessageService(this);
        messages.reload();

        factionsBridge = new FactionsBridge(this);
        if (!factionsBridge.init()) {
            getLogger().severe("Disabling Raid Riot: Factions API not available.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        eventFactionService = new EventFactionService(this);
        if (!eventFactionService.init()) {
            getLogger().severe("Disabling Raid Riot: event factions not configured.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        baseDifficultyStore = new BaseDifficultyStore(this);
        baseDifficultyStore.load();

        FactionBaseClaimProvider factionBaseClaimProvider = new FactionBaseClaimProvider(this);
        factionBaseClaimProvider.init();

        SchematicService schematicService = new SchematicService();
        ClaimBaseProvider claimBaseProvider = new ClaimBaseProvider(this);
        worldResetService = new WorldResetService();
        eventWorldBorderService = new EventWorldBorderService(this);
        BasePlacementService basePlacementService = new BasePlacementService(
                this, schematicService, baseDifficultyStore, factionBaseClaimProvider,
                claimBaseProvider, worldResetService, eventFactionService, eventWorldBorderService);

        respawnQueue = new RespawnQueue(this);
        virtualDeathService = new VirtualDeathService(this);
        eventTeamAccessService = new EventTeamAccessService(this, eventFactionService);
        PredefinedKitService predefinedKitService = new PredefinedKitService(raidRiotConfig);
        ClickableMessageService clickableMessageService = new ClickableMessageService(this);
        QueueManager queueManager = new QueueManager(this, clickableMessageService);
        VoteManager voteManager = new VoteManager(this);
        guiService = new RaidRiotGuiService(this);
        eventManager = new EventManager(this, queueManager, voteManager, basePlacementService,
                worldResetService, respawnQueue, predefinedKitService, guiService,
                eventFactionService, eventWorldBorderService, virtualDeathService);

        breachService = new BreachService(this);
        nakedPatchEnforcer = new NakedPatchEnforcer(this);

        RaidRiotCommand command = new RaidRiotCommand(this, baseDifficultyStore);

        MatchLockNotifier lockNotifier = new MatchLockNotifier(this);
        TntAttributionTracker tntAttributionTracker = new TntAttributionTracker(this);

        Bukkit.getPluginManager().registerEvents(new ExplosionBreachListener(this, tntAttributionTracker, breachService), this);
        Bukkit.getPluginManager().registerEvents(new TntDispenseListener(tntAttributionTracker), this);
        Bukkit.getPluginManager().registerEvents(new TntSpawnListener(tntAttributionTracker), this);
        Bukkit.getPluginManager().registerEvents(
                new BlockBreakListener(this, breachService, lockNotifier, eventTeamAccessService), this);
        Bukkit.getPluginManager().registerEvents(
                new BlockPlaceListener(this, nakedPatchEnforcer, lockNotifier, eventTeamAccessService), this);
        Bukkit.getPluginManager().registerEvents(new DeathListener(this), this);
        Bukkit.getPluginManager().registerEvents(new VirtualCombatListener(this, virtualDeathService), this);
        Bukkit.getPluginManager().registerEvents(new EventFactionAccessListener(this, eventTeamAccessService), this);
        Bukkit.getPluginManager().registerEvents(new FactionCommandListener(this, eventFactionService), this);
        Bukkit.getPluginManager().registerEvents(new RaidRiotGuiListener(this, guiService), this);

        org.bukkit.command.PluginCommand raidriotCommand = getCommand("raidriot");
        if (raidriotCommand != null) {
            raidriotCommand.setExecutor(command);
            raidriotCommand.setTabCompleter(command);
        } else {
            getLogger().severe("Command 'raidriot' missing from plugin.yml — commands will not work.");
        }

        getLogger().info("Raid Riot enabled for Minecadia.");
    }

    @Override
    public void onDisable() {
        if (eventManager != null) {
            eventManager.stopMatch("Server shutdown.");
        }
        if (respawnQueue != null) {
            respawnQueue.cancelAll();
        }
        if (virtualDeathService != null) {
            virtualDeathService.cancelAll();
        }
    }

    public RaidRiotConfig getRaidRiotConfig() {
        return raidRiotConfig;
    }

    public MessageService getMessages() {
        return messages;
    }

    public FactionsBridge getFactionsBridge() {
        return factionsBridge;
    }

    public BaseDifficultyStore getBaseDifficultyStore() {
        return baseDifficultyStore;
    }

    public EventManager getEventManager() {
        return eventManager;
    }

    public RespawnQueue getRespawnQueue() {
        return respawnQueue;
    }

    public VirtualDeathService getVirtualDeathService() {
        return virtualDeathService;
    }

    public EventFactionService getEventFactionService() {
        return eventFactionService;
    }

    public EventTeamAccessService getEventTeamAccessService() {
        return eventTeamAccessService;
    }

    public BreachService getBreachService() {
        return breachService;
    }

    public NakedPatchEnforcer getNakedPatchEnforcer() {
        return nakedPatchEnforcer;
    }

    public WorldResetService getWorldResetService() {
        return worldResetService;
    }

    public RaidRiotGuiService getGuiService() {
        return guiService;
    }
}
