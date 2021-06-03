package com.doit.net.bean;

import java.util.List;

/**
 * Author：Libin on 2020/9/23 16:19
 * Email：1993911441@qq.com
 * Describe：2G设置基础环境参数
 */
public class SetMCRFBean {

    private int boardid;
    private int carrierid;
    private int mcc;
    private int mnc;
    private int lac;
    private int opmode;
    private int dlattn;
    private int ulattn;
    private int sniff;

    //GSM专有参数
    private int ci;
    private int cro;
    private int cfgmode;
    private int fcn;

    //CDMA专有参数
    private List<Integer> fcnmode;

    private int rfState;

    public int getBoardid() {
        return boardid;
    }

    public void setBoardid(int boardid) {
        this.boardid = boardid;
    }

    public int getCarrierid() {
        return carrierid;
    }

    public void setCarrierid(int carrierid) {
        this.carrierid = carrierid;
    }

    public int getMcc() {
        return mcc;
    }

    public void setMcc(int mcc) {
        this.mcc = mcc;
    }

    public int getMnc() {
        return mnc;
    }

    public void setMnc(int mnc) {
        this.mnc = mnc;
    }

    public int getLac() {
        return lac;
    }

    public void setLac(int lac) {
        this.lac = lac;
    }

    public int getOpmode() {
        return opmode;
    }

    public void setOpmode(int opmode) {
        this.opmode = opmode;
    }

    public int getDlattn() {
        return dlattn;
    }

    public void setDlattn(int dlattn) {
        this.dlattn = dlattn;
    }

    public int getUlattn() {
        return ulattn;
    }

    public void setUlattn(int ulattn) {
        this.ulattn = ulattn;
    }

    public int getSniff() {
        return sniff;
    }

    public void setSniff(int sniff) {
        this.sniff = sniff;
    }

    public int getCi() {
        return ci;
    }

    public void setCi(int ci) {
        this.ci = ci;
    }

    public int getCro() {
        return cro;
    }

    public void setCro(int cro) {
        this.cro = cro;
    }

    public int getCfgmode() {
        return cfgmode;
    }

    public void setCfgmode(int cfgmode) {
        this.cfgmode = cfgmode;
    }

    public int getFcn() {
        return fcn;
    }

    public void setFcn(int fcn) {
        this.fcn = fcn;
    }

    public List<Integer> getFcnmode() {
        return fcnmode;
    }

    public void setFcnmode(List<Integer> fcnmode) {
        this.fcnmode = fcnmode;
    }

    public int isRfState() {
        return rfState;
    }

    public void setRfState(int rfState) {
        this.rfState = rfState;
    }

    @Override
    public String toString() {
        return "SetMCRFBean{" +
                "boardid=" + boardid +
                ", carrierid=" + carrierid +
                ", mcc=" + mcc +
                ", mnc=" + mnc +
                ", lac=" + lac +
                ", opmode=" + opmode +
                ", dlattn=" + dlattn +
                ", ulattn=" + ulattn +
                ", sniff=" + sniff +
                ", ci=" + ci +
                ", cro=" + cro +
                ", cfgmode=" + cfgmode +
                ", fcn=" + fcn +
                ", fcnmode=" + fcnmode +
                ", rfState=" + rfState +
                '}';
    }
}
