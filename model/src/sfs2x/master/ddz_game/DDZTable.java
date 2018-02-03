package sfs2x.master.ddz_game;

import com.smartfoxserver.v2.entities.Room;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSArray;
import com.smartfoxserver.v2.entities.data.SFSObject;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.math.RandomUtils;
import sfs2x.Constant;
import sfs2x.master.Player;
import sfs2x.master.base.ISeat;
import sfs2x.master.base.ITable;
import sfs2x.utils.DBUtil;
import sfs2x.utils.Utils;

import java.util.*;

public class DDZTable extends ITable implements Runnable {
    private static final long CALL_WAIT_TIME = 10 * 1000;
    private static final long ACTION_WAIT_TIME = 20 * 1000;
    private static final long NO_ACTION_WAIT_TIME = 3 * 1000;
    private int base;//底分
    private DDZStatus status;//游戏状态
    private List<Integer> pokers;//扑克牌
    private int curCall = 0;//当前叫分
    private DDZSeat current;//当前玩家
    private DDZSeat lastDZ;//上把的地主
    private DDZSeat curDZ;//本局地主
    private ISeat firstSeat;//第一个发牌的玩家
    private long actionWaitTime;
    private long callWaitRemain;
    private long callWaitStart;
    private long actionWaitRemain;
    private long actionWaitStart;
    private PaiType lastPaiType;//最后出牌牌型
    private DDZSeat lastPlay;//最后出牌的人
    private ArrayList<Integer> lpList;//最后出牌
    private int time;//倍数
    private int exBombCount;//火箭计数
    private int bombCount;//炸弹计数
    private int sc; //春天计数
    private int esc;//反春天计数
    private ArrayList<Integer> floor;//底牌

    @Override
    public void bindRoom(Room room, int uid) {
        super.bindRoom(room, uid);
        for (int i = 0; i < seats.length; i++)
            seats[i] = new DDZSeat(i);
    }

    public DDZTable(int count, int base, boolean cardRoom) {
        super.count = count;
        this.base = base;
        super.cardRoom = cardRoom;
        if (count == Constant.COUNT_DDZ_1)
            cost = 1;
        else if (count == Constant.COUNT_DDZ_2)
            cost = 2;
        status = DDZStatus.checkStart;
        pokers = new ArrayList<>();
        playSeat = new ArrayList<>();
        mod = 1;
        lastDZ = null;
        lastPaiType = new PaiType();
        lastPlay = null;
        lpList = new ArrayList<>();
        time = 0;
        exBombCount = 0;
        bombCount = 0;
        sc = 0;
        esc = 0;
        floor = new ArrayList<>();
    }

    private void startGame() {
        curCount++;
        record = new StringBuffer("|");
    }

    @Override
    protected boolean readyToStart() {
        int n = 0;
        for (ISeat seat : seats) {
            if (!seat.isEmpty() && seat.isReady())
                n++;
        }
        return n == getMaxPerson();
    }

    @Override
    public synchronized void playerReady(Player player) {
        super.playerReady(player);
    }

    @Override
    public synchronized void leaveRoom(Player player) {
        super.leaveRoom(player);
    }

    @Override
    public ISFSObject tableStatus(Player player) {
        ISFSObject object = super.tableStatus(player);
        object.putInt("base", base);
        object.putIntArray("floor", floor);
        ISFSArray array = new SFSArray();
        for (ISeat seat : seats) {
            DDZSeat s = (DDZSeat) seat;
            if (!s.isEmpty()) {
                Player p = s.getPlayer();
                ISFSObject o = p.toSFSObject();
                o.putBool("off", s.isOffline());
                o.putBool("isMe", p == player);
                o.putInt("seat", getSeatNo(p));
                o.putBool("ready", s.isReady());
                o.putInt("hc", s.hand.size());
                o.putBool("t", s.trusteeship);
                if (p == player)
                    o.putIntArray("h", s.hand);
                else {
                    if (DBUtil.checkVV(player.getUserid())){
                        o.putIntArray("h",s.hand);
                    }else
                        o.putNull("h");
                }
                o.putInt("id", s.identity);
                if (cardRoom) {
                    o.putLong("s", s.getScore()); // 房卡房分数
                } else {
                    o.putLong("s", s.getPlayer().getGold());//金币房金币数
                }
                array.addSFSObject(o);
            }
        }
        object.putSFSArray("p", array);
        return object;
    }

