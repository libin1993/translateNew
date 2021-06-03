package com.doit.net.bean;

/**
 * Author：Libin on 2020/12/7 13:13
 * Email：1993911441@qq.com
 * Describe：
 */
public class HeartBeatBean {
    private int cdma_sync;
    private int mp_state;
    private String memory_used;
    private String flash_used;

    public int getCdma_sync() {
        return cdma_sync;
    }

    public void setCdma_sync(int cdma_sync) {
        this.cdma_sync = cdma_sync;
    }

    public int getMp_state() {
        return mp_state;
    }

    public void setMp_state(int mp_state) {
        this.mp_state = mp_state;
    }

    public String getMemory_used() {
        return memory_used;
    }

    public void setMemory_used(String memory_used) {
        this.memory_used = memory_used;
    }

    public String getFlash_used() {
        return flash_used;
    }

    public void setFlash_used(String flash_used) {
        this.flash_used = flash_used;
    }

    @Override
    public String toString() {
        return "HeartBeatBean{" +
                "cdma_sync=" + cdma_sync +
                ", mp_state=" + mp_state +
                ", memory_used='" + memory_used + '\'' +
                ", flash_used='" + flash_used + '\'' +
                '}';
    }
}
