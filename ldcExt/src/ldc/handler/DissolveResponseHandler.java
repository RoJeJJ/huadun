package ldc.handler;

import com.smartfoxserver.v2.entities.Room;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;
import sfs2x.master.Player;
import sfs2x.master.ldc_game.LDCTable;
import sfs2x.utils.Utils;

public class DissolveResponseHandler extends BaseClientRequestHandler{
    @Override
    public void handleClientRequest(User user, ISFSObject isfsObject) {
        Player player = Utils.getPlayer(user);
        Room room = getParentExtension().getParentRoom();
        LDCTable table = (LDCTable) Utils.getTable(room);
        boolean dis = isfsObject.getBool("e");
        table.dissolve(player,dis);
    }
}
