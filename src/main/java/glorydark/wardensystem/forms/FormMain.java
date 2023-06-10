package glorydark.wardensystem.forms;

import cn.nukkit.AdventureSettings;
import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.form.element.*;
import cn.nukkit.form.window.FormWindowCustom;
import cn.nukkit.form.window.FormWindowModal;
import cn.nukkit.form.window.FormWindowSimple;
import cn.nukkit.utils.Config;
import glorydark.wardensystem.MainClass;
import glorydark.wardensystem.OfflineData;
import glorydark.wardensystem.PlayerData;
import glorydark.wardensystem.reports.WardenData;
import glorydark.wardensystem.reports.matters.BugReport;
import glorydark.wardensystem.reports.matters.ByPassReport;
import glorydark.wardensystem.reports.matters.Report;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class FormMain {

    public static void showWardenMain(Player player){
        FormWindowSimple window = new FormWindowSimple("协管系统","您好，【协管员】"+player.getName()+"！");
        window.addButton(new ElementButton("处理事务"));
        window.addButton(new ElementButton("处罚系统"));
        window.addButton(new ElementButton("解除处罚"));
        File file = new File(MainClass.path+"/mailbox/"+player.getName()+".yml");
        if(file.exists()) {
            Config config = new Config(file, Config.YAML);
            List<Map<String, Object>> list = config.get("unclaimed", new ArrayList<>());
            if (list.size() > 0) {
                window.addButton(new ElementButton("邮箱系统 [§c§l"+list.size()+"§r]"));
            }else{
                window.addButton(new ElementButton("邮箱系统 [§a§l0§r]"));
            }
        }else{
            window.addButton(new ElementButton("邮箱系统 [§a§l0§r]"));
        }
        window.addButton(new ElementButton("个人中心"));
        window.addButton(new ElementButton("实用工具"));
        window.addButton(new ElementButton("举报/反馈bug"));
        window.addButton(new ElementButton("查询最近数据"));
        window.addButton(new ElementButton("查询玩家状态"));
        Listener.showFormWindow(player, window, FormType.WardenMain);
    }

    public static void showWardenPunish(Player player, String... params){
        FormWindowCustom window = new FormWindowCustom("协管系统 - 处罚系统");
        ElementInput input = new ElementInput("玩家名");
        if(params.length > 0){
            input.setDefaultText(params[0]);
        }
        window.addElement(input);
        List<String> onlinePlayers = new ArrayList<>();
        for(Player p:Server.getInstance().getOnlinePlayers().values()){
            if(p == player){
                continue;
            }
            onlinePlayers.add(p.getName());
        }
        ElementDropdown dropdown = new ElementDropdown("选择在线玩家");
        dropdown.addOption("- 未选择 -");
        dropdown.getOptions().addAll(onlinePlayers);
        window.addElement(dropdown);
        List<String> types = new ArrayList<>();
        types.add("封禁");
        types.add("禁言");
        types.add("警告");
        types.add("踢出");
        window.addElement(new ElementDropdown("处罚类型", types));
        window.addElement(new ElementToggle("是否永久"));
        window.addElement(new ElementSlider("年", 0, 30, 1));
        window.addElement(new ElementSlider("月", 0, 12, 1, 1));
        window.addElement(new ElementSlider("天", 0, 30, 1));
        window.addElement(new ElementSlider("时", 0, 24, 1));
        window.addElement(new ElementSlider("分", 0, 60, 1));
        window.addElement(new ElementSlider("秒", 0, 60, 1));
        window.addElement(new ElementInput("理由"));
        Listener.showFormWindow(player, window, FormType.WardenPunish);
    }

    public static void showWardenPardon(Player player){
        FormWindowCustom window = new FormWindowCustom("协管系统 - 解封系统");
        window.addElement(new ElementInput("玩家名"));
        List<String> types = new ArrayList<>();
        types.add("解封");
        types.add("解禁言");
        window.addElement(new ElementDropdown("处理类型", types));
        window.addElement(new ElementInput("理由"));
        Listener.showFormWindow(player, window, FormType.WardenPardon);
    }

    public static void showUsefulTools(Player player){
        FormWindowSimple window = new FormWindowSimple("协管系统 - 实用工具","请选择您需要的功能！");
        window.addButton(new ElementButton("传送到玩家"));
        window.addButton(new ElementButton("清空背包"));
        switch (player.getGamemode()){
            case 0:
            case 1:
            case 2:
                window.addButton(new ElementButton("切换至观察者模式"));
                break;
            case 3:
                window.addButton(new ElementButton("切换至生存模式"));
                break;
        }
        window.addButton(new ElementButton(player.getAdventureSettings().get(AdventureSettings.Type.FLYING)? "关闭飞行":"开启飞行"));
        window.addButton(new ElementButton("返回"));
        Listener.showFormWindow(player, window, FormType.WardenTools);
    }

    public static void showWardenReportTypeList(Player player){
        FormWindowSimple window = new FormWindowSimple("协管系统 - 处理事务类型","您好，请选择您需要处理的事务类型！");
        window.addButton(new ElementButton("Bug反馈 " +(MainClass.bugReports.size()>0 ? "§c§l["+MainClass.bugReports.size()+"§r]": "[§a§l0§r]")));
        window.addButton(new ElementButton("举报 "+(MainClass.byPassReports.size()>0 ? "§c§l["+MainClass.byPassReports.size()+"§r]": "[§a§l0§r]")));
        window.addButton(new ElementButton("返回"));
        Listener.showFormWindow(player, window, FormType.WardenDealReportMain);
    }

    public static void showWardenBugReport(Player player, BugReport report){
        FormWindowCustom window = new FormWindowCustom("协管系统 - 处理BUG反馈");
        window.addElement(new ElementLabel(report.anonymous? "* 反馈玩家要求匿名，故不公布玩家昵称！" : "反馈玩家："+report.player));
        window.addElement(new ElementLabel("事务信息："+report.info));
        window.addElement(new ElementLabel("反馈时间："+ MainClass.getDate(report.millis)));
        List<String> options = new ArrayList<>();
        options.add("§a已核实");
        options.add("§c已驳回");
        window.addElement(new ElementDropdown("处理结果", options));
        window.addElement(new ElementInput("处理结果简述\n（此项会发送给玩家，留空则显示无）："));
        window.addElement(new ElementDropdown("奖励措施", new ArrayList<>(MainClass.rewards.keySet())));
        Listener.showFormWindow(player, window, FormType.WardenDealBugReport);
    }

    public static void showWardenByPassReport(Player player, ByPassReport report){
        FormWindowCustom window = new FormWindowCustom("协管系统 - 处理举报");
        window.addElement(new ElementLabel(report.anonymous? "* 反馈玩家要求匿名，故不公布玩家昵称！" : "反馈玩家："+report.player));
        window.addElement(new ElementLabel("事务信息："+report.info));
        window.addElement(new ElementLabel("被举报者："+report.suspect));
        window.addElement(new ElementLabel("反馈时间："+ MainClass.getDate(report.millis)));
        List<String> options = new ArrayList<>();
        options.add("§a已核实");
        options.add("§c已驳回");
        window.addElement(new ElementDropdown("处理结果", options));
        window.addElement(new ElementInput("处理结果简述\n（此项会发送给玩家，留空则显示无）："));
        window.addElement(new ElementDropdown("奖励措施", new ArrayList<>(MainClass.rewards.keySet())));
        Listener.showFormWindow(player, window, FormType.WardenDealByPassReport);
    }

    public static void showWardenReportList(Player player, FormType formType){
        FormWindowSimple window = new FormWindowSimple("协管系统 - 选择反馈","请选择您要处理的反馈！");
        switch (formType){
            case WardenDealBugReportList:
                if(MainClass.bugReports.size() > 0){
                    for(Report report: MainClass.bugReports){
                        window.addButton(new ElementButton((report.isAnonymous()? "【匿名反馈】": "【反馈者："+report.getPlayer()+"】")+"\n"+"提交时间:"+MainClass.getDate(report.getMillis())));
                    }
                }else{
                    window.setContent("暂无需要处理的bug反馈！");
                    window.addButton(new ElementButton("返回"));
                }
                Listener.showFormWindow(player, window, FormType.WardenDealBugReportList);
                break;
            case WardenDealByPassReportList:
                if(MainClass.byPassReports.size() > 0) {
                    for (Report report : MainClass.byPassReports) {
                        window.addButton(new ElementButton((report.isAnonymous() ? "【匿名举报】" : "【举报者：" + report.getPlayer() + "】") + "\n" + "提交时间:" + MainClass.getDate(report.getMillis())));
                    }
                }else{
                    window.setContent("暂无需要处理的举报！");
                    window.addButton(new ElementButton("返回"));
                }
                Listener.showFormWindow(player, window, FormType.WardenDealByPassReportList);
                break;
        }
    }

    public static void showSelectPlayer(Player player, FormType type){
        Collection<Player> players = Server.getInstance().getOnlinePlayers().values();
        FormWindowSimple window;
        if(players.size() > 0){
            window = new FormWindowSimple("协管系统 - 传送工具","请选择您要传送到的玩家！");
            for(Player p: players){
                window.addButton(new ElementButton(p.getName()));
            }
        }else{
            window = new FormWindowSimple("协管系统 - 传送工具","目前没有玩家在线！");
            window.addButton(new ElementButton("返回"));
        }
        Listener.showFormWindow(player, window, type);
    }

    public static void showWardenProfile(Player player){
        FormWindowCustom window = new FormWindowCustom("协管系统 - 个人信息");
        WardenData data = MainClass.wardens.get(player.getName());
        window.addElement(new ElementLabel("玩家名："+(data.getPrefixes().size()>0? "【" + data.getPrefixes().get(0)+"§f】": "")+player.getName()));
        DecimalFormat format = new DecimalFormat("#.##");
        //to do: 评分正确率目前得从后台更改
        window.addElement(new ElementLabel("玩家评分："+(data.getGradePlayerCounts() > 0? format.format(new BigDecimal(data.allGradesFromPlayers).divide(new BigDecimal(data.gradePlayerCounts), 2, RoundingMode.HALF_UP)) +" / 5.0": 5.0 +" / 5.0")));
        window.addElement(new ElementLabel("正确率："+(data.getAccumulatedTimes() > 0? (format.format(new BigDecimal("1.0").subtract(new BigDecimal(data.vetoedTimes).divide(new BigDecimal(data.accumulatedTimes), 4, RoundingMode.HALF_UP)).multiply(new BigDecimal(100))) + "%%"): "100%%")));

        window.addElement(new ElementLabel("入职时间："+ data.getJoinTime()));
        window.addElement(new ElementDropdown("更改称号显示", data.getPrefixes()));
        Listener.showFormWindow(player, window, FormType.WardenPersonalInfo);
    }

    public static void showPlayerMain(Player player){
        FormWindowSimple window = new FormWindowSimple("协管系统","您好，【玩家】"+player.getName()+"！");
        window.addButton(new ElementButton("举报/反馈bug"));

        // It is not advisable to use this method into the giant. The frequent read of config will lower the performance if there are so many players.
        File file = new File(MainClass.path+"/mailbox/"+player.getName()+".yml");
        if(file.exists()) {
            Config config = new Config(file, Config.YAML);
            if(config.exists("unclaimed")) {
                List<Map<String, Object>> list = (List<Map<String, Object>>) config.get("unclaimed");
                if (list.size() > 0) {
                    window.addButton(new ElementButton("邮箱系统 [§c§l" + list.size() + "§r]"));
                } else {
                    window.addButton(new ElementButton("邮箱系统 [§a§l0§r]"));
                }
            }else{
                window.addButton(new ElementButton("邮箱系统 [§a§l0§r]"));
            }
        }else{
            window.addButton(new ElementButton("邮箱系统 [§a§l0§r]"));
        }
        window.addButton(new ElementButton("查询最近数据"));
        Listener.showFormWindow(player, window, FormType.PlayerMain);
    }

    public static void showMailBoxMain(Player player){
        File file = new File(MainClass.path+"/mailbox/"+player.getName()+".yml");
        if(file.exists()){
            Config config = new Config(file, Config.YAML);
            List<Map<String, Object>> list = config.get("unclaimed", new ArrayList<>());
            FormWindowSimple window;
            if(list.size() > 0){
                window = new FormWindowSimple("协管系统", "请选择您要查收的邮件！");
                for(Map<String, Object> map: list){
                    window.addButton(new ElementButton("标题：" + map.getOrDefault("title", "无标题")+"\n发信人："+map.getOrDefault("sender", "undefined")));
                }
            }else{
                window = new FormWindowSimple("协管系统", "暂无邮件！");
                window.addButton(new ElementButton("返回"));
            }
            Listener.showFormWindow(player, window, FormType.PlayerMailboxMain);
        }else{
            FormWindowSimple window = new FormWindowSimple("协管系统","暂无邮件！");
            window.addButton(new ElementButton("返回"));
            Listener.showFormWindow(player, window, FormType.PlayerMailboxMain);
        }
    }

    public static void showMailDetail(Player player, Integer index){
        Config config = new Config(MainClass.path+"/mailbox/"+player.getName()+".yml", Config.YAML);
        MainClass.mails.put(player, index);
        Map<String, Object> map = (Map<String, Object>) (config.get("unclaimed", new ArrayList<>())).get(index);
        String string = "邮件名：" + map.getOrDefault("title", "无标题") + "\n发信人：" + map.getOrDefault("sender", "Undefined") + "\n发信时间：" + MainClass.getDate((Long) map.getOrDefault("millis", 0L)) + "\n内容：" + ((String) map.getOrDefault("content", "undefined")).replace("\\n", "\n");
        FormWindowSimple window = new FormWindowSimple("协管系统",string);
        window.addButton(new ElementButton("已读/领取物品"));
        window.addButton(new ElementButton("返回"));
        Listener.showFormWindow(player, window, FormType.PlayerMailboxInfo);
    }

    public static void showReportTypeMenu(Player player){
        FormWindowSimple window = new FormWindowSimple("协管系统","您好，【玩家】"+player.getName()+"！");
        window.addButton(new ElementButton("bug反馈"));
        window.addButton(new ElementButton("举报"));
        Listener.showFormWindow(player, window, FormType.PlayerReportMain);
    }

    public static void showReportReturnMenu(String content, Player player, FormType type){
        FormWindowModal modal = new FormWindowModal("协管系统", content, "返回", "退出");
        Listener.showFormWindow(player, modal, type);
    }

    public static void showReportMenu(Player player, FormType type){
        switch (type){
            case PlayerBugReport:
                FormWindowCustom window = new FormWindowCustom("协管系统 - bug反馈");
                window.addElement(new ElementInput("反馈内容"));
                window.addElement(new ElementToggle("是否匿名"));
                Listener.showFormWindow(player, window, FormType.PlayerBugReport);
                break;
            case PlayerByPassReport:
                FormWindowCustom window1 = new FormWindowCustom("协管系统 - 举报");
                window1.addElement(new ElementInput("举报玩家名"));
                List<String> strings = new ArrayList<>();
                strings.add("- 未选择 -");
                Server.getInstance().getOnlinePlayers().values().forEach(player1 -> strings.add(player1.getName()));
                ElementDropdown dropdown = new ElementDropdown("选择在线玩家");
                dropdown.getOptions().addAll(strings);
                window1.addElement(dropdown);
                window1.addElement(new ElementInput("简述举报内容"));
                window1.addElement(new ElementToggle("是否匿名"));
                Listener.showFormWindow(player, window1, FormType.PlayerByPassReport);
                break;
        }
    }

    public static void showRecentProfile(Player player){
        FormWindowSimple simple = new FormWindowSimple("协管系统 - 最近数据查询", "");
        StringBuilder builder = new StringBuilder("");
        builder.append("§f最近攻击你的玩家:").append("\n");
        PlayerData data = MainClass.playerData.getOrDefault(player, new PlayerData(player));
        if(data.getSourceList().size() > 0){
            for(PlayerData.DamageSource damageSource: data.getSourceList()){
                if(damageSource.getDamager().equals(player.getName())){
                    continue;
                }
                builder.append("§f----------").append("\n")
                        .append("§f* ").append(damageSource.getDamager()).append(": \n")
                        .append("§f上次攻击: ").append((System.currentTimeMillis() - damageSource.getMillis()) / 1000).append("秒前").append("\n")
                        .append("§f累计连续攻击次数： ").append(damageSource.getDamageTime()).append("\n")
                        .append("§f----------").append("\n");
            }
        }else{
            builder.append("§a- 暂无玩家攻击过您！ -").append("\n");
        }

        builder.append("\n").append("§f在你附近的玩家:").append("\n");
        if(data.getSurroundedPlayer().size() > 0){
            for(Player p : data.getSurroundedPlayer()){
                if(p == player){
                    continue;
                }
                builder.append("§f----------").append("\n")
                        .append("§f* ").append(p.getName()).append(": \n")
                        .append("§f展示名: ").append(p.getDisplayName()).append("\n")
                        .append("§f是否正在飞行: ").append(p.getAdventureSettings().get(AdventureSettings.Type.FLYING)).append("\n")
                        .append("§f----------").append("\n");
            }
        }else{
            builder.append("§a- 暂无玩家在您附近！ -").append("\n");
        }

        builder.append("\n").append("§f最近下线的玩家:").append("\n");
        if(MainClass.offlineData.size() > 0){
            for(OfflineData offlineData : MainClass.offlineData){
                builder.append("§f----------").append("\n")
                        .append("* ").append(offlineData.getName()).append(": \n")
                        .append("§f展示名: ").append(offlineData.getDisplayName()).append("\n")
                        .append("§f上次在线: ").append((System.currentTimeMillis() - offlineData.getMillis()) / 1000).append("秒前\n")
                        .append("§f----------").append("\n");
            }
        }else{
            builder.append("§a- 暂无玩家最近下线过！ -");
        }
        simple.setContent(builder.toString());
        simple.addButton(new ElementButton("返回"));
        Listener.showFormWindow(player, simple, FormType.RecentProfile);
    }

    public static void showCheckPlayerDetails(Player player){
        FormWindowCustom custom = new FormWindowCustom("协管系统 - 查询玩家状态");
        custom.addElement(new ElementInput("玩家名"));
        Listener.showFormWindow(player, custom, FormType.PlayerStatus);
    }

}