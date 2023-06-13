package glorydark.wardensystem.data;

import cn.nukkit.utils.Config;
import glorydark.wardensystem.MainClass;
import glorydark.wardensystem.reports.matters.Report;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class WardenData {

    private Report dealing; //处理事务类型

    private int dealBugReportTimes;

    private int dealBypassReportTimes;

    private int accumulatedDealBugReportTimes;

    private int accumulatedDealBypassReportTimes;

    private int banTimes;

    private int muteTimes;

    private int warnTimes;

    private int suspectTimes;

    private int kickTimes;

    private int accumulatedBanTimes;

    private int accumulatedMuteTimes;

    private int accumulatedWarnTimes;

    private int accumulatedSuspectTimes;

    private int accumulatedKickTimes;

    private int gamemodeBefore;

    private int vetoedTimes;

    private int allGradesFromPlayers;

    private int gradePlayerCounts;

    private List<String> prefixes;

    private Config config;

    private String joinTime;

    private WardenLevelType levelType;

    public WardenData(Report dealing, Config config){
        this.gamemodeBefore = 0;
        this.dealing = dealing;
        this.config = config;
        this.prefixes = new ArrayList<>(config.getStringList("prefixes"));
        this.dealBugReportTimes = config.getInt("deal_bug_report_times", 0);
        this.dealBypassReportTimes = config.getInt("deal_bypass_report_times", 0);
        this.accumulatedDealBugReportTimes = config.getInt("accumulated_deal_bug_report_times", dealBugReportTimes);
        this.accumulatedDealBypassReportTimes = config.getInt("accumulated_deal_bypass_report_times", dealBypassReportTimes);
        this.banTimes = config.getInt("ban_times", 0);
        this.muteTimes = config.getInt("mute_times", 0);
        this.warnTimes = config.getInt("warn_times", 0);
        this.suspectTimes = config.getInt("suspect_times", 0);
        this.kickTimes = config.getInt("kick_times", 0);
        this.accumulatedBanTimes = config.getInt("accumulated_ban_times", 0);
        this.accumulatedMuteTimes = config.getInt("accumulated_mute_times", 0);
        this.accumulatedWarnTimes = config.getInt("accumulated_warn_times", 0);
        this.accumulatedSuspectTimes = config.getInt("accumulated_suspect_times", 0);
        this.accumulatedKickTimes = config.getInt("accumulated_kick_times", 0);
        this.vetoedTimes = config.getInt("vetoed_times", 0);
        this.allGradesFromPlayers = config.getInt("all_grades_from_players", 5);
        this.gradePlayerCounts = config.getInt("grade_player_counts", 0);
        this.joinTime = MainClass.getDate(config.getLong("join_time"));
        if(this.dealBypassReportTimes > this.accumulatedDealBypassReportTimes){
            this.accumulatedDealBypassReportTimes = this.dealBypassReportTimes;
        }
        if(this.dealBugReportTimes > this.accumulatedDealBugReportTimes){
            this.accumulatedDealBugReportTimes = this.dealBugReportTimes;
        }
        this.fixConfig();
        this.save();
    }

    protected void fixConfig(){
        if(!config.exists("prefixes")){
            config.set("prefixes", new ArrayList<>());
        }
        if(!config.exists("deal_bug_report_times")){
            config.set("deal_bug_report_times", 0);
        }
        if(!config.exists("deal_bypass_report_times")){
            config.set("deal_bypass_report_times", 0);
        }
        if(!config.exists("accumulated_deal_bug_report_times")){
            config.set("accumulated_deal_bug_report_times", 0);
        }
        if(!config.exists("accumulated_deal_bypass_report_times")){
            config.set("accumulated_deal_bypass_report_times", 0);
        }
        if(!config.exists("ban_times")){
            config.set("ban_times", 0);
        }
        if(!config.exists("mute_times")){
            config.set("mute_times", 0);
        }
        if(!config.exists("warn_times")){
            config.set("warn_times", 0);
        }
        if(!config.exists("suspect_times")){
            config.set("suspect_times", 0);
        }
        if(!config.exists("kick_times")){
            config.set("kick_times", 0);
        }
        if(!config.exists("accumulated_ban_times")){
            config.set("accumulated_ban_times", 0);
        }
        if(!config.exists("accumulated_mute_times")){
            config.set("accumulated_mute_times", 0);
        }
        if(!config.exists("accumulated_warn_times")){
            config.set("accumulated_warn_times", 0);
        }
        if(!config.exists("accumulated_suspect_times")){
            config.set("accumulated_suspect_times", 0);
        }
        if(!config.exists("accumulated_kick_times")){
            config.set("accumulated_kick_times", 0);
        }
        if(!config.exists("vetoed_times")){
            config.set("vetoed_times", 0);
        }
        if(!config.exists("all_grades_from_players")){
            config.set("all_grades_from_players", 5);
        }
        if(!config.exists("grade_player_counts")){
            config.set("grade_player_counts", 0);
        }
        if(!config.exists("join_time")){
            config.set("join_time", System.currentTimeMillis());
        }
        config.save();
    }

    public void addBanTime(){
        banTimes++;
        accumulatedBanTimes++;
        config.set("ban_times", banTimes);
        config.set("accumulated_ban_times", accumulatedBanTimes);
        config.save();
    }

    public void addMuteTime(){
        muteTimes++;
        accumulatedMuteTimes++;
        config.set("mute_times", muteTimes);
        config.set("accumulated_mute_times", accumulatedMuteTimes);
        config.save();
    }

    public void addWarnTime(){
        warnTimes++;
        accumulatedWarnTimes++;
        config.set("warn_times", warnTimes);
        config.set("accumulated_warn_times", accumulatedWarnTimes);
        config.save();
    }

    public void addSuspectTimes(){
        suspectTimes++;
        accumulatedSuspectTimes++;
        config.set("suspect_times", suspectTimes);
        config.set("accumulated_suspect_times", accumulatedSuspectTimes);
        config.save();
    }

    public void addKickTimes(){
        kickTimes++;
        accumulatedKickTimes++;
        config.set("kick_times", kickTimes);
        config.set("accumulated_kick_times", accumulatedKickTimes);
        config.save();
    }

    public void addDealBugReportTime() {
        dealBugReportTimes+=1;
        accumulatedDealBugReportTimes+=1;
        config.set("deal_bug_report_times", config.getInt("deal_bug_report_times", 0) + 1);
        config.set("accumulated_deal_bug_report_times", config.getInt("accumulated_deal_bug_report_times", 0) + 1);
        config.save();
    }

    public void addDealBypassReportTime() {
        dealBypassReportTimes+=1;
        accumulatedDealBypassReportTimes+=1;
        config.set("deal_bypass_report_times", config.getInt("deal_bypass_report_times", 0) + 1);
        config.set("accumulated_deal_bypass_report_times", config.getInt("accumulated_deal_bypass_report_times", 0) + 1);
        config.save();
    }

    public void addVetoedTimes(int vetoedTimes) {
        config.set("vetoed_times", config.getInt("vetoed_times", 0) + 1);
        config.save();
    }

    public void addGradesFromPlayers(int grade) {
        config.set("all_grades_from_players", config.getInt("all_grades_from_players", 0) + grade);
        config.set("grade_player_counts", config.getInt("grade_player_counts", 0) + 1);
        config.save();
    }

    public void addPrefix(String string){
        List<String> prefixes = new ArrayList<>(config.getStringList("prefixes"));
        prefixes.add(string);
        config.set("prefixes", prefixes);
        config.save();
    }

    public void removePrefix(String string){
        List<String> prefixes = new ArrayList<>(config.getStringList("prefixes"));
        prefixes.remove(string);
        config.set("prefixes", prefixes);
        config.save();
    }

    public void save(){
        config.save();
    }
}
