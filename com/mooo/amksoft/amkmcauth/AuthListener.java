package com.mooo.amksoft.amkmcauth;

import java.security.NoSuchAlgorithmException;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class AuthListener implements Listener {

    private final AmkMcAuth plugin;

    public AuthListener(AmkMcAuth instance) {
        this.plugin = instance;
    }

    @EventHandler
    public void sameName(AsyncPlayerPreLoginEvent e) {
        if (Config.kickIfAlreadyOnline) return; // Allow Login if 'KickIfOnline'=false
        AuthPlayer ap = AuthPlayer.getAuthPlayer(e.getName());
        Player p = ap.getPlayer();
        if (p == null) return; // Allow Login if not 'OnLine'
        if (!ap.isLoggedIn()) return; // Allow Login if not 'Logged In'
        e.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, Language.ANOTHER_PLAYER_WITH_NAME.toString());
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void earlyPlayerJoin(PlayerLoginEvent e) {
    //public void join(PlayerLoginEvent e) {
        Player p = e.getPlayer();
        final AuthPlayer ap = AuthPlayer.getAuthPlayer(p);
 
    	if(!ap.isRegistered()){
    		ap.setUserName(p.getName()); // So the PConfManager knows his PlayerName while removing..
    		if(PConfManager.doesPlayerExist(p.getName().toLowerCase())) {
        		this.plugin.getLogger().info(p.getName() + ": "+ Language.PLAYER_REGISTERED_OTHERCASE);
        		//p.kickPlayer(Language.PLAYER_ALREADY_REGISTERED.toString());
            	e.disallow(PlayerLoginEvent.Result.KICK_FULL, p.getName() + ": "+ Language.PLAYER_REGISTERED_OTHERCASE);
        	}
    		else 
    		{
    			// Spawn off a asynchronously process to find the Number of Players from this Ip-Address 
        		new BukkitRunnable() {
        		    @Override
        		    public void run() {
                    	int PlayerCount = PConfManager.countPlayersFromIp(ap.getCurrentIPAddress()); // "192.168.1.7","Userid"
                    	// Store PlayerCount so Register command can find and check on it.
                    	ap.setPlayerIpCount(PlayerCount);
        		    }
        		}.runTaskAsynchronously(AmkMcAuth.getInstance());            	
    		}
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent e) {

        if (this.plugin.getServer().getOnlineMode() && Config.disableIfOnlineMode) return;
        if (!Config.requireLogin) return;
        Player p = e.getPlayer();
        AuthPlayer ap = AuthPlayer.getAuthPlayer(p);

        if (Config.useLoginPermission && !p.hasPermission(Config.loginPermission)) return;
        ap.setLastJoinTimestamp(System.currentTimeMillis());
        ap.setJoinLocation(p.getLocation()); // Save Current (join) Location
    	if (ap.isWithinSession()) {
        	if(ap.isVIP()){ // is on nlplist 
        		p.sendMessage(ChatColor.BLUE + Language.LOGGED_IN_VIA_NLPLIST.toString()); // NLPLIST!!
        		this.plugin.getLogger().info(p.getName() + " "+ Language.WAS_LOGGED_IN_VIA_NLPLIST);
        	}	
        	else
        		{
                if(p.hasPermission("amkauth.nlpwd")) { // has authorization 
        			p.sendMessage(ChatColor.BLUE + Language.LOGGED_IN_VIA_NLPAUTH.toString()); // NLPAUTH!!
        			this.plugin.getLogger().info(p.getName() + " "+ Language.WAS_LOGGED_IN_VIA_NLPAUTH);
                }
                else
        			{
                	if ((Config.sessionsEnabled)) { // Just normal default session time
                		p.sendMessage(ChatColor.BLUE + Language.LOGGED_IN_VIA_SESSION.toString());
                		this.plugin.getLogger().info(p.getName() + " "+ Language.WAS_LOGGED_IN_VIA_SESSION);
                	}
        		}
        	}
        	ap.enableAfterLoginGodmode();
        	ap.setLoggedIn(true);

            if((ap.getEmailAddress().equals("") | ap.getEmailAddress().contains("#")) && Config.emailForceSet) {
				ap.createSetEmailReminder(this.plugin);
        	}

    		return;
        }
        ap.logout(this.plugin); // Illegal Login, just 'Logout' ..
    }

    @EventHandler
    public void godModeAfterLogin(EntityDamageEvent e) {
        if (!Config.godModeAfterLogin) return;
        if (!(e.getEntity() instanceof Player)) return;
        Player p = (Player) e.getEntity();
        AuthPlayer ap = AuthPlayer.getAuthPlayer(p);
        if (!ap.isInAfterLoginGodmode()) return;
        e.setDamage(0);
        e.setCancelled(true);
    }

    @EventHandler
    public void quit(PlayerQuitEvent e) {
        if (!Config.sessionsEnabled) return;
        AuthPlayer ap = AuthPlayer.getAuthPlayer(e.getPlayer());
        ap.setLastQuitTimestamp(System.currentTimeMillis());
        BukkitTask reminder = ap.getCurrentReminderTask();
        if (reminder != null) reminder.cancel();
        if (ap.isLoggedIn()) ap.updateLastIPAddress();
    }

    @EventHandler
    public void kick(PlayerKickEvent e) {
        quit(new PlayerQuitEvent(e.getPlayer(), e.getLeaveMessage()));
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        if (Config.allowMovementWalk) return;
        Player p = e.getPlayer();
        AuthPlayer ap = AuthPlayer.getAuthPlayer(p);
        if (ap.isLoggedIn()) return;
        
        Location to = e.getTo();
        Location from = e.getFrom();
        boolean walked = to.getX() != from.getX() || to.getY() != from.getY() || to.getZ() != from.getZ();
        if(Config.allowMovementTime>0) {
 			//p.sendMessage(ChatColor.BLUE + "Time: " + ap.getLastJoinTimestamp() + " " +Config.allowMovementTime + " " + System.currentTimeMillis());
            if(walked && ap.getLastWalkTimestamp()+Config.allowMovementTime<=System.currentTimeMillis()) {
     			//p.sendMessage(ChatColor.BLUE + "Reset? " + (ap.getLastJoinTimestamp() + Config.allowMovementTime) + " " + System.currentTimeMillis());
            	e.setTo(ap.getJoinLocation());
            	ap.setLastWalkTimestamp(System.currentTimeMillis()); // next allowed walk timeout
            }
        }
        else
        	if (walked || !Config.allowMovementLook) e.setTo(e.getFrom());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent e) {
        Player p = e.getPlayer();
        final AuthPlayer ap = AuthPlayer.getAuthPlayer(p);

    	String Cmnd;
    	String Parm;
        String m = e.getMessage();
        m = m.replaceAll("  ", " ").trim(); // No DoubleSpaces and spaces
        int i = m.indexOf(' ');
        if(i==-1) {
        	Cmnd = m;
        	Parm = "";
        }
        else{
        	Cmnd = m.substring(0, i);
        	Parm = m.substring(i).trim();
        }
		//p.sendMessage("+++" + m + "+++");
		//p.sendMessage("+++" + Cmnd + "+++");
		//p.sendMessage("+++" + Parm + "+++");
        if (" \\l \\li \\login \\logon ".contains(" "+Cmnd+" ")){
        	// Execute PlacerCommand: boolean Player.performCommand(String command);        	
        	if (!p.hasPermission("amkauth.login")) {
        		AmkAUtils.dispNoPerms(p);
                e.setMessage(""); // Clear Message is set.
        		e.setCancelled(true);
        		return;
        	}
        	if (Parm.length() < 1) {
        		if(Config.sessionType.contains("HiddenChat"))
        			p.sendMessage(String.format(Language.USAGE_LOGIN0.toString(),Cmnd));
        		else
        			p.sendMessage(String.format(Language.USAGE_LOGIN2.toString(),Cmnd));
        		p.sendMessage(Language.USAGE_LOGIN1.toString());
                e.setMessage("");
        		e.setCancelled(true);
        		return;
        	}
        	if (!(p instanceof Player)) {
        		p.sendMessage(ChatColor.RED + Language.COMMAND_NO_CONSOLE.toString());
                e.setMessage("");
        		e.setCancelled(true);
        		return;
        	}
        	if (ap.isLoggedIn()) {
        		p.sendMessage(ChatColor.RED + Language.ALREADY_LOGGED_IN.toString());
                e.setMessage("");
        		e.setCancelled(true);
        		return;
        	}
        	String rawPassword = Parm; // Space Support !!!        	
        	for (String disallowed : Config.disallowedPasswords) {
        		if (!rawPassword.equalsIgnoreCase(disallowed)) continue;
        		p.sendMessage(ChatColor.RED + Language.DISALLOWED_PASSWORD.toString());
        	}
        	String hashType = (!ap.getHashType().equalsIgnoreCase(Config.passwordHashType)) ? ap.getHashType() : Config.passwordHashType;
        	try {
        		rawPassword = Hasher.encrypt(rawPassword, hashType);
        	} catch (NoSuchAlgorithmException err) {
        		p.sendMessage(ChatColor.RED + Language.COULD_NOT_LOG_IN.toString());
        		p.sendMessage(ChatColor.RED + Language.ADMIN_SET_UP_INCORRECTLY.toString());
        		p.sendMessage(ChatColor.RED + Language.CONTACT_ADMIN.toString());
                e.setMessage("");
        		e.setCancelled(true);
        		return;
        	}
        	String realPassword = ap.getPasswordHash();
        	if (rawPassword.equals(realPassword)) {
        		ap.login();
        		this.plugin.getLogger().info(p.getName() + " " + Language.HAS_LOGGED_IN);
        		p.sendMessage(ChatColor.BLUE + Language.LOGGED_IN_SUCCESSFULLY.toString());
        	} else {
        		this.plugin.getLogger().warning(p.getName() + " " + Language.USED_INCORRECT_PASSWORD);
                if(Config.KickOnPasswordFail) {
                    e.setMessage("");
                	e.setCancelled(true);
                	final Player pl=p;
                	Bukkit.getScheduler().runTaskLater(this.plugin, new Runnable() {
                		public void run() {
                			pl.kickPlayer(Language.INCORRECT_PASSWORD.toString());
                    	}
                	}, 10L);                		
                	return;
                }
        		p.sendMessage(ChatColor.RED + Language.INCORRECT_PASSWORD.toString());
        	}
        	return;
        }
    
        if (" \\lo \\logoff \\logout ".contains(" "+Cmnd+" ")){
            if (!p.hasPermission("amkauth.logout")) {
                AmkAUtils.dispNoPerms(p);
                e.setMessage("");
                e.setCancelled(true);
                return;
            }
            if (!(p instanceof Player)) {
                p.sendMessage(ChatColor.RED + Language.COMMAND_NO_CONSOLE.toString());
                e.setMessage("");
                e.setCancelled(true);
                return;
            }
            if (!ap.isLoggedIn()) {
                p.sendMessage(ChatColor.RED + Language.NOT_LOGGED_IN.toString());
                e.setMessage("");
                e.setCancelled(true);
                return;
            }
            p.sendMessage(ChatColor.BLUE + Language.LOGGED_OUT.toString());
            ap.setLastQuitTimestamp(System.currentTimeMillis());
            ap.setLastJoinTimestamp(System.currentTimeMillis());
            ap.logout(this.plugin);

            e.setMessage("");
            e.setCancelled(true);
            return;
        }
        if (" \\register ".contains(" "+Cmnd+" ")){
            if (!p.hasPermission("amkauth.register")) {
                AmkAUtils.dispNoPerms(p);
                e.setMessage("");
                e.setCancelled(true);
                return;
            }
        	if (Parm.length() < 1) {
        		if(Config.sessionType.contains("HiddenChat"))
        			p.sendMessage(String.format(Language.USAGE_REGISTER0.toString(),Cmnd));
        		else
        			p.sendMessage(String.format(Language.USAGE_REGISTER2.toString(),Cmnd));
        		p.sendMessage(Language.USAGE_REGISTER1.toString());
                e.setMessage("");
                e.setCancelled(true);
                return;
            }
            if (!(p instanceof Player)) {
                p.sendMessage(ChatColor.RED + Language.COMMAND_NO_CONSOLE.toString());
                e.setCancelled(true);
                return;
            }
            if (ap.isLoggedIn() || ap.isRegistered()) {
                p.sendMessage(ChatColor.RED + Language.ALREADY_REGISTERED.toString());
                e.setMessage("");
                e.setCancelled(true);
                return;
            }

            if (Config.maxUsersPerIpaddress>0) {
            	int PlayerCount = PConfManager.countPlayersFromIp(ap.getCurrentIPAddress()); // "192.168.1.7","Userid"    	
            	//this.plugin.getLogger().info("AuthListener call to countPlayersFromIp have to go to /register command..");
            	//this.plugin.getLogger().info("192.168.1.7" + " found in " + PlayerCount + " Player(s) ");
            	this.plugin.getLogger().info("Login Ip-Address " + ap.getCurrentIPAddress() + " used by " + PlayerCount + " player(s) ");
            	this.plugin.getLogger().info("Configured maximum registered players from one Ip-Address is: " + Config.maxUsersPerIpaddress);
            	if (PlayerCount >= Config.maxUsersPerIpaddress) {
            		p.sendMessage(ChatColor.RED + Language.PLAYER_EXCEEDS_MAXREGS_IP.toString());
            		e.setMessage("");
            		e.setCancelled(true);
            		return;
            	}
            }
        	else
        		{
            	this.plugin.getLogger().info("Maximum allowed registered player counting from one Ip-Address is disabled.");            		
        	}

        	String rawPassword = Parm; // Space Support !!!        	
            for (String disallowed : Config.disallowedPasswords) {
                if (!rawPassword.equalsIgnoreCase(disallowed)) continue;
                p.sendMessage(ChatColor.RED + Language.DISALLOWED_PASSWORD.toString());
                e.setMessage("");
                e.setCancelled(true);
                return;
            }
            if (ap.setPassword(rawPassword, Config.passwordHashType)) {
                this.plugin.getLogger().info(p.getName() + " " + Language.HAS_REGISTERED);
                p.sendMessage(ChatColor.BLUE + Language.PASSWORD_SET_AND_REGISTERED.toString());

                ap.setUserName(p.getName()); 
                PConfManager.addAllPlayer(p.getName().toLowerCase());
    			new BukkitRunnable() {
    				@Override
    				public void run() {        		    	
    					PConfManager.addPlayerToIp(ap.getCurrentIPAddress()); // "192.168.1.7"
    				}
    			}.runTaskAsynchronously(AmkMcAuth.getInstance());
                
                //PConfManager.addPlayerToIp(ap.getCurrentIPAddress());
                
                BukkitTask reminder = ap.getCurrentReminderTask();
                if (reminder != null) reminder.cancel();
                ap.createLoginReminder(this.plugin);

            } else p.sendMessage(ChatColor.RED + Language.PASSWORD_COULD_NOT_BE_SET.toString());

            e.setMessage("");
            e.setCancelled(true);
            return;
        }
        if (" \\changepassword \\changepass \\passchange ".contains(" "+Cmnd+" ")){
            if (!p.hasPermission("amkauth.changepassword")) {
                AmkAUtils.dispNoPerms(p);
                e.setMessage("");
                e.setCancelled(true);
                return;
            }
            if (!(p instanceof Player)) {
                p.sendMessage(ChatColor.RED + Language.COMMAND_NO_CONSOLE.toString());
                e.setCancelled(true);
                return;
            }
            if (!ap.isLoggedIn()) {
                p.sendMessage(ChatColor.RED + Language.YOU_MUST_LOGIN.toString());
                e.setMessage("");
                e.setCancelled(true);
                return;
            }

            String oldPassword;
            String newPassword;
            i = Parm.indexOf(',');
            if(i==-1) {
            	oldPassword = Parm.trim();
            	newPassword = "";
            }
            else{
            	oldPassword = Parm.substring(0, i).trim();
            	newPassword = Parm.substring(i+1).trim(); // +1 = skip de comma
            }            
            if (newPassword.length()<1) {
        		if(Config.sessionType.contains("HiddenChat"))
        			p.sendMessage(String.format(Language.USAGE_REGISTER0.toString(),Cmnd));
        		else
        			p.sendMessage(String.format(Language.USAGE_CHANGEPAS2.toString(),Cmnd));
            	p.sendMessage(String.format(Language.USAGE_CHANGEPAS0.toString(),Cmnd));
            	p.sendMessage(Language.USAGE_CHANGEPAS1.toString());
                e.setMessage("");
                e.setCancelled(true);
                return;
            }
            
            for (String disallowed : Config.disallowedPasswords) {
                if (!newPassword.equalsIgnoreCase(disallowed)) continue;
                p.sendMessage(ChatColor.RED + Language.DISALLOWED_PASSWORD.toString());
                e.setMessage("");
                e.setCancelled(true);
                return;
            }
            try {
                oldPassword = Hasher.encrypt(oldPassword, ap.getHashType());
                newPassword = Hasher.encrypt(newPassword, Config.passwordHashType);
            } catch (NoSuchAlgorithmException f) {
                p.sendMessage(ChatColor.RED + Language.ADMIN_SET_UP_INCORRECTLY.toString());
                p.sendMessage(ChatColor.RED + Language.YOUR_PASSWORD_COULD_NOT_BE_CHANGED.toString());
                e.setMessage("");
                e.setCancelled(true);
                return;
            }
            if (!ap.getPasswordHash().equals(oldPassword)) {
                p.sendMessage(ChatColor.RED + Language.OLD_PASSWORD_INCORRECT.toString());
                e.setMessage("");
                e.setCancelled(true);
                return;
            }
            ap.setHashedPassword(newPassword, Config.passwordHashType);
            p.sendMessage(ChatColor.BLUE + Language.YOUR_PASSWORD_CHANGED.toString());

            e.setMessage("");
            e.setCancelled(true);
            return;
        }

        // -----------------------------------------------------------------------------------
        
        if (ap.isLoggedIn()) return; // LoggedIn, All is well
        if (!Config.allowChat) {
            e.setCancelled(true);
            return;
        }
        e.setMessage(AmkAUtils.colorize(Config.chatPrefix) + e.getMessage());
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent e) {
        if (Config.allowCommands) return;
        Player p = e.getPlayer();
        AuthPlayer ap = AuthPlayer.getAuthPlayer(p);
        if (ap.isLoggedIn()) return;
        String[] split = e.getMessage().split(" ");
        if (split.length < 1) {
            p.sendMessage(ChatColor.RED + Language.YOU_MUST_LOGIN.toString());
            return;
        }
        String root = split[0].substring(1); // the command label (remove /)
        for (String allowed : Config.allowedCommands) {
            if (!allowed.equalsIgnoreCase(e.getMessage().substring(1))) continue;
            return;
        }
        PluginCommand pc = this.plugin.getCommand(root);
        if (pc == null) {
            pc = this.plugin.getServer().getPluginCommand(root);
            if (pc != null) {
                if (Config.allowedCommands.contains(pc.getName())) return;
                for (String alias : pc.getAliases()) if (Config.allowedCommands.contains(alias)) return;
            }
            p.sendMessage(ChatColor.RED + Language.YOU_MUST_LOGIN.toString());
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent e) {
        if (!Config.godMode) return;
        if (!(e.getEntity() instanceof Player)) return;
        Player p = (Player) e.getEntity();
        AuthPlayer ap = AuthPlayer.getAuthPlayer(p);
        if (ap.isLoggedIn()) return;
        e.setDamage(0);
        e.setCancelled(true);
    }

    @EventHandler
    public void onLogin(PlayerLoginEvent e) {
        if (!Config.validateUsernames) return;
        Player p = e.getPlayer();
        if (p.getName().matches(Config.usernameRegex)) return;
        e.setResult(PlayerLoginEvent.Result.KICK_OTHER);
        e.setKickMessage(Language.INVALID_USERNAME.toString());
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        AuthPlayer ap = AuthPlayer.getAuthPlayer(p);
        if (ap.isLoggedIn()) return;
        e.setCancelled(true);
    }

    @EventHandler
    public void onDealDamage(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player)) return;
        Player p = (Player) e.getDamager();
        AuthPlayer ap = AuthPlayer.getAuthPlayer(p);
        if (ap.isLoggedIn()) return;
        e.setCancelled(true);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();
        AuthPlayer ap = AuthPlayer.getAuthPlayer(p);
        if (ap.isLoggedIn()) return;
        e.setCancelled(true);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent e) {
        Player p = e.getPlayer();
        AuthPlayer ap = AuthPlayer.getAuthPlayer(p);
        if (ap.isLoggedIn()) return;
        e.setCancelled(true);
    }

    @EventHandler
    public void onInventory(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        AuthPlayer ap = AuthPlayer.getAuthPlayer(p);
        if (ap.isLoggedIn()) return;
        e.setCancelled(true);
    }

    @EventHandler
    public void sign(SignChangeEvent e) {
        Player p = e.getPlayer();
        AuthPlayer ap = AuthPlayer.getAuthPlayer(p);
        if (ap.isLoggedIn()) return;
        e.setCancelled(true);
    }

    @EventHandler
    public void blockDamage(BlockDamageEvent e) {
        Player p = e.getPlayer();
        AuthPlayer ap = AuthPlayer.getAuthPlayer(p);
        if (ap.isLoggedIn()) return;
        e.setCancelled(true);
    }

    @EventHandler
    public void enchantItem(EnchantItemEvent e) {
        Player p = e.getEnchanter();
        AuthPlayer ap = AuthPlayer.getAuthPlayer(p);
        if (ap.isLoggedIn()) return;
        e.setCancelled(true);
    }

    @EventHandler
    public void onPrepareEnchant(PrepareItemEnchantEvent e) {
        Player p = e.getEnchanter();
        AuthPlayer ap = AuthPlayer.getAuthPlayer(p);
        if (ap.isLoggedIn()) return;
        e.setCancelled(true);
    }

    @EventHandler
    public void playerPortal(PlayerPortalEvent e) {
        Player p = e.getPlayer();
        AuthPlayer ap = AuthPlayer.getAuthPlayer(p);
        if (ap.isLoggedIn()) return;
        e.setCancelled(true);
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent e) {
        Player p = e.getPlayer();
        AuthPlayer ap = AuthPlayer.getAuthPlayer(p);
        if (ap.isLoggedIn()) return;
        e.setCancelled(true);
    }

    @EventHandler
    public void onPickup(PlayerPickupItemEvent e) {
        Player p = e.getPlayer();
        AuthPlayer ap = AuthPlayer.getAuthPlayer(p);
        if (ap.isLoggedIn()) return;
        e.setCancelled(true);
    }

    @EventHandler
    public void onBreakHanging(HangingBreakByEntityEvent e) {
        if (!(e.getRemover() instanceof Player)) return;
        Player p = (Player) e.getRemover();
        AuthPlayer ap = AuthPlayer.getAuthPlayer(p);
        if (ap.isLoggedIn()) return;
        e.setCancelled(true);
    }

    @EventHandler
    public void onPlaceHanging(HangingPlaceEvent e) {
        Player p = e.getPlayer();
        AuthPlayer ap = AuthPlayer.getAuthPlayer(p);
        if (ap.isLoggedIn()) return;
        e.setCancelled(true);
    }

    @EventHandler
    public void onCraft(CraftItemEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        AuthPlayer ap = AuthPlayer.getAuthPlayer(p);
        if (ap.isLoggedIn()) return;
        e.setCancelled(true);
    }

    @EventHandler
    public void onOpen(InventoryOpenEvent e) {
        if (!(e.getPlayer() instanceof Player)) return;
        Player p = (Player) e.getPlayer();
        AuthPlayer ap = AuthPlayer.getAuthPlayer(p);
        if (ap.isLoggedIn()) return;
        e.setCancelled(true);
    }

    @EventHandler
    public void onAnimate(PlayerAnimationEvent e) {
        Player p = e.getPlayer();
        AuthPlayer ap = AuthPlayer.getAuthPlayer(p);
        if (ap.isLoggedIn()) return;
        e.setCancelled(true);
    }

    @EventHandler
    public void onEnterBed(PlayerBedEnterEvent e) {
        Player p = e.getPlayer();
        AuthPlayer ap = AuthPlayer.getAuthPlayer(p);
        if (ap.isLoggedIn()) return;
        e.setCancelled(true);
    }

    @EventHandler
    public void onEmpty(PlayerBucketEmptyEvent e) {
        Player p = e.getPlayer();
        AuthPlayer ap = AuthPlayer.getAuthPlayer(p);
        if (ap.isLoggedIn()) return;
        e.setCancelled(true);
    }

    @EventHandler
    public void onFill(PlayerBucketFillEvent e) {
        Player p = e.getPlayer();
        AuthPlayer ap = AuthPlayer.getAuthPlayer(p);
        if (ap.isLoggedIn()) return;
        e.setCancelled(true);
    }

    @EventHandler
    public void onFish(PlayerFishEvent e) {
        Player p = e.getPlayer();
        AuthPlayer ap = AuthPlayer.getAuthPlayer(p);
        if (ap.isLoggedIn()) return;
        e.setCancelled(true);
    }

    @EventHandler
    public void onGamemode(PlayerGameModeChangeEvent e) {
        Player p = e.getPlayer();
        AuthPlayer ap = AuthPlayer.getAuthPlayer(p);
        if (ap.isLoggedIn()) return;
        e.setCancelled(true);
    }

    @EventHandler
    public void onIntEntity(PlayerInteractEntityEvent e) {
        Player p = e.getPlayer();
        AuthPlayer ap = AuthPlayer.getAuthPlayer(p);
        if (ap.isLoggedIn()) return;
        e.setCancelled(true);
    }

    @EventHandler
    public void onItemConsume(PlayerItemConsumeEvent e) {
        Player p = e.getPlayer();
        AuthPlayer ap = AuthPlayer.getAuthPlayer(p);
        if (ap.isLoggedIn()) return;
        e.setCancelled(true);
    }

    @EventHandler
    public void onShear(PlayerShearEntityEvent e) {
        Player p = e.getPlayer();
        AuthPlayer ap = AuthPlayer.getAuthPlayer(p);
        if (ap.isLoggedIn()) return;
        e.setCancelled(true);
    }

    @EventHandler
    public void toggleSneak(PlayerToggleSneakEvent e) {
        Player p = e.getPlayer();
        AuthPlayer ap = AuthPlayer.getAuthPlayer(p);
        if (ap.isLoggedIn()) return;
        e.setCancelled(true);
    }

    @EventHandler
    public void toggleFly(PlayerToggleFlightEvent e) {
        Player p = e.getPlayer();
        AuthPlayer ap = AuthPlayer.getAuthPlayer(p);
        if (ap.isLoggedIn()) return;
        e.setCancelled(true);
    }

    @EventHandler
    public void toggleSprint(PlayerToggleSprintEvent e) {
        Player p = e.getPlayer();
        AuthPlayer ap = AuthPlayer.getAuthPlayer(p);
        if (ap.isLoggedIn()) return;
        e.setCancelled(true);
    }

    @EventHandler
    public void enterVehicle(VehicleEnterEvent e) {
        if (!(e.getEntered() instanceof Player)) return;
        Player p = (Player) e.getEntered();
        AuthPlayer ap = AuthPlayer.getAuthPlayer(p);
        if (ap.isLoggedIn()) return;
        e.setCancelled(true);
    }

    @EventHandler
    public void exitVehicle(VehicleExitEvent e) {
        if (!(e.getExited() instanceof Player)) return;
        Player p = (Player) e.getExited();
        AuthPlayer ap = AuthPlayer.getAuthPlayer(p);
        if (ap.isLoggedIn()) return;
        e.setCancelled(true);
    }

}
