package com.ubt.en.alpha1e.services;

import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.alibaba.android.arouter.launcher.ARouter;
import com.orhanobut.dialogplus.DialogPlus;
import com.orhanobut.dialogplus.OnClickListener;
import com.orhanobut.dialogplus.ViewHolder;
import com.tencent.android.tpush.XGPushShowedResult;
import com.ubt.baselib.BlueTooth.BTReadData;
import com.ubt.baselib.BlueTooth.BTServiceStateChanged;
import com.ubt.baselib.btCmd1E.BTCmd;
import com.ubt.baselib.btCmd1E.BluetoothParamUtil;
import com.ubt.baselib.btCmd1E.ProtocolPacket;
import com.ubt.baselib.btCmd1E.cmd.BTCmdReadBattery;
import com.ubt.baselib.btCmd1E.cmd.BTCmdStartUpgradeSoft;
import com.ubt.baselib.commonModule.ModuleUtils;
import com.ubt.baselib.customView.BaseBTDisconnectDialog;
import com.ubt.baselib.customView.BaseDialog;
import com.ubt.baselib.customView.BaseLowBattaryDialog;
import com.ubt.baselib.skin.SkinManager;
import com.ubt.baselib.utils.ActivityTool;
import com.ubt.baselib.utils.AppStatusUtils;
import com.ubt.baselib.utils.GsonImpl;
import com.ubt.baselib.utils.ToastUtils;
import com.ubt.bluetoothlib.base.BluetoothState;
import com.ubt.bluetoothlib.blueClient.BlueClientUtil;
import com.ubt.en.alpha1e.R;
import com.ubt.en.alpha1e.ble.dialog.SwitchIngLanguageDialog;
import com.ubt.en.alpha1e.ble.model.BleBaseModelInfo;
import com.ubt.en.alpha1e.ble.model.BleUpgradeProgressRsp;
import com.ubt.en.alpha1e.xinge.XGConstact;
import com.vise.log.ViseLog;
import com.vise.utils.convert.HexUtil;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Timer;
import java.util.TimerTask;

/**
 * @作者：bin.zhang@ubtrobot.com
 * @日期: 2018/1/8 15:16
 * @描述: 处理全局消息的服务
 */

public class GlobalMsgService extends Service {

    private boolean isNeed20Toast = true; //是否需要显示电量低于20%的Toast
    private boolean isNeed5Dialog = true; //是否需要显示电量低于5%的Dialog
    private Timer batteryTimer = null; //电量查询定时器

    private static final int UPDATE_UPGRADE_PROGRESS_RSP = 1;
    private static final int BLUETOOTH_DISCONNECT = 2;
    private static final int ROBOT_REQUEST_UPDATE = 3;
    private SwitchIngLanguageDialog switchProgressDialog = null;
    private BleUpgradeProgressRsp mUpgradeProgressRsp = null;

    private DialogPlus updateDialog = null;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case UPDATE_UPGRADE_PROGRESS_RSP:
                    try {
                        BleUpgradeProgressRsp upgradeProgressRsp = (BleUpgradeProgressRsp) msg.obj;

                        if (upgradeProgressRsp != null && ActivityTool.currentActivity() != null) {
                            mUpgradeProgressRsp = upgradeProgressRsp;

                            if (upgradeProgressRsp.name.equals("chip_instruction") || upgradeProgressRsp.name.equals("chip_firmware")) {
                                int type = 0;
                                if ("chip_firmware".equals(upgradeProgressRsp.name)) {
                                    type = 1;
                                }

                                if (upgradeProgressRsp.result == 0) {
                                    if (upgradeProgressRsp.progress == 100) {
                                        if (switchProgressDialog != null) {
                                            switchProgressDialog.dismiss();
                                            showSetLanguageDialog(ActivityTool.currentActivity(), true, type);
                                        }
                                    } else {
                                        showSwitchLanguageDialog(ActivityTool.currentActivity(), upgradeProgressRsp.progress, type);
                                    }

                                } else if (upgradeProgressRsp.result > 0) {
                                    //不能用1、2，协议定的是1、2，但是测试发送，机器人端失败了，有时候回的是501
                                    //upgradeProgressRsp.result == 1 || upgradeProgressRsp.result == 2
                                    ViseLog.d("switchProgressDialog = " + switchProgressDialog);
                                    if (switchProgressDialog != null) {
                                        switchProgressDialog.dismiss();
                                    }
                                    showSetLanguageDialog(ActivityTool.currentActivity(), false, type);

                                    /*if (upgradeProgressRsp.result == 1) {
                                        showSetLanguageDialog(ActivityTool.currentActivity(), false, type);
                                    } else {
                                        showLowBatteryDialog(ActivityTool.currentActivity());
                                    }*/
                                }
                            }
                        }
                    } catch (Exception ex) {
                        ViseLog.e("ex = " + ex.getMessage());
                    }
                    break;
                case BLUETOOTH_DISCONNECT:
                    try {
                        if (switchProgressDialog != null && switchProgressDialog.isShowing()) {
                            switchProgressDialog.dismiss();
                        }
                    } catch (final Exception e) {
                        // Handle or log or ignore
                        //处理bug27082,但是没有复现，暂时添加cry catch捕获异常
                        ViseLog.e("switchProgressDialog消失异常：" + e.getMessage());
                    } finally {
                        switchProgressDialog = null;
                    }
                    break;
                case ROBOT_REQUEST_UPDATE:
                    ViseLog.d("Message===handleMessage()");
                    showUpdateDialog();
                    break;
                default:
                    break;
            }
        }
    };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        EventBus.getDefault().register(this);
        ViseLog.i("onCreate");
