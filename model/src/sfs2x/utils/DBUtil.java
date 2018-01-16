package sfs2x.utils;

import com.smartfoxserver.v2.db.IDBManager;
import com.smartfoxserver.v2.entities.Room;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSArray;
import com.smartfoxserver.v2.entities.data.SFSObject;
import sfs2x.master.base.ITable;

import java.sql.*;

public class DBUtil {
    private static IDBManager dbManager ;

    public static void initDB(IDBManager manager){
        dbManager = manager;
    }

    public static Connection getConnection(){
        Connection conn = null;
        try {
            conn =  dbManager.getConnection();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return conn;
    }

    public static void close(Connection conn,Statement statement,ResultSet set){
            try {
                if (conn != null)
                    conn.close();
                if (statement != null)
                    statement.close();
                if (set != null)
                    set.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
    }

//    public static void getConfig() {
//        Connection conn = null;
//        PreparedStatement statement = null;
//        ResultSet set = null;
//        try {
//            conn = DBUtil.getConnection();
//            if (conn == null)
//                return;
//            statement = conn.prepareStatement("SELECT * FROM JGroup");
//            set = statement.executeQuery();
//            while (set.next()){
//                int groupID = set.getInt("GroupID");
//                String sign = set.getString("Sign");
//                String groupName = set.getString("GroupName");
//                long minimum = set.getLong("Minimum");
//                long maximum = set.getLong("Maximum");
//                long limit = set.getLong("Limit");
//                Group group = new Group(groupID,sign,groupName,minimum,maximum,limit);
//                Constant.groups.add(group);
//            }
//
//        }catch (SQLException e){
//            e.printStackTrace();
//        }finally {
//            DBUtil.close(conn,statement,set);
//        }
//    }

    public static void signIn(int userid){
        Connection conn = null;
        CallableStatement stmt = null;
        try {
            conn = getConnection();
            if (conn != null){
                stmt = conn.prepareCall("{?=call RecordSignIn(?)}");
                stmt.registerOutParameter(1,Types.INTEGER);
                stmt.setInt(2,userid);
                stmt.execute();
            }
        }catch (SQLException e){
            e.printStackTrace();
        }finally {
            close(conn,stmt,null);
        }
    }

    public static void signOut(int userid){
        Connection conn = null;
        CallableStatement stmt = null;
        try {
            conn = getConnection();
            if (conn != null){
                stmt = conn.prepareCall("{?=call RecordSignOut(?)}");
                stmt.registerOutParameter(1,Types.INTEGER);
                stmt.setInt(2,userid);
                stmt.execute();
            }
        }catch (SQLException e){
            e.printStackTrace();
        }finally {
            close(conn,stmt,null);
        }
    }

    public static boolean lockCard(int userid,long card){
        Connection conn = null;
        CallableStatement stmt = null;
        try {
            conn = getConnection();
            if (conn != null){
                stmt = conn.prepareCall("{?=call lockCard(?,?)}");
                stmt.registerOutParameter(1,Types.INTEGER);
                stmt.setInt(2,userid);
                stmt.setLong(3,card);
               stmt.execute();
                return stmt.getInt(1) == 0;
            }
            return false;
        }catch (SQLException e){
            e.printStackTrace();
            return false;
        }finally {
            close(conn,stmt,null);
        }
    }

    public static void unLockCard(int userid,int cost){
        Connection connection = null;
        PreparedStatement stmt = null;
        try {
            connection = getConnection();
            if (connection != null) {
                stmt = connection.prepareStatement("UPDATE user_info SET lockcard = lockcard - " + cost + " WHERE userid=" + userid);
                stmt.execute();
            }
        }catch (SQLException e){
            e.printStackTrace();
        }finally {
            close(connection,stmt,null);
        }
    }

    public static long costCard(String uuid,int userid,int cost){
        Connection conn = null;
        CallableStatement stmt= null;
        ResultSet set = null;
        try {
            conn = getConnection();
            if (conn != null){
                stmt = conn.prepareCall("{?=call cost_card (?,?,?)}");
                stmt.registerOutParameter(1,Types.INTEGER);
                stmt.setString(2,uuid);
                stmt.setLong(3,userid);
                stmt.setLong(4,cost);
                if (stmt.execute()){
                    set = stmt.getResultSet();
                    if (set.next())
                        return set.getLong("card");
                }
            }
            return -1;
        }catch (SQLException e){
            e.printStackTrace();
            return -1;
        }finally {
            close(conn,stmt,set);
        }
    }

    public static long getCard(int userid){
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet set = null;
        try {
            conn = getConnection();
            if (conn != null){
                stmt = conn.prepareStatement("SELECT card FROM user_info WHERE userid="+userid);
                if (stmt.execute()){
                    set = stmt.getResultSet();
                    if (set.next())
                        return set.getLong("card");
                }
            }
            return -1;
        }catch (SQLException e){
            return -1;
        }finally {
            close(conn,stmt,set);
        }
    }
    public static void newRoom(String name,String uuid,int mod,int cost,int owner){
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = getConnection();
            if (conn != null){
                stmt = conn.prepareStatement("INSERT INTO card_room(roomName,uuid,create_time,mod, owner,cost,deducted,destroyed) VALUES (?,?,?,?,?,?,?,?)");
                stmt.setString(1,name);
                stmt.setString(2,uuid);
                stmt.setString(3,new Timestamp(System.currentTimeMillis()).toString());
                stmt.setInt(4,mod);
                stmt.setInt(5,owner);
                stmt.setInt(6,cost);
                stmt.setInt(7,0);
                stmt.setInt(8,0);
                stmt.execute();
            }
        }catch (SQLException e){
            e.printStackTrace();
        }finally {
            close(conn,stmt,null);
        }
    }
    public static void roomDestroy(String uuid){
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = getConnection();
            if (conn != null){
                stmt = conn.prepareStatement("UPDATE card_room SET destroyed = 1 WHERE uuid='"+uuid+"'");
                stmt.execute();
            }
        }catch (SQLException e){
            e.printStackTrace();
        }finally {
            close(conn,stmt,null);
        }
    }
    public static ISFSObject userRoom(int uid,int page){
        Connection conn = null;
        CallableStatement stmt = null;
        ResultSet set = null;
        int realPage = page;
        int num = 0;
        ISFSObject object = new SFSObject();
        ISFSArray array = new SFSArray();
        try {
            conn = getConnection();
            if (conn != null){
                stmt = conn.prepareCall("{call query_room (?,?,?)}");
                stmt.registerOutParameter(1,Types.INTEGER);
                stmt.setInt(1,0);
                stmt.registerOutParameter(2,Types.INTEGER);
                stmt.setInt(2,page);
                stmt.setInt(3,uid);
                if (stmt.execute()){
                    set = stmt.getResultSet();
                    if (set.next()){
                        do {
                            ISFSObject o = new SFSObject();
                            o.putUtfString("n",String.valueOf(set.getInt("roomName")));//房间号
                            o.putInt("mod",set.getInt("mod"));//游戏类型
                            o.putUtfString("ct",set.getString("create_time"));//创建时间
                            o.putInt("cost",set.getInt("cost"));//花费
                            o.putInt("deducted",set.getInt("deducted"));//是否扣费
                            o.putInt("destroyed",set.getInt("destroyed"));//是否解散
                            array.addSFSObject(o);
                        }while (set.next());
                    }
                }
                num = stmt.getInt(1);
                realPage = stmt.getInt(2);
            }
            object.putSFSArray("r",array);
            object.putInt("num",num);
            object.putInt("page",realPage);
            return object;
        }catch (SQLException e){
            e.printStackTrace();
            return object;
        }finally {
            close(conn,stmt,set);
        }
    }
    public static void gameRecord(Room room){
        ITable table = Utils.getTable(room);
        Connection conn = null;
        PreparedStatement stm = null;
        try {
            conn = getConnection();
            if (conn != null){
                stm = conn.prepareStatement("INSERT INTO dbo.gameRecord(name,uuid,mod,u0,a0,s0,wl0,u1,a1,s1,wl1,u2,a2,s2,wl2,u3,a3,s3,wl3,u4,a4,s4,wl4,u5,a5,s5,wl5,record,create_time,count)" +
                        "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
                stm.setString(1,room.getName());
                stm.setString(2,table.getUuid());
                stm.setInt(3,table.getMod());
                //0号位
                stm.setInt(4,table.getSeats().length > 0?
                        table.getSeats()[0].isEmpty()?0:table.getSeats()[0].getPlayer().getUserid():0);
                stm.setString(5,table.getSeats().length > 0?
                        table.getSeats()[0].isEmpty()?"":table.getSeats()[0].getPlayer().getFaceurl():"");
                stm.setString(6,table.getSeats().length > 0?
                        table.getSeats()[0].isEmpty()?"":table.getSeats()[0].getPlayer().getNickname():"");
                stm.setInt(7,table.getSeats().length > 0?
                        table.getSeats()[0].isEmpty()?0:table.getSeats()[0].getSwl():0);
                //1号位
                stm.setInt(8,table.getSeats().length > 1?
                        table.getSeats()[1].isEmpty()?0:table.getSeats()[1].getPlayer().getUserid():0);
                stm.setString(9,table.getSeats().length > 1?
                        table.getSeats()[1].isEmpty()?"":table.getSeats()[1].getPlayer().getFaceurl():"");
                stm.setString(10,table.getSeats().length > 1?
                        table.getSeats()[1].isEmpty()?"":table.getSeats()[1].getPlayer().getNickname():"");
                stm.setInt(11,table.getSeats().length > 1?
                        table.getSeats()[1].isEmpty()?0:table.getSeats()[1].getSwl():0);
                //2号位
                stm.setInt(12,table.getSeats().length > 2?
                        table.getSeats()[2].isEmpty()?0:table.getSeats()[2].getPlayer().getUserid():0);
                stm.setString(13,table.getSeats().length > 2?
                        table.getSeats()[2].isEmpty()?"":table.getSeats()[2].getPlayer().getFaceurl():"");
                stm.setString(14,table.getSeats().length > 2?
                        table.getSeats()[2].isEmpty()?"":table.getSeats()[2].getPlayer().getNickname():"");
                stm.setInt(15,table.getSeats().length > 2?
                        table.getSeats()[2].isEmpty()?0:table.getSeats()[2].getSwl():0);
                //3号位
                stm.setInt(16,table.getSeats().length > 3?
                        table.getSeats()[3].isEmpty()?0:table.getSeats()[3].getPlayer().getUserid():0);
                stm.setString(17,table.getSeats().length > 3?
                        table.getSeats()[3].isEmpty()?"":table.getSeats()[3].getPlayer().getFaceurl():"");
                stm.setString(18,table.getSeats().length > 3?
                        table.getSeats()[3].isEmpty()?"":table.getSeats()[3].getPlayer().getNickname():"");
                stm.setInt(19,table.getSeats().length > 3?
                        table.getSeats()[3].isEmpty()?0:table.getSeats()[3].getSwl():0);
                //4号位
                stm.setInt(20,table.getSeats().length > 4?
                        table.getSeats()[4].isEmpty()?0:table.getSeats()[4].getPlayer().getUserid():0);
                stm.setString(21,table.getSeats().length > 4?
                        table.getSeats()[4].isEmpty()?"":table.getSeats()[4].getPlayer().getFaceurl():"");
                stm.setString(22,table.getSeats().length > 4?
                        table.getSeats()[4].isEmpty()?"":table.getSeats()[4].getPlayer().getNickname():"");
                stm.setInt(23,table.getSeats().length > 4?
                        table.getSeats()[4].isEmpty()?0:table.getSeats()[4].getSwl():0);
                //5号位
                stm.setInt(24,table.getSeats().length > 5?
                        table.getSeats()[5].isEmpty()?0:table.getSeats()[5].getPlayer().getUserid():0);
                stm.setString(25,table.getSeats().length > 5?
                        table.getSeats()[5].isEmpty()?"":table.getSeats()[5].getPlayer().getFaceurl():"");
                stm.setString(26,table.getSeats().length > 5?
                        table.getSeats()[5].isEmpty()?"":table.getSeats()[5].getPlayer().getNickname():"");
                stm.setInt(27,table.getSeats().length > 5?
                        table.getSeats()[5].isEmpty()?0:table.getSeats()[5].getSwl():0);

                stm.setString(28,table.getRecord());
                stm.setString(29,new Timestamp(System.currentTimeMillis()).toString());
                stm.setInt(30,table.getCurCount());
                stm.execute();
            }
        }catch (SQLException e){
            e.printStackTrace();
        }finally {
            close(conn,stm,null);
        }
    }
    public static void roomRecord(Room room){
        ITable table = Utils.getTable(room);
        Connection conn = null;
        PreparedStatement stm = null;
        try {
            conn = getConnection();
            if (conn != null){
                stm = conn.prepareStatement("INSERT INTO dbo.roomRecord(name,uuid,mod,u0,s0,wl0,u1,s1,wl1,u2,s2,wl2,u3,s3,wl3,u4,s4,wl4,u5,s5,wl5,create_time,tcount,ccount)" +
                        "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
                stm.setString(1,room.getName());
                stm.setString(2,table.getUuid());
                stm.setInt(3,table.getMod());
                //0号位
                stm.setInt(4,table.getSeats().length > 0?
                        table.getSeats()[0].isEmpty()?0:table.getSeats()[0].getPlayer().getUserid():0);
                stm.setString(5,table.getSeats().length > 0?
                        table.getSeats()[0].isEmpty()?"":table.getSeats()[0].getPlayer().getNickname():"");
                stm.setInt(6,table.getSeats().length > 0?
                        table.getSeats()[0].isEmpty()?0:table.getSeats()[0].getTwl():0);
                //1号位
                stm.setInt(7,table.getSeats().length > 1?
                        table.getSeats()[1].isEmpty()?0:table.getSeats()[1].getPlayer().getUserid():0);
                stm.setString(8,table.getSeats().length > 1?
                        table.getSeats()[1].isEmpty()?"":table.getSeats()[1].getPlayer().getNickname():"");
                stm.setInt(9,table.getSeats().length > 1?
                        table.getSeats()[1].isEmpty()?0:table.getSeats()[1].getTwl():0);
                //2号位
                stm.setInt(10,table.getSeats().length > 2?
                        table.getSeats()[2].isEmpty()?0:table.getSeats()[2].getPlayer().getUserid():0);
                stm.setString(11,table.getSeats().length > 2?
                        table.getSeats()[2].isEmpty()?"":table.getSeats()[2].getPlayer().getNickname():"");
                stm.setInt(12,table.getSeats().length > 2?
                        table.getSeats()[2].isEmpty()?0:table.getSeats()[2].getTwl():0);
                //3号位
                stm.setInt(13,table.getSeats().length > 3?
                        table.getSeats()[3].isEmpty()?0:table.getSeats()[3].getPlayer().getUserid():0);
                stm.setString(14,table.getSeats().length > 3?
                        table.getSeats()[3].isEmpty()?"":table.getSeats()[3].getPlayer().getNickname():"");
                stm.setInt(15,table.getSeats().length > 3?
                        table.getSeats()[3].isEmpty()?0:table.getSeats()[3].getTwl():0);
                //4号位
                stm.setInt(16,table.getSeats().length > 4?
                        table.getSeats()[4].isEmpty()?0:table.getSeats()[4].getPlayer().getUserid():0);
                stm.setString(17,table.getSeats().length > 4?
                        table.getSeats()[4].isEmpty()?"":table.getSeats()[4].getPlayer().getNickname():"");
                stm.setInt(18,table.getSeats().length > 4?
                        table.getSeats()[4].isEmpty()?0:table.getSeats()[4].getTwl():0);
                //5号位
                stm.setInt(19,table.getSeats().length > 5?
                        table.getSeats()[5].isEmpty()?0:table.getSeats()[5].getPlayer().getUserid():0);
                stm.setString(20,table.getSeats().length > 5?
                        table.getSeats()[5].isEmpty()?"":table.getSeats()[5].getPlayer().getNickname():"");
                stm.setInt(21,table.getSeats().length > 5?
                        table.getSeats()[5].isEmpty()?0:table.getSeats()[5].getTwl():0);

                stm.setString(22,new Timestamp(System.currentTimeMillis()).toString());
                stm.setInt(23,table.getCount());
                stm.setInt(24,table.isStart()?table.getCurCount() - 1 : table.getCurCount());
                stm.execute();
            }
        }catch (SQLException e){
            e.printStackTrace();
        }finally {
            close(conn,stm,null);
        }
    }
    public static ISFSObject getRoomRecord(int uid){
        Connection conn = null;
        PreparedStatement stm = null;
        ResultSet set = null;
        ISFSObject object = new SFSObject();
        ISFSArray array = new SFSArray();
        try {
            conn = getConnection();
            if (conn != null){
                stm = conn.prepareStatement("SELECT * FROM dbo.roomRecord WHERE u0=? OR u1=? OR u2=? OR u3=? OR u4=? OR u5=?");
                stm.setInt(1,uid);
                stm.setInt(2,uid);
                stm.setInt(3,uid);
                stm.setInt(4,uid);
                stm.setInt(5,uid);
                stm.setInt(6,uid);
                if (stm.execute()){
                    set = stm.getResultSet();
                    if (set.next()){
                        do {
                            ISFSObject o = new SFSObject();
                            o.putUtfString("name",set.getString("name"));
                            o.putInt("mod",set.getInt("mod"));
                            o.putUtfString("uuid",set.getString("uuid"));
                            o.putUtfString("ct",set.getString("create_time"));
                            o.putInt("tcount",set.getInt("tcount"));
                            o.putInt("ccount",set.getInt("ccount"));
                            for (int i=0;i<6;i++){
                                String uidStr = "u"+i;
                                String nickStr = "s"+i;
                                String wlStr = "wl"+i;
                                o.putInt(uidStr,set.getInt(uidStr));
                                o.putUtfString(nickStr,new String(set.getString(nickStr).getBytes("utf-8")));
                                o.putInt(wlStr,set.getInt(wlStr));
                            }
                            array.addSFSObject(o);
                        }while (set.next());
                    }
                }
            }
            object.putSFSArray("r",array);
            return object;
        }catch (Exception e){
            e.printStackTrace();
            return object;
        }finally {
            close(conn,stm,set);
        }
    }
    public static ISFSObject getGameRecord(String uuid){
        Connection conn = null;
        PreparedStatement stm = null;
        ResultSet set = null;
        ISFSObject object = new SFSObject();
        ISFSArray array = new SFSArray();
        try {
            conn = getConnection();
            if (conn != null){
                stm = conn.prepareStatement("SELECT * FROM dbo.gameRecord WHERE uuid=? ORDER BY count ASC ");
                stm.setString(1,uuid);
                if (stm.execute()){
                    set = stm.getResultSet();
                    if (set.next()){
                        do {
                            ISFSObject o = new SFSObject();
                            o.putUtfString("name",set.getString("name"));
                            o.putInt("mod",set.getInt("mod"));
                            o.putUtfString("uuid",set.getString("uuid"));
                            o.putUtfString("ct",set.getString("create_time"));
                            o.putUtfString("record",set.getString("record"));
                            o.putInt("count",set.getInt("count"));
                            for (int i=0;i<6;i++){
                                String uidStr = "u"+i;
                                String avatarStr = "a"+i;
                                String nickStr = "s"+i;
                                String wlStr = "wl"+i;
                                o.putInt(uidStr,set.getInt(uidStr));
                                o.putUtfString(nickStr,new String(set.getString(nickStr).getBytes("utf-8")));
                                o.putInt(wlStr,set.getInt(wlStr));
                                o.putUtfString(avatarStr,set.getString(avatarStr));
                            }
                            array.addSFSObject(o);
                        }while (set.next());
                    }
                }
            }
            object.putSFSArray("r",array);
            return object;
        }catch (Exception e){
            e.printStackTrace();
            return object;
        }finally {
            close(conn,stm,set);
        }
    }
    public static boolean checkVV(int uid){
        Connection conn = null;
        PreparedStatement stm = null;
        ResultSet set = null;
        try {
            conn = getConnection();
            if (conn != null){
                stm = conn.prepareStatement("SELECT * FROM vvip WHERE uid="+uid);
                if (stm.execute()){
                    set = stm.getResultSet();
                    return set.next();
                }
            }
            return false;
        }catch (SQLException e){
            return false;
        }finally {
            close(conn,stm,set);
        }
    }
    public static int setAgent(int uid,int aid){
        Connection conn = null;
        CallableStatement stm = null;
        ResultSet set = null;
        try {
            conn = getConnection();
            if (conn != null){
                stm = conn.prepareCall("{call setAgent (?,?)}");
                stm.setInt(1,uid);
                stm.setInt(2,aid);
                if (stm.execute()){
                    set = stm.getResultSet();
                    if (set.next()) {
                        return set.getInt("parentId");
                    }
                }
            }
           return 0;
        }catch (SQLException e){
            e.printStackTrace();
            return 0;
        }finally {
            close(conn,stm,set);
        }
    }
    public static int systemStatus(){
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet set = null;
        try {
            conn = getConnection();
            if (conn != null){
                stmt = conn.prepareStatement("SELECT StatusValue FROM dbo.SystemStatusInfo WHERE StatusName='LoginEnable'");
                if (stmt.execute()){
                    set = stmt.getResultSet();
                    if (set.next())
                        return set.getInt("StatusValue");
                }
            }
            return 1;
        }catch (SQLException e){
            e.printStackTrace();
            return 1;
        }finally {
            close(conn,stmt,set);
        }
    }
    public static void deleteRecord(){
        Connection conn = null;
        CallableStatement stmt = null;
        try {
            conn = getConnection();
            if (conn != null){
                stmt = conn.prepareCall("{call deleteRecord()}");
                stmt.execute();
            }
        }catch (SQLException e){
            e.printStackTrace();
        }finally {
            close(conn,stmt,null);
        }
    }
}
