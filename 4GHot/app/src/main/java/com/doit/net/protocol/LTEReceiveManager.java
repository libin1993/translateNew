package com.doit.net.protocol;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;

import com.doit.net.application.MyApplication;
import com.doit.net.bean.DeviceState;
import com.doit.net.bean.GetNTCBean;
import com.doit.net.bean.HeartBeatBean;
import com.doit.net.bean.Report2GIMSIBean;
import com.doit.net.bean.Report2GLocBean;
import com.doit.net.bean.Report2GNumberBean;
import com.doit.net.bean.SendSmsAckBean;
import com.doit.net.bean.SetMCRFBean;
import com.doit.net.bean.UBCStateBean;
import com.doit.net.bean.UeidBean;
import com.doit.net.event.EventAdapter;
import com.doit.net.utils.CacheManager;
import com.doit.net.bean.DBUeidInfo;
import com.doit.net.utils.UCSIDBManager;
import com.doit.net.bean.BlackListInfo;
import com.doit.net.socket.ServerSocketUtils;
import com.doit.net.ucsi.R;
import com.doit.net.utils.GsonUtils;
import com.doit.net.utils.LogUtils;
import com.doit.net.utils.ToastUtils;
import com.doit.net.utils.UtilDataFormatChange;

/**
 * Created by Zxc on 2018/10/18.
 */
import org.xutils.DbManager;
import org.xutils.common.util.KeyValue;
import org.xutils.db.sqlite.WhereBuilder;

import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static android.content.Context.NOTIFICATION_SERVICE;

public class LTEReceiveManager {
    //将字节数暂存
    private ArrayList<Byte> listReceiveBuffer = new ArrayList<Byte>();
    //包头的长度
    private short packageHeadLength = 12;

    public boolean initSuccess = false;


    //解析数据
    public synchronized void parseData(String host, byte[] bytesReceived, int receiveCount) {
        //将接收到数据存放在列表中

        for (int i = 0; i < receiveCount; i++) {
            listReceiveBuffer.add(bytesReceived[i]);
        }

        while (true) {
            //得到当前缓存中的长度
            int listReceiveCount = listReceiveBuffer.size();

            //如果缓存长度小于12说明最小包都没有收完整
            if (listReceiveCount < packageHeadLength) {
                break;
            }

            //取出长度
            byte[] contentLength = {listReceiveBuffer.get(0), listReceiveBuffer.get(1)};
            int contentLen = getShortData(contentLength[0], contentLength[1]);


            LogUtils.log("分包大小：" + listReceiveBuffer.size() + "," + contentLen);
            //判断缓存列表中的数据是否达到一个包的数据
            if (listReceiveBuffer.size() < contentLen) {
                LogUtils.log("LTE没有达到整包数:");
                break;
            }

            byte[] tempPackage = new byte[contentLen];
            //取出一个整包
            for (int j = 0; j < contentLen; j++) {
                tempPackage[j] = listReceiveBuffer.get(j);
            }

            //删除内存列表中的数据
            if (contentLen > 0) {
                listReceiveBuffer.subList(0, contentLen).clear();
            }

            //解析包
            parsePackageData(host, tempPackage);
        }

    }


