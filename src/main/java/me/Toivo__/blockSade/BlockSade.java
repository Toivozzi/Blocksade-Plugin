package me.Toivo__.blockSade;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class BlockSade extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {

    private final Map<UUID, BukkitTask> rainTasks = new HashMap<>();
    private final Map<UUID, Boolean> soundEnabled = new HashMap<>();
    private final Map<UUID, Integer> spawnRate = new HashMap<>();
    private final Set<UUID> rainBlocks = new HashSet<>();
    private final Map<UUID, Material> pendingBlocks = new HashMap<>();
    private final Map<UUID, Location> lastBlockLocations = new HashMap<>();
    private final Random random = new Random();

    private final String mainTitle = ChatColor.BLUE + "BlockSade Asetukset";
    private final String soundTitle = ChatColor.GREEN + "Ääni-asetukset";
    private final String blockTitle = ChatColor.GREEN + "Asetukset";

    private final List<Material> normalBlocks = Arrays.stream(Material.values())
            .filter(Material::isBlock)
            .filter(Material::isSolid)
            .filter(m -> !m.isAir())
            .filter(m -> !m.isLegacy())
            .filter(m -> {
                String name = m.name();
                return !name.contains("DOOR")
                        && !name.contains("BED")
                        && !name.contains("TALL")
                        && !name.contains("DOUBLE")
                        && !name.contains("POTTED")
                        && !name.contains("WALL")
                        && !name.contains("WATER")
                        && !name.contains("LAVA")
                        && !name.contains("KELP")
                        && !name.contains("SEAGRASS")
                        && !name.contains("CORAL")
                        && !name.contains("FLOWER")
                        && !name.contains("TULIP")
                        && !name.contains("ORCHID")
                        && !name.contains("DAISY")
                        && !name.contains("POPPY")
                        && !name.contains("DANDELION")
                        && !name.contains("TORCHFLOWER")
                        && !name.contains("PITCHER_PLANT")
                        && !name.contains("SAPLING")
                        && !name.contains("BUSH")
                        && !name.contains("FERN")
                        && !name.equals("GRASS")
                        && !name.equals("SHORT_GRASS")
                        && !name.equals("TALL_GRASS")
                        && !name.contains("VINE")
                        && !name.contains("LILY_PAD")
                        && !name.contains("MUSHROOM")
                        && !name.contains("DEAD_BUSH")
                        && !name.contains("SUGAR_CANE")
                        && !name.contains("FIRE")
                        && !name.contains("BANNER")
                        && !name.contains("TRIPWIRE")
                        && !name.contains("REDSTONE_WIRE")
                        && !name.contains("RAIL")
                        && !name.contains("TORCH")
                        && !name.contains("CANDLE")
                        && !name.contains("PRESSURE_PLATE")
                        && !name.contains("BUTTON")
                        && !name.contains("CARPET")
                        && !name.contains("SIGN")
                        && !name.contains("LEVER")
                        && !name.contains("REPEATER")
                        && !name.contains("COMPARATOR")
                        && !name.contains("HOPPER")
                        && !name.contains("PISTON")
                        && !name.contains("HEAD")
                        && !name.contains("SKULL")
                        && !name.contains("ANVIL")
                        && !name.contains("SLAB")
                        && !name.contains("STAIRS")
                        && !name.contains("FENCE")
                        && !name.contains("GATE")
                        && !name.contains("TRAPDOOR")
                        && !name.contains("LADDER")
                        && !name.contains("SCAFFOLDING")
                        && !name.contains("CHAIN")
                        && !name.contains("LIGHTNING_ROD")
                        && !name.contains("END_ROD")
                        && !name.contains("CAMPFIRE")
                        && !name.contains("BELL")
                        && !name.contains("LECTERN")
                        && !name.contains("COMPOSTER")
                        && !name.contains("CAULDRON")
                        && !name.contains("BREWING_STAND")
                        && !name.contains("ENCHANTING_TABLE")
                        && !name.contains("END_PORTAL")
                        && !name.contains("NETHER_PORTAL")
                        && !name.contains("STRUCTURE_VOID")
                        && !name.contains("BARRIER")
                        && !name.contains("MOVING_PISTON")
                        && !name.equals("SAND")
                        && !name.equals("RED_SAND")
                        && !name.equals("GRAVEL")
                        && !name.contains("CONCRETE_POWDER")
                        && !name.equals("DRAGON_EGG")
                        && !name.equals("SUSPICIOUS_SAND")
                        && !name.equals("SUSPICIOUS_GRAVEL");
            })
            .collect(Collectors.toList());

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        getCommand("blocksade").setExecutor(this);
        getCommand("blocksade").setTabCompleter(this);
        getLogger().info("BlockSade käynnistetty!");
    }

    @Override
    public void onDisable() {
        for (BukkitTask task : rainTasks.values()) {
            task.cancel();
        }

        rainTasks.clear();
        rainBlocks.clear();
        pendingBlocks.clear();
        lastBlockLocations.clear();

        getLogger().info("BlockSade pysäytetty!");
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        stopRain(e.getPlayer());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Tämä komento on tarkoitettu pelaajille.");
            return true;
        }

        Player p = (Player) sender;

        if (args.length == 0) {
            sendHelp(p);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "start":
                if (rainTasks.containsKey(p.getUniqueId())) {
                    p.sendMessage(ChatColor.RED + "Palikkasade on jo käynnissä!");
                    return true;
                }

                startRain(p);
                p.sendMessage(ChatColor.GREEN + "Palikkasade käynnistetty!");
                return true;

            case "stop":
                if (!rainTasks.containsKey(p.getUniqueId())) {
                    p.sendMessage(ChatColor.YELLOW + "Palikkasade ei ole käynnissä.");
                    return true;
                }

                stopRain(p);
                p.sendMessage(ChatColor.RED + "Palikkasade pysäytetty!");
                return true;

            case "info":
                sendHelp(p);
                return true;

            case "asetukset":
                openMainMenu(p);
                return true;

            default:
                p.sendMessage(ChatColor.RED + "Tuntematon komento.");
                sendHelp(p);
                return true;
        }
    }

    private void sendHelp(Player p) {
        p.sendMessage(ChatColor.AQUA + "===== BlockSade =====");
        p.sendMessage(ChatColor.YELLOW + "/blocksade start" + ChatColor.WHITE + " - Aloita palikkasade");
        p.sendMessage(ChatColor.YELLOW + "/blocksade stop" + ChatColor.WHITE + " - Pysäytä palikkasade");
        p.sendMessage(ChatColor.YELLOW + "/blocksade asetukset" + ChatColor.WHITE + " - Avaa asetukset");
        p.sendMessage(ChatColor.YELLOW + "/blocksade info" + ChatColor.WHITE + " - Näytä tämä ohje");
    }

    @Override
    public List<String> onTabComplete(
            CommandSender sender,
            Command command,
            String alias,
            String[] args
    ) {
        if (args.length == 1) {
            String input = args[0].toLowerCase(Locale.ROOT);

            return Arrays.asList("start", "stop", "asetukset", "info")
                    .stream()
                    .filter(s -> s.startsWith(input))
                    .collect(Collectors.toList());
        }

        return java.util.Collections.emptyList();
    }

    private void startRain(Player p) {
        UUID uuid = p.getUniqueId();
        int rate = spawnRate.getOrDefault(uuid, 10);

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!p.isOnline()) {
                    cancel();
                    rainTasks.remove(uuid);
                    return;
                }

                Location playerLoc = p.getLocation();
                World world = p.getWorld();

                Location spawnLoc = playerLoc.clone().add(
                        random.nextInt(11) - 5,
                        20,
                        random.nextInt(11) - 5
                );

                if (spawnLoc.getY() >= world.getMaxHeight()) {
                    spawnLoc.setY(world.getMaxHeight() - 2);
                }

                if (spawnLoc.getY() <= world.getMinHeight()) {
                    return;
                }

                Material mat = normalBlocks.get(random.nextInt(normalBlocks.size()));

                FallingBlock fallingBlock = world.spawnFallingBlock(
                        spawnLoc,
                        mat.createBlockData()
                );

                fallingBlock.setDropItem(false);
                fallingBlock.setHurtEntities(false);
                fallingBlock.setGravity(true);
                fallingBlock.setPersistent(false);

                rainBlocks.add(fallingBlock.getUniqueId());
                pendingBlocks.put(fallingBlock.getUniqueId(), mat);
                lastBlockLocations.put(fallingBlock.getUniqueId(), spawnLoc.clone());

                if (soundEnabled.getOrDefault(uuid, false)) {
                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);
                }
            }
        }.runTaskTimer(this, 0L, rate);

        rainTasks.put(uuid, task);
    }

    private void stopRain(Player p) {
        BukkitTask task = rainTasks.remove(p.getUniqueId());

        if (task != null) {
            task.cancel();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFallingBlockMove(EntityChangeBlockEvent e) {
        if (!(e.getEntity() instanceof FallingBlock)) {
            return;
        }

        UUID id = e.getEntity().getUniqueId();

        if (!rainBlocks.contains(id)) {
            return;
        }

        if (e.getTo().isAir()) {
            return;
        }

        rainBlocks.remove(id);
        pendingBlocks.remove(id);
        lastBlockLocations.remove(id);
    }

    private void placeBlockSafely(Location location, Material material) {
        World world = location.getWorld();

        if (world == null) {
            return;
        }

        int x = location.getBlockX();
        int z = location.getBlockZ();
        int startY = Math.min(world.getMaxHeight() - 1, location.getBlockY());

        for (int y = startY; y >= world.getMinHeight(); y--) {
            Block block = world.getBlockAt(x, y, z);
            Block below = block.getRelative(BlockFace.DOWN);

            if (!block.isEmpty() && !block.isPassable()) {
                continue;
            }

            if (!below.getType().isSolid()) {
                continue;
            }

            if (block.isLiquid() || block.isEmpty() || block.isPassable()) {
                block.setType(material, false);
                return;
            }
        }
    }

    private void openMainMenu(Player p) {
        Inventory inv = Bukkit.createInventory(null, 27, mainTitle);

        inv.setItem(12, createMenuItem(
                Material.NOTE_BLOCK,
                ChatColor.YELLOW + "Ääni-asetukset"
        ));

        inv.setItem(13, null);

        inv.setItem(14, createMenuItem(
                Material.CLOCK,
                ChatColor.GREEN + "Asetukset"
        ));

        inv.setItem(26, createExitItem());

        p.openInventory(inv);
    }

    private void openSoundSettings(Player p) {
        Inventory inv = Bukkit.createInventory(null, 27, soundTitle);
        boolean enabled = soundEnabled.getOrDefault(p.getUniqueId(), false);

        inv.setItem(13, createMenuItem(
                enabled ? Material.NOTE_BLOCK : Material.BARRIER,
                ChatColor.YELLOW + "Äänet: " + (enabled ? "Päällä" : "Pois päältä")
        ));

        inv.setItem(26, createBackItem());

        p.openInventory(inv);
    }

    private void openBlockSettings(Player p) {
        Inventory inv = Bukkit.createInventory(null, 27, blockTitle);
        int currentRate = spawnRate.getOrDefault(p.getUniqueId(), 10);

        inv.setItem(13, createMenuItem(
                Material.CLOCK,
                ChatColor.GREEN + "Spawn-nopeus: " + currentRate + " tick"
        ));

        inv.setItem(26, createBackItem());

        p.openInventory(inv);
    }

    private ItemStack createBackItem() {
        return createMenuItem(
                Material.BOOK,
                ChatColor.DARK_GRAY + "" + ChatColor.BOLD + "Takaisin"
        );
    }

    private ItemStack createExitItem() {
        return createMenuItem(
                Material.BARRIER,
                ChatColor.RED + "" + ChatColor.BOLD + "Poistu"
        );
    }

    private ItemStack createMenuItem(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }

        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) {
            return;
        }

        Player p = (Player) e.getWhoClicked();
        String title = e.getView().getTitle();
        int slot = e.getRawSlot();

        boolean isMenu = title.equals(mainTitle)
                || title.equals(soundTitle)
                || title.equals(blockTitle);

        if (!isMenu) {
            return;
        }

        e.setCancelled(true);

        if (slot < 0 || slot >= e.getView().getTopInventory().getSize()) {
            return;
        }

        if (title.equals(mainTitle)) {
            if (slot == 12) {
                openSoundSettings(p);
            } else if (slot == 14) {
                openBlockSettings(p);
            } else if (slot == 26) {
                p.closeInventory();
            }
            return;
        }

        if (slot == 26) {
            openMainMenu(p);
            return;
        }

        if (title.equals(soundTitle) && slot == 13) {
            UUID uuid = p.getUniqueId();
            boolean enabled = soundEnabled.getOrDefault(uuid, false);
            soundEnabled.put(uuid, !enabled);
            openSoundSettings(p);
            return;
        }

        if (title.equals(blockTitle) && slot == 13) {
            UUID uuid = p.getUniqueId();
            int current = spawnRate.getOrDefault(uuid, 10);
            int next = current == 5 ? 10 : current == 10 ? 20 : 5;

            spawnRate.put(uuid, next);

            if (rainTasks.containsKey(uuid)) {
                stopRain(p);
                startRain(p);
            }

            openBlockSettings(p);
        }
    }
}