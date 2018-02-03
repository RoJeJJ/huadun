package sfs2x.master.ldc_game;

import com.smartfoxserver.v2.entities.Room;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSArray;
import com.smartfoxserver.v2.entities.data.SFSObject;
import org.apache.commons.lang.math.RandomUtils;
import sfs2x.Constant;
import sfs2x.master.Player;
import sfs2x.master.base.ISeat;
import sfs2x.master.base.ITable;
import sfs2x.utils.DBUtil;
import sfs2x.utils.Utils;

import java.util.*;

/**
 * 拉大车table 类
 */
public class LDCTable extends ITable implements Runnable {
    private static final long ACTION_WAIT_TIME = 20*1000;
    private int base;
    private LDCStatus status;
    private List<Integer> pokers;
    private int max;
    private int pot;
    private int turn;
    private LDCSeat current = null;
    private LDCSeat winner = null;
    private ISeat first;
    private ArrayList<LDCSeat> dealOrder;
    private ArrayList<LDCSeat> actionOrder;
    private final int spo;
    private final int[] face;
    private long actionWaitRemain;
    private long actionWaitStart;
    private int lastBet;
    private int curBet;
    private LDCSeat firstAction;

    public LDCTable(int count, int base, boolean cardRoom,int spo) {
        super.count = count;
        this.base = base;
        super.cardRoom = cardRoom;
        if (count == Constant.COUNT_LDC_1)
            cost = Constant.COST_LDC_1;
        else if (count == Constant.COUNT_LDC_2)
            cost = Constant.COST_LDC_2;
        else
            cost = Constant.COST_LDC_3;
        this.spo = spo;
        if (spo == 9)
            face =   new int[]{ 9, 10, 11, 12, 13, 15};
        else
             face = new int[]{8, 9, 10, 11, 12, 13, 15};
        start = false;
        gameStart = false;
        takeOff = false;
        status = LDCStatus.checkStart;
        playSeat = new ArrayList<>();
        pokers = new ArrayList<>();
        curCount = 0;
        turn = 0;
        max = 10;
        dealOrder = new ArrayList<>();
        actionOrder = new ArrayList<>();
        mod = 0;
        lastBet = 0;
        curBet = 0;
        shuffle();
        cheatPokers = new ArrayList<>();
    }

    @Override
    protected boolean readyToStart() {
        int n = 0;
        int m = 0;
        for (ISeat s : seats) {
            if (!s.isEmpty()) {
                n++;
                if (s.isReady())
                    m++;
            }
        }
        return n >= 2 && m == n;
    }

    private void shuffle(){
        // 初始化扑克
        pokers.clear();
        for (int aFace : face) {
            for (int j = 0; j < 4; j++) {
                pokers.add(aFace * 4 + j);
            }
        }
        //洗牌
        for (int i = 0; i < pokers.size(); i++) {
            int k = RandomUtils.nextInt(pokers.size());
            int temp = pokers.get(i);
            pokers.set(i, pokers.get(k));
            pokers.set(k, temp);
        }
    }

    private void startGame() {

        if (winner != null && playSeat.contains(winner))
            first = winner;
        else {
            int i = RandomUtils.nextInt(playSeat.size());
            first = playSeat.get(i);
        }
        pot = 0;
        curCount++;
        turn = 0;
        record = new StringBuffer("|");
    }

