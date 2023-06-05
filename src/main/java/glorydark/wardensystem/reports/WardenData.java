package glorydark.wardensystem.reports;

import cn.nukkit.utils.Config;
import glorydark.wardensystem.MainClass;
import glorydark.wardensystem.reports.matters.Report;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class WardenData{

    public Report dealing; //处理事务类型

    public int accumulatedTimes;

    public int vetoedTimes;

    public int allGradesFromPlayers;

    public int gradePlayerCounts;

    public List<String> prefixes;

    public Config config;

    public String joinTime;

    public WardenData(Report dealing, Config config){
        this.dealing = dealing;
        this.config = config;
        this.prefixes = new ArrayList<>(config.getStringList("prefixes"));
        this.accumulatedTimes = config.getInt("accumulated_times", 0);
        this.vetoedTimes = config.getInt("vetoed_times", 0);
        this.allGradesFromPlayers = config.getInt("all_grades_from_players", 5);
        this.gradePlayerCounts = config.getInt("grade_player_counts", 0);
        this.joinTime = MainClass.getDate(config.getLong("join_time"));
    }

    public void addAccumulatedTimes() {
        config.set("accumulated_times", config.getInt("accumulated_times", 0) + 1);
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
}
