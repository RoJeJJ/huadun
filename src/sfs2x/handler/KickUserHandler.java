package sfs2x.handler;

import com.smartfoxserver.v2.entities.Room;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;
import sfs2x.master.Player;
import sfs2x.master.base.ISeat;
import sfs2x.master.base.ITable;
import sfs2x.utils.Utils;

public class KickUserHandler extends BaseClientRequestHandler{
    @Override
    public void handleClientRequest(User user, ISFSObject isfsObject) {
        Player player = Utils.getPlayer(user);
        int uidToKicked = isfsObject.getInt("uid");
        String roomName = isfsObject.getUtfString("rn");
        Room room = getParentExtension().getParentZone().getRoomByName(roomName);
        if (room == null || !room.isActive())
            return;
        ITable table = Utils.getTable(room);
        if (table.getOwnerId() == player.getUserid() && uidToKicked != player.getUserid()){
            ISeat ToKicked = table.getSeat(uidToKicked);
            if (ToKicked != null && !table.isGameStart()){
                table.leaveRoom(ToKicked.getPlayer());
            }
        }
    }
}
