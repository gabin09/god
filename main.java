package ta1e.god;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.CommandExecutor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Arrays;

public final class God extends JavaPlugin implements CommandExecutor, Listener {

    // 반짝임 효과를 추가하는 메서드
    public void addBlinking(final ItemMeta meta, final int delayTicks) {
        new BukkitRunnable() {
            boolean state = false;

            @Override
            public void run() {
                for (int i = 0; i < meta.getLore().size(); i++) {
                    String line = meta.getLore().get(i);
                    if (state) {
                        line = line.replaceAll("§r$", "§r§k"); // 빛나는 효과 추가
                    } else {
                        line = line.replaceAll("§r§k", "§r"); // 빛나는 효과 제거
                    }
                    meta.getLore().set(i, line);
                }
                state = !state;
            }
        }.runTaskTimer(this, 0, delayTicks);
    }

    @Override
    public void onEnable() {
        // 플러그인이 활성화될 때 실행되는 코드
        this.getCommand("god").setExecutor(this); // "god" 명령어를 이 클래스에서 처리하도록 설정
        getServer().getPluginManager().registerEvents(this, this); // 이벤트 리스너 등록
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("god")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("이 명령어는 플레이어만 사용할 수 있습니다.");
                return true;
            }

            Player player = (Player) sender;

            if (player.hasPermission("god")) {
                if (player.isInvulnerable()) {

                    player.getAllowFlight();
                    player.sendMessage("§c신의 힘이 해제되었습니다.");
                    // 책 제거
                    player.getInventory().removeItem(new ItemStack(Material.BOOK));
                    // 도끼 제거
                    player.getInventory().removeItem(getGodAxe());
                } else {
                    // 플레이어의 무적 상태 활성화

                    player.setAllowFlight(true); // 비행 활성화
                    player.sendMessage("§a신의 힘이 활성화되었습니다.");
                    // 책 지급
                    ItemStack book = getBook();
                    player.getInventory().addItem(book);
                    // 도끼 지급
                    ItemStack godAxe = getGodAxe();
                    player.getInventory().addItem(godAxe);
                }
            } else {
                player.sendMessage("§c당신은 이 명령어를 사용할 권한이 없습니다.");
            }
            return true;
        }
        return false;
    }

    private ItemStack getBook() {
        ItemStack book = new ItemStack(Material.NETHERITE_HELMET);
        ItemMeta meta = book.getItemMeta();
        meta.setDisplayName("§6신들의 헬멧");
        meta.setLore(Arrays.asList(
                "§f신의 보호구를 잠시 동안 얻었다....",
                "§e[§e스킬§e] §f: 장식",
                "§7[등급]:§f신화"));
        // 반짝임 효과를 추가
        addBlinking(meta, 10); // 10 틱마다 반짝임
        book.setItemMeta(meta);
        return book;
    }

    private ItemStack getGodAxe() {
        ItemStack axe = new ItemStack(Material.NETHERITE_AXE);
        ItemMeta meta = axe.getItemMeta();
        meta.setDisplayName("§6스톰브레이커");
        meta.setUnbreakable(true);
        meta.setLore(Arrays.asList(
                "§f신의 힘을 잠시 동안 얻었다....",
                "§e[§e스킬§e] §f: 우클릭:화염구, 좌클릭: 번개",
                "§7[등급]:§f신화"));
        // 반짝임 효과를 추가
        addBlinking(meta, 10); // 10 틱마다 반짝임
        axe.setItemMeta(meta);
        return axe;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item != null && item.getType() == Material.BOOK && item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            if (item.getItemMeta().getDisplayName().equals("§6신들의 지식")) {
                Location targetLocation = findNearestEntity(player.getLocation(), 100);
                if (targetLocation != null) {
                    for (Entity entity : player.getWorld().getNearbyEntities(targetLocation, 1, 1, 1)) {
                        if (entity instanceof Player) {
                            Player targetPlayer = (Player) entity;
                            targetPlayer.setGlowing(true);
                            Bukkit.getScheduler().runTaskLater(this, () -> targetPlayer.setGlowing(false), 200); // 10초 후에 발광 해제
                        }
                    }
                }
            }
        } else if (item != null && item.getType() == Material.NETHERITE_AXE && item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            if (item.getItemMeta().getDisplayName().equals("§6스톰브레이커")) {
                if (event.getAction().name().contains("RIGHT")) { // 우클릭
                    player.launchProjectile(org.bukkit.entity.Fireball.class);
                } else if (event.getAction().name().contains("LEFT")) { // 좌클릭
                    Location targetLocation = findNearestEntity(player.getLocation(), 100);
                    if (targetLocation != null) {
                        player.getWorld().strikeLightningEffect(targetLocation);
                        player.getWorld().strikeLightning(targetLocation);
                    }
                }
            }
        }
    }

    private Location findNearestEntity(Location loc, double radius) {
        Entity closestEntity = null; // 가장 가까운 엔티티
        double closestDistance = Double.MAX_VALUE; // 가장 가까운 거리

        // 주어진 위치 주변의 엔티티를 반복
        for (Entity entity : loc.getWorld().getEntities()) {
            // 플레이어 엔티티가 아닌 경우에만 고려
            if (!(entity instanceof Player)) {
                // 주어진 위치와 엔티티 간의 거리 계산
                double distance = loc.distance(entity.getLocation());
                // 주어진 반경 내에 있는 경우
                if (distance <= radius) {
                    // 가장 가까운 엔티티보다 더 가까운 경우
                    if (distance < closestDistance) {
                        // 현재 엔티티를 가장 가까운 엔티티로 설정
                        closestEntity = entity;
                        closestDistance = distance;
                    }
                }
            }
        }

        // 가장 가까운 엔티티가 있으면 해당 위치를 반환하고, 그렇지 않으면 null 반환
        if (closestEntity != null) {
            return closestEntity.getLocation();
        } else {
            return null;
        }
    }
}
