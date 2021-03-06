package com.ubt.baselib.utils;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.view.View;

import com.orhanobut.dialogplus.DialogPlus;
import com.ubt.baselib.R;
import com.ubt.baselib.customView.BaseDialog;
import com.ubt.baselib.globalConst.PermissionConst;
import com.vise.log.ViseLog;
import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.Permission;
import com.yanzhenjie.permission.PermissionListener;
import com.yanzhenjie.permission.Rationale;
import com.yanzhenjie.permission.RationaleListener;
import com.yanzhenjie.permission.SettingService;

import java.util.Arrays;
import java.util.List;

/**
 * @author：liuhai
 * @date：2017/11/14 11:12
 * @modifier：ubt
 * @modify_date：2017/11/14 11:12
 * [A brief description]
 * version
 */

public class PermissionUtils {

    private Context mContext;
    private PermissionLocationCallback mCallback;
    private static volatile PermissionUtils instance;

    public enum PermissionEnum {
        LOACTION, CAMERA, STORAGE, MICROPHONE
    }

    private PermissionUtils() {
    }

    public static PermissionUtils getInstance() {
        if (instance == null) {
            synchronized (PermissionUtils.class) {
                instance = new PermissionUtils();
            }
        }
        return instance;
    }

    /**
     * 请求授权
     *
     * @param callback 回调结果
     */
    public void request(PermissionLocationCallback callback, PermissionEnum permission, Context context) {
        this.mContext = context;
        this.mCallback = callback;
        String sp_key = "";
        String[] permiss = null;
        switch (permission) {
            case LOACTION:
                sp_key = PermissionConst.SP_PERMISSION_LOCATION;
                permiss = Permission.LOCATION;
                break;
            case CAMERA:
                sp_key = PermissionConst.SP_PERMISSION_CAMERA;
                permiss = Permission.CAMERA;
                break;
            case STORAGE:
                sp_key = PermissionConst.SP_PERMISSION_STORAGE;
                permiss = Permission.STORAGE;
                break;
            case MICROPHONE:
                sp_key = PermissionConst.SP_PERMISSION_MICROPHONE;
                permiss = Permission.MICROPHONE;
                break;
            default:
                break;
        }
        ViseLog.d("sp_key==" + sp_key);
        if (TextUtils.isEmpty(sp_key) || null == permiss) {
            return;
        }
        boolean isFirstLocation = SPUtils.getInstance().getBoolean(sp_key, false);
        if (AndPermission.hasPermission(mContext, permiss)) {
            mCallback.onSuccessful();
        } else if (isFirstLocation && AndPermission.hasAlwaysDeniedPermission(mContext, Arrays.asList(permiss))) {
            mCallback.onRationSetting();
            showRationSettingDialog(permission, context, mCallback);
        } else {
            AndPermission.with(mContext)
                    .requestCode(10000)
                    .permission(permiss)
                    .callback(permissionListener)
                    // rationale作用是：用户拒绝一次权限，再次申请时先征求用户同意，再打开授权对话框；
                    // 这样避免用户勾选不再提示，导致以后无法申请权限。
                    // 你也可以不设置。
                    .rationale(new RationaleListener() {
                        @Override
                        public void showRequestPermissionRationale(int requestCode, Rationale rationale) {
                            rationale.resume();
                        }
                    })
                    .start();
            SPUtils.getInstance().put(sp_key, true);
        }

    }

    /**
     * 用户勾选过不再提醒则显示该设置对话框跳转到应用详情页
     */
    public void showRationSettingDialog(PermissionEnum permission, Context context, final PermissionLocationCallback callback) {
        final SettingService settingService = AndPermission.defineSettingDialog(mContext);
        int message = 0;
        switch (permission) {
            case LOACTION:
                message = R.string.dialog_permission_location_setting;
                break;
            case CAMERA:
                message = R.string.dialog_permission_camera_setting;
                break;
            case STORAGE:
                message = R.string.dialog_permission_storage_setting;
                break;
            case MICROPHONE:
                message = R.string.dialog_permission_microphone_setting;
                break;
            default:
                break;
        }
        ViseLog.d("message==" + message);

        new BaseDialog.Builder(context).setTitle(R.string.base_setting)
                .setMessage(message).
                setConfirmButtonId(R.string.base_setting)
                .setCancleButtonID(R.string.base_cancel)
                .setButtonOnClickListener(new BaseDialog.ButtonOnClickListener() {
                    @Override
                    public void onClick(DialogPlus dialog, View view) {
                        if (view.getId() == R.id.button_confirm) {
                            settingService.execute();
                            if (callback != null) {
                                callback.onCancelRationSetting();
                            }
                        } else if (view.getId() == R.id.button_cancle) {
                            if (callback != null) {
                                callback.onCancelRationSetting();
                            }
                        }
                        dialog.dismiss();
                    }
                }).create().show();


    }


    /**
     * 回调监听。
     */
    private PermissionListener permissionListener = new PermissionListener() {
        @Override
        public void onSucceed(int requestCode, @NonNull List<String> grantPermissions) {
            mCallback.onSuccessful();

        }

        @Override
        public void onFailed(int requestCode, @NonNull List<String> deniedPermissions) {
            mCallback.onFailure();
        }
    };


    public boolean hasPermission(@NonNull String... permissions) {
        return AndPermission.hasPermission(mContext, permissions);
    }


    public interface PermissionLocationCallback {
        /**
         * 授权成功
         */
        void onSuccessful();

        /**
         * 授权失败
         */
        void onFailure();

        /**
         * 已经勾选拒绝过
         */
        void onRationSetting();

        /**
         * 取消RationSetting
         */
        void onCancelRationSetting();
    }

}
