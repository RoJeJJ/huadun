package sfs2x.master.ddz_game;

import sfs2x.master.base.ISeat;

import java.util.*;


public class DDZSeat extends ISeat {
    List<Integer> hand;
    List<List<Integer>> playList;
    int callOpt; // -1 默认状态  0 不叫分 1叫1分 2 叫2分 3 叫3分
    boolean startCall; //开始叫分
    int action;
    PaiType playType;
    int identity;
    boolean maxCall;
    boolean trusteeship;
    List<ArrayList<Integer>> tips;

    DDZSeat(int i) {
        super(i);
        callOpt = -1;//
        startCall = false;
        action = 0;
        playType = new PaiType();
        hand = new ArrayList<>();
        identity = 0;
        playList = new ArrayList<>();
        maxCall = false;
        trusteeship = false;
    }

    @Override
    public void userLeave() {
        super.userLeave();
        identity = 0;
        hand.clear();
        playList.clear();
        callOpt = -1;
        startCall = false;
        action = 0;
        maxCall = false;
        trusteeship = false;
    }

    @Override
    public void initNewGame() {
        super.initNewGame();
        hand.clear();
        playList.clear();
        callOpt = -1;
        startCall = false;
        action = 0;
        playType = new PaiType();
        identity = 0;
        maxCall = false;
        trusteeship = false;
    }

    boolean verifyPlay(List<Integer> cards) {
        if (hand != null && hand.size() != 0) {
            ArrayList<Integer> copyHand = new ArrayList<>(hand);
            for (Integer c : cards) {
                if (copyHand.contains(c))
                    copyHand.remove(c);
                else
                    return false;
            }
        }
        return true;
    }

    public void removePlayCard(List<Integer> card) {
        for (Integer c : card)
            hand.remove(c);
    }

