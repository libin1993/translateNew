package com.doit.net.protocol;

import com.doit.net.utils.FormatUtils;

import java.util.Arrays;

/**
 * Created by Zxc on 2018/10/18.
 */

public class LTEProtocol {

    //当前的SequenceID
    private static int currentSequenceID = 0;

    //当前的SessionID
    private static int currentSessionID = 32768;

    /**
     * 得到会话序号
     */
    public static byte[] getSequenceID() {

        currentSequenceID++;

        if (currentSequenceID > 65535) {
            currentSequenceID = 1;
        }

        return FormatUtils.getInstance().unsignedShortToByte(currentSequenceID);
    }

    /**
     * 得到会话ID
     */
    public static byte[] getSessionID() {
        currentSessionID++;

        if (currentSessionID >= 65535) {
            currentSessionID = 32769;
        }

        return FormatUtils.getInstance().unsignedShortToByte(currentSessionID);
    }
}
