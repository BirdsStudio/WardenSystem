package glorydark.wardensystem;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.utils.Config;
import cn.nukkit.utils.ConfigSection;
import glorydark.wardensystem.forms.FormListener;
import glorydark.wardensystem.forms.FormMain;
import glorydark.wardensystem.reports.WardenData;
import glorydark.wardensystem.reports.matters.BugReport;
import glorydark.wardensystem.reports.matters.ByPassReport;
import glorydark.wardensystem.reports.matters.Reward;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MainClass extends PluginBase {

    public static HashMap<String, WardenData> wardens = new HashMap<>();

    public static List<BugReport> bugReports = new ArrayList<>();

    public static List<ByPassReport> byPassReports = new ArrayList<>();

    public static HashMap<Player, Integer> mails = new HashMap<>();

    public static String path;

    public static HashMap<String, Reward> rewards = new HashMap<>();

    public static List<String> muted = new ArrayList<>();

    public static Logger log;

    @Override
    public void onLoad() {
        this.getLogger().info("WardenSystem 正在加载！");
    }

    @Override
    public void onEnable() {
        path = this.getDataFolder().getPath();
        log = Logger.getLogger("WardenSystem");
        new File(path+"/logs/").mkdirs();
        FileHandler fileHandler;
        try {
            fileHandler = new FileHandler(path+"/logs/"+getDate(System.currentTimeMillis())+".log");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        fileHandler.setFormatter(new LoggerFormatter());
        log.addHandler(fileHandler);
        new File(path+"/bugreports/").mkdirs();
        new File(path+"/bypassreports/").mkdirs();
        new File(path+"/wardens/").mkdirs();
        new File(path+"/mailbox/").mkdirs();
        this.saveResource("config.yml", false);
        this.saveResource("rewards.yml", false);
        Config config = new Config(path+"/config.yml", Config.YAML);
        for(String player: new ArrayList<>(config.getStringList("wardens"))){
            wardens.put(player, new WardenData(null, new Config(path+"/wardens/"+ player + ".yml", Config.YAML)));
        }
        for(File file: Objects.requireNonNull(new File(path + "/bugreports/").listFiles())){
            if(file.isDirectory()){
                continue;
            }
            Config brc = new Config(file, Config.YAML);
            String filename = file.getName().split("\\.")[0];
            BugReport report = new BugReport(brc.getString("info"), brc.getString("player"), Long.parseLong(filename), brc.getBoolean("anonymous"));
            bugReports.add(report);
        }
        for(File file: Objects.requireNonNull(new File(path + "/bypassreports/").listFiles())){
            if(file.isDirectory()){
                continue;
            }
            Config brc = new Config(file, Config.YAML);
            String filename = file.getName().split("\\.")[0];
            ByPassReport report = new ByPassReport(brc.getString("info"), brc.getString("player"), brc.getString("suspect"), Long.parseLong(filename), brc.getBoolean("anonymous"));
            byPassReports.add(report);
        }
        muted.addAll(new ArrayList<>(new Config(path + "/mute.yml", Config.YAML).getKeys(false)));
        Config rewardCfg = new Config(path+"/rewards.yml", Config.YAML);
        rewards.put("无奖励", new Reward(new ConfigSection()));
        for(String key: rewardCfg.getKeys(false)){
            rewards.put(key, new Reward(rewardCfg.getSection(key)));
        }
        this.getServer().getPluginManager().registerEvents(new FormListener(), this);
        this.getServer().getCommandMap().register("", new WardenCommand(config.getString("command")));
        this.getLogger().info("WardenSystem 加载成功！");
    }

    @Override
    public void onDisable() {
        this.getLogger().info("WardenSystem 卸载中...");
    }

    public static String getDate(long millis){
        Date date = new Date(millis);
        SimpleDateFormat format = new SimpleDateFormat("yyyy年MM月dd日 HH时mm分ss秒");
        return format.format(date);
    }

    public static String getUltraPrecisionDate(long millis){
        Date date = new Date(millis);
        SimpleDateFormat format = new SimpleDateFormat("yyyy年MM月dd日 HH时mm分ss秒SSS");
        return format.format(date);
    }

    public static class WardenCommand extends Command {

        public WardenCommand(String name) {
            super(name);
        }

        @Override
        public boolean execute(CommandSender commandSender, String s, String[] strings) {
            if(commandSender.isPlayer()) {
                if(wardens.containsKey(commandSender.getName())) {
                    FormMain.showWardenMain((Player) commandSender);
                }else{
                    FormMain.showPlayerMain((Player) commandSender);
                }
            }else{
                if(strings.length < 2){ return true; }
                switch (strings[0]){
                    case "add":
                        Config config = new Config(path+"/config.yml", Config.YAML);
                        List<String> wardens = new ArrayList<>(config.getStringList("wardens"));
                        if(wardens.contains(strings[1])){
                            commandSender.sendMessage("§c该玩家已为协管！");
                        }else{
                            wardens.add(strings[1]);
                            config.set("wardens", wardens);
                            config.save();
                            MainClass.wardens.put(strings[1], new WardenData(null, new Config()));
                            commandSender.sendMessage("§a成功为玩家【"+strings[1]+"】赋予协管权限！");
                            log.log(Level.INFO, "CONSOLE执行：/warden add "+strings[1]);
                        }
                        break;
                    case "remove":
                        Config config1 = new Config(path+"/config.yml", Config.YAML);
                        List<String> wardens1 = new ArrayList<>(config1.getStringList("wardens"));
                        if(wardens1.contains(strings[1])){
                            wardens1.remove(strings[1]);
                            config1.set("wardens", wardens1);
                            config1.save();
                            MainClass.wardens.remove(strings[1]);
                            commandSender.sendMessage("§a成功卸除玩家【"+strings[1]+"】的协管权限！");
                            log.log(Level.INFO, "CONSOLE执行：/warden remove "+strings[1]);
                        }else{
                            commandSender.sendMessage("§c该玩家不是协管！");
                        }
                        break;
                    case "ban":
                        Config banCfg = new Config(path+"/ban.yml", Config.YAML);
                        List<String> banned = new ArrayList<>(banCfg.getKeys(false));
                        if(banned.contains(strings[1])){
                            commandSender.sendMessage("§c该玩家已被封禁！");
                        }else{
                            banned.add(strings[1]);
                            Map<String, Object> map = new HashMap<>();
                            map.put("start", System.currentTimeMillis());
                            map.put("end", "permanent");
                            map.put("operator", "console");
                            map.put("reason", "控制台封禁");
                            banCfg.set(strings[1], map);
                            banCfg.save();
                            commandSender.sendMessage("§a成功封禁玩家【"+strings[1]+"】！");
                            log.log(Level.INFO, "CONSOLE执行：/warden ban "+strings[1]);

                            Player punished = Server.getInstance().getPlayer(strings[1]);
                            if(punished != null){
                                punished.kick("您已被封禁");
                            }
                            // 向所有在线玩家广播封禁消息
                            String message = "§c玩家 " + strings[1] + " 被封禁了！";
                            Server.getInstance().broadcastMessage(message);
                        }
                        break;
                    case "unban":
                        Config banCfg1 = new Config(path+"/ban.yml", Config.YAML);
                        List<String> banned1 = new ArrayList<>(banCfg1.getKeys(false));
                        if(banned1.contains(strings[1])){
                            banCfg1.remove(strings[1]);
                            banCfg1.save();
                            commandSender.sendMessage("§a成功解封玩家【"+strings[1]+"】！");
                            log.log(Level.INFO, "CONSOLE执行：/warden unban "+strings[1]);
                        }else{
                            commandSender.sendMessage("§c该玩家未被封禁！");
                        }
                        break;
                    case "mute":
                        Config muteCfg = new Config(path+"/mute.yml", Config.YAML);
                        List<String> muted = new ArrayList<>(muteCfg.getKeys(false));
                        if(muted.contains(strings[1])){
                            commandSender.sendMessage("§c该玩家已被禁言！");
                        }else{
                            muted.add(strings[1]);
                            Map<String, Object> map = new HashMap<>();
                            map.put("start", System.currentTimeMillis());
                            map.put("end", "permanent");
                            map.put("operator", "console");
                            map.put("reason", "控制台封禁");
                            muteCfg.set(strings[1], map);
                            muteCfg.save();
                            commandSender.sendMessage("§a成功禁言玩家【"+strings[1]+"】！");
                            MainClass.muted.add(strings[1]);
                            Player punished = Server.getInstance().getPlayer(strings[1]);
                            if(punished != null){
                                punished.sendMessage("您已被禁言");
                            }
                            log.log(Level.INFO, "CONSOLE执行：/warden mute "+strings[1]);
                        }
                        break;
                    case "unmute":
                        Config muteCfg1 = new Config(path+"/mute.yml", Config.YAML);
                        List<String> muted1 = new ArrayList<>(muteCfg1.getKeys(false));
                        if(muted1.contains(strings[1])){
                            muteCfg1.remove(strings[1]);
                            muteCfg1.save();
                            commandSender.sendMessage("§a成功为玩家【"+strings[1]+"】解除禁言！");
                            MainClass.muted.remove(strings[1]);
                            log.log(Level.INFO, "CONSOLE执行：/warden unmute "+strings[1]);
                        }else{
                            commandSender.sendMessage("§c该玩家未被禁言！");
                        }
                        break;
                    case "warn":
                        Player to = Server.getInstance().getPlayer(strings[1]);
                        if(to != null){
                            to.sendMessage("§c您已被警告，请规范您的游戏行为！");
                            commandSender.sendMessage("§a警告已发送！");
                            log.log(Level.INFO, "CONSOLE执行：/warden warn "+strings[1]);
                        }else{
                            commandSender.sendMessage("§c该玩家不存在！");
                        }
                        break;
                    case "kick":
                        Player kicked = Server.getInstance().getPlayer(strings[1]);
                        if(kicked != null){
                            kicked.kick("§c您已被踢出，请规范您的游戏行为！");
                            commandSender.sendMessage("§a已踢出该玩家！");
                            log.log(Level.INFO, "CONSOLE执行：/warden kick "+strings[1]);
                        }else{
                            commandSender.sendMessage("§c该玩家不存在！");
                        }
                        break;
                }
            }
            return true;
        }
    }

    public static String getUnBannedDate(Player player){
        Config config = new Config(path+"/ban.yml", Config.YAML);
        return !String.valueOf(config.get(player.getName()+".end", "")).equals("permanent")? MainClass.getDate(config.getLong(player.getName()+".end")): "永久封禁";
    }

    public static String getUnMutedDate(Player player){
        Config config = new Config(path+"/mute.yml", Config.YAML);
        return !String.valueOf(config.get(player.getName()+".end", "")).equals("permanent")? MainClass.getDate(config.getLong(player.getName()+".end")): "永久封禁";
    }

    public static long getRemainedBannedTime(Player player){
        Config config = new Config(path+"/ban.yml", Config.YAML);
        if(config.exists(player.getName())){
            if(config.get(player.getName()+".end").toString().equals("permanent")){
                return -1;
            }else{
                if(System.currentTimeMillis() >= config.getLong(player.getName()+".end")){
                    config.remove(player.getName());
                    config.save();
                    return 0;
                }else{
                    return config.getLong(player.getName()+".end") - System.currentTimeMillis();
                }
            }
        }
        return 0;
    }

    public static long getRemainedMutedTime(Player player){
        Config config = new Config(path+"/mute.yml", Config.YAML);
        if(config.exists(player.getName())){
            if(config.get(player.getName()+".end").toString().equals("permanent")){
                return -1;
            }else{
                if(System.currentTimeMillis() >= config.getLong(player.getName()+".end")){
                    config.remove(player.getName());
                    config.save();
                    return 0;
                }else{
                    return (config.getLong(player.getName()+".end") - System.currentTimeMillis());
                }
            }
        }
        return 0;
    }
}
