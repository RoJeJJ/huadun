package sfs2x.master.base;

import com.smartfoxserver.v2.entities.Room;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSArray;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.smartfoxserver.v2.exceptions.SFSJoinRoomException;
import com.smartfoxserver.v2.extensions.SFSExtension;
import sfs2x.Command;
import sfs2x.Constant;
import sfs2x.master.Player;
import sfs2x.utils.DBUtil;

import java.util.*;

public abstract class ITable {
    protected String uuid;
    protected int mod;
    protected ISeat[] seats;
    protected int count;//房间总局数,金币房为-1
    protected boolean cardRoom; // true房卡房间,false金币房间
    protected int cost;//开房间消耗的房卡数,金币房间花费为0;
    protected int limit;//金币房的准入条件,房卡房为0;
    protected Room room;
    protected SFSExtension ext;
    protected int ownerId;
    protected boolean start;
    protected boolean gameStart;
    protected int curCount;
    protected boolean takeOff;
    protected StringBuffer record;
    protected ArrayList<ISeat> playSeat;

    public String getRecord(){
        return record.toString();
    }

    public int getCurCount(){
        return curCount;
    }

    public ITable(){
        uuid = UUID.randomUUID().toString().replace("-","");
        playSeat = new ArrayList<>();
    }

    public int getOwnerId() {
        return ownerId;
    }

    public int getCount() {
        return count;
    }

    protected void bindRoom(Room room, int uid){
        this.room = room;
        ext = (SFSExtension) room.getExtension();
        seats = new ISeat[getMaxPerson()];
        ownerId = uid;
        DBUtil.newRoom(room.getName(),uuid,mod,cost,ownerId);
    }



    public boolean isGameStart() {
        return gameStart;
    }

    public boolean isStart() {
        return start;
    }

    protected void sendStartGame() {
        ISFSObject object = new SFSObject();
        if (cardRoom)
            object.putInt("cc", curCount);
        ext.send("start", object, room.getUserList());
    }

    public synchronized int getMaxPerson(){
        return room.getMaxUsers();
    }

    public synchronized int getCurrentPerson(){
        int n = 0;
        for (ISeat s:seats){
            if (!s.isEmpty)
                n++;
        }
        return n;
    }

    protected int getSeatNo(Player player){
        for (int i=0;i<seats.length;i++){
            if (!seats[i].isEmpty && seats[i].mPlayer == player)
                return i;
        }
        throw new IllegalArgumentException("getSeatNo()出错");
    }
    protected ISeat getSeat(Player player){
        for (ISeat s:seats){
            if (!s.isEmpty && s.mPlayer == player)
                return s;
        }
        throw new IllegalArgumentException("getSeat()出错");
    }

    private boolean isExists(Player player){
        if (player == null)
            return false;
        for (ISeat s:seats){
            if (!s.isEmpty&&s.mPlayer == player) {
                return true;
            }
        }
        return false;
    }

    private void setSeat(Player player){
        for (ISeat seat:seats){
            if (seat.isEmpty){
                seat.mPlayer = player;
                seat.isEmpty = false;
                break;
            }
        }
    }
    public int getCost() {
        return cost;
    }

    public void destroy() {
        cancelDisTimer();
        if (timer != null){
            timer.cancel();
            timer = null;
        }
        if (cardRoom && !takeOff){
            DBUtil.unLockCard(ownerId,cost);
        }
        if (cardRoom && gameStart){
            balance();
        }
        DBUtil.roomDestroy(uuid);
        for (ISeat seat:seats){
            if (!seat.isEmpty)
                seat.userLeave();
        }
    }

    protected void setReady(Player p){
        getSeat(p).setReady(true);

        //发送准备
        ISFSObject object = new SFSObject();
        object.putInt("uid",p.getUserid());
        ext.send("ready",object,room.getUserList());
    }

    protected abstract boolean readyToStart();

