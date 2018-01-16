package sfs2x;

import sfs2x.master.Player;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class Constant {
    public static boolean debug = true;
    public static final String PASSWORD = "*ruidi19930723#";
    public static String USERINFO_URI = "https://api.weixin.qq.com/sns/userinfo?access_token=%s&openid=%s";
    public static ConcurrentHashMap<Integer,Player> offlinePlayer = new ConcurrentHashMap<>();
    public static ArrayList<Integer> ROOM_NAMES = new ArrayList<>();

    //lock
    public static final String ROOM_NAME_LOCK = "room_name_lock";


    //开始游戏的等待时间
    public static final int WAIT_START = 30;
    //玩家下注等待时间
    public static final int BET_TIME = 20;



    public static final long DELAY_TIME_VS = 5000;
    public static final long DELAY_TIME_SELTT = 3000;

    //拉大车房间配置
    public static final int COUNT_LDC_1 = 30;
    public static final int COUNT_LDC_2 = 60;
    public static final int COUNT_LDC_3 = 90;

    public static final int COST_LDC_1 = 1;
    public static final int COST_LDC_2 = 2;
    public static final int COST_LDC_3 = 3;

    public static final int COUNT_DDZ_1 = 16;
    public static final int COST_DDZ_1 = 1;
    public static final int COUNT_DDZ_2 = 32;
    public static final int COST_DDZ_2 = 2;
}
