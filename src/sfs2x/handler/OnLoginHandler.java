package sfs2x.handler;

import com.smartfoxserver.bitswarm.sessions.ISession;
import com.smartfoxserver.v2.core.ISFSEvent;
import com.smartfoxserver.v2.core.SFSConstants;
import com.smartfoxserver.v2.core.SFSEventParam;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.exceptions.*;
import com.smartfoxserver.v2.extensions.BaseServerEventHandler;
import com.smartfoxserver.v2.security.DefaultPermissionProfile;
import net.sf.json.JSONObject;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import sfs2x.Constant;
import sfs2x.master.Player;
import sfs2x.utils.DBUtil;
import sfs2x.utils.HttpConnectionManager;

import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Types;


public class OnLoginHandler extends BaseServerEventHandler {
    @Override
    public void handleServerEvent(ISFSEvent isfsEvent) throws SFSException {

        ISFSObject loginData = (ISFSObject) isfsEvent.getParameter(SFSEventParam.LOGIN_IN_DATA);
        ISFSObject outData = (ISFSObject) isfsEvent.getParameter(SFSEventParam.LOGIN_OUT_DATA);
        String openid = (String) isfsEvent.getParameter(SFSEventParam.LOGIN_NAME);
        String password = (String) isfsEvent.getParameter(SFSEventParam.LOGIN_PASSWORD);
        ISession session = (ISession) isfsEvent.getParameter(SFSEventParam.SESSION);
        String ip = session.getAddress();
        String token = loginData.getUtfString("token");

        if (DBUtil.systemStatus() == 1){
            SFSErrorData errData = new SFSErrorData(SFSErrorCode.GENERIC_ERROR);
            errData.addParameter("1");//服务器维护,请稍后再试!
            throw new SFSLoginException("系统暂停", errData);
        }
        if (getApi().checkSecurePassword(session, Constant.PASSWORD, password)) {
            try {
                String nickname ;
                int sex ;
                String headimgurl;
                if (openid.contains("test")){
                    nickname = "测试账号";
                    sex = 0;
                    headimgurl = "null";
                }else {
                    JSONObject jsonObject = getWXUserInfo(openid,token);
                    if (jsonObject.has("errcode")) {
                        SFSErrorData errData = new SFSErrorData(SFSErrorCode.GENERIC_ERROR);
                        errData.addParameter("2");//token过期,请重新获取token
                        throw new SFSLoginException("token过期", errData);
                    }else {
                        openid = jsonObject.getString("openid").trim();
                        nickname = jsonObject.getString("nickname").trim();
                        sex = jsonObject.getInt("sex");
                        headimgurl = jsonObject.getString("headimgurl").trim();
                    }
                }
                if (!verifyAccounts(outData,session,openid,nickname,sex,headimgurl,ip)){
                    SFSErrorData errData = new SFSErrorData(SFSErrorCode.GENERIC_ERROR);
                    errData.addParameter("3");//账号异常,请联系管理员!
                    throw new SFSLoginException("账号异常", errData);
                }
            } catch (IOException e) {
                SFSErrorData errData = new SFSErrorData(SFSErrorCode.GENERIC_ERROR);
                errData.addParameter("4");//登录失败,请稍后再试!
                throw new SFSLoginException("其他登录异常", errData);
            }
        } else {
            SFSErrorData errData = new SFSErrorData(SFSErrorCode.GENERIC_ERROR);
            errData.addParameter("5");
            throw new SFSLoginException("密码错误", errData);
        }
    }
    private JSONObject getWXUserInfo(String openid,String token) throws IOException {
        HttpGet get = null;
        HttpEntity entity = null;
        try {
            HttpClient client = HttpConnectionManager.getHttpClient();
            String url = String.format(Constant.USERINFO_URI,token,openid);
            get = new HttpGet(url);
            HttpResponse response = client.execute(get);
            entity = response.getEntity();
            String reader = EntityUtils.toString(entity,"utf-8");
            return JSONObject.fromObject(reader);
        }finally {
            if (entity != null)
                entity.consumeContent();
            if (get != null){
                get.abort();
            }
        }
    }
    private boolean verifyAccounts(ISFSObject outData,ISession session
            ,String openid,String nickname,int gender,String avatar
            ,String ip) {
        Connection conn = DBUtil.getConnection();
        CallableStatement stm = null;
        ResultSet set = null;
        try {
            if (conn != null){
                stm = conn.prepareCall("{?=call dbo.VerifyAccounts(?,?,?,?,?)}");
                stm.registerOutParameter(1, Types.INTEGER);
                stm.setString(2, openid);
                stm.setString(3, nickname);
                stm.setInt(4, gender);
                stm.setString(5, avatar);
                stm.setString(6, ip);
                if (stm.execute()){
                    set = stm.getResultSet();
                    if (set.next()){
                        int nollity = set.getInt("nullity");
                        if (nollity == 1)
                            return false;
                        else {
                            int uid = set.getInt("userid");
                            String n =  set.getString("nickname");
                            int s =  set.getInt("sex");
                            String face =  set.getString("faceurl");
                            long card =  set.getLong("card");
                            long gold = set.getLong("gold");
                            int parentId = set.getInt("parentId");

                            Player player = new Player(uid,ip,n,s,face,card,gold,parentId);
                            session.setProperty("p",player);

                            String userName = String.format(nickname + "[%d]", uid);
                            outData.putUtfString(SFSConstants.NEW_LOGIN_NAME, userName);
                            session.setProperty("$permission", DefaultPermissionProfile.STANDARD);
                            DBUtil.signIn(uid);
                            return true;
                        }
                    }
                }
            }
            return false;
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }finally {
            DBUtil.close(conn,stm,set);
        }
    }

}