    /**
     *  玩家发送准备
     * @param player 玩家
     */
    public void playerReady(Player player){
        if (!start) {
            ISeat seat = getSeat(player);
            if (!seat.isReady()) {
                setReady(player);
            }
            if (readyToStart()) { //可以开始游戏
                if (cardRoom) { //房卡房
                    if (!takeOff) {
                        long card = DBUtil.costCard(uuid, ownerId, cost);
                        if (card >= 0) { //扣卡成功
                            start = true;
                            takeOff = true;
                            ISFSObject object = new SFSObject();
                            object.putInt("uid",ownerId);
                            object.putLong("card",card);
                            ext.getParentZone().getExtension().handleInternalMessage("card",object);
                        } else { //扣卡失败
                            ext.getApi().removeRoom(room);
                        }
                    } else { //已经扣除房卡
                        start = true;
                    }
                } else //金币房,直接开始游戏
                    start = true;
                if (start) {
                    gameStart = true;
                    playSeat.clear();
                    for (ISeat s : seats) {
                        if (s.isReady())
                            playSeat.add(s);
                    }
                    for (int i = 0; i < playSeat.size(); i++) {
                        if (i == playSeat.size() - 1)
                            playSeat.get(i).setNext(playSeat.get(0));
                        else
                            playSeat.get(i).setNext(playSeat.get(i + 1));
                    }
                }
            }
        }
    }
    public ISFSObject tableStatus(Player player){
        ISFSObject object = new SFSObject();
        object.putInt("mod",mod);
        object.putInt("person",getMaxPerson());
        object.putUtfString("name", room.getName());//房间号
        object.putBool("cardRoom", cardRoom);// 房卡房,金币房
        object.putBool("start", start);//是否正在在游戏
        if (cardRoom) {
            object.putBool("gs", gameStart);//房卡房,是否已经开局
            object.putInt("count", count);//房卡房总局数
            object.putInt("cc", curCount);//房卡房当前局数
        }
        return object;
    }

    public int getMod() {
        return mod;
    }

    /**
     *  玩家重连
     * @param player 玩家
     */
    public abstract void reconnect(Player player);

