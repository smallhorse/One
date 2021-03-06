package com.ubt.baselib.globalConst;

/**
 * @作者：bin.zhang@ubtrobot.com
 * @日期: 2018/1/18 13:06
 * @描述: 全局的常量定义
 */

public interface Constant1E {
    /**
     * 机器人productId 和DSN
     */
    String SP_ROBOT_PRODUCT_ID = "sp_robot_product_id";
    String SP_ROBOT_DSN = "sp_robot_dsn";

    public final String IS_FIRST_ENTER_GREET = "first_enter_greet";

    public final String IS_FIRST_ENTER_WIFI_LIST = "first_search_wifi";
    /**
     * 进入蓝牙状态页面
     */
    public final String ENTER_BLESTATU_ACTIVITY = "enter_ble_statu_activity";
    /**
     * 用户账号属性
     */
    String SP_USER_ID = "sp_login_userId";
    String SP_USER_TOKEN = "sp_login_token";
    String SP_USER_EMAIL = "sp_user_email";
    String SP_LOGIN = "sp_login";
    String SP_USER_NAME= "sp_user_name";
    String SP_USER_SEX = "sp_user_sex";
    String SP_USER_AGE = "sp_user_age";
    /**
     * 用户信息保存
     */
    String SP_USER_INFO = "sp_user_info";
    String SP_USER_IMAGE = "sp_user_image";
    String SP_USER_NICKNAME = "sp_user_nickname";
    String PRINCIPLE_PROGRESS = "sp_principle_progress_";
    String SP_SHOW_REMOTE_PUBLISH = "sp_show_remote_publish";
    String SP_AUTO_UPGRADE = "sp_auto_upgrade";
    String PRINCIPLE_ENTER_PROGRESS = "sp_principle_enter_progress";
    String SP_SHOW_COMMON_GUIDE = "sp_show_common_guide";


    int KEY_NICK_NAME = 1;  //昵称
    int KEY_NICK_SEX = 2; //性别
    int KEY_NICK_AGE = 3; //年龄
    int KEY_NICK_GRADE = 4; //年级
    int KEY_NICK_HEAD = 5; //头像

    public static final String EMPTY_NICK_NAME = "empty_nick_name";
    public static final String EMPTY_SEX = "empty_sex";
    public static final String EMPTY_BIRTHDAY = "empty_birthDay";


    public static final String REMOTE_SHOW_TIP = "remote_show_tip";


    /**
     * 当前语言包版本
     */
    public static final String CURRENT_APP_LANGUAGE_VERSION = "current_app_language_version";

    /**
     * 当前选择的语言
     */
    public static final String CURRENT_APP_LANGUAGE = "current_app_language";


    /**
     * 语言包地址
     */
    public static final String LANUGAGE_PATH = "skins";

    /**
     * 语言包下载
     */
    public static final String LANUGAGE_DOWN_PATh = "download";

    /**
     * 语言包名称
     */
    public static final String LANGUAGE_NAME = "language.skin";
    /**
     * 语言压缩包名称
     */
    public static final String LANGUAGE_ZIP_NAME = "language.zip";


    //用户信息相关
    public static final String PRODUCT_ID = "10401";
    public static final String APPID = "100010074";
    public static final String APPKEY = "ff16f3c367ba4a6aae2b1f4f0ce6717e";
}
