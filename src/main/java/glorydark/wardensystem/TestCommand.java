package glorydark.wardensystem;

import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.form.window.FormWindowSimple;

public class TestCommand extends Command {

    public TestCommand(String name) {
        super(name);
    }

    @Override
    public boolean execute(CommandSender commandSender, String s, String[] strings) {
        if(commandSender.isPlayer()) {
            FormWindowSimple simple = new FormWindowSimple("233", "233");
            Player player = (Player) commandSender;
            player.showFormWindow(simple);
        }
        return true;
    }
}
