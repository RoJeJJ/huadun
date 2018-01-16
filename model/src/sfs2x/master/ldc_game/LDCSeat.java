package sfs2x.master.ldc_game;

import sfs2x.master.base.ISeat;
import java.util.ArrayList;
import java.util.List;

public class LDCSeat extends ISeat {
    List<Integer> seen;
    List<Integer> blinds;
    boolean fold;
    boolean chipIn;
//    int lastChipIn;
//    int action;//0 def;1等待action 2 acition , -1 弃牌
    boolean startAction;
    int borrow;
    int seenPoint;
    int blindsPoint;
    List<Integer> bet;
    int cheat;

    public void initNewGame(){
        super.initNewGame();
        seen.clear();
        blinds.clear();
        bet.clear();
        fold = false;
        chipIn = false;
//        lastChipIn = 0;
//        action = 0;
        startAction = false;
        borrow = 0;
        seenPoint = 0;
        blindsPoint = 0;
    }
    LDCSeat(int i){
        super(i);
        seen = new ArrayList<>();
        blinds = new ArrayList<>();
        bet = new ArrayList<>();
        fold = false;
        chipIn = false;
//        action = 0;
        startAction = false;
        disCode = 0;
        borrow = 0;
        seenPoint = 0;
        blindsPoint = 0;
    }

    @Override
    public void userLeave(){
        super.userLeave();
        seen.clear();
        blinds.clear();
        fold =false;
        chipIn = false;
//        action = 0;
        startAction = false;
//        lastChipIn = 0;
        bet.clear();

        disCode = 0;
        borrow = 0;
        seenPoint = 0;
        blindsPoint = 0;
    }

    public int totalBet(){
        int  n= 0;
        for (int i=1;i<bet.size();i++)
            n +=bet.get(i);
        return n;
    }
}
