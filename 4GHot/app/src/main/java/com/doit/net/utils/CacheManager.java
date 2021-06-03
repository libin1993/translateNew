/*
 * Copyright (C) 2011-2016 dshine.com.cn
 * All rights reserved.
 * ShangHai Dshine - http://www.dshine.com.cn
 */
package com.doit.net.utils;

import android.content.Context;
import android.text.TextUtils;

import com.doit.net.bean.DeviceState;
import com.doit.net.bean.LocationBean;
import com.doit.net.bean.LteCellConfig;
import com.doit.net.bean.LteChannelCfg;
import com.doit.net.bean.LteEquipConfig;
import com.doit.net.bean.ScanFreqRstBean;
import com.doit.net.bean.SetMCRFBean;
import com.doit.net.bean.UeidBean;
import com.doit.net.bean.BlackListInfo;
import com.doit.net.bean.DBChannel;
import com.doit.net.protocol.LTESendManager;
import com.doit.net.protocol.Send2GManager;
import com.doit.net.view.MySweetAlertDialog;

import org.apache.commons.lang3.math.NumberUtils;
import org.xutils.DbManager;
import org.xutils.ex.DbException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * @author 杨维(wiker)
 * @version 1.0
 * @date 2016-4-26 下午3:37:39
 */
public class CacheManager {
    public static byte equipType4G;  //4G设备类型
    public static byte equipType2G;  //2G设备类型

    public static String GSMSoftwareVersion;  //GSM软件版本
    public static String CDMASoftwareVersion;  //CDMA软件版本


    public static LocationBean currentLocation = null;


    public static boolean isReportBattery = false;  //是否上报电量

    public static DeviceState deviceState = new DeviceState();


    public static boolean locMode = false;  //是否开启搜寻功能

    public static boolean initSuccess4G = false;   //4G初始化成功
    public static boolean initSuccess2G = false;    //2G初始化成功


    public static boolean checkLicense = false; //连接成功后校验证书

    public static List<SetMCRFBean> paramList = new ArrayList<>(); //2G设备参数


    public static boolean getLocMode() {
        return locMode;
    }

    private static LteCellConfig cellConfig;
    private static LteEquipConfig equipConfig;
    public static List<LteChannelCfg> channels = new ArrayList<>();

    public static boolean isClearWhiteList = false; //设备首次启动，需清空白名单
    public static boolean isScanSuccess = false; //2G设备是否扫网成功

    public static void setLocMode(boolean locMode) {
        CacheManager.locMode = locMode;
    }


    public static void updateLoc(String imsi, int type) {
        if (currentLocation == null) {
            currentLocation = new LocationBean();
        }
        SPUtils.setImsi(imsi);
        currentLocation.setImsi(imsi);
        currentLocation.setType(type);
    }




