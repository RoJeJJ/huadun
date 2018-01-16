package sfs2x.utils;

import com.smartfoxserver.v2.entities.Room;
import com.smartfoxserver.v2.entities.User;
import sfs2x.master.base.ITable;
import sfs2x.master.Player;

public class Utils {
    public static Player getPlayer(User user){
        return (Player) user.getSession().getProperty("p");
    }
    public static ITable getTable(Room room){
        return (ITable) room.getProperty("t");
    }
    public static void delay(long time){
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
