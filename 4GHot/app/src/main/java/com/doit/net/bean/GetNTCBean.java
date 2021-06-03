package com.doit.net.bean;

import java.util.List;

/**
 * Author：Libin on 2020/9/24 10:56
 * Email：1993911441@qq.com
 * Describe：
 */
public class GetNTCBean {
    private int result;
    private String swver;
    private String ntcver;
    private int c0rf = -1;
    private int c1rf = -1;
    private String maxtrans;
    private int BBInitState;
    private int rf = -1;
    private int sync;

    public int getResult() {
        return result;
    }

    public void setResult(int result) {
        this.result = result;
    }

    public String getSwver() {
        return swver;
    }

    public void setSwver(String swver) {
        this.swver = swver;
    }

    public String getNtcver() {
        return ntcver;
    }

    public void setNtcver(String ntcver) {
        this.ntcver = ntcver;
    }

    public int getC0rf() {
        return c0rf;
    }

    public void setC0rf(int c0rf) {
        this.c0rf = c0rf;
    }

    public int getC1rf() {
        return c1rf;
    }

    public void setC1rf(int c1rf) {
        this.c1rf = c1rf;
    }

    public String getMaxtrans() {
        return maxtrans;
    }

    public void setMaxtrans(String maxtrans) {
        this.maxtrans = maxtrans;
    }

    public int getBBInitState() {
        return BBInitState;
    }

    public void setBBInitState(int BBInitState) {
        this.BBInitState = BBInitState;
    }

    public int getRf() {
        return rf;
    }

    public void setRf(int rf) {
        this.rf = rf;
    }

    public int getSync() {
        return sync;
    }

    public void setSync(int sync) {
        this.sync = sync;
    }
}
