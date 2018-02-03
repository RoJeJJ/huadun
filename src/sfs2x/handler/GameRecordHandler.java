package sfs2x.handler;

import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;
import sfs2x.utils.DBUtil;

public class GameRecordHandler extends BaseClientRequestHandler{
    @Override
    public void handleClientRequest(User user, ISFSObject isfsObject) {
        String uuid = isfsObject.getUtfString("uuid");
        ISFSObject object = DBUtil.getGameRecord(uuid);
//        System.out.println(object.toJson());
        send("gr", object,user);
    }
}