    public boolean analysePlay(List<Integer> cards) {
        Collections.sort(cards);
        int[] n = new int[21];
        for (int c : cards)
            n[c / 4]++;
        int c1 = 0, c2 = 0, c3 = 0, c4 = 0;
        for (int i : n) {
            if (i == 1)
                c1++;
            else if (i == 2)
                c2++;
            else if (i == 3)
                c3++;
            else if (i == 4)
                c4++;
        }
        //单张
        if (cards.size() == 1) {
            playType.type = PaiType.single;
            playType.count = 1;
            playType.assistant = cards.get(0) / 4;
            return true;
        }
        //一对
        if (cards.size() == 2) {
            if (c2 == 1) {
                playType.type = PaiType.pair;
                playType.count = 2;
                playType.assistant = cards.get(0) / 4;
                return true;
            } else if (cards.get(0) / 4 > 16 && cards.get(1) / 4 > 16) {
                playType.type = PaiType.super_bomb;
                playType.count = 2;
                playType.assistant = 0;
                return true;
            }
        }
        //三张
        if (cards.size() == 3) {
            if (c3 == 1) {
                playType.type = PaiType.three;
                playType.count = 3;
                playType.assistant = cards.get(0) / 4;
                return true;
            }
        }
        //四个 三带一 或者炸弹
        if (cards.size() == 4) {
            if (c1 == 1 && c3 == 1) {
                int x = 0;
                for (int i = 0; i < n.length; i++) {
                    if (n[i] == 3) {
                        x = i;
                        break;
                    }
                }
                playType.type = PaiType.three_single;
                playType.count = 4;
                playType.assistant = x;
                return true;
            } else if (c4 == 1) {
                playType.type = PaiType.bomb;
                playType.count = 4;
                playType.assistant = cards.get(0) / 4;
                return true;
            }
        }
        //三带一对
        if (cards.size() == 5) {
            if (c2 == 1 && c3 == 1) {
                int x = 0;
                for (int i = 0; i < n.length; i++) {
                    if (n[i] == 3) {
                        x = i;
                        break;
                    }
                }
                playType.type = PaiType.three_pair;
                playType.count = 5;
                playType.assistant = x;
                return true;
            }
        }
        //四带二
        if (cards.size() == 6) {
            if ((c1 == 2 && c4 == 1) || (c2 == 1 && c4 == 1)) {
                int x = 0;
                for (int i = 0; i < n.length; i++) {
                    if (n[i] == 4) {
                        x = i;
                        break;
                    }
                }
                playType.type = PaiType.four_two_single;
                playType.count = 6;
                playType.assistant = x;
                return true;
            }
        }
        //四带两对
        if (cards.size() == 8) {
            if ((c2 == 2 && c4 == 1)) {
                int x = 0;
                for (int i = 0; i < n.length; i++) {
                    if (n[i] == 4) {
                        x = i;
                        break;
                    }
                }
                playType.type = PaiType.four_two_pair;
                playType.count = 6;
                playType.assistant = x;
                return true;
            }
        }
        //连对
        if (c1 == 0 && c2 >= 3 && c3 == 0 && c4 == 0) {
            boolean straight = true;
            ArrayList<Integer> pairs = new ArrayList<>();
            for (int i = 0; i < n.length; i++) {
                if (n[i] == 2) {
                    pairs.add(i);
                }
            }
            for (int i = 1; i < pairs.size(); i++) {
                if (pairs.get(i - 1) + 1 != pairs.get(i)) {
                    straight = false;
                    break;
                }
            }
            if (straight) {
                playType.type = PaiType.series_pair;
                playType.count = cards.size();
                playType.assistant = Collections.max(pairs);
                return true;
            }
        }

        //飞机
        ArrayList<Integer> three = new ArrayList<>();
        for (int i = 0; i < n.length; i++) {
            if (n[i] >= 3)
                three.add(i);
        }
        if (three.size() >= 2) {
            for (int i = three.size(); i >= 2; i--) {
                for (int j = 0; j <= three.size() - i; j++) {
                    ArrayList<Integer> plane = new ArrayList<>();
                    plane.add(three.get(j));
                    for (int k = j+1; k < j + i; k++) {
                        if (three.get(k-1) + 1 == three.get(k))
                            plane.add(three.get(k));
                        else
                            break;
                    }
                    if (plane.size() == i) {
                        int[] temp = Arrays.copyOf(n, n.length);
                        for (int face : plane)
                            temp[face] = temp[face] - 3;
                        int t1 = 0, t2 = 0, t3 = 0, t4 = 0;
                        for (int t : temp) {
                            if (t == 1)
                                t1++;
                            else if (t == 2)
                                t2++;
                            else if (t == 3)
                                t3++;
                            else if (t == 4)
                                t4++;
                        }
                        if (t1 + t2 + t3 + t4 == 0) {
                            playType.type = PaiType.plane;
                            playType.count = cards.size();
                            playType.assistant = Collections.max(plane);
                            return true;
                        }else if (t1+t2*2+t3*3+t4*4 == i){
                            playType.type = PaiType.plane_single;
                            playType.count = cards.size();
                            playType.assistant = Collections.max(plane);
                            return true;
                        }else if (t1 == 0 && t3 == 0 && t2+t4*2 == i){
                            playType.type = PaiType.plane_pair;
                            playType.count = cards.size();
                            playType.assistant = Collections.max(plane);
                            return true;
                        }
                    }
                }
            }
        }
//        if ((c1 == 0 && c2 == 0 && c3 >= 2 && c4 == 0) ||(c1+c2*2+c4*4 == c3) || (c1 == 0 && c2+c4*2 == c3)){
//            boolean straight = true;
//            ArrayList<Integer> three = new ArrayList<>();
//            for (int i=0;i<n.length;i++){
//                if (n[i] == 3) {
//                    three.add(i);
//                }
//            }
//            for (int i=1;i<three.size();i++){
//                if (three.get(i-1)+1 != three.get(i)) {
//                    straight = false;
//                    break;
//                }
//            }
//            if (straight){
//                playType.type = PaiType.plane;
//                playType.count = cards.size();
//                playType.assistant = Collections.max(three);
//                return true;
//            }
//        }
        //顺子
        if (cards.size() >= 5 && c1 == cards.size()) {
            boolean straight = true;
            for (int i = 1; i < cards.size(); i++) {
                if (cards.get(i - 1) / 4 + 1 != cards.get(i) / 4) {
                    straight = false;
                    break;
                }
            }
            if (straight) {
                playType.type = PaiType.tractor;
                playType.count = cards.size();
                playType.assistant = Collections.max(cards) / 4;
                return true;
            }
        }
        return false;
    }

    boolean IsMaxCall() {
        boolean flag = false;
        if (hand.size() > 0) {
            if (hand.contains(18 * 4) && hand.contains(20 * 4))
                flag = true;
            else if (hand.contains(16 * 4) && hand.contains(16 * 4 + 1) && hand.contains(16 * 4 + 2) && hand.contains(16 * 4 + 3))
                flag = true;
        }
        return flag;
    }
}
