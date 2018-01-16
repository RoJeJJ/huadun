package ldc;

import com.smartfoxserver.v2.SmartFoxServer;
import com.smartfoxserver.v2.core.SFSEventType;
import com.smartfoxserver.v2.extensions.SFSExtension;
import ldc.handler.*;
import sfs2x.master.Player;
import sfs2x.master.base.ITable;
import sfs2x.master.ldc_game.LDCTable;
import sfs2x.utils.Utils;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class LDCExtension extends SFSExtension{
    private ScheduledFuture taskHandler;
    private ITable iTable;
    @Override
    public void init() {
        addEventHandler(SFSEventType.USER_JOIN_ROOM,JoinRoomHandler.class);//加入房间
        addRequestHandler("r",UserReadyHandler.class);//准备
        addRequestHandler("lr",UserLeaveRoomHandler.class);//离开房间
        addRequestHandler("a",UserActionHandler.class);//action
        addRequestHandler("e",DissolveResponseHandler.class);
        addRequestHandler("vv",VVHandler.class);
        addRequestHandler("v",VHandler.class);

        iTable = Utils.getTable(getParentRoom());
        taskHandler = SmartFoxServer.getInstance().getTaskScheduler().scheduleAtFixedRate((LDCTable)iTable,0,30, TimeUnit.MILLISECONDS);
    }


    @Override
    public Object handleInternalMessage(String cmdName, Object params) {
        if (cmdName.equals("offline")){
            Player player = (Player) params;
            iTable.disConnected(player);
        }else if (cmdName.equals("join")){
            Player p = (Player) params;
            iTable.requestJoin(p);
        }else if (cmdName.equals("s")){
            iTable.checkRoom();
        }
        return null;
    }

    @Override
    public void destroy() {
        super.destroy();
        taskHandler.cancel(false);
        iTable.destroy();
    }
}
