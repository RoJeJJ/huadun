package sfs2x.handler;

import com.smartfoxserver.v2.entities.Room;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;
import sfs2x.master.Player;
import sfs2x.master.base.ITable;
import sfs2x.utils.Utils;

public class DismissRoomHandler extends BaseClientRequestHandler{
    @Override
    public void handleClientRequest(User user, ISFSObject isfsObject) {
        Player player = Utils.getPlayer(user);
        String name = isfsObject.getUtfString("name");
        Room room = getParentExtension().getParentZone().getRoomByName(name);
        if (room != null && room.isActive()){
            ITable iTable = Utils.getTable(room);
            if (iTable.getOwnerId() == player.getUserid() && !iTable.isGameStart() && iTable.getCurrentPerson() == 0)
                getApi().removeRoom(room);
        }
    }
}
