package sfs2x.handler;

import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;
import sfs2x.master.Player;
import sfs2x.utils.DBUtil;
import sfs2x.utils.Utils;

public class SetAgentHandler extends BaseClientRequestHandler{
    @Override
    public void handleClientRequest(User user, ISFSObject isfsObject) {
        Player player = Utils.getPlayer(user);
        Integer agentId = isfsObject.getInt("pid");
        if (agentId == null)
            throw new IllegalArgumentException("客户端参数错误!");
        else {
            int parentId = DBUtil.setAgent(player.getUserid(),agentId);
            player.setParentId(parentId);
            ISFSObject object = new SFSObject();
            object.putInt("pid",parentId);
            send("agent", object, user);
        }
    }
}