    /**
     * 玩家加入房间
     * @param player 玩家
     */
    protected void join(Player player){
        try {
            User user = player.getUser();
            List<User> users = room.getUserList();
            users.remove(user);
            boolean rec;
            player.setRoom(room);
            if (isExists(player)) { //断线重连
                rec = true;
                getSeat(player).offline = false;

                //通知其他玩家,该玩家上线
                ISFSObject object = new SFSObject();
                object.putInt("uid", player.getUserid());
                ext.send("ol", object, users);
            } else { //新加入的玩家
                rec = false;
                setSeat(player);

                //通知其他玩家,该玩家加入
                ISFSObject object = player.toSFSObject();
                object.putInt("seat", getSeatNo(player));
                ext.send("join", object, users);
            }
            //发送房间的当前状态给玩家
            if (user != null) {
                ext.send("detail", tableStatus(player), user);
            }
            if (rec)
                reconnect(player);
            if (dissolve && dissolver != null)
                sendDissolve(player);
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    //解散标志
    private boolean dissolve = false;
    private Player dissolver = null;
    private Timer disTimer = null;

    private void userLeave(Player player) {
        ISFSObject object = new SFSObject();
        object.putInt("uid", player.getUserid());
        ext.send("leave", object, room.getUserList());
        User user = player.getUser();
        if (user != null)
            ext.getApi().leaveRoom(user, room);
        ISeat seat = getSeat(player);
        seat.userLeave();
    }

    public void leaveRoom(Player player) {
        if (cardRoom) {
            if (!gameStart) { // 房间未开局,可以离开房间
                userLeave(player);
                if (getCurrentPerson() == 0)
                    ext.getApi().removeRoom(room);
            } else { // 房间已经开局,不能离开房间,直接申请解散
                if (!dissolve) {
                    dissolve = true;
                    dissolver = player;
                    ISeat seat = getSeat(player);
                    seat.disCode = 1;
                    disTimer = new Timer();
                    disTimer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            synchronized (ITable.this) {
                                for (ISeat s : seats) {
                                    if (!s.isEmpty && s.disCode == 0)
                                        s.disCode = 1;
                                }
                                verifyDissolve();
                            }
                        }
                    }, 5 * 60000);
                    sendDissolve(null);
                }
            }
        }
    }

    public void dissolve(Player player, boolean dis) {
        if (dissolve && dissolver != null) {
            ISeat seat = getSeat(player);
            if (seat != null && !seat.isEmpty){
                if (seat.disCode == 0) {
                    if (dis)
                        seat.disCode = 1;
                    else
                        seat.disCode = 2;
                    sendDissolve(null);
                    verifyDissolve();
                }
            }
        }
    }

    private void sendDissolve(Player player) {
        ISFSObject object = new SFSObject();
        object.putInt("uid", dissolver.getUserid());
        object.putUtfString("name", dissolver.getNickname());
        ISFSArray array = new SFSArray();
        for (ISeat seat : seats) {
            if (!seat.isEmpty()) {
                ISFSObject o = new SFSObject();
                o.putInt("uid", seat.getPlayer().getUserid());
                o.putUtfString("name", seat.getPlayer().getNickname());
                o.putInt("e", seat.disCode);
                array.addSFSObject(o);
            }
        }
        object.putSFSArray("p", array);
        if (player == null)
            ext.send("dis", object, room.getUserList());
        else {
            User user = player.getUser();
            if (user != null)
                ext.send("dis", object, user);
        }

    }

    private void cancelDisTimer(){
        if (disTimer != null){
            disTimer.cancel();
            disTimer = null;
        }
    }

    private void verifyDissolve() {
        int n = 0;//选择同意的人数
        int m = 0;//选择不同意的人数
        int l = 0;//未选择的人数
        for (ISeat s : seats) {
            if (!s.isEmpty()) {
                if (s.disCode == 1)
                    n++;
                else if (s.disCode == 2)
                    m++;
                else
                    l++;
            }
        }
        if ((10 * n) / getCurrentPerson() > 5) {//同意人数大于70%,解散房间
            dissolver = null;
            dissolve = false;
            cancelDisTimer();
            ext.getApi().removeRoom(room);
        } else if (((10 * m) / getCurrentPerson() >= 5) || (l == 0)) {
            dissolver = null;
            dissolve = false;
           cancelDisTimer();
            for (ISeat s : seats) {
                if (!s.isEmpty())
                    s.disCode = 0;
            }
            ext.send("ec", null, room.getUserList());
        }
    }

    public abstract void balance();

    public void disConnected(Player player) {
        if (isExists(player)){
            Constant.offlinePlayer.put(player.getUserid(),player);
            ISFSObject object = new SFSObject();
            object.putInt("uid",player.getUserid());
            object.putInt("seat", getSeatNo(player));
            ext.send("offline",object,room.getUserList());
        }
    }

    public String getUuid() {
        return uuid;
    }

    public ISeat[] getSeats() {
        return seats;
    }

    public void requestJoin(Player p){
        ISFSObject object = new SFSObject();
        if (isGameStart()){
            object.putInt("err", 3);//游戏已经开始
            ext.send(Command.JOIN_ROOM, object, p.getUser());
        }else if (p.getRoom() != null && p.getRoom() != room) {
            object.putInt("err", 4);//已经在别的房间了
            ext.send(Command.JOIN_ROOM, object, p.getUser());
        } else if (room.containsUser(p.getUser())) {
            object.putInt("err", 5);//已经在房间中了
            ext.send(Command.JOIN_ROOM, object, p.getUser());
        } else if (getCurrentPerson() == getMaxPerson()) {
            object.putInt("err", 6);//房间满了
            ext.send(Command.JOIN_ROOM, object, p.getUser());
        } else {
            try {
                ext.getApi().joinRoom(p.getUser(), room,null,false,p.getUser().getLastJoinedRoom());
            } catch (SFSJoinRoomException e) {
                object.putInt("err", 7);//加入失败
                ext.send(Command.JOIN_ROOM, object, p.getUser());
            }
        }
    }

    private Timer timer = null;
    protected void delayAutoReady(){
        if (timer != null){
            timer.cancel();
            timer = null;
        }
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                for (ISeat seat:seats){
                    if (!seat.isReady()){
                        playerReady(seat.getPlayer());
                    }
                }
            }
        },30*1000);
    }

    public synchronized void checkRoom() {
    }
}
