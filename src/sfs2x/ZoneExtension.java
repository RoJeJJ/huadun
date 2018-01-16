package sfs2x;

import com.smartfoxserver.v2.SmartFoxServer;
import com.smartfoxserver.v2.core.SFSEventType;
import com.smartfoxserver.v2.entities.Room;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.smartfoxserver.v2.extensions.SFSExtension;
import sfs2x.handler.*;
import sfs2x.master.Player;
import sfs2x.utils.DBUtil;

import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ZoneExtension extends SFSExtension {
    public ConcurrentMap<Integer,Player> olPlayers = new ConcurrentHashMap<>();
    @Override
    public void init() {
        initData();
        DBUtil.initDB(getParentZone().getDBManager());
        addEventHandler(SFSEventType.USER_LOGIN, OnLoginHandler.class);
        addEventHandler(SFSEventType.USER_JOIN_ZONE, OnZoneJoinHandler.class);
        addEventHandler(SFSEventType.ROOM_REMOVED, OnRoomRemovedHandler.class);
        addEventHandler(SFSEventType.USER_DISCONNECT, OnDisconnectedHandler.class);
        addEventHandler(SFSEventType.USER_LOGOUT, OnDisconnectedHandler.class);
        addEventHandler(SFSEventType.ROOM_ADDED, OnRoomAddedHandler.class);

        addRequestHandler(Command.PING, PingHandler.class);//接收ping
        addRequestHandler("cr", CreateRoomHandler.class);//创建房间
        addRequestHandler("jr", RequestJoinRoomHandler.class);//加入房间
        addRequestHandler("ur", UserRoomHandler.class);
        addRequestHandler("curRoom", UserCurRoomHandler.class);
        addRequestHandler("dmr", DismissRoomHandler.class);
        addRequestHandler("rr", RoomRecordHandler.class);
        addRequestHandler("gr", GameRecordHandler.class);
        addRequestHandler("sa", SetAgentHandler.class);
        deleteTask();
    }

    @Override
    public Object handleInternalMessage(String cmdName, Object params) {
        if ("card".equals(cmdName)) {
            ISFSObject object = (ISFSObject) params;
            int uid = object.getInt("uid");
            long card = object.getLong("card");
            Player player = olPlayers.get(uid);
            if (player != null){
                player.setCard(card);
                ISFSObject o = new SFSObject();
                o.putLong("card", card);
                send("card", o, player.getUser());
            }
            return "success";
        } else if ("halt".equals(cmdName)) {
            for (User user : getParentZone().getUserList())
                getApi().logout(user);
            for (Room room : getParentZone().getRoomList()) {
                getApi().removeRoom(room);
            }
            SmartFoxServer.getInstance().halt();
        }
        return null;
    }

    private void initData() {
        for (int i = 100000; i < 1000000; i++) {
            Constant.ROOM_NAMES.add(i);
        }
    }

    @Override
    public void destroy() {
        super.destroy();
    }

    private void deleteTask() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 6);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long now = Calendar.getInstance().getTimeInMillis();
        long taskTime = cal.getTimeInMillis();
        Timer timer = new Timer();
        long delay;
        if (taskTime > now)
            delay = taskTime - now;
        else {
            cal.set(Calendar.DAY_OF_MONTH, cal.get(Calendar.DAY_OF_MONTH) + 1);
            delay = cal.getTimeInMillis() - now;
        }
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                for (Room room : getParentZone().getRoomList()) {
                    room.getExtension().handleInternalMessage("s", null);
                }
                DBUtil.deleteRecord();
            }
        }, delay, 24 * 60 * 60 * 1000);
    }
}