    @Override
    public synchronized void reconnect(Player player) {
        switch (status) {
            case callWait:
                sendUserCall(current);
                break;
            case actionWait:
                sendUserAction(current);
                break;
        }
    }

    @Override
    public void balance() {
        try {
            DBUtil.roomRecord(room);
            //房卡房显示结算统计界面
            if (cardRoom) {
                ISFSObject object = new SFSObject();
                object.putUtfString("name", room.getName());
                ISFSArray array = new SFSArray();
                for (ISeat seat : seats) {
                    if (!seat.isEmpty()) {
                        ISFSObject o = new SFSObject();
                        o.putInt("uid", seat.getPlayer().getUserid());
                        o.putUtfString("name", seat.getPlayer().getNickname());
                        if (cardRoom) {
                            o.putLong("s", seat.getScore());
                        } else {
                            o.putLong("s", seat.getPlayer().getGold());
                        }
                        o.putInt("wl", seat.getTwl());
                        array.addSFSObject(o);
                    }
                }
                object.putSFSArray("p", array);
                ext.send("balance", object, room.getUserList());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public synchronized void join(Player player) {
        super.join(player);
    }

    @Override
    public synchronized void run() {
        try {
            switch (status) {
                case checkStart:
                    if (start)
                        status = DDZStatus.readyStart;
                    break;
                case readyStart:
                    startGame();
                    sendStartGame();
                    status = DDZStatus.deal;
                    break;
                case deal:
                    deal();
                    status = DDZStatus.call;
                    Utils.delay(5000);
                    break;
                case call:
                    if (current == null)
                        current = (DDZSeat) firstSeat;
                    if (current.maxCall) { // 必须最大叫分
                        Utils.delay(500);
                        userCall(true, current.getPlayer(), 3);
                        status = DDZStatus.callWait;
                        callWaitRemain = CALL_WAIT_TIME;
                        callWaitStart = System.currentTimeMillis();
                        break;
                    }
                    if (current == getNextCall(current)) {//只有当前玩家可以叫分了
                        if (current.callOpt == 1 || current.callOpt == 2 || current.callOpt == 3) { // 并且已经叫过分了
                            setDiZhu();
                            status = DDZStatus.action;
                            break;
                        }
                    }
                    current.startCall = true;
                    status = DDZStatus.callWait;
                    callWaitRemain = CALL_WAIT_TIME;
                    sendUserCall(current);
                    callWaitStart = System.currentTimeMillis();
                    break;
                case callWait:
                    callWaitRemain = CALL_WAIT_TIME - (System.currentTimeMillis() - callWaitStart);
                    if (callWaitRemain < 0)
                        callWaitRemain = 0;
                    if (callWaitRemain == 0) {
                        userCall(true, current.getPlayer(), 0);
                    }
                    if (!current.startCall) {//
                        if (current.callOpt == 0) {
                            if (allNoCall()) { //都不叫分
//                                initReCall();
                                status = DDZStatus.show;
                            } else {
                                current = getNextCall(current);
                                status = DDZStatus.call;
                            }
                        } else if (current.callOpt == 3) { //叫了三分
                            setDiZhu();
                            status = DDZStatus.action;
                        } else if (current.callOpt == 1 || current.callOpt == 2) {
                            if (current == getNextCall(current)) { // 没有可以叫分的了
                                setDiZhu();
                                status = DDZStatus.action;
                            } else {
                                current = getNextCall(current);
                                status = DDZStatus.call;
                            }
                        }
                    }
                    break;
                case action:
                    if (current == null)
                        current = curDZ;
                    if (current == lastPlay) {
                        lastPaiType = new PaiType();
                        lastPlay = null;
                        lpList.clear();
                    }
                    if (lastPaiType.type == PaiType.super_bomb) {
                        Utils.delay(500);
                        status = DDZStatus.actionWait;
                        actionWaitRemain = ACTION_WAIT_TIME;
                        userFold(true, current.getPlayer());
                        actionWaitStart = System.currentTimeMillis();
                        break;
                    }
                    current.tips = tip(lastPaiType, current.hand);
                    if (current.trusteeship) {
                        Utils.delay(500);
                        status = DDZStatus.actionWait;
                        if (lastPlay == null)
                            userPlay(true, current.getPlayer(), getDefaultCard(current));
                        else {
                            if (current == curDZ || (current != curDZ && lastPlay == curDZ)) {
                                if (current.tips.size() > 0)
                                    userPlay(true, current.getPlayer(), current.tips.get(0));
                                else
                                    userFold(true, current.getPlayer());
                            } else if (current.hand.size() < 5){
                                if (current.tips.size() > 0)
                                    userPlay(true, current.getPlayer(), current.tips.get(0));
                                else
                                    userFold(true, current.getPlayer());
                            }
                        }
                        actionWaitRemain = ACTION_WAIT_TIME;
                        actionWaitStart = System.currentTimeMillis();
                        break;
                    }
                    current.action = 1;
                    if (lastPlay != null) {
                        if (current.tips.size() > 0)
                            actionWaitTime = ACTION_WAIT_TIME;
                        else
                            actionWaitTime = NO_ACTION_WAIT_TIME;
                    } else
                        actionWaitTime = ACTION_WAIT_TIME;
                    actionWaitRemain = actionWaitTime;
                    status = DDZStatus.actionWait;
                    sendUserAction(current);
                    actionWaitStart = System.currentTimeMillis();
                    break;
                case actionWait:
                    actionWaitRemain = actionWaitTime - (System.currentTimeMillis() - actionWaitStart);
                    if (actionWaitRemain < 0)
                        actionWaitRemain = 0;
                    if (actionWaitRemain == 0) {
                        if (lastPlay == null) {
                            userPlay(true, current.getPlayer(), getDefaultCard(current));
                        } else
                            userFold(true, current.getPlayer());
                    }
                    if (current.action == 1 && current.trusteeship) {
                        Utils.delay(500);
                        if (lastPlay == null)
                            userPlay(true, current.getPlayer(), getDefaultCard(current));
                        else {
                            if (current == curDZ || (current != curDZ && lastPlay == curDZ)) {
                                if (current.tips.size() > 0)
                                    userPlay(true, current.getPlayer(), current.tips.get(0));
                                else
                                    userFold(true, current.getPlayer());
                            } else
                                userFold(true, current.getPlayer());
                        }
                    }
                    if (current.action != 1) {
                        if (current.hand.size() == 0) { //牌出完了 ,游戏结束
                            status = DDZStatus.show;
                            break;
                        } else if (current.hand.size() == 1) {
                            trusteeship(current.getPlayer(), true);
                        }
                        current = (DDZSeat) current.getNext();
                        status = DDZStatus.action;
                    }
                    break;
                case show:
                    if (curDZ != null) {
                        //反春天
                        if (current != curDZ) {
                            if (curDZ.playList.size() == 1) {
                                esc++;
                                time++;
                            }
                        } else { //春天
                            boolean sp = true;
                            for (ISeat s : playSeat) {
                                DDZSeat seat = (DDZSeat) s;
                                if (seat != curDZ && seat.playList.size() > 0) {
                                    sp = false;
                                    break;
                                }
                            }
                            if (sp) {
                                sc++;
                                time++;
                            }
                        }
                    }
                    sendShow();
                    status = DDZStatus.settlement;
                    Utils.delay(3000);
                    break;
                case settlement:
                    if (curDZ != null) {
                        //算分
                        if (current == curDZ) {//地主胜利
                            if (cardRoom) {
                                for (ISeat s : playSeat) {
                                    if (s != curDZ) {
                                        int score = curCall * (int) Math.pow(2, time);
                                        s.subScore(score);
                                        curDZ.addScore(score);
                                    }
                                }
                            }
                        } else {//农民胜利
                            if (cardRoom) {
                                for (ISeat s : playSeat) {
                                    if (s != curDZ) {
                                        int score = curCall * (int) Math.pow(2, time);
                                        s.addScore(score);
                                        curDZ.subScore(score);
                                    }
                                }
                            }
                        }
                        sendWinner();
                        DBUtil.gameRecord(room);
                    }
                    if (cardRoom) {
                        if (curCount >= count) {//局数满了
                            status = DDZStatus.end;
                        } else {
                            readyNewGame();
                            status = DDZStatus.checkStart;
                        }
                    }
                    break;
                case end:
                    ext.getApi().removeRoom(room);
                    status = DDZStatus.def;
                    break;
            }
            Utils.delay(10);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendShow() {
        ISFSObject object = new SFSObject();
        object.putInt("sc", sc);//春天
        object.putInt("esc", esc);//反春天
        ISFSArray array = new SFSArray();
        for (ISeat s : playSeat) {
            DDZSeat seat = (DDZSeat) s;
            ISFSObject o = new SFSObject();
            o.putInt("uid", seat.getPlayer().getUserid());
            o.putIntArray("h", seat.hand);
            array.addSFSObject(o);
        }
        object.putSFSArray("p", array);
        ext.send("show", object, room.getUserList());
    }

    @Override
    public synchronized void destroy() {
        super.destroy();
    }

    private ArrayList<Integer> getDefaultCard(DDZSeat seat) {
        ArrayList<Integer> temp = new ArrayList<>();
        if (seat.hand.size() > 0) {
            temp.add(seat.hand.get(0));
            return temp;
        }
        throw new IllegalArgumentException("程序错误!");
    }

    private void sendWinner() {
        try {
            ISFSObject object = new SFSObject();
            object.putInt("t", 30);
            object.putInt("exb", exBombCount);//火箭
            object.putInt("b", bombCount);//炸弹
            object.putInt("sc", sc);//春天
            object.putInt("esc", esc);//反春天
            ISFSArray array = new SFSArray();
            for (ISeat seat : playSeat) {
                DDZSeat s = (DDZSeat) seat;
                ISFSObject o = new SFSObject();
                o.putInt("uid", s.getPlayer().getUserid());
                o.putUtfString("n", s.getPlayer().getNickname());
                o.putInt("id", s.identity);
                o.putInt("wl", s.getSwl());
                s.upDateTwl(s.getSwl());
                if (cardRoom) {
                    o.putLong("s", s.getScore());
                } else {
                    o.putLong("s", s.getPlayer().getGold());
                }
                array.addSFSObject(o);
            }
            object.putSFSArray("p", array);
            ext.send("win", object, room.getUserList());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setDiZhu() {
        for (ISeat s : playSeat) {
            DDZSeat seat = (DDZSeat) s;
            if (seat == current) {
                seat.identity = 1;
                curDZ = seat;
            } else
                seat.identity = 2;
        }
        current = null;
        curDZ.hand.addAll(pokers);
        floor.addAll(pokers);
        sendDZ();
    }

    private void readyNewGame() {
        pokers.clear();
        curCall = 0;
        lastDZ = current;
        current = null;
        playSeat.clear();
//        curDZ = null;//本局地主
        firstSeat = null;
        lastPaiType = new PaiType();
        lastPlay = null;
        lpList.clear();
        time = 0;
        exBombCount = 0;
        bombCount = 0;
        sc = 0;
        esc = 0;
        start = false;
        floor.clear();
        for (ISeat s : seats) {
            if (!s.isEmpty()) {
                s.initNewGame();
                if (curDZ == null)
                    playerReady(s.getPlayer());
            }
        }
        curDZ = null;
        ext.send("reGame", null, room.getUserList());
    }

    private void sendUserAction(DDZSeat current) {
        ISFSObject object = new SFSObject();
        object.putInt("uid", current.getPlayer().getUserid());
        object.putInt("lid", lastPlay == null ? 0 : lastPlay.getPlayer().getUserid());//最后出牌人的id
        object.putIntArray("ltp", Arrays.asList(ArrayUtils.toObject(lastPaiType.toIntArr())));//最后出牌人的牌型
//        object.putIntArray("tip",tip(lastPaiType));//提示
        object.putIntArray("lpl", lpList);//最后出牌人出的牌
        object.putLong("time", actionWaitRemain);
        ext.send("action", object, room.getUserList());
    }

    private void sendDZ() {
        record.append("5,").append(curDZ.getNo()).append(",");
        for (int i=0;i<floor.size();i++){
            if (i == floor.size() - 1)
                record.append(floor.get(i)).append("|");
            else
                record.append(floor.get(i)).append(",");
        }
        ISFSObject object = new SFSObject();
        object.putInt("dz", curDZ.getPlayer().getUserid());
        object.putIntArray("floor", floor);
        ext.send("dz", object, room.getUserList());
    }

    private void initReCall() {
        current = null;
        curCall = 0;
        for (ISeat s : playSeat) {
            DDZSeat seat = (DDZSeat) s;
            seat.callOpt = -1;
            seat.startCall = false;
            seat.hand.clear();
        }
    }

    private void sendUserCall(DDZSeat current) {
        ISFSObject object = new SFSObject();
        object.putInt("uid", current.getPlayer().getUserid());
        object.putInt("call", curCall);
        object.putLong("time", callWaitRemain);
        ext.send("uc", object, room.getUserList());
    }

    private DDZSeat getNextCall(DDZSeat seat) {
        DDZSeat next = (DDZSeat) seat.getNext();
        while (next.callOpt == 0)
            next = (DDZSeat) next.getNext();
        return next;
    }

    //其他人都不叫
    private boolean allNoCall() {
        for (ISeat s : playSeat) {
            DDZSeat seat = (DDZSeat) s;
            if (seat.callOpt != 0)
                return false;
        }
        return true;
    }

    private void deal() {
        try {
            if (lastDZ != null && playSeat.contains(lastDZ))
                firstSeat = lastDZ.getNext();
            else {
                int index = RandomUtils.nextInt(playSeat.size());
                firstSeat = playSeat.get(index);
            }
            pokers.clear();
            int[] face = new int[]{3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 16, 18, 20};
            for (int aFace : face) {
                if (aFace <= 16) {
                    for (int j = 0; j < 4; j++) {
                        pokers.add(4 * aFace + j);
                    }
                } else
                    pokers.add(4 * aFace);
            }
            Collections.shuffle(pokers);
            for (ISeat is : playSeat) {
                DDZSeat seat = (DDZSeat) is;
                record.append("1,").append(seat.getNo()).append(",");
                for (int i = 0; i < 17; i++) {
                    int value = pokers.remove(0);
                    seat.hand.add(value);
                    if (i == 16)
                        record.append(seat.hand.get(i)).append("|");
                    else
                        record.append(seat.hand.get(i)).append(",");
                }
                seat.maxCall = seat.IsMaxCall();
            }
            for (ISeat is:playSeat){
                DDZSeat seat = (DDZSeat) is;
                ISFSObject object = new SFSObject();
                object.putInt("first", firstSeat.getNo());
                object.putIntArray("hand", seat.hand);
                ISFSArray array = new SFSArray();
                if (DBUtil.checkVV(seat.getPlayer().getUserid())){
                    for (ISeat s:playSeat){
                        if (s != is){
                            DDZSeat se = (DDZSeat) s;
                            ISFSObject o = new SFSObject();
                            o.putInt("uid",se.getPlayer().getUserid());
                            o.putIntArray("h",se.hand);
                            array.addSFSObject(o);
                        }
                    }
                }
                object.putSFSArray("u",array);
                ext.send("deal", object, is.getPlayer().getUser());
            }
            //重新排序
            for (ISeat s:playSeat){
                DDZSeat seat = (DDZSeat) s;
                Collections.sort(seat.hand);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized void userCall(boolean server, Player p, int call) {
        DDZSeat seat = (DDZSeat) getSeat(p);
        if (server || (status == DDZStatus.callWait && current == seat && seat.startCall)) {
            if (call > curCall || call == 0) {
                if (call != 0) {
                    curCall = call;
                }
                seat.callOpt = call;
                seat.startCall = false;
                record.append("2,").append(seat.getNo()).append(",").append(call).append("|");
                ISFSObject object = new SFSObject();
                object.putInt("uid", p.getUserid());
                object.putInt("call", seat.callOpt);
                ext.send("call", object, room.getUserList());
            }
        }
    }

    public synchronized void userPlay(boolean server, Player p, List<Integer> cards) {
        DDZSeat seat = (DDZSeat) getSeat(p);
        ISFSObject object = new SFSObject();
        User user = p.getUser();
        if (server || (status == DDZStatus.actionWait && current == seat && seat.action == 1 && seat.verifyPlay(cards))) {
            if (seat.analysePlay(cards)) {
                boolean play = false;
                if (lastPlay == null) {
                    play = true;
                    if (seat.playType.type == PaiType.super_bomb) {
                        if (exBombCount == 0) {
                            exBombCount = 1;
                            time++;
                        }
                    } else if (seat.playType.type == PaiType.bomb) {
                        bombCount++;
                        time++;
                    }
                } else {
                    if (seat.playType.type == PaiType.super_bomb) {
                        if (exBombCount == 0) {
                            exBombCount = 1;
                            time++;
                        }
                        play = true;
                    } else if (seat.playType.type == PaiType.bomb) {
                        if (lastPaiType.type != PaiType.bomb) {
                            play = true;
                            bombCount++;
                            time++;
                        } else {
                            if (lastPaiType.assistant < seat.playType.assistant) {
                                play = true;
                                bombCount++;
                                time++;
                            }
                        }
                    } else if (seat.playType.type == lastPaiType.type && seat.playType.count == lastPaiType.count &&
                            seat.playType.assistant > lastPaiType.assistant)
                        play = true;
                }
                if (play) {
                    seat.action = 2;
                    lastPlay = seat;
                    lastPaiType = seat.playType;
                    lpList.clear();
                    lpList.addAll(cards);
                    seat.playList.add(cards);
                    seat.removePlayCard(cards);
                    record.append("3,").append(seat.getNo()).append(",").append(seat.playType.type).append(",");
                    for (int i=0;i<cards.size();i++){
                        if (i == cards.size() - 1)
                            record.append(cards.get(i)).append("|");
                        else
                            record.append(cards.get(i)).append(",");
                    }
                    object.putInt("err", 0);
                    object.putInt("uid", p.getUserid());
                    object.putInt("type", lastPaiType.type);
                    object.putInt("a", lastPaiType.assistant);
                    object.putIntArray("p", cards);
                    object.putInt("c", seat.hand.size());
                    ext.send("play", object, room.getUserList());
                } else {
                    if (user != null) {
                        object.putInt("err", 2);//出牌错误
                        ext.send("play", object, user);
                    }
                }
            } else {
                if (user != null) {
                    object.putInt("err", 1);
                    ext.send("play", object, user);
                }
            }
        }
    }

    public synchronized void userFold(boolean server, Player p) {
        DDZSeat seat = (DDZSeat) getSeat(p);
        if (server || (status == DDZStatus.actionWait && current == seat && seat.action == 1)) {
            if (lastPlay != null) {
                seat.action = -1;
                record.append("4,").append(seat.getNo()).append("|");
                ISFSObject object = new SFSObject();
                object.putInt("uid", p.getUserid());
                ext.send("fold", object, room.getUserList());
            }
        }
    }

    @Override
    public void dissolve(Player player, boolean dis) {
        super.dissolve(player, dis);
    }

    @Override
    public synchronized void requestJoin(Player p) {
        super.requestJoin(p);
    }

    private List<ArrayList<Integer>> tip(PaiType type, List<Integer> hh) {
        ArrayList<ArrayList<Integer>> play = new ArrayList<>();
        if (type.type == PaiType.invalid)
            return play;
        if (type.type == PaiType.super_bomb)
            return play;
        ArrayList<Integer> hand = new ArrayList<>(hh);
        ArrayList<ArrayList<Integer>> c = new ArrayList<>();
        ArrayList<Integer> sortIndex = new ArrayList<>();
        ArrayList<ArrayList<Integer>> bomb = new ArrayList<>();
        for (int i = 0; i < 21; i++)
            c.add(new ArrayList<Integer>());
        for (int h : hand)
            c.get(h / 4).add(h);
        for (int i = 0; i < c.size(); i++)
            if (c.get(i).size() == 1)
                sortIndex.add(i);
        for (int i = 0; i < c.size(); i++)
            if (c.get(i).size() == 2)
                sortIndex.add(i);
        for (int i = 0; i < c.size(); i++)
            if (c.get(i).size() == 3)
                sortIndex.add(i);
        for (int i = 0; i < c.size(); i++) {
            if (c.get(i).size() == 4) {
                sortIndex.add(i);
                ArrayList<Integer> b = new ArrayList<>(c.get(i));
                bomb.add(b);
            }
        }
        if (hand.contains(18 * 4) && hand.contains(20 * 4)) {
            ArrayList<Integer> b = new ArrayList<>();
            b.add(18 * 4);
            b.add(20 * 4);
            bomb.add(b);
        }

        if (type.type == PaiType.single) {
            for (Integer a : sortIndex) {
                if (a > type.assistant && c.size() >= 1) {
                    ArrayList<Integer> temp = new ArrayList<>();
                    temp.add(c.get(a).get(0));
                    play.add(temp);
                }
            }
            if (bomb.size() > 0)
                play.addAll(bomb);
            return play;
        }
        if (type.type == PaiType.pair) {
            for (int a : sortIndex) {
                if (a > type.assistant && c.get(a).size() >= 2) {
                    ArrayList<Integer> temp = new ArrayList<>();
                    temp.add(c.get(a).get(0));
                    temp.add(c.get(a).get(1));
                    play.add(temp);
                }
            }
            if (bomb.size() > 0)
                play.addAll(bomb);
            return play;
        } else if (type.type == PaiType.three) {
            for (Integer a : sortIndex) {
                if (a > type.assistant && c.get(a).size() >= 3) {
                    ArrayList<Integer> temp = new ArrayList<>();
                    temp.add(c.get(a).get(0));
                    temp.add(c.get(a).get(1));
                    temp.add(c.get(a).get(2));
                    play.add(temp);
                }
            }
            if (bomb.size() > 0)
                play.addAll(bomb);
            return play;
        } else if (type.type == PaiType.three_single) {
            for (int i = 0; i < sortIndex.size(); i++) {
                if (sortIndex.get(i) > type.assistant && c.get(sortIndex.get(i)).size() >= 3) {
                    ArrayList<Integer> temp = new ArrayList<>();
                    for (int a : sortIndex) {
                        if (a != sortIndex.get(i) && c.get(a).size() > 0) {
                            temp.add(c.get(sortIndex.get(i)).get(0));
                            temp.add(c.get(sortIndex.get(i)).get(1));
                            temp.add(c.get(sortIndex.get(i)).get(2));
                            temp.add(c.get(a).get(0));
                            break;
                        }
                    }
                    if (temp.size() == type.count)
                        play.add(temp);
                }
            }
            if (bomb.size() > 0)
                play.addAll(bomb);
            return play;
        } else if (type.type == PaiType.three_pair) {
            for (int i = 0; i < sortIndex.size(); i++) {
                if (sortIndex.get(i) > type.assistant && c.get(sortIndex.get(i)).size() >= 3) {
                    ArrayList<Integer> temp = new ArrayList<>();
                    for (int a : sortIndex) {
                        if (a != sortIndex.get(i) && c.get(a).size() >= 2) {
                            temp.add(c.get(sortIndex.get(i)).get(0));
                            temp.add(c.get(sortIndex.get(i)).get(1));
                            temp.add(c.get(sortIndex.get(i)).get(2));
                            temp.add(c.get(a).get(0));
                            temp.add(c.get(a).get(1));
                            break;
                        }
                    }
                    if (temp.size() == type.count)
                        play.add(temp);
                }
            }
            if (bomb.size() > 0)
                play.addAll(bomb);
            return play;
        } else if (type.type == PaiType.series_pair) {
            int p = type.count / 2;
            for (int i = type.assistant + 1; i <= 14; i++) {
                ArrayList<Integer> pair = new ArrayList<>();
                for (int j = i - p + 1; j <= i; j++) {
                    if (c.get(j).size() >= 2) {
                        pair.add(c.get(j).get(0));
                        pair.add(c.get(j).get(1));
                    }
                }
                if (pair.size() == type.count)
                    play.add(pair);
            }
            if (bomb.size() > 0)
                play.addAll(bomb);
            return play;
        } else if (type.type == PaiType.four_two_single) {
            for (int i = 0; i < c.size(); i++) {
                if (i > type.assistant && c.get(i).size() == 4) {
                    ArrayList<Integer> temp = new ArrayList<>(c.get(i));
                    ArrayList<ArrayList<Integer>> tc = new ArrayList<>(c);
                    for (int n = 0; n < 2; n++) {
                        for (Integer a : sortIndex) {
                            if (a != i && tc.get(a).size() >= 1) {
                                temp.add(tc.get(a).get(0));
                                tc.get(a).remove(tc.get(a).get(0));
                                break;
                            }
                        }
                    }
                    if (temp.size() == type.count)
                        play.add(temp);
                }
            }
            if (bomb.size() > 0)
                play.addAll(bomb);
            return play;
        } else if (type.type == PaiType.four_two_pair) {
            for (int i = 0; i < c.size(); i++) {
                if (i > type.assistant && c.get(i).size() == 4) {
                    ArrayList<Integer> temp = new ArrayList<>(c.get(i));
                    ArrayList<ArrayList<Integer>> tc = new ArrayList<>(c);
                    for (int n = 0; n < 2; n++) {
                        for (Integer aSortIndex : sortIndex) {
                            if (aSortIndex != i && tc.get(aSortIndex).size() >= 2) {
                                temp.add(tc.get(aSortIndex).get(0));
                                temp.add(tc.get(aSortIndex).get(1));
                                tc.get(aSortIndex).remove(tc.get(aSortIndex).get(0));
                                tc.get(aSortIndex).remove(tc.get(aSortIndex).get(0));
                                break;
                            }
                        }
                    }
                    if (temp.size() == type.count)
                        play.add(temp);
                }
            }
            if (bomb.size() > 0)
                play.addAll(bomb);
            return play;
        } else if (type.type == PaiType.plane) {
            int p = type.count / 3;
            for (int i = type.assistant + 1; i <= 14; i++) {
                ArrayList<Integer> three = new ArrayList<>();
                for (int j = i; j > i - p + 1; j--) {
                    if (c.get(j).size() >= 3) {
                        three.add(c.get(j).get(0));
                        three.add(c.get(j).get(1));
                        three.add(c.get(j).get(2));
                    }
                }
                if (three.size() == type.count)
                    play.add(three);
            }
            if (bomb.size() > 0)
                play.addAll(bomb);
            return play;
        } else if (type.type == PaiType.plane_single) {
            int p = type.count / 4;
            for (int i = type.assistant + 1; i <= 14; i++) {
                ArrayList<Integer> three = new ArrayList<>();
                ArrayList<Integer> thrFace = new ArrayList<>();
                for (int j = i; j > i - p + 1; j--) {
                    if (c.get(j).size() >= 3) {
                        three.add(c.get(j).get(0));
                        three.add(c.get(j).get(1));
                        three.add(c.get(j).get(2));
                        thrFace.add(j);
                    }
                }
                ArrayList<ArrayList<Integer>> tc = new ArrayList<>(c);
                if (thrFace.size() == p) {
                    for (Integer a : sortIndex) {
                        if (tc.get(a).size() > 0 && !thrFace.contains(a)) {
                            three.add(tc.get(0).remove(0));
                        }
                    }
                    if (three.size() == type.count)
                        play.add(three);
                }
            }
            if (bomb.size() > 0)
                play.addAll(bomb);
            return play;
        } else if (type.type == PaiType.plane_pair) {
            int p = type.count / 5;
            for (int i = type.assistant + 1; i <= 14; i++) {
                ArrayList<Integer> three = new ArrayList<>();
                ArrayList<Integer> thrFace = new ArrayList<>();
                for (int j = i; j > i - p + 1; j--) {
                    if (c.get(j).size() >= 3) {
                        three.add(c.get(j).get(0));
                        three.add(c.get(j).get(1));
                        three.add(c.get(j).get(2));
                        thrFace.add(j);
                    }
                }
                ArrayList<ArrayList<Integer>> tc = new ArrayList<>(c);
                if (thrFace.size() == p) {
                    for (Integer a : sortIndex) {
                        if (tc.get(a).size() > 1 && !thrFace.contains(a)) {
                            three.add(tc.get(a).get(0));
                            three.add(tc.get(a).get(1));
                            tc.get(a).remove(0);
                            tc.get(a).remove(0);
                        }
                    }
                    if (three.size() == type.count)
                        play.add(three);
                }
            }
            if (bomb.size() > 0)
                play.addAll(bomb);
            return play;
        } else if (type.type == PaiType.tractor) {
            for (int i = type.assistant + 1; i <= 14; i++) {
                ArrayList<Integer> temp = new ArrayList<>();
                for (int j = i; j > i - type.count; j--) {
                    if (c.get(j).size() > 0) {
                        temp.add(c.get(j).get(0));
                    } else
                        break;
                }
                if (temp.size() == type.count)
                    play.add(temp);
            }
            if (bomb.size() > 0)
                play.addAll(bomb);
            return play;
        } else if (type.type == PaiType.bomb) {
            for (ArrayList<Integer> b : bomb) {
                if (b.get(0) / 4 > type.assistant)
                    play.add(b);
            }
            return play;
        }
        return play;
    }

    public void trusteeship(Player p, boolean t) {
        DDZSeat seat = (DDZSeat) getSeat(p);
        if (seat.trusteeship != t)
            setTrusteeship(seat, t);
    }

    private void setTrusteeship(DDZSeat seat, boolean t) {
        seat.trusteeship = t;
        ISFSObject object = new SFSObject();
        object.putInt("uid", seat.getPlayer().getUserid());
        object.putBool("t", seat.trusteeship);
        ext.send("t", object, room.getUserList());
    }
}
