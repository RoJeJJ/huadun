package sfs2x.handler;

import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;
import sfs2x.master.Player;
import sfs2x.utils.DBUtil;
import sfs2x.utils.Utils;

public class RoomRecordHandler extends BaseClientRequestHandler{
    @Override
    public void handleClientRequest(User user, ISFSObject isfsObject) {
        Player p = Utils.getPlayer(user);
        ISFSObject object = DBUtil.getRoomRecord(p.getUserid());
        System.out.println(object.toJson());
        send("rr", object,user);
    }
}
