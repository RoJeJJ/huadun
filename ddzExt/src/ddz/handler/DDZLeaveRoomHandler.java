package ddz.handler;

import com.smartfoxserver.v2.entities.Room;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;
import sfs2x.master.Player;
import sfs2x.master.ddz_game.DDZTable;
import sfs2x.utils.Utils;

public class DDZLeaveRoomHandler extends BaseClientRequestHandler{
    @Override
    public void handleClientRequest(User user, ISFSObject isfsObject) {
        Player player = Utils.getPlayer(user);
        Room room = getParentExtension().getParentRoom();
        DDZTable table = (DDZTable) Utils.getTable(room);
        table.leaveRoom(player);
    }
}
