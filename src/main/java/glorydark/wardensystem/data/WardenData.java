package glorydark.wardensystem.data;

import cn.nukkit.utils.Config;
import glorydark.wardensystem.MainClass;
import glorydark.wardensystem.reports.matters.Report;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class WardenData {

    public Report dealing; //处理事务类型

    public int dealBugReportTimes;

    public int dealBypassReportTimes;

    public int accumulatedDealBugReportTimes;

    public int accumulatedDealBypassReportTimes;

    public int gamemodeBefore;

    public int vetoedTimes;

    public int allGradesFromPlayers;

    public int gradePlayerCounts;

    public List<String> prefixes;

    public Config config;

    public String joinTime;

    public WardenLevelType levelType;

    public WardenData(Report dealing, Config config){
        this.gamemodeBefore = 0;
        this.dealing = dealing;
        this.config = config;
        this.prefixes = new ArrayList<>(config.getStringList("prefixes"));
        this.dealBugReportTimes = config.getInt("deal_bug_report_times", 0);
        this.dealBypassReportTimes = config.getInt("deal_bypass_report_times", 0);
        this.accumulatedDealBugReportTimes = config.getInt("accumulated_deal_bug_report_times", dealBugReportTimes);
        this.accumulatedDealBypassReportTimes = config.getInt("accumulated_deal_bypass_report_times", dealBypassReportTimes);
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
