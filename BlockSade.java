package me.Toivo__.blockSade;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

public class BlockSade extends JavaPlugin implements Listener {
   private final Map<UUID, Integer> rainTasks = new HashMap();
   private final Map<UUID, Boolean> soundEnabled = new HashMap();
   private final Map<UUID, Integer> spawnRate = new HashMap();
   private final Map<UUID, Boolean> includeSpecialBlocks = new HashMap();
   private final Map<UUID, Material> hatChoice = new HashMap();
   private final List<Material> normalBlocks = Arrays.stream(Material.values()).filter(Material::isBlock).filter((m) -> m.isSolid()).toList();
   private final List<Material> specialBlocks = Arrays.stream(Material.values()).filter(Material::isBlock).filter((m) -> !m.isSolid()).toList();
   private final Material[] hatOptions;

   public BlockSade() {
      this.hatOptions = new Material[]{Material.DIRT, Material.STONE, Material.DIAMOND_BLOCK, Material.TNT, Material.PUMPKIN, Material.EMERALD_BLOCK, Material.REDSTONE_BLOCK, Material.LAPIS_BLOCK, Material.GOLD_BLOCK, Material.IRON_BLOCK};
   }

   public void onEnable() {
      Bukkit.getPluginManager().registerEvents(this, this);

      try {
         Field bukkitCommandMap = Bukkit.getServer().getClass().getDeclaredField("commandMap");
         bukkitCommandMap.setAccessible(true);
         CommandMap commandMap = (CommandMap)bukkitCommandMap.get(Bukkit.getServer());
         commandMap.register("blocksade", new BlocksadeCommand());
      } catch (Exception e) {
         e.printStackTrace();
      }

      this.getLogger().info("Blocksade enabled!");
   }

   public void onDisable() {
      this.getLogger().info("Blocksade disabled!");
   }

   @EventHandler
   public void onJoin(PlayerJoinEvent e) {
      Player p = e.getPlayer();
      String var10001 = String.valueOf(ChatColor.YELLOW);
      p.sendMessage(var10001 + "[PalikkaSade] " + String.valueOf(ChatColor.GREEN) + "Tällä serverillä on käytössä PalikkaSade plugin! " + String.valueOf(ChatColor.AQUA) + "Aloita tekemällä /blocksade info");
   }