    //解析成包数据
    private void parsePackageData(String host, byte[] tempPackage) {
        if (tempPackage.length < 12)
            return;

        LTEReceivePackage receivePackage = new LTEReceivePackage();

        receivePackage.setHost(host);

        //第一步取出包的长度
        short packageLength = getShortData(tempPackage[0], tempPackage[1]);
        receivePackage.setPackageLength(packageLength);

        //第二步取出CheckNum
        short packageCheckNum = getShortData(tempPackage[2], tempPackage[3]);
        receivePackage.setPackageCheckNum(packageCheckNum);

        //第三步取出序号
//        short packageSequence = getShortData(tempPackage[4], tempPackage[5]);
        receivePackage.setPackageSequence(new byte[]{tempPackage[4], tempPackage[5]});

        //第四步取出SessionID
//        short packageSessionID = getShortData(tempPackage[6], tempPackage[7]);
        receivePackage.setPackageSessionID(new byte[]{tempPackage[6], tempPackage[7]});

        //第五步取出主协议类型EquipType
        byte packageEquipType = tempPackage[8];
        receivePackage.setPackageEquipType(packageEquipType);

        //设备类型
        if (packageEquipType == (byte) 57) {
            CacheManager.equipType4G = packageEquipType;
            ServerSocketUtils.REMOTE_4G_HOST = host;
        } else if (packageEquipType == (byte) 0xFF) {
            CacheManager.equipType2G = packageEquipType;
            ServerSocketUtils.REMOTE_2G_HOST = host;
        }

        //第六步取出预留类型Reserve
        byte packageReserve = tempPackage[9];
        receivePackage.setPackageReserve(packageReserve);
        //UtilBaseLog.printLog("LTE收到(packageReserve):"+receivePackage.getPackageReserve());

        //第七步取出主协议类型MainType
        byte packageMainType = tempPackage[10];
        receivePackage.setPackageMainType(packageMainType);
        //UtilBaseLog.printLog("LTE收到(packageMainType):"+receivePackage.getPackageMainType());

        //第八步取出主协议类型Type
        byte packageSubType = tempPackage[11];
        receivePackage.setPackageSubType(packageSubType);


        //第九部取出内容
        //1.计算子协议内容包的长度
        int subPacketLength = packageLength - packageHeadLength;
        byte[] byteSubContent = new byte[subPacketLength];
        //2.取出子协议内容
        if (subPacketLength > 0) {
            System.arraycopy(tempPackage, packageHeadLength, byteSubContent, 0, byteSubContent.length);
        }
        receivePackage.setByteSubContent(byteSubContent);
        LogUtils.log(receivePackage.toString());
        LogUtils.log("TCP接收：ip:" + host + ",Type:" + packageMainType + ";  SubType:0x" + Integer.toHexString(receivePackage.getPackageSubType())
                + ";  子协议:" + new String(receivePackage.getByteSubContent(), StandardCharsets.UTF_8));
        //实时解析协议
        realTimeResponse(receivePackage);
    }


