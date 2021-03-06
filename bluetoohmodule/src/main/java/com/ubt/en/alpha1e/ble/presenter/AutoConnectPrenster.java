package com.ubt.en.alpha1e.ble.presenter;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;

import com.ubt.baselib.BlueTooth.BTDeviceFound;
import com.ubt.baselib.BlueTooth.BTDiscoveryStateChanged;
import com.ubt.baselib.BlueTooth.BTReadData;
import com.ubt.baselib.BlueTooth.BTServiceStateChanged;
import com.ubt.baselib.BlueTooth.BTStateChanged;
import com.ubt.baselib.BlueTooth.BleDevice;
import com.ubt.baselib.EnterBackgroundEvent;
import com.ubt.baselib.MyLifecycleCallback;
import com.ubt.baselib.btCmd1E.BTCmd;
import com.ubt.baselib.btCmd1E.ProtocolPacket;
import com.ubt.baselib.btCmd1E.cmd.BTCmdHandshake;
import com.ubt.baselib.customView.BaseBTDisconnectDialog;
import com.ubt.baselib.model1E.ManualEvent;
import com.ubt.baselib.utils.ULog;
import com.ubt.bluetoothlib.base.BluetoothState;
import com.ubt.bluetoothlib.blueClient.BlueClientUtil;
import com.vise.log.ViseLog;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.litepal.crud.DataSupport;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author：liuhai
 * @date：2018/4/11 11:11
 * @modifier：ubt
 * @modify_date：2018/4/11 11:11
 * [A brief description]
 * version
 */

public class AutoConnectPrenster {

    private static final String TAG = "BleConnectPrenster";
    private BlueClientUtil mBlueClient;
    private int mConnectState;
    private int mPreState;
    private Context mContext;
    public List<BleDevice> mBleDevices = new ArrayList<>();
    boolean isConnecting;

    private Date lastTime_onConnectState = null;

    private Date lastTime_DV_HANDSHAKE = null;//握手次数时间

    private boolean isManualConnectMode = false;//是否进入手动连接蓝牙页面
    private boolean isManualDisConnect = false;//手动断开蓝牙


    private boolean isCancleScan;

    private static final long TIME_OUT = 30 * 1000;

    /**
     * 蓝牙搜索超时
     */
    private static final int MESSAG_SEARCH_TIMEOUT = 0x111;

    /**
     * 蓝牙连接超时
     */
    private static final int MESSAG_CONNECT_TIMEOUT = 0x112;

    /**
     * 握手连接超时
     */
    private static final int MESSAG_HANDSHAKE_TIMEOUT = 0x113;


    /**
     * 初始化服务
     *
     * @param context
     */

    public void register(Context context) {
        this.mContext = context;
        mBlueClient = BlueClientUtil.getInstance();
        EventBus.getDefault().register(this);
        connectBleDevice();
        ViseLog.d("threadNmae==  register  " + Thread.currentThread().getName());
    }


    /**
     * 监听到蓝牙开启立即申请定位权限
     *
     * @param stateChanged
     */
    @Subscribe
    public void onActionStateChanged(BTStateChanged stateChanged) {
        ViseLog.i(stateChanged.toString());
        if (stateChanged.getState() == BTStateChanged.STATE_ON) {
            ViseLog.e("开启蓝牙");
            if (!isManualConnectMode && !isManualDisConnect) {
                connectBleDevice();
            }
        }
    }

    /***
     *连接蓝牙设备
     */
    private void connectBleDevice() {
        //如果APP在前台并且没有连接蓝牙
        if (!MyLifecycleCallback.isBackground() && mBlueClient.getConnectionState() != 3 && !isManualDisConnect && !isManualConnectMode) {
            BleDevice bleDevice = DataSupport.findFirst(BleDevice.class);
            if (bleDevice != null && mBlueClient.isEnabled()) {
                ViseLog.e("connectBleDevice" + bleDevice.toString());
                mHandler.removeMessages(MESSAG_CONNECT_TIMEOUT);
                mBlueClient.connect(bleDevice.getMac());
                isConnecting = true;
                isCancleScan = true;
                mHandler.sendEmptyMessageDelayed(MESSAG_CONNECT_TIMEOUT, TIME_OUT);
            }
        }
    }