   private void startRain(Player p) {
      int rate = (Integer)this.spawnRate.getOrDefault(p.getUniqueId(), 10);
      boolean allowSpecial = (Boolean)this.includeSpecialBlocks.getOrDefault(p.getUniqueId(), false);
      int taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> {
         Random random = new Random();
         Location loc = p.getLocation().add((double)(random.nextInt(10) - 5), (double)15.0F, (double)(random.nextInt(10) - 5));
         List<Material> pool = new ArrayList(this.normalBlocks);
         if (allowSpecial) {
            pool.addAll(this.specialBlocks);
         }

         Material mat = (Material)pool.get(random.nextInt(pool.size()));
         FallingBlock block = p.getWorld().spawnFallingBlock(loc, mat.createBlockData());
         block.setDropItem(false);
         if ((Boolean)this.soundEnabled.getOrDefault(p.getUniqueId(), true)) {
            p.playSound(loc, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0F, 1.0F);
         }

      }, 0L, (long)rate);
      this.rainTasks.put(p.getUniqueId(), taskId);
   }

   private void stopRain(Player p) {
      if (this.rainTasks.containsKey(p.getUniqueId())) {
         Bukkit.getScheduler().cancelTask((Integer)this.rainTasks.remove(p.getUniqueId()));
      }

   }

   private void openMainMenu(Player p) {
      Inventory inv = Bukkit.createInventory((InventoryHolder)null, 27, String.valueOf(ChatColor.BLUE) + "PalikkaSade asetukset");
      inv.setItem(11, this.createMenuItem(Material.NOTE_BLOCK, String.valueOf(ChatColor.YELLOW) + "Ääni-asetukset"));
      inv.setItem(13, this.createMenuItem(Material.CLOCK, String.valueOf(ChatColor.GREEN) + "Palikka-asetukset"));
      inv.setItem(15, this.createMenuItem(Material.CHEST, String.valueOf(ChatColor.LIGHT_PURPLE) + "Kosmetiikka"));
      p.openInventory(inv);
   }

   private void openSoundSettings(Player p) {
      String var10002 = String.valueOf(ChatColor.GREEN);
      Inventory inv = Bukkit.createInventory((InventoryHolder)null, 27, var10002 + "Ääni-asetukset");
      boolean enabled = (Boolean)this.soundEnabled.getOrDefault(p.getUniqueId(), true);
      Material var10003 = enabled ? Material.NOTE_BLOCK : Material.BARRIER;
      String var10004 = String.valueOf(ChatColor.YELLOW);
      inv.setItem(13, this.createMenuItem(var10003, var10004 + "Äänet: " + (enabled ? "Päällä" : "Pois päältä")));
      p.openInventory(inv);
   }

   private void openBlockSettings(Player p) {
      String var10002 = String.valueOf(ChatColor.GREEN);
      Inventory inv = Bukkit.createInventory((InventoryHolder)null, 27, var10002 + "Palikka-asetukset");
      int currentRate = (Integer)this.spawnRate.getOrDefault(p.getUniqueId(), 10);
      Material var10003 = Material.CLOCK;
      String var10004 = String.valueOf(ChatColor.GREEN);
      inv.setItem(11, this.createMenuItem(var10003, var10004 + "Spawn-nopeus: " + currentRate + " tick"));
      boolean includeSpecial = (Boolean)this.includeSpecialBlocks.getOrDefault(p.getUniqueId(), false);
      var10003 = Material.GRASS_BLOCK;
      var10004 = String.valueOf(ChatColor.AQUA);
      inv.setItem(15, this.createMenuItem(var10003, var10004 + "Sisällytä erikoispalikat: " + (includeSpecial ? "Kyllä" : "Ei")));
      p.openInventory(inv);
   }

   private void openCosmeticMenu(Player p) {
      Inventory inv = Bukkit.createInventory((InventoryHolder)null, 27, String.valueOf(ChatColor.LIGHT_PURPLE) + "Kosmetiikka");
      inv.setItem(13, this.createMenuItem(Material.DIAMOND_HELMET, String.valueOf(ChatColor.LIGHT_PURPLE) + "Valitse hattu"));
      p.openInventory(inv);
   }

   private void openHatMenu(Player p) {
      Inventory inv = Bukkit.createInventory((InventoryHolder)null, 27, String.valueOf(ChatColor.DARK_PURPLE) + "Valitse hattu");

      for(int i = 0; i < this.hatOptions.length && i < 27; ++i) {
         Material var10003 = this.hatOptions[i];
         String var10004 = String.valueOf(ChatColor.GRAY);
         inv.setItem(i, this.createMenuItem(var10003, var10004 + this.hatOptions[i].name()));
      }

      p.openInventory(inv);
   }

   private ItemStack createMenuItem(Material mat, String name) {
      ItemStack item = new ItemStack(mat);
      ItemMeta meta = item.getItemMeta();
      meta.setDisplayName(name);
      item.setItemMeta(meta);
      return item;
   }

   @EventHandler
   public void onInventoryClick(InventoryClickEvent e) {
      HumanEntity var3 = e.getWhoClicked();
      if (var3 instanceof Player p) {
         String var6 = e.getView().getTitle();
         int slot = e.getRawSlot();
         if (var6.equals(String.valueOf(ChatColor.BLUE) + "PalikkaSade asetukset")) {
            e.setCancelled(true);
            if (slot == 11) {
               this.openSoundSettings(p);
            } else if (slot == 13) {
               this.openBlockSettings(p);
            } else if (slot == 15) {
               this.openCosmeticMenu(p);
            }
         } else if (var6.equals(String.valueOf(ChatColor.GREEN) + "Ääni-asetukset")) {
            e.setCancelled(true);
            if (slot == 13) {
               boolean enabled = (Boolean)this.soundEnabled.getOrDefault(p.getUniqueId(), true);
               this.soundEnabled.put(p.getUniqueId(), !enabled);
               this.openSoundSettings(p);
            }
         } else if (var6.equals(String.valueOf(ChatColor.GREEN) + "Palikka-asetukset")) {
            e.setCancelled(true);
            if (slot == 11) {
               int current = (Integer)this.spawnRate.getOrDefault(p.getUniqueId(), 10);
               if (current == 5) {
                  this.spawnRate.put(p.getUniqueId(), 10);
               } else if (current == 10) {
                  this.spawnRate.put(p.getUniqueId(), 20);
               } else {
                  this.spawnRate.put(p.getUniqueId(), 5);
               }

               this.openBlockSettings(p);
            } else if (slot == 15) {
               boolean includeSpecial = (Boolean)this.includeSpecialBlocks.getOrDefault(p.getUniqueId(), false);
               this.includeSpecialBlocks.put(p.getUniqueId(), !includeSpecial);
               this.openBlockSettings(p);
            }
         } else if (var6.equals(String.valueOf(ChatColor.LIGHT_PURPLE) + "Kosmetiikka")) {
            e.setCancelled(true);
            if (slot == 13) {
               this.openHatMenu(p);
            }
         } else if (var6.equals(String.valueOf(ChatColor.DARK_PURPLE) + "Valitse hattu")) {
            e.setCancelled(true);
            ItemStack clicked = e.getCurrentItem();
            if (clicked != null && clicked.getType() != Material.AIR) {
               this.hatChoice.put(p.getUniqueId(), clicked.getType());
               p.getInventory().setHelmet(new ItemStack(clicked.getType()));
               p.closeInventory();
               String var10001 = String.valueOf(ChatColor.LIGHT_PURPLE);
               p.sendMessage(var10001 + "Hattu asetettu: " + clicked.getType().name());
            }
         }

      }
   }

   private class BlocksadeCommand extends Command {
      BlocksadeCommand() {
         super("blocksade");
      }

      public boolean execute(CommandSender sender, String commandLabel, String[] args) {
         if (!(sender instanceof Player p)) {
            return false;
         } else if (args.length == 0) {
            if (BlockSade.this.rainTasks.containsKey(p.getUniqueId())) {
               p.sendMessage(String.valueOf(ChatColor.RED) + "Palikkasade on jo käynnissä!");
               return true;
            } else {
               BlockSade.this.startRain(p);
               p.sendMessage(String.valueOf(ChatColor.GREEN) + "Palikkasade käynnistetty!");
               return true;
            }
         } else if (args[0].equalsIgnoreCase("stop")) {
            BlockSade.this.stopRain(p);
            p.sendMessage(String.valueOf(ChatColor.RED) + "Palikkasade pysäytetty!");
            return true;
         } else if (args[0].equalsIgnoreCase("info")) {
            p.sendMessage(String.valueOf(ChatColor.AQUA) + "Blocksade komennot:");
            String var10001 = String.valueOf(ChatColor.YELLOW);
            p.sendMessage(var10001 + "/blocksade" + String.valueOf(ChatColor.WHITE) + " - Aloita palikkasade");
            var10001 = String.valueOf(ChatColor.YELLOW);
            p.sendMessage(var10001 + "/blocksade stop" + String.valueOf(ChatColor.WHITE) + " - Pysäytä palikkasade");
            var10001 = String.valueOf(ChatColor.YELLOW);
            p.sendMessage(var10001 + "/blocksade info" + String.valueOf(ChatColor.WHITE) + " - Näytä komennot");
            var10001 = String.valueOf(ChatColor.YELLOW);
            p.sendMessage(var10001 + "/blocksade asetukset" + String.valueOf(ChatColor.WHITE) + " - Avaa asetusvalikko");
            return true;
         } else if (args[0].equalsIgnoreCase("asetukset")) {
            BlockSade.this.openMainMenu(p);
            return true;
         } else {
            return false;
         }
      }
   }
}