    //实时回复协议
    private void realTimeResponse(LTEReceivePackage receivePackage) {
        //判断数据来自4G还是2G
        int type = 0;
        if (TextUtils.equals(receivePackage.getHost(), ServerSocketUtils.REMOTE_4G_HOST)) {
            type = 4;
        }
        if (TextUtils.equals(receivePackage.getHost(), ServerSocketUtils.REMOTE_2G_HOST)) {
            type = 2;
        }
        switch (type) {
            case 4:   //4g
                switch (receivePackage.getPackageMainType()) {

                    case LTE_PT_LOGIN.PT_LOGIN:
                        LTE_PT_LOGIN.loginResp(receivePackage);

                        break;
                    case LTE_PT_ADJUST.PT_ADJUST:
//                        LTE_PT_ADJUST.response(receivePackage);
                        break;

                    case LTE_PT_SYSTEM.PT_SYSTEM:
                        switch (receivePackage.getPackageSubType()) {
                            case LTE_PT_SYSTEM.SYSTEM_REBOOT_ACK:
                            case LTE_PT_SYSTEM.SYSTEM_SET_DATETIME_ASK:
                            case LTE_PT_SYSTEM.SYSTEM_UPGRADE_ACK:
                            case LTE_PT_SYSTEM.SYSTEM_GET_LOG_ACK:
                                LTE_PT_SYSTEM.processCommonSysResp(receivePackage);
                                break;
                        }

                        break;
                    case LTE_PT_PARAM.PT_PARAM:
                        switch (receivePackage.getPackageSubType()) {
                            case LTE_PT_PARAM.PARAM_SET_ENB_CONFIG_ACK:
                            case LTE_PT_PARAM.PARAM_SET_CHANNEL_CONFIG_ACK:
                            case LTE_PT_PARAM.PARAM_SET_CHANNEL_ON_ACK:
                            case LTE_PT_PARAM.PARAM_SET_BLACK_NAMELIST_ACK:
                            case LTE_PT_PARAM.PARAM_SET_RT_IMSI_ACK:
                            case LTE_PT_PARAM.PARAM_SET_CHANNEL_OFF_ACK:
                            case LTE_PT_PARAM.PARAM_SET_FTP_CONFIG_ACK:
                            case LTE_PT_PARAM.PARAM_CHANGE_TAG_ACK:
                            case LTE_PT_PARAM.PARAM_SET_NAMELIST_ACK:
                            case LTE_PT_PARAM.PARAM_CHANGE_NAMELIST_ACK:
                            case LTE_PT_PARAM.PARAM_CHANGE_BAND_ACK:
                            case LTE_PT_PARAM.PARAM_SET_SCAN_FREQ_ACK:
                            case LTE_PT_PARAM.PARAM_SET_FAN_ACK:
                            case LTE_PT_PARAM.PPARAM_SET_LOC_IMSI_ACK:
                            case LTE_PT_PARAM.PARAM_SET_ACTIVE_MODE_ACK:
                            case LTE_PT_PARAM.PARAM_RPT_UPGRADE_STATUS:

                                LTE_PT_PARAM.processSetResp(receivePackage);
                                break;

                            case LTE_PT_PARAM.PARAM_GET_ENB_CONFIG_ACK:
                                LTE_PT_PARAM.processEnbConfigQuery(receivePackage);
                                break;

                            case LTE_PT_PARAM.PARAM_GET_ACTIVE_MODE_ASK:
                                LogUtils.log("工作模式查询:" + UtilDataFormatChange.bytesToString(receivePackage.getByteSubContent(), 0));
                                break;

                            case LTE_PT_PARAM.PARAM_RPT_HEATBEAT:
                                LTE_PT_PARAM.processRPTHeartbeat(receivePackage);
                                break;

                            case LTE_PT_PARAM.PARAM_GET_NAMELIST_ACK:
                                LTE_PT_PARAM.processNamelistQuery(receivePackage);
                                break;
                            case LTE_PT_PARAM.PARAM_RPT_BLACK_NAME:
                                LTE_PT_PARAM.processRptBlackName(receivePackage);
                                break;
                            case LTE_PT_PARAM.PARAM_RPT_SCAN_FREQ:
                                LTE_PT_PARAM.processRPTFreqScan(receivePackage);
                                break;
                            case LTE_PT_PARAM.RPT_SRSP_GROUP:
                                LTE_PT_PARAM.processLocRpt(receivePackage);
                                break;
                        }

                        break;
                }
                break;
            case 2:    //2G
                switch (receivePackage.getPackageMainType()) {
                    case MsgType2G.PT_LOGIN:
                        LTE_PT_LOGIN.loginResp(receivePackage);

                        new Timer().schedule(new TimerTask() {
                            @Override
                            public void run() {
                                Send2GManager.getUBCState();
                            }
                        }, 500);

                        if (!CacheManager.initSuccess2G) {
                            new Timer().schedule(new TimerTask() {
                                @Override
                                public void run() {
                                    if (!CacheManager.initSuccess2G) {
                                        Send2GManager.getParamsConfig();
                                    }
                                }
                            }, 1000);

                            new Timer().schedule(new TimerTask() {
                                @Override
                                public void run() {
                                    if (!CacheManager.initSuccess2G) {
                                        Send2GManager.getCommonConfig();
                                    }
                                }
                            }, 2000);
                        }
                        break;
                    case MsgType2G.PT_ADJUST:
                        LTE_PT_ADJUST.response(receivePackage);

                        LogUtils.log("2G初始化" + CacheManager.initSuccess2G);
                        if (!CacheManager.initSuccess2G) {

                            new Timer().schedule(new TimerTask() {
                                @Override
                                public void run() {
                                    Send2GManager.getUBCState();
                                }
                            }, 500);

                            new Timer().schedule(new TimerTask() {
                                @Override
                                public void run() {
                                    if (!CacheManager.initSuccess2G) {
                                        Send2GManager.getParamsConfig();
                                    }
                                }
                            }, 1000);

                            new Timer().schedule(new TimerTask() {
                                @Override
                                public void run() {
                                    if (!CacheManager.initSuccess2G) {
                                        Send2GManager.getCommonConfig();
                                    }
                                }
                            }, 2000);
                        }
                        break;
                    case MsgType2G.PT_RESP:
//                        LogUtils.log("2G登录成功");
                        break;
                    case MsgType2G.PT_PARAM:
                        switch (receivePackage.getPackageSubType()) {
                            case MsgType2G.COMMON_SUBTYPE:  //查询、设置返回的数据格式和主动上报的数据格式不一致
                                byte responseType = receivePackage.getByteSubContent()[0];
                                byte[] subContent = new byte[receivePackage.getByteSubContent().length - 1];
                                //2.取出子协议内容
                                System.arraycopy(receivePackage.getByteSubContent(), 1, subContent, 0, subContent.length);

                                LogUtils.log("2G通用回复子类型：0x" + Integer.toHexString(responseType));
                                switch (responseType) {
                                    case MsgType2G.GET_NTC_CONFIG:
                                        parseCommonConfig(subContent);
                                        break;
                                    case MsgType2G.GET_MCRF_CONFIG:
                                        parseParamsConfig(subContent);
                                        break;
                                    case MsgType2G.GET_WHITELIST_STATE:
                                        parseUBCState(subContent);
                                        break;
                                    case MsgType2G.SET_RF_SWITCH:
                                        Send2GManager.getCommonConfig();
                                        break;
                                    case MsgType2G.SET_MCRF_CONFIG:
                                        Send2GManager.getParamsConfig();
                                        break;
                                    case MsgType2G.SET_LOC_IMSI:
                                        LogUtils.log("2G定位下发成功");
                                        break;
                                    case MsgType2G.SET_BLACK_NAMELIST:
                                        LogUtils.log("黑名单下发成功");
                                        break;
                                }
                                break;
                            case MsgType2G.RPT_IMSI_INFO:
                                parseImsiReport(receivePackage);
                                break;
                            case MsgType2G.RPT_ISDN_INFO:
                                parsePhoneNumber(receivePackage);
                                break;
                            case MsgType2G.RPT_IMSI_LOC_INFO:
                                parseImsiLoc(receivePackage);
                                break;
                            case MsgType2G.RPT_HEARTBEAT_INFO:
                                parseHeartBeat(receivePackage);
                                break;

                        }
                        break;

                }

                break;
        }
    }


