package sfs2x.handler;

import com.smartfoxserver.v2.entities.Room;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSArray;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;
import sfs2x.master.Player;
import sfs2x.master.base.ITable;
import sfs2x.utils.Utils;

public class UserCurRoomHandler extends BaseClientRequestHandler{
    @Override
    public void handleClientRequest(User user, ISFSObject isfsObject) {
        Player player = Utils.getPlayer(user);
        ISFSArray array = new SFSArray();
        for (Room room : getParentExtension().getParentZone().getRoomList()){
            ITable iTable = Utils.getTable(room);
            if (iTable.getOwnerId() == player.getUserid()){
                array.addSFSObject(iTable.tableStatus(player));
            }
        }
        ISFSObject object = new SFSObject();
        object.putSFSArray("r",array);
        send("curRoom",object,user);
    }
}
