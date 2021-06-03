package com.doit.net.protocol;

import com.doit.net.bean.BlackListBean;
import com.doit.net.bean.SendSmsBean;
import com.doit.net.bean.SetMCRFBean;
import com.doit.net.bean.Set2GRFBean;
import com.doit.net.event.EventAdapter;
import com.doit.net.utils.BlackBoxManger;
import com.doit.net.bean.BlackListInfo;
import com.doit.net.utils.CacheManager;
import com.doit.net.bean.DBImsi;
import com.doit.net.utils.UCSIDBManager;
import com.doit.net.socket.ServerSocketUtils;
import com.doit.net.utils.GsonUtils;
import com.doit.net.utils.LogUtils;
import com.doit.net.utils.UtilDataFormatChange;

import org.json.JSONException;
import org.json.JSONObject;
import org.xutils.DbManager;
import org.xutils.ex.DbException;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


/**
 * Author：Libin on 2020/9/27 14:44
 * Email：1993911441@qq.com
 * Describe：2G协议发送
 */
public class Send2GManager {

    public static void sendData(byte mainType, byte subType, byte[] content) {
        LTESendPackage sendPackage = new LTESendPackage();
        //设置Sequence ID
        sendPackage.setPackageSequence(GSMProtocol.getSequenceID());
        //设置Session ID
        sendPackage.setPackageSessionID(GSMProtocol.getSessionID());
        //设置EquipType
        sendPackage.setPackageEquipType(CacheManager.equipType2G);
        //设置预留
        sendPackage.setPackageReserve((byte) 0xff);
        //设置主类型
        sendPackage.setPackageMainType(mainType);
        //设置子类型
        sendPackage.setPackageSubType(subType);
        sendPackage.setByteSubContent(content);

        //设置校验位
        sendPackage.setPackageCheckNum(sendPackage.getCheckNum());

        //获取整体的包
        byte[] tempSendBytes = sendPackage.getPackageContent();

        LogUtils.log("TCP发送：IP:"+ServerSocketUtils.REMOTE_2G_HOST+
                "; Type:" + sendPackage.getPackageMainType() + ";  SubType:0x" + Integer.toHexString(sendPackage.getPackageSubType()) + ";  子协议:" + UtilDataFormatChange.bytesToString(sendPackage.getByteSubContent(), 0));
        LogUtils.log(sendPackage.toString());
        ServerSocketUtils.getInstance().sendData(ServerSocketUtils.REMOTE_2G_HOST, tempSendBytes);

    }