    /**
     * 进入退出手动连接
     *
     * @param manualEvent 进入为true 退出为false
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void doEntryManalConnect(ManualEvent manualEvent) {
        if (manualEvent.getEvent() == ManualEvent.Event.MANUAL_ENTER) {//进入蓝牙联网页面
            this.isManualConnectMode = manualEvent.isManual();
            ViseLog.d("是否进入蓝牙连接页面=isManualConnectMode==" + isManualConnectMode);
            if (isManualConnectMode && mBlueClient.getConnectionState() != 3) {
                ViseLog.d("进入蓝牙联网页面");
            } else {
                ViseLog.d("退出蓝牙联网页面");
                startScanBleDevice();
            }
        } else if (manualEvent.getEvent() == ManualEvent.Event.MANUAL_DISCONNECT) {//手动断开蓝牙连接
            this.isManualDisConnect = manualEvent.isManual();
        }
    }


    /**
     * APP是否进入后台
     *
     * @param backgroundEvent
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void doEntryManalConnect(EnterBackgroundEvent backgroundEvent) {
        if (backgroundEvent.getEvent() == EnterBackgroundEvent.Event.ENTER_BACKGROUND) {
            ViseLog.d("APP退到后台");
            isCancleScan = true;
            // cancelScanBleDevice();
        } else if (backgroundEvent.getEvent() == EnterBackgroundEvent.Event.ENTER_RESUME) {
            ViseLog.d("APP进入前台");
            startScanBleDevice();
        }
    }


    /**
     * 发现蓝牙设备
     *
     * @param deviceFound
     */
    @Subscribe
    public void onBlueDeviceFound(BTDeviceFound deviceFound) {
        BluetoothDevice device = deviceFound.getBluetoothDevice();
        int rssi = (short) deviceFound.getRssi();
        String name = device.getName();
        if (name != null && name.toLowerCase().contains("alpha1e") && !isManualConnectMode) {
            dealBleDevice(device);
            //  ViseLog.i("搜到蓝牙设备:" + deviceFound.getBluetoothDevice().getAddress());

        }

    }

    /**
     * 处理每次获取到的蓝牙设备
     *
     * @param device
     */

    public synchronized void dealBleDevice(BluetoothDevice device) {
        ViseLog.d("isconnecting===" + isConnecting);
        if (isConnecting) {
            return;
        }
        if (device != null) {
            BleDevice bleDevice = DataSupport.findFirst(BleDevice.class);
            String mac = device.getAddress();
            if (bleDevice != null) {
                ViseLog.d("localrecord===" + bleDevice.getMac() + "   mac====" + mac);
            }
            if (bleDevice != null && !TextUtils.isEmpty(mac)
                    && bleDevice.getMac().equals(mac) && !isManualConnectMode && !isManualDisConnect) {
                connectBleDevice();
            }

        }

    }

