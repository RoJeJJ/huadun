package sfs2x.master.ddz_game;

public class PaiType {
    public static final int invalid = -1;//无效
    public static final int single = 0; //单张 1
    public static final int pair = 1;//对子 2
    public static final int series_pair = 2;//连对 >6
    public static final int three = 3;//三个 3
    public static final int three_single = 4;//三带一 4
    public static final int three_pair = 5;//三带一对 5
    public static final int plane = 6;//飞机 无翅膀
    public static final int plane_single = 7;//飞机带单张
    public static final int plane_pair = 8;//飞机带对
    public static final int bomb = 9;//炸弹 4
    public static final int four_two_single = 10;//四带二 6
    public static final int four_two_pair = 11;//四带两对 8
    public static final int super_bomb = 12;//王炸 2
    public static final int tractor = 13;//拖拉机 > 5

    int type;//牌型
    int count;//牌张数
    int assistant;//用于比牌大小

    PaiType(){
        type = invalid;
        count = 0;
    }

    public int[] toIntArr(){
        return new int[]{type,count,assistant};
    }
}
