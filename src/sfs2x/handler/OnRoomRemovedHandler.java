package sfs2x.handler;

import com.smartfoxserver.v2.core.ISFSEvent;
import com.smartfoxserver.v2.core.SFSEventParam;
import com.smartfoxserver.v2.entities.Room;
import com.smartfoxserver.v2.exceptions.SFSException;
import com.smartfoxserver.v2.extensions.BaseServerEventHandler;
import sfs2x.Constant;

public class OnRoomRemovedHandler extends BaseServerEventHandler{
    @Override
    public void handleServerEvent(ISFSEvent isfsEvent) throws SFSException {
        Room room = (Room) isfsEvent.getParameter(SFSEventParam.ROOM);
        int roomName = Integer.parseInt(room.getName());

        //回收房间名字,到房间名字数组
        synchronized (Constant.ROOM_NAME_LOCK){
            if (!Constant.ROOM_NAMES.contains(roomName))
                Constant.ROOM_NAMES.add(roomName);
        }
    }
}
