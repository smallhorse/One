package com.ubt.loginmodule.useredit;

import android.app.Activity;

import com.ubt.baselib.model1E.UserAllModel;
import com.ubt.baselib.model1E.UserModel;
import com.ubt.baselib.mvp.BasePresenter;
import com.ubt.baselib.mvp.BaseView;

import java.util.List;

/**
 * MVPPlugin
 * 邮箱 784787081@qq.com
 */

public class UserEditContract {
    public interface View extends BaseView {
        void getAgeDataList(List<String> list);

        void takeImageFromShoot();

        void takeImageFromAblum();

        void ageSelectItem(int type, String age);

        void updateUserModelSuccess(UserModel userModel);
        void updateUserModelFailed(String str);

        void updateLoopData(UserAllModel userAllModel);
    }

    interface Presenter extends BasePresenter<View> {
        void showImageHeadDialog(Activity activity);

        void showImageCenterHeadDialog(Activity activity);

        void showAgeDialog(Activity activity, List<String> ageList, int currentPosition);

        void showGradeDialog(Activity activity, int currentPosition, List<String> list);
    }
}
