package sfs2x.master;

import com.smartfoxserver.v2.entities.Room;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSObject;

import java.io.UnsupportedEncodingException;

public class Player {
    private int userid;
    private String nickname;
    private int sex;

    public String getFaceurl() {
        return faceurl;
    }

    private String faceurl;
    private long card;//房卡
    private User user;
    private String ip;
    private long gold;
    private Room room;
    private int parentId;


    public Player(int userid, String ip, String nickname, int sex, String faceurl, long card, long gold, int parentId) {

        this.userid = userid;
        this.ip = ip;
        try {
            this.nickname = new String(nickname.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            this.nickname = nickname;
        }
        this.sex = sex;
        this.faceurl = faceurl;
        this.card = card;
        this.gold = gold;
        room = null;
        this.parentId = parentId;
    }

    public int getParentId() {
        return parentId;
    }

    public void setParentId(int parentId) {
        this.parentId = parentId;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public long getGold() {
        return gold;
    }

    public void addGold(int g) {
        gold += g;
    }

    public void subGold(int g) {
        gold -= g;
    }


    public ISFSObject toSFSObject() {
        ISFSObject object = new SFSObject();
        object.putInt("uid", userid);
        object.putUtfString("name", nickname);
        object.putInt("sex", sex);
        object.putUtfString("face", faceurl);
        object.putLong("card", card);
        object.putLong("gold", gold);
        object.putUtfString("ip", ip);
        object.putInt("pid", parentId);
        return object;
    }

    public User getUser() {
        return user;
    }

    public int getUserid() {
        return userid;
    }


    public long getCard() {
        return card;
    }

    public void setCard(long card) {
        this.card = card;
    }


    public String getNickname() {
        return nickname;
    }

    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        this.room = room;
    }
}
