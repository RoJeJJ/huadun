package sfs2x.handler;

import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;
import sfs2x.master.Player;
import sfs2x.utils.DBUtil;
import sfs2x.utils.Utils;

/**
 * 代开记录
 */
public class UserRoomHandler extends BaseClientRequestHandler{
    @Override
    public void handleClientRequest(User user, ISFSObject isfsObject) {
        Player player = Utils.getPlayer(user);
        int page = isfsObject.getInt("page");
        ISFSObject object = DBUtil.userRoom(player.getUserid(),page);
        send("ur",object,user);
    }
}
