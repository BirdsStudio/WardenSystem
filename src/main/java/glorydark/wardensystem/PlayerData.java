package glorydark.wardensystem;

import cn.nukkit.Player;
import cn.nukkit.Server;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.stream.Collectors;

@AllArgsConstructor
public class PlayerData {

    Player player;
    List<DamageSource> sourceList;

    public List<DamageSource> getSourceList() {
        refreshSourceList();
        return sourceList;
    }

    public void addDamageSource(Player damager){
        DamageSource source = sourceList.stream().filter(damageSource -> damageSource.damager.equals(damager.getName())).collect(Collectors.toList()).get(0);
        if(source == null){
            source = new DamageSource(damager.getName(), System.currentTimeMillis(), 0);
        }
        sourceList.remove(source);
        sourceList.add(new DamageSource(damager.getName(), System.currentTimeMillis(), source.getDamageTime() + 1));
    }

    protected void refreshSourceList(){
        sourceList.removeIf(DamageSource::isExpired);
    }

    // 获取以玩家为中心16格内的玩家
    public List<Player> getSurroundedPlayer(){
        return Server.getInstance().getOnlinePlayers().values().stream().filter(p -> p.getLevel() == player.getLevel() && p.distance(player) < 16).collect(Collectors.toList());
    }

    @Data
    @AllArgsConstructor
    public static class DamageSource{

        String damager;

        long millis;

        int damageTime;

        public boolean isExpired(){
            return System.currentTimeMillis() - millis >= 60000;
        }

    }
}
