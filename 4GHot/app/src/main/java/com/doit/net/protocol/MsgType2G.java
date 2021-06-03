package com.doit.net.protocol;

/**
 * Author：Libin on 2020/9/27 15:11
 * Email：1993911441@qq.com
 * Describe：2G消息类型
 */
public class MsgType2G {
    //主类型
    public static final byte PT_LOGIN = 0x01;   //登录协议
    public static final byte PT_FILE = 0x02;     //文件操作协议
    public static final byte PT_ADJUST = 0x03;  //校时协议
    public static final byte PT_SYSTEM = 0x04;  //设备系统操作协议
    public static final byte PT_PARAM = 0x05;   //参数查询、设置协议
    public static final byte PT_WARNING = 0x06;  //异常通知协议
    public static final byte PT_RESP = 0x07;    //普通通用回复协议(登录成功上报)


    //子协议
    public static final byte COMMON_SUBTYPE = (byte) 0x01;   //设置、查询通用回复subtype
    public static final byte SET_TRANSNUM = (byte) 0x02;   //设置翻译次数
    public static final byte SET_MCRF_CONFIG = (byte) 0x03;   //设置运营商参数、工作模式
    public static final byte SET_RF_SWITCH = (byte) 0x04;   //开关射频
    public static final byte SET_SMS_CONFIG = (byte) 0x09;   //发送短信
    public static final byte SET_SMS_CONFIG_ACK = (byte) 0x89;   //发送短信应答
    public static final byte SET_LOC_IMSI = (byte) 0x06;   //设置定位imsi
    public static final byte SET_BLACK_NAMELIST = (byte) 0x07;   //设置黑名单手机号
    public static final byte SET_WHITELIST_STATE = (byte) 0x08;   //设置清空白名单

    public static final byte GET_NTC_CONFIG = 0x20;   //查询基本环境参数
    public static final byte GET_MCRF_CONFIG = 0x21;   //查询运营商参数、工作模式
    public static final byte GET_WHITELIST_STATE = 0x23;   //查询设备启动标识

    public static final byte REBOOT_DEVICE = (byte) 0x53;   //重启设备

    public static final byte RPT_ISDN_INFO = (byte) 0x30;   //号码翻译上报
    public static final byte RPT_IMSI_INFO = (byte) 0x31;   //imsi上报
    public static final byte RPT_HEARTBEAT_INFO = (byte) 0x32;   //心跳
    public static final byte RPT_IMSI_LOC_INFO = (byte) 0x33;   //2G定位上报


}