    @Override
    public synchronized void run() {
        try {
            switch (status) {
                case checkStart:
                    if (start)
                        status = LDCStatus.readyStart;
                    break;
                case readyStart:
                    startGame();
                    sendStartGame();
                    status = LDCStatus.addBaseScore;
                    break;
                case addBaseScore:
                    if (cardRoom) { //房卡房
                        for (ISeat s : playSeat) {
                            LDCSeat seat = (LDCSeat) s;
                            seat.subScore(base);
                            seat.bet.add(base);
                            pot += base;
                        }
                    } else { //金币房
                        for (ISeat s : playSeat) {
                            LDCSeat seat = (LDCSeat) s;
                            Player p = s.getPlayer();
                            p.subGold(base);
                            seat.bet.add(base);
                            pot += base;
                        }
                    }
                    sendAddBase();
                    status = LDCStatus.deal;
                    break;
                case deal:
                    sendDeal();
                    status = LDCStatus.action;
                    break;
                case action:
                    if (current == null) {
                        startNewTurn();
                        actionOrder.clear();
                        current = getMaxFace();
                        firstAction = current;
                    }
                    if (allFoldExcept(current)) { //场上只有一名玩家了,该玩家胜出
                        winner = current;
                        status = LDCStatus.settlement;
                        break;
                    }
                    actionOrder.add(current);
                    if (turn == 1 && firstAction == current && current.seen.get(current.seen.size() - 1)/4 == 15){ //如果是第一轮下注,并且,最后的牌的A,自动最大下注
                        userAction(current.getPlayer(),max,true);
                        status = LDCStatus.waitAction;
                        actionWaitRemain = ACTION_WAIT_TIME;
                        actionWaitStart = System.currentTimeMillis();
                        break;
                    }
                    current.startAction = true;
                    status = LDCStatus.waitAction;
                    actionWaitRemain = ACTION_WAIT_TIME;
                    sendUserAction(current);
                    actionWaitStart = System.currentTimeMillis();
                    break;
                case waitAction:
                    actionWaitRemain = ACTION_WAIT_TIME - (System.currentTimeMillis() - actionWaitStart);
                    if (actionWaitRemain < 0)
                        actionWaitRemain = 0;
                    if (actionWaitRemain == 0) {
                        userAction(current.getPlayer(),0,true);
                    }
                    if (!current.startAction) { //
                        if (current.fold )
                            actionOrder.remove(current);
                        if (allChipIn()){
                            if (actionOrder.size() == 1){
                                winner = actionOrder.get(0);
                                status = LDCStatus.settlement;
                                break;
                            }
                            if (turn == 3)
                                status = LDCStatus.bipai;
                            else {
                                current = null;
                                Utils.delay(800);
                                sendDeal();
                                status = LDCStatus.action;
                            }
                        }else {
                            if (current == firstAction && current.fold ) {
                                current = getMaxFace();
                                firstAction = current;
                                status = LDCStatus.action;
                            }else {
                                current = getNext(current);
                                status = LDCStatus.action;
                            }
                        }
                    }
                    break;
                case bipai:
                    winner = getWinner();
                    status = LDCStatus.settlement;
                    break;
                case settlement:
                    Utils.delay(800);
                    winner.addScore(pot);
                    pot = 0;
                    sendWinner();
                    DBUtil.gameRecord(room);
                    if (cardRoom) {
                        if (count == curCount)
                            status = LDCStatus.end;
                        else {
                            readyNewGame();
//                            delayAutoReady();
                        }
                    } else {
                        readyNewGame();
//                        delayAutoReady();
                    }
                    break;
                case end:
                    ext.getApi().removeRoom(room);
                    status = LDCStatus.def;
                    break;
            }
            Utils.delay(10);
        } catch (Exception e) {
            ext.trace(e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean allFoldExcept(LDCSeat current){
        for (ISeat seat:playSeat){
            LDCSeat s = (LDCSeat) seat;
            if (s != current){
                if (!s.fold)
                    return false;
            }
        }
        return true;
    }

    private boolean allChipIn(){
        for (ISeat s:playSeat){
            LDCSeat seat = (LDCSeat) s;
            if (!seat.fold && !seat.chipIn)
                return false;
        }
        return true;
    }

    private void readyNewGame() {
        status = LDCStatus.checkStart;
        current = null;
        lastBet = 0;
        curBet = 0;
        turn = 0;
        start = false;
        cheatPokers.clear();
        for (ISeat seat : seats) {
            LDCSeat s = (LDCSeat) seat;
            s.initNewGame();
        }
        shuffle();
    }

    private void sendAddBase() {
        ISFSObject object = new SFSObject();
        object.putInt("pot", pot);
        object.putInt("b", base);
        ISFSArray array = new SFSArray();
        for (ISeat s : playSeat) {
            LDCSeat seat = (LDCSeat) s;
            ISFSObject o = new SFSObject();
            o.putInt("uid", seat.getPlayer().getUserid());
            if (cardRoom) {
                o.putLong("s", seat.getScore());// 分数
            } else
                o.putLong("s", seat.getPlayer().getGold());
            o.putInt("bs", seat.totalBet());//已下注
            array.addSFSObject(o);
        }
        object.putSFSArray("ab", array);
        ext.send("ab", object, room.getUserList());
        record.append("0,-1,").append(base).append("|");
    }

    private void sendWinner() {
        ISFSObject object = new SFSObject();
        object.putInt("wid", winner.getPlayer().getUserid());
        object.putInt("t",30);
        record.append("3,").append(winner.getNo()).append(",").append(winner.getSwl()).append("|");
        ISFSArray array = new SFSArray();
        for (ISeat seat : playSeat) {
            LDCSeat s = (LDCSeat) seat;
            Player p = s.getPlayer();
            ISFSObject o = new SFSObject();
            o.putInt("uid", p.getUserid());
            o.putUtfString("n", p.getNickname());
            o.putInt("p", s.seenPoint);
            o.putIntArray("h", s.seen);
            o.putBool("f", s.fold);
            if (cardRoom) {
                o.putLong("s", s.getScore());
            } else {
                o.putLong("s", s.getPlayer().getGold());
            }
            o.putInt("wl", s.getSwl());
            s.upDateTwl(s.getSwl());
            array.addSFSObject(o);
        }
        object.putSFSArray("p", array);
        ext.send("win", object, room.getUserList());
    }

    private LDCSeat getWinner() {
        LDCSeat max = null;
        for (ISeat a: playSeat) {
            LDCSeat aPlaySeat = (LDCSeat) a;
            if (max == null) {
                if (!aPlaySeat.isEmpty() && !aPlaySeat.fold)
                    max = aPlaySeat;
            } else {
                if (!aPlaySeat.isEmpty() && !aPlaySeat.fold) {
                    if (aPlaySeat.seenPoint > max.seenPoint)
                        max = aPlaySeat;
                }
            }
        }
        if (max != null) {
            ArrayList<LDCSeat> winners = new ArrayList<>();
            for (ISeat seat : playSeat) {
                LDCSeat s = (LDCSeat) seat;
                if (!s.isEmpty() && !s.fold && s.seenPoint == max.seenPoint)
                    winners.add(s);
            }
            if (winners.size() == 1)
                return winners.get(0);
            else {
                while (true) {
                    for (LDCSeat seat : actionOrder)
                        if (winners.contains(seat))
                            return seat;
                }
            }
        }
        throw new IllegalArgumentException("游戏错误,没有找到本局赢家!");
    }

    private void sendUserAction(LDCSeat current) {
        ISFSObject object = new SFSObject();
        object.putInt("uid", current.getPlayer().getUserid());
        object.putInt("turn", turn);
        object.putInt("lb",lastBet);
        object.putInt("cb",curBet);
        object.putLong("time", actionWaitRemain);//时间
        object.putInt("pot", pot);//奖金池
//        object.putInt("ucb", current.lastChipIn);//已经下了多少分
//        object.putBool("raise", lastChipIn < max && current.lastChipIn == 0);
//        object.putBool("follow", true);
//        object.putBool("fold", !current.fold);
        ext.send("action", object, room.getUserList());
    }

    private void startNewTurn() {
        turn++;
        if (turn > 1){
            lastBet = curBet;
            curBet = 0;
        }
        for (ISeat s : playSeat) {
            LDCSeat seat = (LDCSeat) s;
            if (!seat.fold) {
                seat.chipIn = false;
                seat.startAction = false;
            }
        }
    }

    private LDCSeat getNext(LDCSeat seat) {
        LDCSeat next = (LDCSeat) seat.getNext();
        while (!playSeat.contains(next) || next.fold) {
            next = (LDCSeat) next.getNext();
        }
        return next;
    }

    private LDCSeat getMaxFace() {
        LDCSeat seat = null;
        for (LDCSeat aPlaySeat : dealOrder) {
            if (!aPlaySeat.fold && !aPlaySeat.chipIn){
                if (seat == null) {
                    seat = aPlaySeat;
                } else {
                    if (aPlaySeat.seen.get(aPlaySeat.seen.size() - 1)/4 > seat.seen.get(seat.seen.size() - 1)/4)
                        seat = aPlaySeat;
                }
            }
        }
        return seat;
    }

    private void sendDeal() {
        StringBuilder sort = new StringBuilder();
        if (turn == 0) {
            dealOrder.clear();
            for (int i = 0; i < 3; i++) {
                LDCSeat seat = (LDCSeat) first;
                do {
                    if (playSeat.contains(seat) && !seat.fold) {
                        sort.append(seat.getNo());
                        if (!dealOrder.contains(seat))
                            dealOrder.add(seat);
                        int ranNum = pokers.remove(0);
                        record.append("1,").append(seat.getNo()).append(",").append(seat.borrow).append(",").append(ranNum).append("|");
                        seat.seen.add(ranNum);
                        if (i == 2) {
                            seat.blinds.add(ranNum);
                        } else
                            seat.blinds.add(-1);
                    }
                    seat = (LDCSeat) seat.getNext();
                } while (seat != first);
            }
        } else {
            LDCSeat maxFace = actionOrder.get(0);
            LDCSeat seat = maxFace;
            dealOrder.clear();
            int last = 0;
            do {
                if (playSeat.contains(seat) && !seat.fold) {
                    if (!dealOrder.contains(seat))
                        dealOrder.add(seat);
                    sort.append(seat.getNo());
                    int ranNum;
                    if (pokers.size() > 0) {
                        if (seat.cheat != 0 && pokers.contains(seat.cheat) && cheatPokers.contains(seat.cheat)){
                            ranNum = seat.cheat;
                            seat.cheat = 0;
                            pokers.remove(Integer.valueOf(ranNum));
                            cheatPokers.remove(Integer.valueOf(ranNum));
                        }else {
                            ArrayList<Integer> temps = new ArrayList<>(pokers);
                            temps.removeAll(cheatPokers);
                            if (temps.size() > 0) {
                                ranNum = temps.remove(0);
                                pokers.remove(Integer.valueOf(ranNum));
                            }else {
                                ranNum = pokers.remove(0);
//                                cheatPokers.remove(Integer.valueOf(ranNum));
                            }
                        }
                        if (pokers.size() == 0)
                            last = ranNum;
                    } else {
                        ranNum = last;
                        seat.borrow = last;
                    }
                    seat.seen.add(ranNum);
                    seat.blinds.add(ranNum);
                    record.append("1,").append(seat.getNo()).append(",").append(seat.borrow).append(",").append(ranNum).append("|");
                }
                seat = (LDCSeat) seat.getNext();
            } while (seat != maxFace);
        }

        point();

        for (ISeat is : playSeat) {
            LDCSeat seat = (LDCSeat) is;
            User user = seat.getPlayer().getUser();
            if (user != null) {
                ISFSObject object = new SFSObject();
                object.putUtfString("sort", sort.toString());
                ISFSArray array = new SFSArray();
                for (ISeat iss : playSeat) {
                    LDCSeat s = (LDCSeat) iss;
                    ISFSObject o = new SFSObject();
                    o.putInt("uid", s.getPlayer().getUserid());
                    o.putBool("isMe", seat.getPlayer() == s.getPlayer());
                    o.putInt("p", s.blindsPoint);
                    o.putInt("brw", s.borrow);
                    if (seat.getPlayer() == s.getPlayer())
                        o.putIntArray("h", s.seen);
                    else
                        o.putIntArray("h", s.blinds);
                    array.addSFSObject(o);
                }
                object.putSFSArray("p", array);
                ext.send("hand", object, user);
            }
        }
        Utils.delay((long) (1000+(0.1f * sort.length() + 0.5f) * 1000));
    }


    @Override
    public void bindRoom(Room room, int uid) {
        super.bindRoom(room, uid);
        for (int i = 0; i < seats.length; i++) {
            seats[i] = new LDCSeat(i);
        }
    }

    @Override
    public synchronized void playerReady(Player player) {
        super.playerReady(player);
    }

    private int getTotalBet(){
        int total = 0;
        for (ISeat s:playSeat){
            LDCSeat seat = (LDCSeat) s;
            total += seat.totalBet();
        }
        return total;
    }


    @Override
    public ISFSObject tableStatus(Player player) {
        ISFSObject object = super.tableStatus(player);
        object.putInt("base", base);
        object.putInt("pot", pot); //底池
        object.putInt("lb",lastBet);
        object.putInt("cb",curBet);
        object.putInt("spo",spo);
        object.putInt("tableBet",getTotalBet());
        ISFSArray array = new SFSArray();
        for (ISeat seat : seats) {
            LDCSeat s = (LDCSeat) seat;
            if (!s.isEmpty()) {
                Player p = s.getPlayer();
                ISFSObject o = p.toSFSObject();
                o.putBool("off", s.isOffline());
                o.putBool("ready", s.isReady());
                o.putInt("seat", getSeatNo(p));//玩家座位
                o.putBool("bh", s.blinds.size() != 0);//是否有手牌
                o.putBool("isMe", p == player);
                o.putInt("pot", pot);
                o.putBool("fold", s.fold);
                if (s.blinds.size() != 0) {//有手牌
                    if (player == p) {//是玩家自己
                        o.putIntArray("h", s.seen);
                    } else //是别人
                        o.putIntArray("h", s.blinds);
                    o.putInt("brw", s.borrow);
                    o.putInt("bp", s.blindsPoint);
                }
                o.putIntArray("bl", s.bet);
                o.putInt("seatBet",s.totalBet());
                if (cardRoom) {
                    o.putLong("s", s.getScore()); // 房卡房,分数
                } else {
                    o.putLong("s", s.getPlayer().getGold());
                }
                array.addSFSObject(o);
            }
        }
        object.putSFSArray("p", array);
        return object;
    }

    @Override
    public void reconnect(Player player) {
        switch (status) {
            case waitAction:
                sendUserAction(current);
                break;
        }
    }


    /**
     * 玩家加入房间
     *
     * @param player 玩家
     */
    @Override
    public synchronized void join(Player player) {
        super.join(player);
    }

    @Override
    public void balance() {
        if (pot != 0) {
            if (cardRoom) {
                for (ISeat seat : playSeat)
                    seat.subScore(seat.getSwl());
            }
        }
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
                    }else {
                        o.putLong("s", seat.getPlayer().getGold());
                    }
                    o.putInt("wl",seat.getTwl());
                    array.addSFSObject(o);
                }
            }
            object.putSFSArray("p", array);
            ext.send("balance", object, room.getUserList());
        }
    }

    @Override
    public synchronized void requestJoin(Player p) {
        super.requestJoin(p);
    }


    @Override
    public synchronized void destroy() {
        super.destroy();
    }

    @Override
    public synchronized void dissolve(Player player, boolean dis) {
        super.dissolve(player, dis);
    }

    @Override
    public synchronized void leaveRoom(Player player) {
        super.leaveRoom(player);
    }

    private void point() {
        for (ISeat s : playSeat) {
            LDCSeat seat = (LDCSeat) s;
            if (!seat.fold) {
                int sp = 0;
                int bp = 0;
                int[] sc = new int[face.length];
                int[] bc = new int[face.length];
                for (int i = 0; i < seat.seen.size(); i++) {
                    int sn = seat.seen.get(i) / 4;
                    int bn = seat.blinds.get(i) / 4;
                    if (seat.seen.get(i) != seat.borrow){
                        for (int j=0;j<face.length;j++)
                            if (sn == face[j])
                                sc[j]++;
//                        if (sn == 8)
//                            sc[0]++;
//                        else if (sn == 9)
//                            sc[1]++;
//                        else if (sn == 10)
//                            sc[2]++;
//                        else if (sn == 11)
//                            sc[3]++;
//                        else if (sn == 12)
//                            sc[4]++;
//                        else if (sn == 13)
//                            sc[5]++;
//                        else if (sn == 15)
//                            sc[6]++;
                    }
                    if (seat.blinds.get(i) != seat.borrow){
                        for (int j=0;j<face.length;j++)
                            if (bn == face[j])
                                bc[j]++;
//                        if (bn == 8)
//                            bc[0]++;
//                        else if (bn == 9)
//                            bc[1]++;
//                        else if (bn == 10)
//                            bc[2]++;
//                        else if (bn == 11)
//                            bc[3]++;
//                        else if (bn == 12)
//                            bc[4]++;
//                        else if (bn == 13)
//                            bc[5]++;
//                        else if (bn == 15)
//                            bc[6]++;
                    }
                }
                for (int n : sc) {
                    if (n == 3)
                        sp += 30;
                    else if (n == 4)
                        sp += 60;
                }
                for (int n : bc) {
                    if (n == 3)
                        bp += 30;
                    else if (n == 4)
                        bp += 60;
                }

                for (int n : seat.seen)
                    sp += n / 4;
                for (int n : seat.blinds)
                    bp += n / 4;
                seat.seenPoint = sp;
                seat.blindsPoint = bp;
            }
        }
    }

    public synchronized void userAction(Player p,int bet,boolean server) {
        LDCSeat seat = (LDCSeat) getSeat(p);
        if (server || (status == LDCStatus.waitAction && current == seat && seat.startAction)){
            if (bet == 0){
                seat.fold = true;
                seat.chipIn = true;
            }else {
                if (lastBet == 0) {
                    if (curBet == 0) {
                        if (bet >= base && bet <= max) {
                            curBet = bet;
                            seat.bet.add(bet);
                            seat.chipIn = true;
                            if (cardRoom) {
                                seat.subScore(bet);
                                pot += bet;
                            }
                        }
                    } else {
                        if (curBet == bet) {
                            seat.bet.add(bet);
                            if (cardRoom) {
                                seat.subScore(bet);
                                pot += bet;
                            }
                            seat.chipIn = true;
                        }
                    }
                } else {
                    if (curBet == 0) {
                        if (bet >= lastBet && bet <= max) {
                            curBet = bet;
                            seat.bet.add(bet);
                            if (cardRoom) {
                                seat.subScore(bet);
                                pot += bet;
                            }
                            seat.chipIn = true;
                        }
                    } else {
                        if (curBet == bet) {
                            if (cardRoom) {
                                seat.subScore(bet);
                                pot += bet;
                            }
                            seat.bet.add(bet);
                            seat.chipIn = true;
                        }
                    }
                }
            }
            if (seat.chipIn){
                record.append("2,").append(seat.getNo()).append(",").append(bet).append(",").append(seat.totalBet()).append("|");
                seat.startAction = false;
                ISFSObject object = new SFSObject();
                object.putInt("uid",p.getUserid());
                object.putInt("lb",lastBet);
                object.putInt("cb",curBet);
                object.putInt("tableBet",getTotalBet());
                if (cardRoom)
                    object.putLong("s",seat.getScore());
                else
                    object.putLong("s",p.getGold());
                object.putInt("pot",pot);
                object.putInt("seatBet",seat.totalBet());
                if (seat.fold)
                    object.putInt("bet",0);
                else
                    object.putInt("bet",seat.bet.get(seat.bet.size() - 1));
                ext.send("bet",object,room.getUserList());
            }
        }
    }

    public synchronized void getVV(Player p) {
        User user = p.getUser();
        if (user != null) {
            ISFSObject object = new SFSObject();
            ArrayList<Integer> temps = new ArrayList<>(pokers);
            temps.removeAll(cheatPokers);
            object.putIntArray("p", temps);
            ext.send("vv", object, user);
        }
    }

    private ArrayList<Integer> cheatPokers;

    public synchronized void setV(Player p,int po) {
        User user = p.getUser();
        LDCSeat seat = (LDCSeat) getSeat(p);
        if (start){
            if (!seat.fold && pokers.contains(po) && !cheatPokers.contains(po)){
                cheatPokers.remove(Integer.valueOf(seat.cheat));
                seat.cheat = po;
                cheatPokers.add(po);
                if (user != null){
                    ISFSObject object = new SFSObject();
                    object.putInt("err",0);
                    ext.send("v",object,user);
                }
                return;
            }
        }
        ISFSObject object = new SFSObject();
        object.putInt("err",1);
        ext.send("v",object,user);
    }
}
