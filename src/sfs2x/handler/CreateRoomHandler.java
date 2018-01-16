package sfs2x.handler;

import com.smartfoxserver.v2.annotations.Instantiation;
import com.smartfoxserver.v2.api.CreateRoomSettings;
import com.smartfoxserver.v2.entities.Room;
import com.smartfoxserver.v2.entities.SFSRoomRemoveMode;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.smartfoxserver.v2.exceptions.SFSCreateRoomException;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;
import sfs2x.Command;
import sfs2x.Constant;
import sfs2x.master.Player;
import sfs2x.master.ddz_game.DDZTable;
import sfs2x.master.ldc_game.LDCTable;
import sfs2x.utils.DBUtil;
import sfs2x.utils.Utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * 请求创建房间
 */
@Instantiation(Instantiation.InstantiationMode.SINGLE_INSTANCE)
public class CreateRoomHandler extends BaseClientRequestHandler {
    @Override
    public synchronized void handleClientRequest(User user, ISFSObject isfsObject) {
        Integer gamecode = isfsObject.getInt("gc");//创建哪种游戏的房间
        Integer mode = isfsObject.getInt("m");//代开还是自己玩
        ISFSObject object = new SFSObject();
        Player player = Utils.getPlayer(user);
        if (gamecode == null || mode == null){
            object.putInt("err",0);
            send(Command.CREATE_ROOM,object,user);
            return;
        }
        if (gamecode == 0){ // 拉大车游戏
            Integer count = isfsObject.getInt("c"); // 局数

            if (count == null || (count != Constant.COUNT_LDC_1 && count != Constant.COUNT_LDC_2 && count != Constant.COUNT_LDC_3)){//参数错误
                object.putInt("err",0);
                send(Command.CREATE_ROOM,object,user);
                return;
            }
            LDCTable table = new LDCTable(count,1,true);
            if (DBUtil.lockCard(player.getUserid(),table.getCost())){ //锁定房卡
                CreateRoomSettings roomSettings = new CreateRoomSettings();
                synchronized (Constant.ROOM_NAME_LOCK) {
                    int index = new Random().nextInt(Constant.ROOM_NAMES.size());
                    String roomName = String.valueOf(Constant.ROOM_NAMES.remove(index));
                    roomSettings.setName(roomName);
                }
                roomSettings.setAutoRemoveMode(SFSRoomRemoveMode.NEVER_REMOVE);
                roomSettings.setDynamic(true);
                roomSettings.setGame(true);
                roomSettings.setMaxUsers(6);
                roomSettings.setMaxSpectators(0);

                Map<Object,Object> map = new HashMap<>();
                map.put("t",table);
                roomSettings.setRoomProperties(map);

                CreateRoomSettings.RoomExtensionSettings extensionSettings =
                        new CreateRoomSettings.RoomExtensionSettings("ldc_ext", "ldc.LDCExtension");
                roomSettings.setExtension(extensionSettings);
                try {
                    Room room = getApi().createRoom(getParentExtension().getParentZone(),roomSettings,null);
                    table.bindRoom(room,player.getUserid());
                    if (mode == 0) // 自己开房玩
                        room.getExtension().handleInternalMessage("join",player);
                }catch (SFSCreateRoomException e){
                    e.printStackTrace();
                    DBUtil.unLockCard(player.getUserid(),table.getCost());
                    object.putInt("err",2);//创建失败
                    send(Command.CREATE_ROOM,object,user);
                }
            }else {
                object.putInt("err",1);
                send(Command.CREATE_ROOM,object,user);
            }
        }else if (gamecode == 1){ //斗地主
            Integer count = isfsObject.getInt("c");
            if (count == null || (count != Constant.COUNT_DDZ_1 && count != Constant.COUNT_DDZ_2) ){//参数错误
                object.putInt("err",0);
                send("cr",object,user);
                return;
            }
            DDZTable table = new DDZTable(count,1,true);
            if (DBUtil.lockCard(player.getUserid(),table.getCost())){ // 锁定房卡
                CreateRoomSettings roomSettings = new CreateRoomSettings();
                synchronized (Constant.ROOM_NAME_LOCK) {
                    int index = new Random().nextInt(Constant.ROOM_NAMES.size());
                    String roomName = String.valueOf(Constant.ROOM_NAMES.remove(index));
                    roomSettings.setName(roomName);
                }
                roomSettings.setAutoRemoveMode(SFSRoomRemoveMode.NEVER_REMOVE);
                roomSettings.setDynamic(true);
                roomSettings.setGame(true);
                roomSettings.setMaxUsers(3);
                roomSettings.setMaxSpectators(0);

                Map<Object,Object> map = new HashMap<>();
                map.put("t",table);
                roomSettings.setRoomProperties(map);

                CreateRoomSettings.RoomExtensionSettings extensionSettings =
                        new CreateRoomSettings.RoomExtensionSettings("ddz_ext", "ddz.DDZExtension");
                roomSettings.setExtension(extensionSettings);
                try {
                    Room room = getApi().createRoom(getParentExtension().getParentZone(),roomSettings,null);
                    table.bindRoom(room,player.getUserid());
                    if (mode == 0) // 自己开房玩
                        room.getExtension().handleInternalMessage("join",player);
                }catch (Exception e){
                    e.printStackTrace();
                    DBUtil.unLockCard(player.getUserid(),table.getCost());
                    object.putInt("err",2);//创建失败
                    send(Command.CREATE_ROOM,object,user);
                }
            }else {
                object.putInt("err",1);
                send(Command.CREATE_ROOM,object,user);
            }
        }
    }
}
