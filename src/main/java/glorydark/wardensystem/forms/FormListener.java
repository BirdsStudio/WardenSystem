package glorydark.wardensystem.forms;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.command.ConsoleCommandSender;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.*;
import cn.nukkit.form.response.FormResponseCustom;
import cn.nukkit.form.window.FormWindow;
import cn.nukkit.form.window.FormWindowCustom;
import cn.nukkit.form.window.FormWindowModal;
import cn.nukkit.form.window.FormWindowSimple;
import cn.nukkit.utils.Config;
import glorydark.wardensystem.MainClass;
import glorydark.wardensystem.reports.WardenData;
import glorydark.wardensystem.reports.matters.BugReport;
import glorydark.wardensystem.reports.matters.ByPassReport;

import java.io.File;
import java.util.*;
import java.util.logging.Level;

public class FormListener implements Listener {
    public static final HashMap<Player, HashMap<Integer, FormType>> UI_CACHE = new HashMap<>();
    
    public static void showFormWindow(Player player, FormWindow window, FormType guiType) {
        UI_CACHE.computeIfAbsent(player, i -> new HashMap<>()).put(player.showFormWindow(window), guiType);
    }
    
    @EventHandler
    public void Join(PlayerJoinEvent event){
        Player player = event.getPlayer();
        long bannedRemained = MainClass.getRemainedBannedTime(player);
        if(bannedRemained != 0){
            player.kick("§c您已被封禁\n§e解封时间："+MainClass.getUnBannedDate(player));
            return;
        }
        long mutedRemained = MainClass.getRemainedMutedTime(player);
        if(mutedRemained != 0){
            player.sendMessage("§c您已被禁言\n§e解封时间："+MainClass.getUnMutedDate(player));
            return;
        }
        if(MainClass.wardens.containsKey(player.getName())){
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
        if(window.getResponse() == null){ return; }
        int id = window.getResponse().getClickedButtonId();
        switch (guiType){
            case WardenMain:
                switch (id){
                    case 0:
                        FormMain.showWardenReportTypeList(player);
                        break;
                    case 1:
                        FormMain.showWardenPunish(player);
                        break;
                    case 2:
                        FormMain.showWardenPardon(player);
                        break;
                    case 3:
                        FormMain.showMailBoxMain(player);
                        break;
                    case 4:
                        FormMain.showWardenProfile(player);
                        break;
                    case 5:
                        FormMain.showUsefulTools(player);
                        break;
                    case 6:
                        FormMain.showReportTypeMenu(player);
                        break;
                }
                break;
            case WardenTools:
                switch (id){
                    case 1:
                        FormMain.showSelectPlayer(player, FormType.WardenTeleportTools);
                        break;
                    case 2:
                        player.getInventory().clearAll();
                        player.sendMessage("§a您的背包已清空！");
                        MainClass.log.log(Level.INFO, "操作员["+player.getName()+"]使用清空背包功能！");
                        break;
                    case 3:
                    case 4:
                        switch (window.getResponse().getClickedButton().getText()){
                            case "切换至生存模式":
                                player.setGamemode(0);
                                break;
                            case "切换至创造模式":
                                player.setGamemode(1);
                                break;
                            case "切换至观察者模式":
                                player.setGamemode(3);
                                break;
                        }
                        break;
                }
                break;
            case WardenDealBugReportList:
                BugReport select = MainClass.bugReports.get(id);
                MainClass.wardens.get(player.getName()).dealing = select;
                FormMain.showWardenBugReport(player, select);
                break;
            case WardenDealByPassReport:
                ByPassReport select1 = MainClass.byPassReports.get(id);
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
                }
                break;
            case PlayerMailboxMain:
                FormMain.showMailDetail(player, id);
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
        }
    }

    private void formWindowCustomOnClick(Player player, FormWindowCustom window, FormType guiType) {
        if(window.getResponse() == null){ return; }
        FormResponseCustom response = window.getResponse();
        switch (guiType){
            case WardenDealBugReport:
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
                    map.put("content", "内容:["+bugReport.getInfo()+"]，请勿恶意或乱反馈！");
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
                player.sendMessage("§a处理完成，已经将您的处理结果保存至后台！");
                MainClass.log.log(Level.INFO, "操作员["+player.getName()+"]处理bug反馈完毕，具体信息详见：bugreports/"+saveName+".yml");
                break;
            case WardenDealByPassReport:
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
                    map1.put("title", "感谢您向协管团队反馈bug！");
                    map1.put("content", "非常感谢您帮助我们发现服务器中潜在的bug！");
                    map1.put("millis", System.currentTimeMillis());
                    map1.put("commands", MainClass.rewards.get(reward1).getCommands());
                    map1.put("messages", MainClass.rewards.get(reward1).getMessages());
                    list1.add(map1);
                    config1.set("unclaimed", list1);
                    config1.save();
                }else{
                    Config config = new Config(MainClass.path+"/mailbox/"+byPassReport.getPlayer()+".yml", Config.YAML);
                    List<Map<String, Object>> list = config.get("unclaimed", new ArrayList<>());
                    Map<String, Object> map = new HashMap<>();
                    map.put("sender", "协管团队");
                    map.put("title", "您的举报已被驳回！");
                    map.put("content", "内容:["+byPassReport.getInfo()+"]，请勿恶意或乱举报他人！");
                    map.put("millis", System.currentTimeMillis());
                    map.put("commands", new ArrayList<>());
                    map.put("messages", new ArrayList<>());
                    list.add(map);
                    config.set("unclaimed", list);
                    config.save();
                }
                bypassConfig.save();
                bypassFile.delete();
                MainClass.byPassReports.remove(byPassReport);
                player.sendMessage("§a处理完成，已经将您的处理结果保存至后台！");
                MainClass.log.log(Level.INFO, "操作员["+player.getName()+"]处理bug反馈完毕，具体信息详见：bypassreports/"+saveName1+".yml");
                break;
            case WardenPersonalInfo:
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
                    MainClass.log.log(Level.INFO, "["+player.getName()+"]提交bug反馈，具体内容："+ newBugReport);
                }else{
                    player.sendMessage("§c您填写的信息不完整，不予提交，请重试！");
                }
                break;
            case PlayerByPassReport:
                String bypassInfo = response.getInputResponse(1);
                String suspect = response.getInputResponse(0);
                if(!bypassInfo.equals("") && !suspect.equals("")) {
                    Config s1 = new Config(MainClass.path + "/bugreports/" + System.currentTimeMillis() + ".yml", Config.YAML);
                    boolean bypassBoolean = response.getToggleResponse(2);
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
                    MainClass.log.log(Level.INFO, "["+player.getName()+"]提交bug反馈，具体内容："+ newBypassReport);
                }else{
                    player.sendMessage("§c您填写的信息不完整，不予提交，请重试！");
                }
                break;
            case WardenPardon:
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
                String punishedPn;
                if(response.getResponse(0) != null && response.getInputResponse(0).equals("")){
                    punishedPn = response.getDropdownResponse(1).getElementContent();
                }else{
                    punishedPn = response.getInputResponse(0);
                }
                if(punishedPn.equals("")){
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
                }
                player.sendMessage("§a处罚成功！");
                break;
        }
    }

    private void formWindowModalOnClick(Player player, FormWindowModal window, FormType guiType) {
        if(window.getResponse() == null){ return; }
        switch (guiType){
        }
    }
}
