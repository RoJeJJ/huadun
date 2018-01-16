package ddz.handler;

import com.smartfoxserver.v2.entities.Room;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;
import sfs2x.master.Player;
import sfs2x.master.ddz_game.DDZTable;
import sfs2x.utils.Utils;

public class UserCallHandler extends BaseClientRequestHandler{
    @Override
    public void handleClientRequest(User user, ISFSObject isfsObject) {
        Player p = Utils.getPlayer(user);
        Room room = getParentExtension().getParentRoom();
        DDZTable table = (DDZTable) Utils.getTable(room);
        Integer call = isfsObject.getInt("call");
        if (call == null || (call != 0 && call != 1 && call != 2 && call != 3))
            throw new IllegalArgumentException("客户端参数错误");
        else
            table.userCall(false,p,call);
    }
}
