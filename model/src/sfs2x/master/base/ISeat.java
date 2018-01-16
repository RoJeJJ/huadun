package sfs2x.master.base;

import sfs2x.Constant;
import sfs2x.master.Player;


public abstract class ISeat {
    protected boolean isEmpty;
    private int no;
    protected Player mPlayer;
    protected boolean offline;
    private int score;//房卡房的分数
    private boolean ready;
    private ISeat next;
    protected int disCode;

    public int getSwl() {
        return swl;
    }

    private int swl;
    private int twl;

    public int getTwl() {
        return twl;
    }

    public void upDateTwl(int add) {
        this.twl += add;
    }

    protected ISeat(int i){
        isEmpty = true;
        mPlayer = null;
        offline = false;
        score = 0;
        swl = 0;
        disCode = 0;
        ready = false;
        no = i;
        twl = 0;
    }

    public void initNewGame(){
        ready = false;
        swl = 0;
    }


    public void userLeave(){
        isEmpty = true;
        if (mPlayer != null) {
            mPlayer.setRoom(null);
            Constant.offlinePlayer.remove(mPlayer.getUserid());
        }
        mPlayer = null;
        offline = false;
        score = 0;
        ready = false;
        disCode = 0;
        swl = 0;
    }
    public ISeat getNext() {
        return next;
    }

    public void setNext(ISeat next) {
        this.next = next;
    }

    public boolean isEmpty() {
        return isEmpty;
    }

    public Player getPlayer() {
        return mPlayer;
    }

    public int getScore() {
        return score;
    }


    public boolean isReady() {
        return ready;
    }

    public void setReady(boolean ready) {
        this.ready = ready;
    }

    public void addScore(int s){
        score += s;
        swl += s;
    }

    public void subScore(int s){
        score -= s;
        swl -= s;
    }

    public int getNo() {
        return no;
    }

    public void addGold(int g){
        if (mPlayer != null){
            mPlayer.addGold(g);
            swl += g;
        }
    }
    public void subGold(int g){
        if (mPlayer != null){
            mPlayer.subGold(g);
            swl -= g;
        }
    }

    public boolean isOffline() {
        return offline;
    }
}
