package glorydark.wardensystem.forms;

import cn.nukkit.AdventureSettings;
import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.command.ConsoleCommandSender;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.entity.EntityDamageByEntityEvent;
import cn.nukkit.event.player.*;
import cn.nukkit.form.response.FormResponseCustom;
import cn.nukkit.form.window.FormWindow;
import cn.nukkit.form.window.FormWindowCustom;
import cn.nukkit.form.window.FormWindowModal;
import cn.nukkit.form.window.FormWindowSimple;
import cn.nukkit.utils.Config;
import glorydark.wardensystem.MainClass;
import glorydark.wardensystem.data.OfflineData;
import glorydark.wardensystem.data.PlayerData;
import glorydark.wardensystem.data.SuspectData;
import glorydark.wardensystem.data.WardenData;
import glorydark.wardensystem.reports.matters.BugReport;
import glorydark.wardensystem.reports.matters.ByPassReport;

import java.io.File;
import java.util.*;
import java.util.logging.Level;

public class Listener implements cn.nukkit.event.Listener {
    public static final HashMap<Player, HashMap<Integer, FormType>> UI_CACHE = new HashMap<>();
    
    public static void showFormWindow(Player player, FormWindow window, FormType guiType) {
        UI_CACHE.computeIfAbsent(player, i -> new HashMap<>()).put(player.showFormWindow(window), guiType);
    }

    @EventHandler
    public void PlayerQuitEvent(PlayerQuitEvent event){
        Player player = event.getPlayer();
        MainClass.offlineData.add(new OfflineData(player));
        MainClass.log.log(Level.INFO, "操作员["+player.getName()+"退出服务器，飞行状态"+player.getAdventureSettings().get(AdventureSettings.Type.FLYING)+"，游戏模式："+player.getGamemode()+"！");
    }

    @EventHandler
    public void EntityDamageByEntityEvent(EntityDamageByEntityEvent event){
        if(event.getDamager() instanceof Player && event.getEntity() instanceof Player){
            Player player = (Player) event.getEntity();
            PlayerData data = MainClass.playerData.getOrDefault(player, new PlayerData(player));
            data.addDamageSource((Player) event.getDamager());
            MainClass.playerData.put(player, data);
        }
    }
    
    @EventHandler
    public void PlayerJoinEvent(PlayerJoinEvent event){
        Player player = event.getPlayer();
        long bannedRemained = MainClass.getRemainedBannedTime(player.getName());
        if(bannedRemained != 0){
            player.kick("§c您已被封禁\n§e解封时间："+MainClass.getUnBannedDate(player));
            return;
        }
        long mutedRemained = MainClass.getRemainedMutedTime(player.getName());
        if(mutedRemained != 0){
            player.sendMessage("§c您已被禁言\n§e解封时间："+MainClass.getUnMutedDate(player));
            return;
        }
        if(MainClass.wardens.containsKey(player.getName())){
            MainClass.log.log(Level.INFO, "操作员["+player.getName()+"进入服务器！");
            if(MainClass.bugReports.size() > 0){
                player.sendMessage("§e目前有【§c"+MainClass.bugReports.size()+"§e】个bug反馈信息待处理！");
            }else{
                player.sendMessage("§a目前暂无未处理的bug反馈消息！");
            }
            if(MainClass.byPassReports.size() > 0){
                player.sendMessage("§e目前有【§c"+MainClass.byPassReports.size()+"§e】个举报信息待处理！");
            }else{
                player.sendMessage("§a目前暂无未处理的举报消息！");
            }
            if(MainClass.suspectList.size() > 0){
                List<String> suspectOnlineList = new ArrayList<>();
                for(String s: MainClass.suspectList.keySet()){
                    Player p = Server.getInstance().getPlayer(s);
                    if(p != null){
                        suspectOnlineList.add(p.getName());
                    }
                }
                player.sendMessage("现在在线的嫌疑玩家："+ Arrays.toString(suspectOnlineList.toArray()));
            }
        }
        if(MainClass.suspectList.containsKey(player.getName())){
            player.sendMessage("§c您已被列为嫌疑玩家，请端正游戏行为！");
        }
        File file = new File(MainClass.path+"/mailbox/"+player.getName()+".yml");
        if(file.exists()) {
            Config config = new Config(file, Config.YAML);
            List<Map<String, Object>> list = config.get("unclaimed", new ArrayList<>());
            player.sendMessage("§e目前有【§c"+list.size()+"§e】个未读邮件！");
        }else{
            player.sendMessage("§a目前暂无未读邮件！");
        }
    }

