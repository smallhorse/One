package com.ubt.blockly.main;

import android.content.Context;

import com.ubt.baselib.BlueTooth.BTReadData;
import com.ubt.baselib.BlueTooth.BTServiceStateChanged;
import com.ubt.baselib.btCmd1E.BTCmd;
import com.ubt.baselib.btCmd1E.BluetoothParamUtil;
import com.ubt.baselib.btCmd1E.ProtocolPacket;
import com.ubt.baselib.btCmd1E.cmd.BTCmdActionStopPlay;
import com.ubt.baselib.btCmd1E.cmd.BTCmdGetActionList;
import com.ubt.baselib.btCmd1E.cmd.BTCmdPlayAction;
import com.ubt.baselib.btCmd1E.cmd.BTCmdPlayEmoj;
import com.ubt.baselib.btCmd1E.cmd.BTCmdPlaySound;
import com.ubt.baselib.btCmd1E.cmd.BTCmdRead6DState;
import com.ubt.baselib.btCmd1E.cmd.BTCmdReadAcceleration;
import com.ubt.baselib.btCmd1E.cmd.BTCmdSetLedEffect;
import com.ubt.baselib.btCmd1E.cmd.BTCmdSetPir;
import com.ubt.baselib.btCmd1E.cmd.BTCmdSoundStopPlay;
import com.ubt.baselib.btCmd1E.cmd.BTCmdStartWalk;
import com.ubt.baselib.btCmd1E.cmd.BTCmdStopEyeLed;
import com.ubt.baselib.btCmd1E.cmd.BTCmdStopPlayEmoji;
import com.ubt.baselib.btCmd1E.cmd.BTCmdSwitchEditStatus;
import com.ubt.baselib.mvp.BasePresenterImpl;
import com.ubt.bluetoothlib.base.BluetoothState;
import com.ubt.bluetoothlib.blueClient.BlueClientUtil;
import com.vise.log.ViseLog;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

/**
 * @author admin
 * @className
 * @description
 * @date
 * @update
 */


public class BlocklyPresenter extends BasePresenterImpl<BlocklyContract.View> implements BlocklyContract.Presenter {

    private Context context;
    private BlueClientUtil mBlueClient;
    private int mConnectState ;
    private List<String> actionList = new ArrayList<>();


    @Override
    public void register(Context context) {
        this.context = context;
        mBlueClient = BlueClientUtil.getInstance();
        EventBus.getDefault().register(this);
    }

    @Override
    public void getActionList() {
        if(mBlueClient.getConnectionState() == 3){
            ViseLog.d("getActionList");
            actionList.clear();
            mBlueClient.sendData(new BTCmdGetActionList("action").toByteArray());
        }
    }

