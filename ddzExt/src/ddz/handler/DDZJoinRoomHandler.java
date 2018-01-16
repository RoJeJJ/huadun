package ddz.handler;

import com.smartfoxserver.v2.core.ISFSEvent;
import com.smartfoxserver.v2.core.SFSEventParam;
import com.smartfoxserver.v2.entities.Room;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.exceptions.SFSException;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;
import com.smartfoxserver.v2.extensions.BaseServerEventHandler;
import sfs2x.master.Player;
import sfs2x.master.ddz_game.DDZTable;
import sfs2x.utils.Utils;

public class DDZJoinRoomHandler extends BaseServerEventHandler{
    @Override
    public void handleServerEvent(ISFSEvent isfsEvent){
        User user = (User) isfsEvent.getParameter(SFSEventParam.USER);
        Player p = Utils.getPlayer(user);
        Room room = getParentExtension().getParentRoom();
        DDZTable table = (DDZTable) Utils.getTable(room);
        table.join(p);
    }
}