//        initBTListener();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ViseLog.i("onDestroy");
        EventBus.getDefault().unregister(this);
    }

    /**
     * 蓝牙连接断开状态
     *
     * @param serviceStateChanged
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onBluetoothServiceStateChanged(BTServiceStateChanged serviceStateChanged) {
        ViseLog.i("getState:" + serviceStateChanged.toString());
        switch (serviceStateChanged.getState()) {
            case BluetoothState.STATE_CONNECTED://蓝牙配对成功
                queryBattery(true);
                break;
            case BluetoothState.STATE_CONNECTING://正在连接
                ViseLog.d("正在连接");
                break;
            case BluetoothState.STATE_DISCONNECTED:
                queryBattery(false);
                ViseLog.d("蓝牙连接断开 isForceDisBT:" + AppStatusUtils.isForceDisBT()
                        + "   isBtBussiness:" + AppStatusUtils.isBtBussiness());
                isNeed20Toast = true;
                isNeed5Dialog = true;
                mHandler.sendEmptyMessage(BLUETOOTH_DISCONNECT);
                if (!AppStatusUtils.isForceDisBT()) {
                    if (AppStatusUtils.isBtBussiness()) {
                        ViseLog.e("特殊处理状态，不弹窗");
                        return;
                    }
                    BaseBTDisconnectDialog.getInstance().show(new BaseBTDisconnectDialog.IDialogClick() {
                        @Override
                        public void onConnect() {
                            ARouter.getInstance().build(ModuleUtils.Bluetooh_BleStatuActivity).navigation();
                            BaseBTDisconnectDialog.getInstance().dismiss();
                        }

                        @Override
                        public void onCancel() {
                            ARouter.getInstance().build(ModuleUtils.Main_MainActivity).navigation();
                            BaseBTDisconnectDialog.getInstance().dismiss();
                        }
                    });
                } else { //强制退出，不处理
                    AppStatusUtils.setIsForceDisBT(false);
                }
                break;
            default:

                break;
        }
    }

    @Subscribe
    public void onDataSynEvent(XGPushShowedResult xgPushShowedResult) {
        ViseLog.i("onDataSynEvent event---->" + xgPushShowedResult.getContent());
        try {
            JSONObject mJson = new JSONObject(xgPushShowedResult.getCustomContent());
            if (mJson.getString("category").equals(XGConstact.BEHAVIOUR_HABIT)) {
                if (mJson.get("eventId") != null) {
                    Log.d("TPush", " contents" + xgPushShowedResult.getContent());
                   /* new HibitsAlertDialog(AppManager.getInstance().currentActivity()).builder()
                            .setCancelable(true)
                            .setEventId(mJson.get("eventId").toString())
                            .setMsg(xgPushShowedResult.getContent())
                            .show();*/
                    //  new LowBatteryDialog(AppManager.getInstance().currentActivity()).setBatteryThresHold(1000000).builder().show();
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    @Subscribe
    public void onBTRead(BTReadData data) {
        parseBTCmd(data);
    }

    private void parseBTCmd(BTReadData data) {
        ProtocolPacket packet = data.getPack();
        switch (packet.getmCmd()) {
            case BTCmd.DV_READ_BATTERY: //更新电量
                ViseLog.i("电量data:" + HexUtil.encodeHexStr(packet.getmParam()));
                ViseLog.i("电量 isNeed20Toast:" + isNeed20Toast + "  isNeed5Dialog:" + isNeed5Dialog +
                        "   isBussiness:" + AppStatusUtils.isBussiness());
                if (packet.getmParamLen() < 4) {
                    ViseLog.e("错误参数，丢弃!!!");
                    return;
                }
                int power = packet.getmParam()[3];
                AppStatusUtils.setCurrentPower(power);
                AppStatusUtils.setChargingStatus(packet.getmParam()[2]);
                if (0x00 == packet.getmParam()[2]) {
                    if (power > 5 && power <= 20) {
                        if (isNeed20Toast) {
                            isNeed20Toast = false;
                            ToastUtils.showCustomShortWithGravity(R.layout.base_toast_lowpower_20, Gravity.CENTER, 0, 0);
                        }
                        isNeed5Dialog = true;
                    } else if (power <= 5) {
                        if (isNeed5Dialog) {
                            isNeed5Dialog = false;
                            if (!AppStatusUtils.isBussiness()) { //非特殊处理模块集中弹低电提示
                                BaseLowBattaryDialog.getInstance().showLow5Dialog(null);
                            }
                        }
                    } else {
                        isNeed20Toast = true;
                    }
                }
                break;
            case BTCmd.DV_DO_UPGRADE_SOFT:
                ViseLog.d("系统软件请求升级1111111===="+packet.getmParam()[0]);
                if (0x01 == packet.getmParam()[0]) {
                    ViseLog.d("系统软件请求升级222222===="+packet.getmParam()[0]);
                    // BaseUpdateTipDialog.getInstance().show();
                    Message message = new Message();
                    message.what = ROBOT_REQUEST_UPDATE;
                    mHandler.sendMessage(message);
                }
                break;
            case BTCmd.DV_COMMON_CMD:
                ViseLog.d("DV_COMMON_CMD = " + BluetoothParamUtil.bytesToString(packet.getmParam()));
                String commonCmdJson = BluetoothParamUtil.bytesToString(packet.getmParam());
                BleBaseModelInfo bleBaseModel = GsonImpl.get().toObject(commonCmdJson, BleBaseModelInfo.class);
                ViseLog.d("bleBaseModel.event = " + bleBaseModel.event);
                if (bleBaseModel.event == 9) {
                    BleUpgradeProgressRsp switchLanguageRsp = GsonImpl.get().toObject(commonCmdJson, BleUpgradeProgressRsp.class);
                    ViseLog.d("switchLanguageRsp = " + switchLanguageRsp);

                    Message msg = new Message();
                    msg.what = UPDATE_UPGRADE_PROGRESS_RSP;
                    msg.obj = switchLanguageRsp;
                    mHandler.sendMessage(msg);
                }
                break;
            default:
                break;
        }
    }

    public boolean isRobotConnected() {
        return BlueClientUtil.getInstance().getConnectionState() == BTServiceStateChanged.STATE_CONNECTED;
    }

    public void queryBattery(boolean isStart) {
        if (!isRobotConnected()) {
            ViseLog.e("robot not connected");
            return;
        }
        if (isStart) {
            if (batteryTimer != null) {
                batteryTimer.cancel();
            }
            batteryTimer = new Timer();
            batteryTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if (isRobotConnected()) {
                        BlueClientUtil.getInstance().sendData(new BTCmdReadBattery().toByteArray());
                    }
                }
            }, 200, 60000);//每1分钟执行一次
        } else {
            if (batteryTimer != null) {
                batteryTimer.cancel();
                batteryTimer = null;
            }
        }
    }

    /**
     * 切换语言对话框
     */
    public void showSwitchLanguageDialog(Context context, int progress, int type) {

        if (mDialogPlus != null) {
            mDialogPlus.dismiss();
            mDialogPlus = null;
        }

        ViseLog.d("-switchLanguageDialog->");
        if (switchProgressDialog == null) {
            switchProgressDialog = new SwitchIngLanguageDialog(context, type).setCancel(false);
            switchProgressDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialogInterface) {
                    switchProgressDialog = null;
                }
            });
        }

        switchProgressDialog.setProgress(progress);
        if (!switchProgressDialog.isShowing()) {
            switchProgressDialog.doShow();
        }
    }

    /**
     * 显示设置语言对话框
     */
    public void showSetLanguageDialog(Context context, boolean isSuccess, int type) {
        if (mDialogPlus != null) {
            mDialogPlus.dismiss();
            mDialogPlus = null;
        }

        String message;
        int imgId;
        if (isSuccess) {
            if (type == 0) {
                if (TextUtils.isEmpty(mUpgradeProgressRsp.language)) {
                    message = SkinManager.getInstance().getTextById(R.string.about_robot_language_changing_success_1);
                } else {
                    message = SkinManager.getInstance().getTextById(R.string.about_robot_language_changing_success).replaceAll("#", mUpgradeProgressRsp.language);
                }

            } else {
                message = SkinManager.getInstance().getTextById(R.string.about_robot_upgrade_success);
            }
            imgId = R.drawable.img_language_ok;
        } else {
            if (type == 0) {
                message = SkinManager.getInstance().getTextById(R.string.about_robot_language_changing_fail);
            } else {
                message = SkinManager.getInstance().getTextById(R.string.about_robot_upgrade_fail);
            }
            imgId = R.drawable.img_language_failed;
        }

        View contentView = LayoutInflater.from(context).inflate(R.layout.base_dialog_set_language_result, null);
        TextView tvResult = contentView.findViewById(R.id.tv_result);
        tvResult.setText(message);
        ((ImageView) contentView.findViewById(R.id.iv_result)).setImageResource(imgId);
        ViewHolder viewHolder = new ViewHolder(contentView);
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        int width = (int) ((display.getWidth()) * 0.55); //设置宽度
        mDialogPlus = DialogPlus.newDialog(context)
                .setContentHolder(viewHolder)
                .setGravity(Gravity.CENTER)
                .setContentWidth(width)
                .setContentBackgroundResource(android.R.color.transparent)
                .setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(DialogPlus dialog, View view) {
                        if (view.getId() == R.id.btn_ok) {//点击确定以后刷新列表并解锁下一关
                            dialog.dismiss();
                            mDialogPlus = null;
                        }
                    }
                })
                .setCancelable(false)
                .create();
        mDialogPlus.show();


    }

    DialogPlus mDialogPlus = null;

    /**
     * 显示低电量
     */
    public void showLowBatteryDialog(Context context) {

        String titleMsg = SkinManager.getInstance().getTextById(R.string.about_robot_language_low_battery_tips_1);
        String detailMsg = SkinManager.getInstance().getTextById(R.string.about_robot_language_low_battery_tips_2);

        View contentView = LayoutInflater.from(context).inflate(R.layout.ble_dialog_low_battery, null);
        TextView tvTitle = contentView.findViewById(R.id.tv_title);
        TextView tvMessage = contentView.findViewById(R.id.tv_message);
        tvTitle.setText(titleMsg);
        tvMessage.setText(detailMsg);

        ViewHolder viewHolder = new ViewHolder(contentView);
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        int width = (int) ((display.getWidth()) * 0.55); //设置宽度

        DialogPlus.newDialog(context)
                .setContentHolder(viewHolder)
                .setGravity(Gravity.CENTER)
                .setContentWidth(width)
                .setContentBackgroundResource(android.R.color.transparent)
                .setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(DialogPlus dialog, View view) {
                        if (view.getId() == R.id.btn_ok) {//点击确定以后刷新列表并解锁下一关

                            dialog.dismiss();
                        }
                    }
                })
                .setCancelable(false)
                .create().show();

    }

    private void showUpdateDialog() {
        if (updateDialog != null && updateDialog.isShowing()) {
            updateDialog.dismiss();
            updateDialog = null;
        }
        ViseLog.d("=====showUpdateDialog===");
        if (ActivityTool.currentActivity() != null) {
            ViseLog.d("ActivityTool.currentActivity()==="+ActivityTool.currentActivity());
            updateDialog = new BaseDialog.Builder(ActivityTool.currentActivity())
                    .setMessage(R.string.base_install_tip)
                    .setConfirmButtonId(R.string.base_not_now)
                    .setConfirmButtonColor(R.color.black)
                    .setCancleButtonID(R.string.base_update)
                    .setCancleButtonColor(R.color.base_blue)
                    .setButtonOnClickListener(new BaseDialog.ButtonOnClickListener() {
                        @Override
                        public void onClick(DialogPlus dialog, View view) {
                            if (view.getId() == R.id.button_confirm) {
                                dialog.dismiss();
                            } else if (view.getId() == R.id.button_cancle) {
                                dialog.dismiss();
                                BlueClientUtil.getInstance().sendData(new BTCmdStartUpgradeSoft(BTCmdStartUpgradeSoft.START_UPDATE).toByteArray());
                                ViseLog.d("发送系统版本升级" + new BTCmdStartUpgradeSoft(BTCmdStartUpgradeSoft.START_UPDATE).toString());
                            }
                        }
                    }).create();
            updateDialog.show();
        }
    }

}