    //初始化，重置名单，黑名单管控，其余指派
    public static void resetNameList() {
        if (!CacheManager.getLocState()) {
            String blackIMSI = "";  //黑名单
            //添加管控imsi
            try {

                List<BlackListInfo> blackList = UCSIDBManager.getDbManager().selector(BlackListInfo.class).findAll();
                if (blackList != null && blackList.size() > 0) {
                    for (int i = 0; i < blackList.size(); i++) {
                        if (!TextUtils.isEmpty(blackList.get(i).getImsi()) && !blackList.get(i).getImsi().startsWith("46003")) {
                            blackIMSI += blackList.get(i).getImsi() + ",";
                            blackList.get(i).setBlock(1);
                            UCSIDBManager.getDbManager().update(blackList.get(i));

                        }
                    }
                }
            } catch (DbException e) {
                e.printStackTrace();
            }


            if (!TextUtils.isEmpty(blackIMSI)) {
                blackIMSI = blackIMSI.substring(0, blackIMSI.length() - 1);
            }

            CacheManager.redirect2G("", blackIMSI, CacheManager.isClearWhiteList ? "" : null, "redirect");

            if (!TextUtils.isEmpty(blackIMSI)) {
                String finalBlackIMSI = blackIMSI;
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        LTESendManager.changeNameList("add", "block", finalBlackIMSI);
                    }
                }, 1000);
            }
        }
    }


    //黑名单管控
    public static void addBlockNameList(List<UeidBean> ueidList) {
        StringBuilder blackIMSI = new StringBuilder();  //黑名单
        String imsi = "";  //定位IMSI
        if (CacheManager.getLocState()) {
            imsi = CacheManager.getCurrentLocation().getImsi();
        }
        try {
            List<BlackListInfo> blackList = UCSIDBManager.getDbManager().selector(BlackListInfo.class).findAll();
            if (blackList == null) {
                return;
            }

            for (int i = 0; i < blackList.size(); i++) {
                BlackListInfo blackListInfo = blackList.get(i);
                for (int j = 0; j < ueidList.size(); j++) {
                    UeidBean ueidBean = ueidList.get(j);
                    if (!TextUtils.isEmpty(blackListInfo.getImsi()) && blackListInfo.getImsi().equals(ueidBean.getImsi())
                            && !ueidBean.getImsi().equals(imsi) && !ueidBean.getImsi().startsWith("46003") && blackListInfo.getBlock() != 1) {
                        blackIMSI.append(ueidBean.getImsi()).append(",");
                        break;
                    }
                }
            }

        } catch (DbException e) {
            e.printStackTrace();
        }

        if (!TextUtils.isEmpty(blackIMSI.toString())) {
            blackIMSI = new StringBuilder(blackIMSI.substring(0, blackIMSI.length() - 1));
        }

        if (!TextUtils.isEmpty(blackIMSI.toString())) {
            LTESendManager.changeNameList("add", "block", blackIMSI.toString());
        }
    }

    /**
     * @param imsi
     * @param type 开始定位
     */
    public static void startLoc(String imsi, int type) {

        CacheManager.getCurrentLocation().setLocateStart(true);

        LogUtils.log("开始定位：" + imsi + "," + type);


        //添加管控imsi

        String blackIMSI = "";  //黑名单
        String blockIMSI = imsi;  //管控的黑名单

        try {
            List<BlackListInfo> blackList = UCSIDBManager.getDbManager().selector(BlackListInfo.class).findAll();
            if (blackList != null && blackList.size() > 0) {
                for (int i = 0; i < blackList.size(); i++) {
                    if (!TextUtils.isEmpty(blackList.get(i).getImsi()) && !blackList.get(i).getImsi().equals(imsi)
                            && !blackList.get(i).getImsi().startsWith("46003")) {
                        blackIMSI += blackList.get(i).getImsi() + ",";
                    }
                }
            }
        } catch (DbException e) {
            e.printStackTrace();
        }


        if (!TextUtils.isEmpty(blackIMSI)) {
            blackIMSI = blackIMSI.substring(0, blackIMSI.length() - 1);

            blockIMSI += "," + blackIMSI;
        }


        if (type == 1) {  //4G定位

            //目标imsi吸附，其余的回公网
            LTESendManager.setNameList(null, null,
                    "", blockIMSI, "reject");

            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    LTESendManager.exchangeFcn(imsi);
                }
            }, 1000);

            String finalBlockIMSI = blockIMSI;

            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    LTESendManager.changeNameList("add", "block", finalBlockIMSI);
                }
            }, 1500);


            for (int i = 0; i < CacheManager.getChannels().size(); i++) {
                int index = i;
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        if (CacheManager.getChannels().size() > index) {
                            boolean isOpenRF = false;
                            int band = Integer.parseInt(CacheManager.getChannels().get(index).getBand());
                            //管控的黑名单
                            String[] split = finalBlockIMSI.split(",");
                            for (String s : split) {
                                String plmn = UtilOperator.getOperatorName(s);
                                //移动判断b3和tdd; 联通电信判断fdd
                                if (((band == 3 || band >= 33) && "CTJ".equals(plmn)) || (band <= 25 && !"CTJ".equals(plmn))) {
                                    isOpenRF = true;
                                    break;
                                }
                            }

                            if (isOpenRF) {
                                LTESendManager.openRf(CacheManager.getChannels().get(index).getIdx());
                                CacheManager.getChannels().get(index).setRFState(true);
                            } else {
                                LTESendManager.closeRf(CacheManager.getChannels().get(index).getIdx());
                            }
                        }
                    }
                }, 2000 + index * 150);
            }


