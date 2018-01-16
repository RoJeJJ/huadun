package ldc.handler;

import com.smartfoxserver.v2.core.ISFSEvent;
import com.smartfoxserver.v2.core.SFSEventParam;
import com.smartfoxserver.v2.entities.Room;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.extensions.BaseServerEventHandler;
import sfs2x.master.Player;
import sfs2x.master.ldc_game.LDCTable;
import sfs2x.utils.Utils;

public class JoinRoomHandler extends BaseServerEventHandler{
    @Override
    public void handleServerEvent(ISFSEvent isfsEvent)  {
        User user = (User) isfsEvent.getParameter(SFSEventParam.USER);
        Player player = Utils.getPlayer(user);
        Room room = getParentExtension().getParentRoom();
        LDCTable table = (LDCTable) Utils.getTable(room);
        table.join(player);
    }
}
