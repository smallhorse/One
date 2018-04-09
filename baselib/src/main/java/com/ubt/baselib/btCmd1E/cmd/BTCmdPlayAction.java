package com.ubt.baselib.btCmd1E.cmd;

import android.text.TextUtils;

import com.ubt.baselib.btCmd1E.BTCmd;
import com.ubt.baselib.btCmd1E.BaseBTReq;

import java.io.UnsupportedEncodingException;

/**
 * @作者：bin.zhang@ubtrobot.com
 * @日期: 2018/1/25 15:15
 * @描述:
 */

public class BTCmdPlayAction extends BaseBTReq {
    byte    cmd = BTCmd.DV_PLAYACTION;
    byte[] parm = {0x30};

    public BTCmdPlayAction(String actionName){
        try {
            if(!TextUtils.isEmpty(actionName)) {
                initReq(cmd, actionName.getBytes("UTF-8"));
            }else{
                initReq(cmd, parm);
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }
}
