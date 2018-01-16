package sfs2x.handler;

import com.smartfoxserver.v2.core.ISFSEvent;
import com.smartfoxserver.v2.core.SFSEventParam;
import com.smartfoxserver.v2.entities.Room;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.smartfoxserver.v2.exceptions.SFSException;
import com.smartfoxserver.v2.extensions.BaseServerEventHandler;
import sfs2x.Constant;
import sfs2x.ZoneExtension;
import sfs2x.master.Player;
import sfs2x.utils.DBUtil;
import sfs2x.utils.Utils;

public class OnZoneJoinHandler extends BaseServerEventHandler{
    @Override
    public void handleServerEvent(ISFSEvent isfsEvent) throws SFSException {
        User user = (User) isfsEvent.getParameter(SFSEventParam.USER);
        ZoneExtension zext = (ZoneExtension) getParentExtension();
        user.setPrivilegeId((short) 2);
        Player player = Utils.getPlayer(user);
        Player curP = Constant.offlinePlayer.get(player.getUserid());
        if (curP != null) {
            player = curP;
            Constant.offlinePlayer.remove(player.getUserid());
            user.getSession().setProperty("p",player);
        }
        player.setUser(user);
        zext.olPlayers.putIfAbsent(player.getUserid(),player);
        send("u",player.toSFSObject(),user);

        if (DBUtil.checkVV(player.getUserid())){
            send("ct",null,user);
        }
        if (player.getParentId() == 0){
            send("sa",null,user);
        }

        Room room = player.getRoom();
        if (room != null && room.isActive()){
            getApi().joinRoom(user,room,null,false,user.getLastJoinedRoom());
        }else
            player.setRoom(null);
    }
}