    /**
     * 查询设备参数
     */
    public static void getCommonConfig() {
        getCommonConfig(0);

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                getCommonConfig(1);
            }
        }, 500);

    }

    /**
     * @param boardId 查询设备参数
     */
    public static void getCommonConfig(int boardId) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("boardid", boardId);
            LogUtils.log("查询NTC");
            sendData(MsgType2G.PT_PARAM, MsgType2G.GET_NTC_CONFIG, jsonObject.toString().getBytes(StandardCharsets.UTF_8));

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    /**
     * 查询运营商参数、工作模式
     */
    public static void getParamsConfig() {
        getParamsConfig(0, 0);

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                getParamsConfig(0, 1);
            }
        }, 500);


        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                getParamsConfig(1, 0);
            }
        }, 1000);

    }


    /**
     * 查询运营商参数、工作模式
     */
    public static void getParamsConfig(int boardId, int carrierId) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("boardid", boardId);
            jsonObject.put("carrierid", carrierId);
            LogUtils.log("查询MCRF");
            sendData(MsgType2G.PT_PARAM, MsgType2G.GET_MCRF_CONFIG, jsonObject.toString().getBytes(StandardCharsets.UTF_8));

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    /**
     * 设置运营商参数、工作模式
     */
    public static void setParamsConfig(SetMCRFBean params) {
        sendData(MsgType2G.PT_PARAM, MsgType2G.SET_MCRF_CONFIG, GsonUtils.objectToString(params).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * @param level 下行功率等级   1：低  2：中 3:高
     */
    public static void setPowerLevel(int level) {

        for (int i = 0; i < CacheManager.paramList.size(); i++) {
            int index = i;
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    SetMCRFBean setMCRFBean = CacheManager.paramList.get(index);
                    if (level == 1) {
                        setMCRFBean.setDlattn(15);
                    } else if (level == 2) {
                        setMCRFBean.setDlattn(7);
                    } else if (level == 3) {
                        setMCRFBean.setDlattn(0);
                    }

                    sendData(MsgType2G.PT_PARAM, MsgType2G.SET_MCRF_CONFIG, GsonUtils.objectToString(setMCRFBean).getBytes(StandardCharsets.UTF_8));
                }
            }, index * 1500);
        }
    }


    /**
     * 设置运营商参数、工作模式
     */
    public static void setRFState(int state) {
        setRFState(0, 0, state);

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                setRFState(0, 1, state);
            }
        }, 1000);


        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                setRFState(1, 0, state);
            }
        }, 2000);
    }


    /**
     * 设置射频
     */
    public static void setBoardRFState(int ctjState, int ctuState, int ctcState) {
        setRFState(0, 0, ctjState);

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                setRFState(0, 1, ctuState);
            }
        }, 500);


        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                setRFState(1, 0, ctcState);
            }
        }, 1000);
    }


    /**
     * 设置运营商参数、工作模式
     */
    public static void setRFState(int boardId, int carrierId, int state) {
        Set2GRFBean bean = new Set2GRFBean();

        bean.setBoardid(boardId);
        bean.setCarrierid(carrierId);
        bean.setState(state);

        sendData(MsgType2G.PT_PARAM, MsgType2G.SET_RF_SWITCH, GsonUtils.objectToString(bean).getBytes(StandardCharsets.UTF_8));
    }


    /**
     * 重启设备
     */
    public static void rebootDevice() {
        sendData(MsgType2G.PT_SYSTEM, MsgType2G.REBOOT_DEVICE, null);

        EventAdapter.call(EventAdapter.ADD_BLACKBOX, BlackBoxManger.REBOOT_2G_DEVICE);
    }

    /**
     * @param imsi  开始、结束定位
     * @param state
     */
    public static void setLocIMSI(String imsi, int state) {

        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("switch", state);
            jsonObject.put("imsi", imsi);
            LogUtils.log("2G定位:" + jsonObject.toString());
            LogUtils.log(new String(jsonObject.toString().getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8));
            sendData(MsgType2G.PT_PARAM, MsgType2G.SET_LOC_IMSI, jsonObject.toString().getBytes(StandardCharsets.UTF_8));

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    /**
     * 设置黑名单
     */
    public static void setBlackList() {
        DbManager dbManager = UCSIDBManager.getDbManager();
        List<String> blackList = new ArrayList<>();
        try {
            List<BlackListInfo> blackInfoList = dbManager.selector(BlackListInfo.class).findAll();
            if (blackInfoList != null) {
                for (int i = 0; i < blackInfoList.size(); i++) {
                    blackList.add(blackInfoList.get(i).getMsisdn());
                }
            }
        } catch (DbException e) {
            e.printStackTrace();
        }

        BlackListBean blackListBean = new BlackListBean();
        blackListBean.setNamelist(blackList);
        LogUtils.log("修改名单(黑):" + GsonUtils.objectToString(blackListBean));
        sendData(MsgType2G.PT_PARAM, MsgType2G.SET_BLACK_NAMELIST, GsonUtils.objectToString(blackListBean).getBytes(StandardCharsets.UTF_8));
    }


    /**
     * @param action   all:群发，one:单发，clear:停止发送
     * @param count    发送次数
     * @param interval 发送间隔
     * @param content  发送内容
     */
    public static void sendSms(String action, int count, int interval, String content) {
        SendSmsBean smsBean = new SendSmsBean();
        List<String> imsiList = new ArrayList<>();
        if ("one".equals(action)) {
            DbManager dbManager = UCSIDBManager.getDbManager();
            try {
                List<DBImsi> dbImsiList = dbManager.selector(DBImsi.class).findAll();
                if (dbImsiList != null) {
                    for (int i = 0; i < dbImsiList.size(); i++) {
                        imsiList.add(dbImsiList.get(i).getImsi());
                    }
                } else {
                    action = "all";
                }
            } catch (DbException e) {
                e.printStackTrace();
            }
        }

        smsBean.setAction(action);
        smsBean.setImsi(imsiList);
        smsBean.setSmsnum("106957135");
        smsBean.setInterval(String.valueOf(interval));
        smsBean.setPeriod(String.valueOf(count * interval));
        smsBean.setContent(content);

        LogUtils.log("发送短信:" + GsonUtils.objectToString(smsBean));
        sendData(MsgType2G.PT_PARAM, MsgType2G.SET_SMS_CONFIG, GsonUtils.objectToString(smsBean).getBytes(StandardCharsets.UTF_8));
    }


    public static void getUBCState() {
        sendData(MsgType2G.PT_PARAM, MsgType2G.GET_WHITELIST_STATE, null);
    }


    //设置清空白名单标识
    public static void setUBCState() {

        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("action", 1);
            LogUtils.log("设置UBC:" + jsonObject.toString());
            LogUtils.log(new String(jsonObject.toString().getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8));
            sendData(MsgType2G.PT_PARAM, MsgType2G.SET_WHITELIST_STATE, jsonObject.toString().getBytes(StandardCharsets.UTF_8));

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }
}