    @EventHandler
    public void PlayerChatEvent(PlayerChatEvent event){
        if(MainClass.muted.contains(event.getPlayer().getName())){
            event.getPlayer().sendMessage("§c您已被禁言，预计解封时间："+MainClass.getUnMutedDate(event.getPlayer()));
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void PlayerCommandPreprocessEvent(PlayerCommandPreprocessEvent event){
        if(MainClass.muted.contains(event.getPlayer().getName())){
            if(event.getMessage().startsWith("me")){
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void PlayerFormRespondedEvent(PlayerFormRespondedEvent event){
        Player p = event.getPlayer();
        FormWindow window = event.getWindow();
        if (p == null || window == null) {
            return;
        }
        FormType guiType = UI_CACHE.containsKey(p) ? UI_CACHE.get(p).get(event.getFormID()) : null;
        if(guiType == null){
            return;
        }
        UI_CACHE.get(p).remove(event.getFormID());
        if (event.getResponse() == null) {
            return;
        }
        if (window instanceof FormWindowSimple) {
            this.formWindowSimpleOnClick(p, (FormWindowSimple) window, guiType);
        }
        if (window instanceof FormWindowCustom) {
            this.formWindowCustomOnClick(p, (FormWindowCustom) window, guiType);
        }
        if (window instanceof FormWindowModal) {
            this.formWindowModalOnClick(p, (FormWindowModal) window, guiType);
        }
    }
    private void formWindowSimpleOnClick(Player player, FormWindowSimple window, FormType guiType) {
        if(window.getResponse() == null){
            if(MainClass.wardens.containsKey(player.getName())){
                MainClass.wardens.get(player.getName()).setDealing(null);
            }
            return;
        }
        int id = window.getResponse().getClickedButtonId();
        switch (guiType){
            case WardenMain:
                switch (id){
                    case 0:
                        FormMain.showWardenReportTypeList(player);
                        break;
                    case 1:
                        FormMain.showWardenPunishType(player);
                        break;
                    case 2:
                        FormMain.showMailBoxMain(player);
                        break;
                    case 3:
                        FormMain.showReportTypeMenu(player);
                        break;
                    case 4:
                        FormMain.showUsefulTools(player);
                        break;
                    case 5:
                        FormMain.showWardenProfile(player);
                        break;
                }
                break;
            case WardenTools:
                switch (id){
                    case 0:
                        FormMain.showSelectPlayer(player, FormType.WardenTeleportTools);
                        break;
                    case 1:
                        player.getInventory().clearAll();
                        player.sendMessage("§a您的背包已清空！");
                        MainClass.log.log(Level.INFO, "操作员["+player.getName()+"]使用清空背包功能！");
                        break;
                    case 2:
                        switch (window.getResponse().getClickedButton().getText()){
                            case "切换至生存模式":
                                player.setGamemode(0);
                                MainClass.log.log(Level.INFO, "操作员["+player.getName()+"]切换至生存模式！");
                                break;
                            /* case "切换至创造模式":
                                player.setGamemode(1);
                                break;
                             */
                            case "切换至观察者模式":
                                player.setGamemode(3);
                                MainClass.log.log(Level.INFO, "操作员["+player.getName()+"切换至观察者模式");
                                break;
                        }
                        break;
                    case 3:
                        switch (window.getResponse().getClickedButton().getText()){
                            case "开启飞行":
                                setFlying(player, true);
                                MainClass.log.log(Level.INFO, "操作员["+player.getName()+"开启飞行！");
                                break;
                            case "关闭飞行":
                                setFlying(player, false);
                                MainClass.log.log(Level.INFO, "操作员["+player.getName()+"关闭飞行！");
                                break;
                        }
                        break;
                    case 4:
                        FormMain.showRecentProfile(player);
                        break;
                    case 5:
                        FormMain.showWardenMain(player);
                        break;
                }
                break;
            case WardenDealBugReportList:
                String text = window.getResponse().getClickedButton().getText();
                if(text.equals("返回") || text.equals("")){
                    FormMain.showWardenReportTypeList(player);
                    return;
                }
                BugReport select = MainClass.bugReports.get(id);
                if(MainClass.wardens.values().stream().anyMatch(wardenData -> wardenData.getDealing() == select)){
                    FormMain.showReportReturnMenu("该bug反馈已有人在处理！", player, FormType.DealBugReportReturn);
                    return;
                }
                MainClass.wardens.get(player.getName()).dealing = select;
                FormMain.showWardenBugReport(player, select);
                break;
            case WardenDealByPassReportList:
                text = window.getResponse().getClickedButton().getText();
                if(text.equals("返回") || text.equals("")){
                    FormMain.showWardenReportTypeList(player);
                    return;
                }
                ByPassReport select1 = MainClass.byPassReports.get(id);
                if(MainClass.wardens.values().stream().anyMatch(wardenData -> wardenData.getDealing() == select1)){
                    FormMain.showReportReturnMenu("该举报已有人在处理！", player, FormType.DealByPassReportReturn);
                    return;
                }
                MainClass.wardens.get(player.getName()).dealing = select1;
                FormMain.showWardenByPassReport(player, select1);
                break;
            case WardenDealReportMain:
                switch (id){
                    case 0:
                        FormMain.showWardenReportList(player, FormType.WardenDealBugReportList);
                        break;
                    case 1:
                        FormMain.showWardenReportList(player, FormType.WardenDealByPassReportList);
                        break;
                    case 2:
                        FormMain.showWardenMain(player);
                        break;
                }
                break;
            case WardenTeleportTools:
                String pn = window.getResponse().getClickedButton().getText();
                if(pn.equals("返回")){
                    FormMain.showWardenMain(player);
                    return;
                }
                Player p = Server.getInstance().getPlayer(pn);
                if(p != null){
                    Server.getInstance().dispatchCommand(new ConsoleCommandSender(), "tp "+player.getName()+" "+p.getName());
                    player.sendMessage("§a已传送到："+p.getName());
                    MainClass.log.log(Level.INFO, "操作员["+player.getName()+"]使用传送功能，传送到玩家"+p.getName()+"！");
                }else{
                    player.sendMessage("§c玩家不在线或不存在！");
                }
                break;
            case PlayerMain:
                switch (id){
                    case 0:
                        FormMain.showReportTypeMenu(player);
                        break;
                    case 1:
                        FormMain.showMailBoxMain(player);
                        break;
                    case 2:
                        FormMain.showRecentProfile(player);
                        break;
                }
                break;
            case PlayerMailboxMain:
                switch (window.getResponse().getClickedButton().getText()){
                    case "返回":
                        if(MainClass.wardens.containsKey(player.getName())) {
                            FormMain.showWardenMain(player);
                        }else{
                            FormMain.showPlayerMain(player);
                        }
                        return;
                    case "一键已读":
                        Config mailInfoConfig = new Config(MainClass.path+"/mailbox/"+player.getName()+".yml", Config.YAML);
                        List<Map<String, Object>> list = (List<Map<String, Object>>) mailInfoConfig.get("unclaimed");
                        List<Map<String, Object>> claimed = new ArrayList<>(mailInfoConfig.get("claimed", new ArrayList<>()));
                        int i = 0;
                        for(Map<String, Object> map : list) {
                            for (String message : new ArrayList<>((List<String>) map.getOrDefault("messages", new ArrayList<>()))) {
                                player.sendMessage(message.replace("{player}", player.getName()));
                            }

                            for (String command : new ArrayList<>((List<String>) map.getOrDefault("commands", new ArrayList<>()))) {
                                Server.getInstance().dispatchCommand(new ConsoleCommandSender(), command.replace("{player}", player.getName()));
                            }
                            claimed.add(list.get(i));
                            i++;
                        }
                        mailInfoConfig.set("unclaimed", new ArrayList<>());
                        mailInfoConfig.set("claimed", claimed);
                        mailInfoConfig.save();
                        player.sendMessage("§a您已一键已读且领取所有邮件！");
                        return;
                }
                FormMain.showMailDetail(player, id - 1);
                break;
            case PlayerMailboxInfo:
                switch (id){
                    case 0:
                        Config mailInfoConfig = new Config(MainClass.path+"/mailbox/"+player.getName()+".yml", Config.YAML);
                        List<Map<String, Object>> list = (List<Map<String, Object>>) mailInfoConfig.get("unclaimed");
                        int index = MainClass.mails.get(player);
                        Map<String, Object> map = (list.get(index));
                        for(String message: new ArrayList<>((List<String>) map.getOrDefault("messages", new ArrayList<>()))){
                            player.sendMessage(message.replace("{player}", player.getName()));
                        }

                        for(String command: new ArrayList<>((List<String>) map.getOrDefault("commands", new ArrayList<>()))){
                            Server.getInstance().dispatchCommand(new ConsoleCommandSender(), command.replace("{player}", player.getName()));
                        }

                        List<Map<String, Object>> newList = new ArrayList<>();
                        newList.add(list.get(index));

                        list.remove(index);
                        if(list.size() > 0) {
                            mailInfoConfig.set("unclaimed", list);
                        }else{
                            mailInfoConfig.remove("unclaimed");
                        }
                        mailInfoConfig.set("claimed", newList);
                        mailInfoConfig.save();
                        player.sendMessage("§a邮件已查看！");
                        break;
                    case 1:
                        FormMain.showMailBoxMain(player);
                        break;
                }
                break;
            case PlayerReportMain:
                switch (id){
                    case 0:
                        FormMain.showReportMenu(player, FormType.PlayerBugReport);
                        break;
                    case 1:
                        FormMain.showReportMenu(player, FormType.PlayerByPassReport);
                        break;
                }
                break;
            case RecentProfile:
                Server.getInstance().dispatchCommand(player, "r");
                break;
            case WardenPunishType:
                switch (id){
                    case 0:
                        FormMain.showWardenPunish(player);
                        break;
                    case 1:
                        FormMain.showWardenPardon(player);
                        break;
                    case 2:
                        FormMain.showCheckPlayerDetails(player);
                        break;
                    case 3:
                        FormMain.showWardenMain(player);
                        break;
                }
                break;
        }
    }

    private void formWindowCustomOnClick(Player player, FormWindowCustom window, FormType guiType) {
        FormResponseCustom response = window.getResponse();
        switch (guiType){
            case WardenDealBugReport:
                if(response == null){
                    FormMain.showWardenReportList(player, FormType.WardenDealBugReportList);
                    return;
                }
                BugReport bugReport = (BugReport) MainClass.wardens.get(player.getName()).dealing;
                File bugFile = new File(MainClass.path+"/bugreports/"+bugReport.getMillis()+".yml");
                String saveName = MainClass.getUltraPrecisionDate(System.currentTimeMillis());
                Config bugConfig = new Config(MainClass.path+"/bugreports/old/"+saveName+".yml", Config.YAML);
                bugConfig.set("info", bugReport.getInfo());
                bugConfig.set("player", bugReport.getPlayer());
                bugConfig.set("millis", MainClass.getDate(bugReport.getMillis()));
                bugConfig.set("end_millis", saveName);
                boolean acceptBugReport = response.getDropdownResponse(3).getElementID() == 0;
                bugConfig.set("view", acceptBugReport);
                bugConfig.set("comments", response.getInputResponse(4));
                if(acceptBugReport){
                    String reward = response.getDropdownResponse(5).getElementContent();
                    bugConfig.set("rewards", reward);
                    Config config = new Config(MainClass.path+"/mailbox/"+bugReport.getPlayer()+".yml", Config.YAML);
                    List<Map<String, Object>> list = config.get("unclaimed", new ArrayList<>());
                    Map<String, Object> map = new HashMap<>();
                    map.put("sender", "协管团队");
                    map.put("title", "感谢您向协管团队反馈bug！");
                    map.put("content", "非常感谢您帮助我们发现服务器中潜在的bug！");
                    map.put("millis", System.currentTimeMillis());
                    map.put("commands", MainClass.rewards.get(reward).getCommands());
                    map.put("messages", MainClass.rewards.get(reward).getMessages());
                    list.add(map);
                    config.set("unclaimed", list);
                    config.save();
                }else{
                    Config config = new Config(MainClass.path+"/mailbox/"+bugReport.getPlayer()+".yml", Config.YAML);
                    List<Map<String, Object>> list = config.get("unclaimed", new ArrayList<>());
                    Map<String, Object> map = new HashMap<>();
                    map.put("sender", "协管团队");
                    map.put("title", "您的反馈已被驳回！");
                    map.put("content", "内容:["+bugReport.getInfo()+"]，我们暂未查明对应的反馈，请您等待我们的回信！");
                    map.put("millis", System.currentTimeMillis());
                    map.put("commands", new ArrayList<>());
                    map.put("messages", new ArrayList<>());
                    list.add(map);
                    config.set("unclaimed", list);
                    config.save();
                }
                bugConfig.save();
                bugFile.delete();
                MainClass.bugReports.remove(bugReport);
                FormMain.showReportReturnMenu("处理成功", player, FormType.DealBugReportReturn);
                MainClass.wardens.get(player.getName()).addAccumulatedTimes();
                MainClass.log.log(Level.INFO, "操作员["+player.getName()+"]处理bug反馈完毕，具体信息详见：bugreports/"+saveName+".yml");
                break;
            case WardenDealByPassReport:
                if(response == null){
                    FormMain.showWardenReportList(player, FormType.WardenDealByPassReportList);
                    return;
                }
                ByPassReport byPassReport = (ByPassReport) MainClass.wardens.get(player.getName()).dealing;
                File bypassFile = new File(MainClass.path+"/bypassreports/"+byPassReport.getMillis()+".yml");
                String saveName1 = MainClass.getUltraPrecisionDate(System.currentTimeMillis());
                Config bypassConfig = new Config(MainClass.path+"/bypassreports/old/"+saveName1+".yml", Config.YAML);
                bypassConfig.set("info", byPassReport.getInfo());
                bypassConfig.set("cheater", byPassReport.getSuspect());
                bypassConfig.set("player", byPassReport.getPlayer());
                bypassConfig.set("millis", MainClass.getDate(byPassReport.getMillis()));
                bypassConfig.set("end_millis", saveName1);
                boolean acceptBypassReport = response.getDropdownResponse(4).getElementID() == 0;
                bypassConfig.set("view", acceptBypassReport);
                bypassConfig.set("comments", response.getInputResponse(5));
                if(acceptBypassReport){
                    String reward1 = response.getDropdownResponse(6).getElementContent();
                    bypassConfig.set("rewards", reward1);
                    Config config1 = new Config(MainClass.path+"/mailbox/"+byPassReport.getPlayer()+".yml", Config.YAML);
                    List<Map<String, Object>> list1 = config1.get("unclaimed", new ArrayList<>());
                    Map<String, Object> map1 = new HashMap<>();
                    map1.put("sender", "协管团队");
                    map1.put("title", "感谢您向协管团队举报违规玩家！");
                    map1.put("content", "非常感谢您帮助我们维护本服务器的环境！");
                    map1.put("millis", System.currentTimeMillis());
                    map1.put("commands", MainClass.rewards.get(reward1).getCommands());
                    map1.put("messages", MainClass.rewards.get(reward1).getMessages());
                    list1.add(map1);
                    config1.set("unclaimed", list1);
                    config1.save();
                    FormMain.showWardenPunish(player, byPassReport.getSuspect());
                }else{
                    Config config = new Config(MainClass.path+"/mailbox/"+byPassReport.getPlayer()+".yml", Config.YAML);
                    List<Map<String, Object>> list = config.get("unclaimed", new ArrayList<>());
                    Map<String, Object> map = new HashMap<>();
                    map.put("sender", "协管团队");
                    map.put("title", "您的举报已被驳回！");
                    map.put("content", "内容:["+byPassReport.getInfo()+"]，我们暂时未查明该玩家的作弊行为，请您等待我们的回信！");
                    map.put("millis", System.currentTimeMillis());
                    map.put("commands", new ArrayList<>());
                    map.put("messages", new ArrayList<>());
                    list.add(map);
                    config.set("unclaimed", list);
                    config.save();
                    FormMain.showReportReturnMenu("处理成功", player, FormType.DealByPassReportReturn);
                }
                bypassConfig.save();
                bypassFile.delete();
                MainClass.byPassReports.remove(byPassReport);
                MainClass.wardens.get(player.getName()).addAccumulatedTimes();
                MainClass.log.log(Level.INFO, "操作员["+player.getName()+"]处理bug反馈完毕，具体信息详见：bypassreports/"+saveName1+".yml");
                break;
            case WardenPersonalInfo:
                if(response == null){
                    return;
                }
                WardenData data = MainClass.wardens.get(player.getName());
                List<String> old = data.getPrefixes();
                if(old.size() > 1){
                    String selectPrefix = response.getDropdownResponse(4).getElementContent();
                    if(!old.get(0).equals(selectPrefix)){
                        List<String> newRank = new ArrayList<>();
                        newRank.add(selectPrefix);
                        for(String prefix: old){
                            if(!prefix.equals(selectPrefix)){
                                newRank.add(prefix);
                            }
                        }
                        Config personalConfig = data.config;
                        personalConfig.set("prefixes", newRank);
                        personalConfig.save();
                        data.setPrefixes(newRank);
                        player.sendMessage("§a已保存称号设置！");
                    }
                }
                break;
            case PlayerBugReport:
                if(response == null){
                    return;
                }
                String bugInfo = response.getInputResponse(0);
                if(!bugInfo.equals("")) {
                    Config s0 = new Config(MainClass.path + "/bugreports/" + System.currentTimeMillis() + ".yml", Config.YAML);
                    s0.set("player", player.getName());
                    s0.set("info", bugInfo);
                    boolean bugBoolean = response.getToggleResponse(1);
                    long millis = System.currentTimeMillis();
                    s0.set("anonymous", bugBoolean);
                    s0.set("millis", millis);
                    s0.save();
                    BugReport newBugReport = new BugReport(bugInfo, player.getName(), millis, bugBoolean);
                    MainClass.bugReports.add(newBugReport);
                    player.sendMessage("§a感谢您的反馈，我们正在全力核查中...");
                    MainClass.wardens.forEach((s, wardenData) -> {
                        Player p = Server.getInstance().getPlayer(s);
                        if(p != null){
                            p.sendMessage("§a您有新的bug反馈需处理！");
                        }
                    });
                    MainClass.log.log(Level.INFO, "["+player.getName()+"]提交bug反馈，具体内容："+ newBugReport);
                }else{
                    player.sendMessage("§c您填写的信息不完整，不予提交，请重试！");
                }
                break;
            case PlayerByPassReport:
                if(response == null){
                    return;
                }
                String bypassInfo = response.getInputResponse(2);
                String suspect;
                if(response.getResponse(0) != null && response.getInputResponse(0).equals("")){
                    suspect = response.getDropdownResponse(1).getElementContent();
                }else{
                    suspect = response.getInputResponse(0);
                }
                if(!bypassInfo.equals("") && !suspect.equals("") && !suspect.equals("- 未选择 -")) {
                    Config s1 = new Config(MainClass.path + "/bypassreports/" + System.currentTimeMillis() + ".yml", Config.YAML);
                    boolean bypassBoolean = response.getToggleResponse(3);
                    long millis1 = System.currentTimeMillis();
                    s1.set("player", player.getName());
                    s1.set("suspect", suspect);
                    s1.set("info", bypassInfo);
                    s1.set("anonymous", bypassBoolean);
                    s1.set("millis", millis1);
                    ByPassReport newBypassReport = new ByPassReport(bypassInfo, player.getName(), suspect, millis1, bypassBoolean);
                    MainClass.byPassReports.add(newBypassReport);
                    s1.save();
                    player.sendMessage("§a感谢您的举报，我们正在全力核查中...");
                    MainClass.wardens.forEach((s, wardenData) -> {
                        Player p = Server.getInstance().getPlayer(s);
                        if(p != null){
                            p.sendMessage("§a您有新的举报信息需处理！");
                        }
                    });
                    MainClass.log.log(Level.INFO, "["+player.getName()+"]提交举报信息，具体内容："+ newBypassReport);
                }else{
                    player.sendMessage("§c您填写的信息不完整，不予提交，请重试！");
                }
                break;
            case WardenPardon:
                if(response == null){
                    return;
                }
                String pardonedPn = response.getInputResponse(0);
                if(pardonedPn.equals("")){
                    player.sendMessage("§c您填写的信息不完整，不予提交，请重试！");
                    return;
                }else{
                    if(!Server.getInstance().lookupName(pardonedPn).isPresent()){
                        player.sendMessage("§c玩家"+pardonedPn+"不存在于服务器！");
                        return;
                    }
                }
                Config dealtConfigPardon;
                Player pardonedPlayer = Server.getInstance().getPlayer(pardonedPn);
                switch (response.getDropdownResponse(1).getElementID()) {
                    case 0:
                        dealtConfigPardon = new Config(MainClass.path + "/ban.yml", Config.YAML);
                        if(dealtConfigPardon.exists(pardonedPn)) {
                            dealtConfigPardon.remove(pardonedPn);
                            dealtConfigPardon.save();
                            MainClass.log.log(Level.INFO, "[" + player.getName() + "]成功解禁玩家[" + pardonedPn + "]，理由："+response.getInputResponse(2));
                        }else{
                            pardonedPlayer.sendMessage("§c该玩家未被封禁！");
                        }
                        break;
                    case 1:
                        dealtConfigPardon = new Config(MainClass.path + "/mute.yml", Config.YAML);
                        if(dealtConfigPardon.exists(pardonedPn)) {
                            dealtConfigPardon.remove(pardonedPn);
                            dealtConfigPardon.save();
                            MainClass.muted.remove(pardonedPn);
                            MainClass.log.log(Level.INFO, "[" + player.getName() + "]成功为玩家[" + pardonedPn + "]解除禁言，理由："+response.getInputResponse(2));
                            if(pardonedPlayer != null){
                                pardonedPlayer.sendMessage("§a您已被解除禁言！");
                            }
                        }else{
                            pardonedPlayer.sendMessage("§c该玩家未被禁言！");
                        }
                        break;
                }
                break;
            case WardenPunish:
                if(response == null){
                    return;
                }
                String punishedPn;
                if(response.getResponse(0) != null && response.getInputResponse(0).equals("")){
                    punishedPn = response.getDropdownResponse(1).getElementContent().replace("§6", "");
                }else{
                    punishedPn = response.getInputResponse(0);
                }
                if(punishedPn.equals("") || punishedPn.equals("- 未选择 -")){
                    player.sendMessage("§c您填写的信息不完整，不予提交，请重试！");
                    return;
                }else{
                    if(!Server.getInstance().lookupName(punishedPn).isPresent()){
                        player.sendMessage("§c玩家"+punishedPn+"不存在于服务器！");
                        return;
                    }
                }
                Config config;
                Player punished = Server.getInstance().getPlayer(punishedPn);
                switch (response.getDropdownResponse(2).getElementID()){
                    case 0:
                        config = new Config(MainClass.path + "/ban.yml", Config.YAML);
                        if(response.getToggleResponse(3)){
                            config.set(punishedPn+".start", System.currentTimeMillis());
                            config.set(punishedPn+".end", "permanent");
                        }else{
                            Calendar calendar = new Calendar.Builder().setInstant(System.currentTimeMillis()).build();
                            calendar.add(Calendar.YEAR, (int) response.getSliderResponse(4));
                            calendar.add(Calendar.MONTH, (int) response.getSliderResponse(5));
                            calendar.add(Calendar.DATE, (int) response.getSliderResponse(6));
                            calendar.add(Calendar.HOUR, (int) response.getSliderResponse(7));
                            calendar.add(Calendar.MINUTE, (int) response.getSliderResponse(8));
                            calendar.add(Calendar.SECOND, (int) response.getSliderResponse(9));
                            config.set(punishedPn+".start", System.currentTimeMillis());
                            config.set(punishedPn+".end", calendar.getTimeInMillis());
                        }
                        config.set(punishedPn+".operator", player.getName());
                        config.set(punishedPn+".reason", response.getInputResponse(10));
                        config.save();
                        MainClass.log.log(Level.INFO, "["+player.getName()+"]成功封禁玩家["+punishedPn+"]");
                        if(punished != null){
                            punished.kick("您已被封禁!");
                        }
                        break;
                    case 1:
                        config = new Config(MainClass.path + "/mute.yml", Config.YAML);
                        MainClass.log.log(Level.INFO, "["+player.getName()+"]成功禁言玩家["+punishedPn+"]");
                        if(punished != null){
                            if(response.getToggleResponse(3)){
                                config.set(punishedPn+".start", System.currentTimeMillis());
                                config.set(punishedPn+".end", "permanent");
                            }else{
                                Calendar calendar = new Calendar.Builder().setInstant(System.currentTimeMillis()).build();
                                calendar.add(Calendar.YEAR, (int) response.getSliderResponse(4));
                                calendar.add(Calendar.MONTH, (int) response.getSliderResponse(5));
                                calendar.add(Calendar.DATE, (int) response.getSliderResponse(6));
                                calendar.add(Calendar.HOUR, (int) response.getSliderResponse(7));
                                calendar.add(Calendar.MINUTE, (int) response.getSliderResponse(8));
                                calendar.add(Calendar.SECOND, (int) response.getSliderResponse(9));
                                config.set(punishedPn+".start", System.currentTimeMillis());
                                config.set(punishedPn+".end", calendar.getTimeInMillis());
                            }
                            config.set(punishedPn+".operator", player.getName());
                            config.set(punishedPn+".reason", response.getInputResponse(10));
                            config.save();
                            punished.sendMessage("您已被禁言!");
                            MainClass.muted.add(punishedPn);
                        }
                        break;
                    case 2:
                        if(punished != null){
                            punished.sendMessage("§c您已被警告，请规范您的游戏行为！");
                            player.sendMessage("§a警告已发送！");
                            MainClass.log.log(Level.INFO, "操作员["+player.getName()+"]使用警告功能，警告玩家"+punishedPn+"！");
                        }else{
                            player.sendMessage("§c该玩家不在线或不存在！");
                        }
                        break;
                    case 3:
                        if(punished != null){
                            punished.sendMessage("§c您已被踢出，请规范您的游戏行为！");
                            player.sendMessage("§a警告已发送！");
                            MainClass.log.log(Level.INFO, "操作员["+player.getName()+"]使用警告功能，警告玩家"+punishedPn+"！");
                        }else{
                            player.sendMessage("§c该玩家不在线或不存在！");
                        }
                        break;
                    case 4:
                        config = new Config(MainClass.path + "/suspects.yml", Config.YAML);
                        MainClass.log.log(Level.INFO, "["+player.getName()+"]成功添加嫌疑玩家["+punishedPn+"]");
                        if(punished != null){
                            Calendar calendar = new Calendar.Builder().setInstant(System.currentTimeMillis()).build();
                            calendar.add(Calendar.DATE, 7);
                            config.set(punishedPn+".start", System.currentTimeMillis());
                            config.set(punishedPn+".end", calendar.getTimeInMillis());
                            MainClass.suspectList.put(punishedPn, new SuspectData(punishedPn, System.currentTimeMillis(), calendar.getTimeInMillis()));
                            config.set(punishedPn+".operator", player.getName());
                            config.set(punishedPn+".reason", response.getInputResponse(10));
                            config.save();
                            punished.sendMessage("您已被列入嫌疑玩家，请端正您的游戏行为。");
                        }
                        break;
                }
                player.sendMessage("§a处罚成功！");
                break;
            case PlayerStatus:
                if(response == null){
                    return;
                }
                StringBuilder builder = new StringBuilder("");
                String name = response.getInputResponse(0);
                if(!name.equals("") && Server.getInstance().lookupName(name).isPresent()){
                    long bannedRemained = MainClass.getRemainedBannedTime(name);
                    if(bannedRemained <= 0L){
                        if(bannedRemained == -1){
                            builder.append("封禁状态：§e永久封禁");
                        }else {
                            builder.append("封禁状态：§a未被封禁");
                        }
                    }else{
                        builder.append("封禁状态：§e封禁中【解禁时间：").append(MainClass.getDate(bannedRemained)).append("】");
                    }
                    builder.append("\n").append("§f");
                    long muteRemained = MainClass.getRemainedMutedTime(name);
                    if(muteRemained <= 0L){
                        if(muteRemained == -1){
                            builder.append("封禁状态：§e永久封禁");
                        }else {
                            builder.append("封禁状态：§a未被封禁");
                        }
                    }else{
                        builder.append("封禁状态：§e封禁中【解禁时间：").append(MainClass.getDate(muteRemained)).append("】");
                    }
                }else{
                    FormMain.showReportReturnMenu("该玩家不存在！", player, FormType.PlayerStatusReturn);
                }
                FormMain.showReportReturnMenu(builder.toString(), player, FormType.PlayerStatusReturn);
                break;
        }
    }

    private void formWindowModalOnClick(Player player, FormWindowModal window, FormType guiType) {
        if(window.getResponse() == null){ return; }
        switch (guiType){
            case DealBugReportReturn:
                if(window.getResponse().getClickedButtonId() == 0){
                    FormMain.showWardenReportList(player, FormType.WardenDealBugReportList);
                }
                break;
            case DealByPassReportReturn:
                if(window.getResponse().getClickedButtonId() == 0){
                    FormMain.showWardenReportList(player, FormType.WardenDealByPassReportList);
                }
                break;
            case PlayerStatusReturn:
                if(window.getResponse().getClickedButtonId() == 0){
                    FormMain.showWardenMain(player);
                }
                break;
        }
    }

    public void setFlying(Player player, boolean bool){
        AdventureSettings settings = player.getAdventureSettings();
        settings.set(AdventureSettings.Type.ALLOW_FLIGHT, bool);
        if(!bool) {
            settings.set(AdventureSettings.Type.FLYING, false);
        }
        player.setAdventureSettings(settings);
        player.getAdventureSettings().update();
    }
}