    /**
     * 读取蓝牙回调数据
     *
     * @param readData
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onReadData(BTReadData readData) {
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
            case BTCmd.DV_HANDSHAKE:
                if (!isManualConnectMode) {
                    // 校验握手时间接受多次数据-----------start
                    Date curDate = new Date(System.currentTimeMillis());
                    float time_difference = 1000;
                    if (lastTime_DV_HANDSHAKE != null) {
                        time_difference = curDate.getTime()
                                - lastTime_DV_HANDSHAKE.getTime();
                    }
                    lastTime_DV_HANDSHAKE = curDate;

                    if (time_difference < 1000) {
                        ViseLog.d("1S 接收到多次握手成功次数");
                        return;
                    }
                    BaseBTDisconnectDialog.getInstance().dismiss();
                    ViseLog.e("-----------握手成功----------与机器人正式连接");
                    ULog.d("AutoConnectPresenter","自动连接时握手协议成功");
                    ManualEvent manualEvent = new ManualEvent(ManualEvent.Event.CONNECT_ROBOT_SUCCESS);
                    manualEvent.setManual(true);
                    EventBus.getDefault().post(manualEvent);
                    mHandler.removeMessages(MESSAG_HANDSHAKE_TIMEOUT);
                }
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
    @Subscribe
    public void onBluetoothServiceStateChanged(BTServiceStateChanged serviceStateChanged) {
        ViseLog.i("getState:" + serviceStateChanged.toString());
        mConnectState = serviceStateChanged.getState();
        switch (serviceStateChanged.getState()) {
            case BluetoothState.STATE_CONNECTED://蓝牙配对成功
                ViseLog.d("蓝牙配对成功");
                if (!isManualConnectMode) {

                    // stopConnectBleTask();
                    mHandler.removeMessages(MESSAG_CONNECT_TIMEOUT);
                    // 收到蓝牙连接状态命令时间相隔-----------start
                    Date curDate = new Date(System.currentTimeMillis());
                    float time_difference = 1000;
                    if (lastTime_onConnectState != null) {
                        time_difference = curDate.getTime() - lastTime_onConnectState.getTime();
                    }
                    lastTime_onConnectState = curDate;
                    if (time_difference < 500) {
                        return;
                    }

                    mBlueClient.sendData(new BTCmdHandshake().toByteArray());
                    mHandler.sendEmptyMessageDelayed(MESSAG_HANDSHAKE_TIMEOUT, TIME_OUT);
                }
                break;
            case BluetoothState.STATE_CONNECTING://正在连接
                ViseLog.e("正在连接");
                break;
            case BluetoothState.STATE_DISCONNECTED:
                ViseLog.e("蓝牙连接断开");
                isConnecting = false;
                startScanBleDevice();
                break;
            default:

                break;
        }
    }

    /**
     * 蓝牙搜索状态
     *
     * @param stateChanged
     */
    @Subscribe
    public void onActionDiscoveryStateChanged(BTDiscoveryStateChanged stateChanged) {
        ViseLog.i("getDiscoveryState:" + stateChanged.getDiscoveryState());
        if (stateChanged.getDiscoveryState() == BTDiscoveryStateChanged.DISCOVERY_STARTED) {
            ViseLog.d("开始扫描-----------");
        } else if (stateChanged.getDiscoveryState() == BTDiscoveryStateChanged.DISCOVERY_FINISHED) {
            //蓝牙在固定时间扫描结束后重新扫描
            ViseLog.d("蓝牙扫描结束------onActionDiscoveryStateChanged" + "---isCancleScan===" + isCancleScan + " -----isManualDisConnect==" + isManualDisConnect + " isManualConnectMode==" + isManualConnectMode);
            if (!isCancleScan && !isManualDisConnect && mBlueClient.getConnectionState() != 3 && !isManualConnectMode) {
                ViseLog.d("蓝牙扫描结束后重新开始扫描------");
                mBlueClient.startScan();
            }
        }

    }


    public void unRegister() {
        mHandler.removeMessages(MESSAG_SEARCH_TIMEOUT);
        mHandler.removeMessages(MESSAG_CONNECT_TIMEOUT);
        mHandler.removeMessages(MESSAG_HANDSHAKE_TIMEOUT);
        EventBus.getDefault().unregister(this);
        mBlueClient.cancelScan();
        isCancleScan = true;
    }


    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MESSAG_SEARCH_TIMEOUT:
                    ViseLog.d("没有搜索到蓝牙!");
                    break;
                case MESSAG_CONNECT_TIMEOUT:
//                    isConnecting = false;
//                    mBlueClient.disconnect();
                    ViseLog.d("连接蓝牙超时失败");
//                    if (mView != null) {
//                        mView.connectFailed();
//                    }
//                    break;
                case MESSAG_HANDSHAKE_TIMEOUT:
                    isConnecting = false;
                    if (isManualConnectMode && isManualDisConnect) {
                        mBlueClient.disconnect();
                        ViseLog.d("握手超时失败");
                        startScanBleDevice();
                    }
                    break;
            }
        }
    };


    /**
     * 扫描蓝牙
     */
    private void startScanBleDevice() {
        ViseLog.d("开始扫描蓝牙startScanBleDevice" + "isManualDisConnect==" + isManualDisConnect+"  ----isManualConnectMode=="+isManualConnectMode);
        if (!isManualDisConnect && !isManualConnectMode && mBlueClient.getConnectionState() != BluetoothState.STATE_CONNECTED && !MyLifecycleCallback.isBackground()) {
            isCancleScan = false;
            ViseLog.d("开始扫描蓝牙startScanBleDevice");
            mBlueClient.startScan();
        }
    }

    private void cancelScanBleDevice() {
        isCancleScan = true;
        if (mBlueClient.getConnectionState() != BluetoothState.STATE_CONNECTED && mBlueClient.getConnectionState() != BluetoothState.STATE_CONNECTING) {
            ViseLog.d("取消扫描蓝牙");
            mBlueClient.cancelScan();
        }
    }

}
