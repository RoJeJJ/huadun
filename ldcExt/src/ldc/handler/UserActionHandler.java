package ldc.handler;

import com.smartfoxserver.v2.entities.Room;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;
import sfs2x.master.Player;
import sfs2x.master.ldc_game.LDCTable;
import sfs2x.utils.Utils;

public class UserActionHandler extends BaseClientRequestHandler{
    @Override
    public void handleClientRequest(User user, ISFSObject isfsObject) {
        Player player = Utils.getPlayer(user);
        Room room = getParentExtension().getParentRoom();
        LDCTable table = (LDCTable) Utils.getTable(room);
        Integer bet = isfsObject.getInt("bet");
        if (bet == null)
            throw new IllegalArgumentException("客户端发送参数错误!");
        else
            table.userAction(player,bet,false);
    }
}
