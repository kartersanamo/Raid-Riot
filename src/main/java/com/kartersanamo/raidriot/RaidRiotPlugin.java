package com.kartersanamo.raidriot;

import com.kartersanamo.raidriot.arena.ArenaStore;
import com.kartersanamo.raidriot.arena.SchematicBaseProvider;
import com.kartersanamo.raidriot.breach.BreachService;
import com.kartersanamo.raidriot.combat.NakedPatchEnforcer;
import com.kartersanamo.raidriot.combat.RespawnQueue;
import com.kartersanamo.raidriot.command.AdminArenaCommand;
import com.kartersanamo.raidriot.command.AdminSelectionSession;
import com.kartersanamo.raidriot.command.RaidRiotCommand;
import com.kartersanamo.raidriot.config.RaidRiotConfig;
import com.kartersanamo.raidriot.faction.ClaimBaseProvider;
import com.kartersanamo.raidriot.faction.FactionsBridge;
import com.kartersanamo.raidriot.listener.BlockBreakListener;
import com.kartersanamo.raidriot.listener.BlockPlaceListener;
import com.kartersanamo.raidriot.listener.DeathListener;
import com.kartersanamo.raidriot.listener.ExplosionBreachListener;
import com.kartersanamo.raidriot.listener.MatchLockNotifier;
import com.kartersanamo.raidriot.listener.TntAttributionTracker;
import com.kartersanamo.raidriot.listener.TntDispenseListener;
import com.kartersanamo.raidriot.listener.TntSpawnListener;
import com.kartersanamo.raidriot.match.EventManager;
import com.kartersanamo.raidriot.message.MessageService;
import com.kartersanamo.raidriot.world.SchematicService;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class RaidRiotPlugin extends JavaPlugin {

    private static RaidRiotPlugin instance;

    private RaidRiotConfig raidRiotConfig;
    private MessageService messages;
    private FactionsBridge factionsBridge;
    private ArenaStore arenaStore;
    private EventManager eventManager;
    private RespawnQueue respawnQueue;
    private BreachService breachService;
    private NakedPatchEnforcer nakedPatchEnforcer;

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

        arenaStore = new ArenaStore(this);
        arenaStore.loadAll();

        SchematicService schematicService = new SchematicService();
        SchematicBaseProvider schematicBaseProvider = new SchematicBaseProvider(this, schematicService);
        ClaimBaseProvider claimBaseProvider = new ClaimBaseProvider(this);
        respawnQueue = new RespawnQueue(this);
        eventManager = new EventManager(this, schematicBaseProvider, claimBaseProvider, respawnQueue);
        breachService = new BreachService(this);
        nakedPatchEnforcer = new NakedPatchEnforcer(this);

        AdminSelectionSession selectionSession = new AdminSelectionSession();
        AdminArenaCommand adminArenaCommand = new AdminArenaCommand(this, arenaStore, selectionSession);
        RaidRiotCommand command = new RaidRiotCommand(this, arenaStore, adminArenaCommand);

        MatchLockNotifier lockNotifier = new MatchLockNotifier(this);
        TntAttributionTracker tntAttributionTracker = new TntAttributionTracker(this);

        Bukkit.getPluginManager().registerEvents(new ExplosionBreachListener(this, tntAttributionTracker, breachService), this);
        Bukkit.getPluginManager().registerEvents(new TntDispenseListener(tntAttributionTracker), this);
        Bukkit.getPluginManager().registerEvents(new TntSpawnListener(tntAttributionTracker), this);
        Bukkit.getPluginManager().registerEvents(new BlockBreakListener(this, breachService, lockNotifier), this);
        Bukkit.getPluginManager().registerEvents(new BlockPlaceListener(this, nakedPatchEnforcer, lockNotifier), this);
        Bukkit.getPluginManager().registerEvents(new DeathListener(this), this);

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
        if (eventManager != null && eventManager.hasActiveMatch()) {
            eventManager.stopMatch("Server shutdown.");
        }
        if (respawnQueue != null) {
            respawnQueue.cancelAll();
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

    public ArenaStore getArenaStore() {
        return arenaStore;
    }

    public EventManager getEventManager() {
        return eventManager;
    }

    public RespawnQueue getRespawnQueue() {
        return respawnQueue;
    }

    public BreachService getBreachService() {
        return breachService;
    }

    public NakedPatchEnforcer getNakedPatchEnforcer() {
        return nakedPatchEnforcer;
    }
}