//            new Timer().schedule(new TimerTask() {
//                @Override
//                public void run() {
//                    LTESendManager.openAllRf();
//                }
//            }, 2000);


//            Send2GManager.setLocIMSI("", 0);

            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    Send2GManager.setRFState(0);
                }
            }, 4000);


        } else {
            //目标imsi重定向，其余的回公网
            String redirectIMSI = !"CTC".equals(UtilOperator.getOperatorName(imsi)) ? imsi : "";


            CacheManager.redirect2G(redirectIMSI, blackIMSI, null, "reject");

            if (!TextUtils.isEmpty(redirectIMSI)) {
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        //添加指派imsi
                        LTESendManager.changeNameList("add", "redirect", redirectIMSI);
                    }
                }, 1000);
            }

            if (!TextUtils.isEmpty(blackIMSI)) {
                String finalBlackIMSI = blackIMSI;
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        //添加指派imsi
                        LTESendManager.changeNameList("add", "block", finalBlackIMSI);
                    }
                }, 1500);
            }


            for (int i = 0; i < CacheManager.getChannels().size(); i++) {
                int index = i;
                String finalBlackList = blackIMSI;
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        if (CacheManager.getChannels().size() > index) {
                            boolean isOpenRF = false;
                            int band = Integer.parseInt(CacheManager.getChannels().get(index).getBand());
                            //管控的黑名单
                            if (!TextUtils.isEmpty(finalBlackList)) {
                                String[] split = finalBlackList.split(",");
                                for (String s : split) {
                                    String plmn = UtilOperator.getOperatorName(s);
                                    //移动判断b3和tdd; 联通电信判断fdd
                                    if (((band == 3 || band >= 33) && "CTJ".equals(plmn)) || (band <= 25 && !"CTJ".equals(plmn))) {
                                        isOpenRF = true;
                                        break;
                                    }
                                }
                            }

                            if (!isOpenRF && (((band == 3 || band >= 33) && "CTJ".equals(UtilOperator.getOperatorName(imsi)))
                                    || (band <= 25 && "CTU".equals(UtilOperator.getOperatorName(imsi))))) {
                                isOpenRF = true;
                            }

                            if (isOpenRF) {
                                LTESendManager.openRf(CacheManager.getChannels().get(index).getIdx());
                                CacheManager.getChannels().get(index).setRFState(true);
                            } else {
                                LTESendManager.closeRf(CacheManager.getChannels().get(index).getIdx());
                            }
                        }
                    }
                }, 2000 + index * 150);
            }

//            if ("CTC".equals(UtilOperator.getOperatorName(imsi))) {
//                Send2GManager.setBoardRFState("0","0","1");
//                for (Set2GParamsBean.Params params : CacheManager.paramList) {
//                    if(params.getBoardid().equals("1")) {
//                        params.setRfState(true);
//                    }
//                }
//            }else {
//                Send2GManager.setBoardRFState("1","1","0");
//                for (Set2GParamsBean.Params params : CacheManager.paramList) {
//                    if(params.getBoardid().equals("0")) {
//                        params.setRfState(true);
//                    }
//                }
//            }


            if ("CTC".equals(UtilOperator.getOperatorName(imsi))) {
                Send2GManager.setBoardRFState(0, 0, 1);
                for (SetMCRFBean params : CacheManager.paramList) {
                    if (params.getBoardid() == 1) {
                        params.setRfState(1);
                        break;
                    }
                }
            } else if ("CTU".equals(UtilOperator.getOperatorName(imsi))) {
                Send2GManager.setBoardRFState(0, 1, 0);
                for (SetMCRFBean params : CacheManager.paramList) {
                    if (params.getBoardid() == 0 && params.getCarrierid() == 1) {
                        params.setRfState(1);
                        break;
                    }
                }
            } else {
                Send2GManager.setBoardRFState(1, 0, 0);
                for (SetMCRFBean params : CacheManager.paramList) {
                    if (params.getBoardid() == 0 && params.getCarrierid() == 0) {
                        params.setRfState(1);
                        break;
                    }
                }
            }


