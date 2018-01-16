package sfs2x.handler;

import com.smartfoxserver.v2.entities.Room;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;
import sfs2x.Command;
import sfs2x.master.Player;
import sfs2x.utils.Utils;

/**
 * 加入房间
 */
public class RequestJoinRoomHandler extends BaseClientRequestHandler {
    @Override
    public void handleClientRequest(User user, ISFSObject isfsObject) {
        ISFSObject object = new SFSObject();
        Player player = Utils.getPlayer(user);
        String roomName = isfsObject.getUtfString("name");
        if (roomName == null || "".equals(roomName.trim())){
            object.putInt("err", 1);//参数错误
            send(Command.JOIN_ROOM, object, user);
        }else {
            Room room = getParentExtension().getParentZone().getRoomByName(roomName);
            if (room == null || !room.isActive()) {
                object.putInt("err", 2);//房间不存在
                send(Command.JOIN_ROOM, object, user);
            } else {
                room.getExtension().handleInternalMessage("join", player);
            }
        }
    }
}