    @Override
    public void unRegister() {
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void playAction(String actionName) {
        if(mBlueClient.getConnectionState() == 3){
            mBlueClient.sendData(new BTCmdPlayAction(actionName).toByteArray());
        }

    }

    @Override
    public void stopAction() {
        if(mBlueClient.getConnectionState() == 3){
            mBlueClient.sendData(new BTCmdActionStopPlay().toByteArray());
        }

    }

    @Override
    public void playEmoji(String emojiName) {
        if(mBlueClient.getConnectionState() == 3){
            mBlueClient.sendData(new BTCmdPlayEmoj(emojiName).toByteArray());
        }

    }

    @Override
    public void playSound(String soundName) {
        if(mBlueClient.getConnectionState() == 3){
            mBlueClient.sendData(new BTCmdPlaySound(soundName).toByteArray());
        }
    }

    @Override
    public void playLedLight(byte[] params) {
        if(mBlueClient.getConnectionState() == 3){
            mBlueClient.sendData(new BTCmdSetLedEffect(params).toByteArray());
        }
    }

    @Override
    public void doWalk(byte direct, byte speed, byte[] step) {
        byte[] params = new byte[6];
        params[0] = direct;
        params[1] = speed;
        params[2] = step[3];
        params[3] = step[2];
        params[4] = step[1];
        params[5] = step[0];
        if(mBlueClient.getConnectionState() ==3){
            mBlueClient.sendData(new BTCmdStartWalk(params).toByteArray());
        }
    }

    @Override
    public void stopPlayAudio() {
        if(mBlueClient.getConnectionState() ==3){
            mBlueClient.sendData(new BTCmdSoundStopPlay().toByteArray());
        }
    }

    @Override
    public void stopLedLight() {
        if(mBlueClient.getConnectionState() ==3){
            mBlueClient.sendData(new BTCmdStopEyeLed().toByteArray());
        }
    }

    @Override
    public void stopPlayEmoji() {
        if(mBlueClient.getConnectionState() ==3){
            mBlueClient.sendData(new BTCmdStopPlayEmoji().toByteArray());
        }
    }

    @Override
    public void doReadInfraredSensor(byte cmd) {
        if(mBlueClient.getConnectionState() == 3){
            mBlueClient.sendData(new BTCmdSetPir(cmd).toByteArray());
        }
    }

    @Override
    public void doRead6Dstate() {
        if(mBlueClient.getConnectionState() == 3){
            mBlueClient.sendData(new BTCmdRead6DState().toByteArray());
        }
    }

    @Override
    public void doReadTemperature(byte cmd) {
        if(mBlueClient.getConnectionState() == 3){
            mBlueClient.sendData(new BTCmdReadAcceleration(cmd).toByteArray());
        }
    }

    @Override
    public void startOrStopRun(byte cmd) {
        if(mBlueClient.getConnectionState() == 3){
            mBlueClient.sendData(new BTCmdSwitchEditStatus(cmd).toByteArray());
        }
    }


    /**
     * 读取蓝牙回调数据
     *
     * @param readData
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onReadData(BTReadData readData) {
//        ViseLog.i("data:" + HexUtil.encodeHexStr(readData.getDatas()));
//        BTCmdHelper.parseBTCmd(readData.getDatas(), this);
        onProtocolPacket(readData);
    }

    /**
     * 蓝牙数据解析回调
     *
     * @param readData
     */
    private void onProtocolPacket(BTReadData readData) {
        ProtocolPacket packet = readData.getPack();
        switch (packet.getmCmd()) {
            case BTCmd.UV_GETACTIONFILE:
                ViseLog.d("UV_GETACTIONFILE:" + new String(packet.getmParam()));
                actionList.add(new String(packet.getmParam()));
                break;
            case BTCmd.UV_STOPACTIONFILE:
                ViseLog.d("UV_STOPACTIONFILE:" + actionList.toString());
                if(mView != null) {
                    mView.getActionList(actionList);
                }
                break;
            case BTCmd.DV_ACTION_FINISH:
                String actionName = BluetoothParamUtil.bytesToString(packet.getmParam());
                if(mView != null) {
                    mView.notePlayFinish(actionName);
                }
                break;
            case BTCmd.DV_SET_PLAY_EMOJI:
                if(packet.getmParam()[0] == 1){
                    if(mView != null){
                        ViseLog.d("playEmojiFinish");
                        mView.playEmojiFinish();
                    }
                }
                break;
            case BTCmd.DV_SET_PLAY_SOUND:
                if(packet.getmParam()[0] == 1){
                    if(mView != null){
                        ViseLog.d("playSoundFinish");
                        mView.playSoundFinish();
                    }
                }
                break;
            case BTCmd.DV_WALK:
                if(packet.getmParam()[0] == 0 || packet.getmParam()[0] == 3){
                    if(mView != null){
                        mView.doWalkFinish();
                    }
                }
                break;
            case BTCmd.DV_READ_INFRARED_DISTANCE:
                if(packet.getmParam()[0] != -1){
                    if(mView != null) {
                        mView.infraredSensor(packet.getmParam()[0]);
                    }
                }
                break;
            case BTCmd.DV_6D_GESTURE:
                if(packet.getmParam()[0] != -1){
                    if(mView != null) {
                        mView.read6DState(packet.getmParam()[0]);
                    }
                }
                break;
            case BTCmd.DV_READ_ROBOT_ACCELERATION: //用于温湿度传感器
                String value = BluetoothParamUtil.bytesToString(packet.getmParam());
                if(mView != null){
                    mView.tempState(value);
                }
                break;
            case BTCmd.DV_INTO_EDIT:

                break;
            default:
                break;
        }
    }

    /**
     * 蓝牙连接断开状态
     *
     * @param serviceStateChanged
     */
    @org.greenrobot.eventbus.Subscribe
    public void onBluetoothServiceStateChanged(BTServiceStateChanged serviceStateChanged) {
        ViseLog.i("getState:" + serviceStateChanged.toString());
        mConnectState = serviceStateChanged.getState();
        switch (serviceStateChanged.getState()) {
            case BluetoothState.STATE_CONNECTED://蓝牙配对成功
                ViseLog.d("蓝牙配对成功");
                // stopConnectBleTask();
                break;
            case BluetoothState.STATE_CONNECTING://正在连接
                ViseLog.e("正在连接");
                break;
            case BluetoothState.STATE_DISCONNECTED:
                ViseLog.e("蓝牙连接断开");
                unRegister();
                break;
            default:

                break;
        }
    }

    public int  getConnectState(){
        return mConnectState;
    }
}
