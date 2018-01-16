package sfs2x.handler;

import com.smartfoxserver.v2.entities.Room;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSArray;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;
import sfs2x.master.Player;
import sfs2x.master.base.ITable;
import sfs2x.utils.Utils;

public class UserCurRoomHandler extends BaseClientRequestHandler{
    @Override
    public void handleClientRequest(User user, ISFSObject isfsObject) {
        Player player = Utils.getPlayer(user);
        ISFSArray array = new SFSArray();
        for (Room room : getParentExtension().getParentZone().getRoomList()){
            ITable iTable = Utils.getTable(room);
            if (iTable.getOwnerId() == player.getUserid()){
                ISFSObject o = new SFSObject();
                o.putUtfString("n",room.getName()); //房间号
                o.putInt("mod",iTable.getMod());//游戏类型
                o.putBool("gs",iTable.isGameStart());//游戏是否开局
                o.putInt("c",iTable.getCount());//总局数
                o.putInt("cc",iTable.getCurCount());//当前局数
                o.putInt("p",iTable.getMaxPerson());//总人数
                o.putInt("cp",iTable.getCurrentPerson());//当前人数
                array.addSFSObject(o);
            }
        }
        ISFSObject object = new SFSObject();
        object.putSFSArray("r",array);
        send("curRoom",object,user);
    }
}