//            new Timer().schedule(new TimerTask() {
//                @Override
//                public void run() {
//                    LTESendManager.openAllRf();
//                }
//            }, 2000);
//
//
//            if ("CTC".equals(UtilOperator.getOperatorName(imsi))) {
//                Send2GManager.setRFState("1");
//            } else {
//                Send2GManager.setGSMRFState("1");
//            }

            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    Send2GManager.setLocIMSI(imsi, 1);
                }
            }, 2000);

        }

    }


    //停止定位，恢复默认参数
    public static void resetParams() {

        try {
            DbManager dbManager = UCSIDBManager.getDbManager();
            //B3频段恢复默认频点
            DBChannel channelB3 = dbManager.selector(DBChannel.class)
                    .where("band", "=", "3")
                    .and("is_check", "=", "1")
                    .findFirst();
            if (channelB3 != null) {
                String idx = "";
                for (LteChannelCfg channel : CacheManager.getChannels()) {
                    if (channel.getBand().equals("3")) {
                        idx = channel.getIdx();
                        break;
                    }
                }

                if (!TextUtils.isEmpty(idx)) {

                    LTESendManager.setChannelConfig(idx, channelB3.getFcn(),
                            "46000,46001", "", "", "", "", "");

                    for (LteChannelCfg channel : CacheManager.channels) {
                        if (channel.getIdx().equals(idx)) {
                            channel.setFcn(channelB3.getFcn());
                            channel.setPlmn("46000,46001");
                            break;
                        }
                    }
                }

            }

            //B1频段恢复默认频点
            DBChannel channelB1 = dbManager.selector(DBChannel.class)
                    .where("band", "=", "1")
                    .and("is_check", "=", "1")
                    .findFirst();
            if (channelB1 != null) {
                String idx = "";
                for (LteChannelCfg channel : CacheManager.getChannels()) {
                    if (channel.getBand().equals("1")) {
                        idx = channel.getIdx();
                        break;
                    }
                }

                if (!TextUtils.isEmpty(idx)) {
                    LTESendManager.setChannelConfig(idx, channelB1.getFcn(),
                            "", "", "", "", "", "");

                    for (LteChannelCfg channel : CacheManager.channels) {
                        if (channel.getIdx().equals(idx)) {
                            channel.setFcn(channelB1.getFcn());
                            break;
                        }
                    }
                }

            }

        } catch (DbException e) {
            e.printStackTrace();
        }

    }

    public static boolean getLocState() {
        if (currentLocation == null)
            return false;

        return currentLocation.isLocateStart();
    }

    public static LocationBean getCurrentLocation() {
        return currentLocation;
    }


    /**
     * 检查设备是否连接，并提示
     *
     * @param context
     * @return
     */
    public static boolean checkDevice(Context context) {
        if (!isDeviceOk()) {
            new MySweetAlertDialog(context, MySweetAlertDialog.ERROR_TYPE)
                    .setTitleText("错误")
                    .setContentText("设备未就绪")
                    .show();
            return false;
        }
        return true;
    }

    public static boolean isDeviceOk() {
        return initSuccess4G && cellConfig != null && channels.size() != 0 && equipConfig != null && initSuccess2G && paramList.size() > 0;
    }


    /**
     * 重置一下状态，一般设备需要重启时调用
     */
    public static void clearCache4G() {
        cellConfig = null;
        channels.clear();
        equipConfig = null;
    }


    public static LteCellConfig getCellConfig() {
        return cellConfig;
    }

    public static LteEquipConfig getLteEquipConfig() {
        return equipConfig;
    }

    public static List<LteChannelCfg> getChannels() {
        return channels;
    }

    public static void setCellConfig(LteCellConfig cellConfig) {
        CacheManager.cellConfig = cellConfig;
    }

    public static void setEquipConfig(LteEquipConfig equipConfig) {
        CacheManager.equipConfig = equipConfig;
    }


    public synchronized static void addChannel(LteChannelCfg cfg) {
        for (LteChannelCfg channel : channels) {
            if (channel.getIdx().equals(cfg.getIdx())) {
                channel.setFcn(cfg.getFcn());
                channel.setBand(cfg.getBand());
                channel.setGa(cfg.getGa());
                channel.setPa(cfg.getPa());
                channel.setPlmn(cfg.getPlmn());
                channel.setRlm(cfg.getRlm());
                channel.setAutoOpen(cfg.getAutoOpen());
                channel.setAltFcn(cfg.getAltFcn());
                channel.setChangeBand(cfg.getChangeBand());
                return;
            }
        }

        channels.add(cfg);
        Collections.sort(channels, new Comparator<LteChannelCfg>() {
            @Override
            public int compare(LteChannelCfg lhs, LteChannelCfg rhs) {
                return NumberUtils.toInt(lhs.getBand()) - NumberUtils.toInt(rhs.getBand());
            }
        });
    }


    /**
     * 重定向到2G
     */
    public static void redirect2G(String nameListRedirect, String nameListBlock, String nameListReject, String nameListRestAction) {

        int mobileFcn = 0;
        int unicomFcn = 0;

        for (SetMCRFBean params : CacheManager.paramList) {
            if (params.getBoardid() == 0) {
                if (params.getCarrierid() == 0) {
                    mobileFcn = params.getFcn();
                }

                if (params.getCarrierid() == 1) {
                    unicomFcn = params.getFcn();
                }
            }
        }
        LogUtils.log("重定向到2G:" + mobileFcn + "," + unicomFcn);

        if (mobileFcn > 0 && unicomFcn > 0) {
            String redirectConfig = "46000,2," + mobileFcn + "#46002,2," + mobileFcn + "#46007,2," + mobileFcn + "#46001,2," + unicomFcn;
            LTESendManager.setNameList(redirectConfig, nameListReject,
                    nameListRedirect, nameListBlock, nameListRestAction);

            //清空白名单
            if (CacheManager.isClearWhiteList) {
                CacheManager.isClearWhiteList = false;
                Send2GManager.setUBCState();
            }
        }
    }

    //将RF状态更新到内存
    public synchronized static void updateRFState(String idx, boolean rf) {

        for (LteChannelCfg channel : channels) {
            if (channel.getIdx().equals(idx)) {
                channel.setRFState(rf);
                return;
            }
        }
    }

    public static LteChannelCfg getChannelByIdx(String idx) {
        for (LteChannelCfg channel : channels) {
            if (channel.getIdx().equals(idx)) {
                return channel;
            }
        }
        return null;
    }


    public static void setHighGa(boolean on_off) {
        if (on_off) {
            for (LteChannelCfg channel : channels) {
                if (Integer.parseInt(channel.getGa()) <= 10) {
                    LTESendManager.setChannelConfig(channel.getIdx(), "", "", "", String.valueOf(Integer.parseInt(channel.getGa()) * 5), "", "", "");
                    channel.setGa(String.valueOf(Integer.parseInt(channel.getGa()) * 5));
                }
            }
        } else {
            for (LteChannelCfg channel : channels) {
                if (Integer.parseInt(channel.getGa()) > 10) {
                    LTESendManager.setChannelConfig(channel.getIdx(), "", "", "", String.valueOf(Integer.parseInt(channel.getGa()) / 5), "", "", "");
                    channel.setGa(String.valueOf(Integer.parseInt(channel.getGa()) / 5));
                }
            }
        }
    }


    public static void changeBand(String idx, String changeBand) {

        LTESendManager.changeBand(idx, changeBand);

        //下发切换之后，等待生效，设置默认频点
        String fcn = LTESendManager.getCheckedFcn(changeBand);
        if (!TextUtils.isEmpty(fcn)) {
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    LTESendManager.setChannelConfig(idx, fcn, "", "", "", "", "", "");
                }
            }, 2000);
        }

    }

    /**
     * 查询黑名单IMSI
     */
    public static List<String> getBlackIMSIList() {
        DbManager dbManager = UCSIDBManager.getDbManager();
        List<String> blackList = new ArrayList<>();
        try {
            List<BlackListInfo> blackInfoList = dbManager.selector(BlackListInfo.class).findAll();
            if (blackInfoList != null) {
                for (int i = 0; i < blackInfoList.size(); i++) {
                    if (!TextUtils.isEmpty(blackInfoList.get(i).getImsi())) {
                        blackList.add(blackInfoList.get(i).getImsi());
                    }
                }
            }
        } catch (DbException e) {
            e.printStackTrace();
        }
        return blackList;
    }
}
