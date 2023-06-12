package glorydark.wardensystem;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.scheduler.NukkitRunnable;
import cn.nukkit.utils.Config;
import cn.nukkit.utils.ConfigSection;
import glorydark.wardensystem.data.*;
import glorydark.wardensystem.forms.FormMain;
import glorydark.wardensystem.forms.WardenEventListener;
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
import java.util.stream.Collectors;

public class MainClass extends PluginBase {

    public static HashMap<String, WardenData> wardens = new HashMap<>();

    public static List<BugReport> bugReports = new ArrayList<>();

    public static List<ByPassReport> byPassReports = new ArrayList<>();

    public static HashMap<Player, Integer> mails = new HashMap<>();

    public static String path;

    public static HashMap<String, Reward> rewards = new HashMap<>();

    public static List<String> muted = new ArrayList<>();

    public static HashMap<String, SuspectData> suspectList = new HashMap<>();

    public static Logger log;

    public static List<OfflineData> offlineData = new ArrayList<>();

    public static HashMap<Player, PlayerData> playerData = new HashMap<>();

    public static List<String> forbid_modify_worlds;

    @Override
    public void onLoad() {
        this.getLogger().info("WardenSystem 正在加载！");
    }

    @Override
    public void onEnable() {
        path = this.getDataFolder().getPath();
        log = Logger.getLogger("WardenSystem_"+ UUID.randomUUID());
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
        forbid_modify_worlds = new ArrayList<>(config.getStringList("forbid_modify_worlds"));
        for(String player: new ArrayList<>(config.getStringList("admins"))){
            WardenData data = new WardenData(null, new Config(path+"/wardens/"+ player + ".yml", Config.YAML));
            data.setLevelType(WardenLevelType.ADMIN);
            wardens.put(player, data);
        }
        for(String player: new ArrayList<>(config.getStringList("wardens"))){
            if(wardens.containsKey(player)){
                continue;
            }
            WardenData data = new WardenData(null, new Config(path+"/wardens/"+ player + ".yml", Config.YAML));
            data.setLevelType(WardenLevelType.NORMAL);
            wardens.put(player, data);
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
        Config suspects = new Config(path+"/suspects.yml", Config.YAML);
        for(String s: suspects.getKeys(false)){
            suspectList.put(s, new SuspectData(s, suspects.getLong(s +".start"), suspects.getLong(s +".end")));
        }
        Config rewardCfg = new Config(path+"/rewards.yml", Config.YAML);
        rewards.put("无奖励", new Reward(new ConfigSection()));
        for(String key: rewardCfg.getKeys(false)){
            rewards.put(key, new Reward(rewardCfg.getSection(key)));
        }
        new NukkitRunnable() {
            @Override
            public void run() {
                offlineData.removeIf(OfflineData::isExpired);
            }
        }.runTaskTimer(this, 0, 20);
        this.getServer().getPluginManager().registerEvents(new WardenEventListener(), this);
        this.getServer().getCommandMap().register("", new WardenCommand(config.getString("command")));
        // this.getServer().getCommandMap().register("", new TestCommand("test"));
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
                switch (strings[0]){
                    case "admin":
                        if(strings.length < 2){ return true; }
                        if(!Server.getInstance().lookupName(strings[1]).isPresent()){
                            commandSender.sendMessage("§c找不到玩家！");
                            return true;
                        }
                        Config config = new Config(path+"/config.yml", Config.YAML);
                        List<String> admins = new ArrayList<>(config.getStringList("admins"));
                        if(admins.contains(strings[1])){
                            admins.remove(strings[1]);
                            commandSender.sendMessage("§a成功取消玩家【"+strings[1]+"】协管主管权限！");
                            log.log(Level.INFO, "CONSOLE执行：/warden admin "+strings[1] + "，§a成功取消玩家【"+strings[1]+"】协管主管权限！");
                            return true;
                        }
                        List<String> wardens = new ArrayList<>(config.getStringList("wardens"));
                        if(wardens.contains(strings[1])){
                            wardens.remove(strings[1]);
                            config.set("wardens", wardens);
                        }
                        admins.add(strings[1]);
                        config.set("admins", admins);
                        config.save();
                        commandSender.sendMessage("§a成功为玩家【"+strings[1]+"】赋予协管主管权限！");
                        log.log(Level.INFO, "CONSOLE执行：/warden admin "+strings[1]+"，§a成功为玩家【"+strings[1]+"】赋予协管主管权限！");
                        break;
                    case "add":
                        if(strings.length < 2){ return true; }
                        if(!Server.getInstance().lookupName(strings[1]).isPresent()){
                            commandSender.sendMessage("§c找不到玩家！");
                            return true;
                        }
                        config = new Config(path+"/config.yml", Config.YAML);
                        wardens = new ArrayList<>(config.getStringList("wardens"));
                        if(wardens.contains(strings[1])){
                            commandSender.sendMessage("§c该玩家已为协管！");
                        }else{
                            wardens.add(strings[1]);
                            config.set("wardens", wardens);
                            config.save();
                            WardenData data = new WardenData(null, new Config(path+"/wardens/"+strings[1]+".yml", Config.YAML));
                            MainClass.wardens.put(strings[1], data);
                            commandSender.sendMessage("§a成功为玩家【"+strings[1]+"】赋予协管权限！");
                            log.log(Level.INFO, "CONSOLE执行：/warden add "+strings[1]);
                        }
                        break;
                    case "remove":
                        if(strings.length < 2){ return true; }
                        if(!Server.getInstance().lookupName(strings[1]).isPresent()){
                            commandSender.sendMessage("§c找不到玩家！");
                            return true;
                        }
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
                        if(strings.length < 2){ return true; }
                        if(!Server.getInstance().lookupName(strings[1]).isPresent()){
                            commandSender.sendMessage("§c找不到玩家！");
                            return true;
                        }
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
                            Server.getInstance().broadcastMessage("§e["+strings[1]+"] 因游戏作弊被打入小黑屋！");
                        }
                        break;
                    case "unban":
                        if(strings.length < 2){ return true; }
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
                        if(strings.length < 2){ return true; }
                        if(!Server.getInstance().lookupName(strings[1]).isPresent()){
                            commandSender.sendMessage("§c找不到玩家！");
                            return true;
                        }
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
                            Server.getInstance().broadcastMessage("§e["+strings[1]+"] 因违规发言被禁止发言！");
                        }
                        break;
                    case "unmute":
                        if(strings.length < 2){ return true; }
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
                        if(strings.length < 2){ return true; }
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
                        if(strings.length < 2){ return true; }
                        Player kicked = Server.getInstance().getPlayer(strings[1]);
                        if(kicked != null){
                            kicked.kick("§c您已被踢出，请规范您的游戏行为！");
                            commandSender.sendMessage("§a已踢出该玩家！");
                            log.log(Level.INFO, "CONSOLE执行：/warden kick "+strings[1]);
                        }else{
                            commandSender.sendMessage("§c该玩家不存在！");
                        }
                        break;
                    case "list":
                        if(MainClass.bugReports.size() > 0) {
                            commandSender.sendMessage("bug反馈：");
                            for (BugReport report : MainClass.bugReports) {
                                commandSender.sendMessage("- 反馈玩家：" + report.getPlayer() + "，反馈内容：" + report.getInfo() + "，日期：" + MainClass.getDate(report.getMillis()) + "，毫秒数：" + report.millis);
                            }
                        }else{
                            commandSender.sendMessage("暂无未处理的bug反馈！");
                        }
                        if(MainClass.byPassReports.size() > 0) {
                            commandSender.sendMessage("bypass反馈：");
                            for (ByPassReport report : MainClass.byPassReports) {
                                commandSender.sendMessage("- 反馈玩家：" + report.getPlayer() + "，被举报玩家，" + report.getSuspect() + "，反馈内容：" + report.getInfo() + "，日期：" + MainClass.getDate(report.getMillis()) + "，毫秒数：" + report.millis);
                            }
                        }else{
                            commandSender.sendMessage("暂无未处理的bug反馈！");
                        }
                        break;
                    case "refreshworkload":
                        if(MainClass.wardens.size() > 0){
                            for (WardenData value : MainClass.wardens.values()) {
                                value.setDealBugReportTimes(0);
                                value.setDealBypassReportTimes(0);
                                value.save();
                            }
                        }
                        break;
                    case "workload":
                        if(MainClass.wardens.size() > 0){
                            log.log(Level.INFO, "CONSOLE执行：/warden workload");
                            Map<String, Integer> cacheMap = new HashMap<>();
                            for (Map.Entry<String, WardenData> entry : MainClass.wardens.entrySet()) {
                                if(entry.getValue().getLevelType() == WardenLevelType.ADMIN){
                                    continue;
                                }
                                cacheMap.put(entry.getKey(), entry.getValue().getDealBugReportTimes()+entry.getValue().getDealBugReportTimes());
                            }
                            List<Map.Entry<String, Integer>> list = cacheMap.entrySet().stream().sorted(Comparator.comparingInt(Map.Entry::getValue)).collect(Collectors.toList());
                            if(list.size() == 0){
                                log.log(Level.INFO, "CONSOLE执行结果：暂无数据");
                                commandSender.sendMessage("暂无数据");
                            }
                            StringBuilder builder = new StringBuilder("- 最拉协管榜单（Bottom） -");
                            int i = 1;
                            for(Map.Entry<String, Integer> entry: list){
                                builder.append("\n[").append(i).append("] ").append(entry.getKey()).append(" - ").append(entry.getValue());
                                i++;
                            }

                            Collections.reverse(list);
                            builder.append("\n\n").append("- 优秀协管榜单（Top） -");
                            i = 1;
                            for(Map.Entry<String, Integer> entry: list){
                                builder.append("\n[").append(i).append("] ").append(entry.getKey()).append(" - ").append(entry.getValue());
                                i++;
                            }
                            commandSender.sendMessage(builder.toString());
                            log.log(Level.INFO, "CONSOLE执行结果：\n"+ builder);
                        }
                        break;
                }
            }
            return true;
        }
    }

    public static String getUnBannedDate(String player){
        Config config = new Config(path+"/ban.yml", Config.YAML);
        return !String.valueOf(config.get(player+".end", "")).equals("permanent")? MainClass.getDate(config.getLong(player+".end")): "永久封禁";
    }

    public static String getUnMutedDate(String player){
        Config config = new Config(path+"/mute.yml", Config.YAML);
        return !String.valueOf(config.get(player+".end", "")).equals("permanent")? MainClass.getDate(config.getLong(player+".end")): "永久封禁";
    }

    public static long getRemainedBannedTime(String player){
        Config config = new Config(path+"/ban.yml", Config.YAML);
        if(config.exists(player)){
            if(config.get(player+".end").toString().equals("permanent")){
                return -1;
            }else{
                if(System.currentTimeMillis() >= config.getLong(player+".end")){
                    config.remove(player);
                    config.save();
                    return 0;
                }else{
                    return config.getLong(player+".end") - System.currentTimeMillis();
                }
            }
        }
        return 0;
    }

    public static long getRemainedMutedTime(String player){
        Config config = new Config(path+"/mute.yml", Config.YAML);
        if(config.exists(player)){
            if(config.get(player+".end").toString().equals("permanent")){
                return -1;
            }else{
                if(System.currentTimeMillis() >= config.getLong(player+".end")){
                    config.remove(player);
                    config.save();
                    return 0;
                }else{
                    return (config.getLong(player+".end") - System.currentTimeMillis());
                }
            }
        }
        return 0;
    }
}