    /**
     * @param subContent 解析2G基本环境参数
     */
    private void parseCommonConfig(byte[] subContent) {
        try {
            GetNTCBean responseBean = GsonUtils.jsonToBean(new String(subContent,
                    StandardCharsets.UTF_8), GetNTCBean.class);
            if (responseBean != null) {
                if (responseBean.getC0rf() >= 0) {
                    if (responseBean.getBBInitState() == 0) {
                        CacheManager.isScanSuccess = true;
                    }

                    CacheManager.GSMSoftwareVersion = responseBean.getSwver();

                    boolean isContainCTJ = false;
                    boolean isContainCTU = false;
                    for (int i = 0; i < CacheManager.paramList.size(); i++) {
                        SetMCRFBean params = CacheManager.paramList.get(i);
                        if (params.getBoardid() == 0 && params.getCarrierid() == 0) {
                            isContainCTJ = true;
                            params.setRfState(responseBean.getC0rf() == 1 ? 1 : 0);
                        }

                        if (params.getBoardid() == 0 && params.getCarrierid() == 1) {
                            isContainCTU = true;
                            params.setRfState(responseBean.getC1rf() == 1 ? 1 : 0);
                        }
                    }

                    if (!isContainCTJ) {
                        SetMCRFBean paramsCTJ = new SetMCRFBean();
                        paramsCTJ.setBoardid(0);
                        paramsCTJ.setCarrierid(0);
                        paramsCTJ.setRfState(responseBean.getC0rf() == 1 ? 1 : 0);

                        CacheManager.paramList.add(paramsCTJ);
                    }

                    if (!isContainCTU) {
                        SetMCRFBean paramsCTU = new SetMCRFBean();
                        paramsCTU.setBoardid(0);
                        paramsCTU.setCarrierid(1);
                        paramsCTU.setRfState(responseBean.getC1rf() == 1 ? 1 : 0);
                        CacheManager.paramList.add(paramsCTU);
                    }

                } else {
                    CacheManager.CDMASoftwareVersion = responseBean.getSwver();

                    boolean isContainCTC = false;
                    for (int i = 0; i < CacheManager.paramList.size(); i++) {
                        SetMCRFBean params = CacheManager.paramList.get(i);
                        if (params.getBoardid() == 1) {
                            isContainCTC = true;
                            params.setRfState(responseBean.getRf() == 1 ? 1 : 0);
                            break;
                        }

                    }

                    if (!isContainCTC) {
                        SetMCRFBean paramsCTC = new SetMCRFBean();
                        paramsCTC.setBoardid(1);
                        paramsCTC.setCarrierid(0);
                        paramsCTC.setRfState(responseBean.getRf() == 1 ? 1 : 0);

                        CacheManager.paramList.add(paramsCTC);
                    }
                }

                for (SetMCRFBean setMCRFBean : CacheManager.paramList) {
                    LogUtils.log("2G参数1：" + setMCRFBean.toString());
                }
                EventAdapter.call(EventAdapter.REFRESH_DEVICE_2G);
                EventAdapter.call(EventAdapter.RF_STATUS_RPT);
                EventAdapter.call(EventAdapter.RF_STATUS_LOC);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * @param subContent 解析2G设备参数
     */
    private void parseParamsConfig(byte[] subContent) {
        //扫网未成功继续下发
        if (!CacheManager.isScanSuccess) {
            return;
        }
        try {
            SetMCRFBean responseBean = GsonUtils.jsonToBean(new String(subContent,
                    StandardCharsets.UTF_8), SetMCRFBean.class);
            if (responseBean != null) {
                int boardID;
                int carrierID;
                if (responseBean.getFcn() == 0) {
                    boardID = 1;
                    carrierID = 0;
                } else if (responseBean.getFcn() > 0 && responseBean.getFcn() <= 94) {
                    boardID = 0;
                    carrierID = 0;
                } else {
                    boardID = 0;
                    carrierID = 1;
                }
                boolean isContain = false;  //是否已存在


                for (int i = 0; i < CacheManager.paramList.size(); i++) {
                    SetMCRFBean params = CacheManager.paramList.get(i);
                    if (boardID == params.getBoardid()
                            && carrierID == params.getCarrierid()) {
                        isContain = true;

                        responseBean.setBoardid(boardID);
                        responseBean.setCarrierid(carrierID);
                        responseBean.setRfState(params.isRfState());
                        CacheManager.paramList.set(i, responseBean);
                        break;
                    }
                }

                if (!isContain) {
                    responseBean.setBoardid(boardID);
                    responseBean.setCarrierid(carrierID);
                    CacheManager.paramList.add(responseBean);
                }
                EventAdapter.call(EventAdapter.REFRESH_DEVICE_2G);
            }

            for (SetMCRFBean setMCRFBean : CacheManager.paramList) {
                LogUtils.log("2G参数2：" + setMCRFBean.toString());
            }

            LogUtils.log("载波数量：" + CacheManager.paramList.size() + "," + initSuccess);

            if (CacheManager.paramList.size() < 2) {
                return;
            }

            int carrierCount = 0;

            //移动联通载波
            for (int i = 0; i < CacheManager.paramList.size(); i++) {
                if (CacheManager.paramList.get(i).getBoardid() == 0) {
                    carrierCount++;
                }
            }

            if (carrierCount >= 2 && !initSuccess) {
                initSuccess = true;
                CacheManager.initSuccess2G = true;
                LogUtils.log("2G初始化成功，4G初始化结果：" + CacheManager.initSuccess4G);
                if (CacheManager.initSuccess4G) {
                    CacheManager.deviceState.setDeviceState(DeviceState.NORMAL);
                }

                //2G切换成采集模式
//                if (!(CacheManager.getLocState() && CacheManager.getCurrentLocation().getType() == 0)) {
//                    Send2GManager.setLocIMSI("", 0);
//                }

                //2G指派
                if (CacheManager.initSuccess4G) {
                    CacheManager.resetNameList();
                }

                //设置黑名单
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        Send2GManager.setBlackList();
                    }
                }, 2000);

            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * imsi上报
     */
    private void parseImsiReport(LTEReceivePackage receivePackage) {
        Report2GIMSIBean responseBean = GsonUtils.jsonToBean(new String(receivePackage.getByteSubContent(),
                StandardCharsets.UTF_8), Report2GIMSIBean.class);
        List<UeidBean> ueidList = new ArrayList<>();
        for (List<String> imsiList : responseBean.getImsilist()) {

            UeidBean ueidBean = new UeidBean();

            ueidBean.setImsi(imsiList.get(0));
            int rssi = Integer.parseInt(imsiList.get(2)) + 125;

            if (rssi < 0) {
                rssi = 0;
            }

            if (rssi > 100) {
                rssi = 100;
            }
            ueidBean.setSrsp(rssi);
            ueidBean.setRedirect(true);

            ueidList.add(ueidBean);

            LogUtils.log("2G采号上报：IMSI:" + imsiList.get(0) + "    强度:" + imsiList.get(2));
        }

        if (ueidList.size() > 0) {
            CacheManager.addBlockNameList(ueidList);
            EventAdapter.call(EventAdapter.SHIELD_RPT, ueidList);
        }
    }


    /**
     * 手机号上报
     */
    private synchronized void parsePhoneNumber(LTEReceivePackage receivePackage) {
        Report2GNumberBean responseBean = GsonUtils.jsonToBean(new String(receivePackage.getByteSubContent(),
                StandardCharsets.UTF_8), Report2GNumberBean.class);

        if (responseBean.getTranslist() == null) {
            return;
        }

        List<UeidBean> ueidList = new ArrayList<>();
        for (List<String> imsiList : responseBean.getTranslist()) {
            try {

                DbManager dbManager = UCSIDBManager.getDbManager();
                //修改手机号
                KeyValue keyValue1 = new KeyValue("msisdn", imsiList.get(1));
                dbManager.update(DBUeidInfo.class, WhereBuilder.b("imsi", "=", imsiList.get(0)), keyValue1);

                //修改黑名单
                BlackListInfo blackListInfo = dbManager.selector(BlackListInfo.class).where("msisdn", "=", imsiList.get(1)).findFirst();
                if (blackListInfo != null) {
                    blackListInfo.setImsi(imsiList.get(0));
                    dbManager.update(blackListInfo);
                    notice("手机号:" + imsiList.get(1) + "    IMSI:" + imsiList.get(0));
                }
                LogUtils.log("翻译上报：手机号:" + imsiList.get(1) + "    IMSI:" + imsiList.get(0));


                UeidBean ueidBean = new UeidBean();

                ueidBean.setImsi(imsiList.get(0));
                ueidBean.setSrsp(-1);
                ueidBean.setNumber(imsiList.get(1));
                ueidBean.setRedirect(true);

                ueidList.add(ueidBean);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (ueidList.size() > 0) {
            CacheManager.addBlockNameList(ueidList);
            EventAdapter.call(EventAdapter.SHIELD_RPT, ueidList);
        }

    }

    /**
     * 翻译上报，通知
     */
    public static void notice(String content) {
        String id = "channel";
        String name = "号码翻译";
        NotificationManager notificationManager = (NotificationManager) MyApplication.mContext.getSystemService(NOTIFICATION_SERVICE);
        Notification notification;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel mChannel = new NotificationChannel(id, name, NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(mChannel);
            notification = new Notification.Builder(MyApplication.mContext, id)
                    .setContentTitle("目标手机上报")
                    .setContentText(content)
                    .setSmallIcon(R.drawable.august_first)
                    .build();
        } else {
            notification = new NotificationCompat.Builder(MyApplication.mContext, id)
                    .setContentTitle("目标手机上报")
                    .setContentText(content)
                    .setSmallIcon(R.drawable.august_first)
                    .build();
        }
        notification.flags = Notification.FLAG_AUTO_CANCEL;
        notificationManager.notify(123, notification);

    }

    /**
     * @param receivePackage 定位上报
     */
    private void parseImsiLoc(LTEReceivePackage receivePackage) {
        String data = new String(receivePackage.getByteSubContent(), StandardCharsets.UTF_8);
        LogUtils.log("2G定位上报：IMSI:" + data);
        Report2GLocBean responseBean = GsonUtils.jsonToBean(data, Report2GLocBean.class);

        if (CacheManager.getLocState() && responseBean.getImsi().equals(CacheManager.getCurrentLocation().getImsi())
                && CacheManager.getCurrentLocation().getType() == 0) {

            int rssi = responseBean.getRssi() + 125;
            if (rssi < 0) {
                rssi = 0;
            }
            if (rssi > 100) {
                rssi = 100;
            }
            EventAdapter.call(EventAdapter.LOCATION_RPT, rssi + "");

            LogUtils.log("2G定位上报：IMSI:" + responseBean.getImsi() + ",强度：" + responseBean.getRssi());

        }

        if (!(CacheManager.getLocState() && CacheManager.getCurrentLocation().getType() == 0 &&
                responseBean.getImsi().equals(CacheManager.getCurrentLocation().getImsi()))) {
            Send2GManager.setLocIMSI(responseBean.getImsi(), 0);
        }
    }

    /**
     * @param receivePackage 2G状态
     */
    private void parseHeartBeat(LTEReceivePackage receivePackage) {

        HeartBeatBean responseBean = GsonUtils.jsonToBean(new String(receivePackage.getByteSubContent(),
                StandardCharsets.UTF_8), HeartBeatBean.class);
        LogUtils.log("2G心跳：" + responseBean.toString());
        EventAdapter.call(EventAdapter.RPT_HEARTBEAT_2G, responseBean);
    }

    /**
     * @param receivePackage 2G状态
     */
    private void parseSendSmsAck(LTEReceivePackage receivePackage) {

        SendSmsAckBean responseBean = GsonUtils.jsonToBean(new String(receivePackage.getByteSubContent(),
                StandardCharsets.UTF_8), SendSmsAckBean.class);
        if ("0".equals(responseBean.getResult())) {
            ToastUtils.showMessage("短信发送成功");
        }

    }

    private void parseUBCState(byte[] subContent) {
        UBCStateBean responseBean = GsonUtils.jsonToBean(new String(subContent,
                StandardCharsets.UTF_8), UBCStateBean.class);
        LogUtils.log("查询UBC回复：" + responseBean.toString());
        if (responseBean.getState() != 1) {
            CacheManager.isClearWhiteList = true;
        }
    }


    //获取short
    private short getShortData(byte tempByte1, byte tempByte2) {
        byte[] tempByteData = {tempByte1, tempByte2};
        return UtilDataFormatChange.byteToShort(tempByteData);
    }

    public void clearReceiveBuffer() {
        LogUtils.log("清除缓存");
        listReceiveBuffer.clear();
    }
}
