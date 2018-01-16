package ddz;

import com.smartfoxserver.v2.SmartFoxServer;
import com.smartfoxserver.v2.core.SFSEventType;
import com.smartfoxserver.v2.extensions.SFSExtension;
import com.smartfoxserver.v2.util.TaskScheduler;
import ddz.handler.*;
import sfs2x.master.Player;
import sfs2x.master.base.ITable;
import sfs2x.master.ddz_game.DDZTable;
import sfs2x.utils.Utils;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class DDZExtension extends SFSExtension {
    private ScheduledFuture taskHandler;
    private ITable iTable;
    @Override
    public void init() {
        addEventHandler(SFSEventType.USER_JOIN_ROOM,DDZJoinRoomHandler.class);
        addRequestHandler("call",UserCallHandler.class);
        addRequestHandler("play",UserPlayHandler.class);
        addRequestHandler("fold",UserFoldHandler.class);
        addRequestHandler("lr",DDZLeaveRoomHandler.class);
        addRequestHandler("r",UserReadyHandler.class);
        addRequestHandler("e",DismissResponse.class);
        addRequestHandler("t",TrusteeshipHandler.class);

        iTable = Utils.getTable(getParentRoom());
        taskHandler = SmartFoxServer.getInstance().getTaskScheduler().scheduleAtFixedRate((DDZTable)iTable,0,30, TimeUnit.MILLISECONDS);
    }

    @Override
    public void destroy() {
        super.destroy();
        taskHandler.cancel(false);
        iTable.destroy();
    }
    @Override
    public Object handleInternalMessage(String cmdName, Object params) {
        if (cmdName.equals("offline")){
            Player player = (Player) params;
            iTable.disConnected(player);
        }else if (cmdName.equals("join")){
            Player p = (Player) params;
            iTable.requestJoin(p);
        }
        return null;
    }
}
