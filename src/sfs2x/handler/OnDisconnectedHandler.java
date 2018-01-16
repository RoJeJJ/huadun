package sfs2x.handler;

import com.smartfoxserver.v2.core.ISFSEvent;
import com.smartfoxserver.v2.core.SFSEventParam;
import com.smartfoxserver.v2.entities.Room;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.extensions.BaseServerEventHandler;
import sfs2x.ZoneExtension;
import sfs2x.master.Player;
import sfs2x.utils.DBUtil;
import sfs2x.utils.Utils;

public class OnDisconnectedHandler extends BaseServerEventHandler{
    @Override
    public void handleServerEvent(ISFSEvent isfsEvent) {
        User user = (User) isfsEvent.getParameter(SFSEventParam.USER);
        ZoneExtension zext = (ZoneExtension) getParentExtension();
        Player player = Utils.getPlayer(user);
        user.getSession().removeProperty("p");
        zext.olPlayers.remove(player.getUserid());
        DBUtil.signOut(player.getUserid());
        Room room = player.getRoom();
        if (room != null && room.isActive()) {//在房间中
            room.getExtension().handleInternalMessage("offline",player);
        }else
            player.setRoom(null);
    }
}
