package com.ubt.en.alpha1e.action;

import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.ubt.baselib.mvp.MVPBaseActivity;
import com.ubt.en.alpha1e.action.adapter.SelectGridAdapter;
import com.ubt.en.alpha1e.action.contact.SaveActionContact;
import com.ubt.en.alpha1e.action.model.ActionTypeModel;
import com.ubt.en.alpha1e.action.model.ActionsEditHelper;
import com.ubt.en.alpha1e.action.presenter.SaveActionPrenster;
import com.ubt.htslib.base.NewActionInfo;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

public class ActionSaveActivity extends MVPBaseActivity<SaveActionContact.View, SaveActionPrenster> implements SaveActionContact.View, BaseQuickAdapter.OnItemClickListener {

    @BindView(R2.id.iv_back)
    ImageView mIvBack;
    @BindView(R2.id.tv_main_title)
    TextView mTvMainTitle;
    @BindView(R2.id.iv_save)
    ImageView mIvSave;
    @BindView(R2.id.iv_save_arrow)
    ImageView mIvSaveArrow;
    @BindView(R2.id.rl_title)
    RelativeLayout mRlTitle;
    @BindView(R2.id.img_action_logo)
    ImageView mImgActionLogo;
    @BindView(R2.id.iv_demo1)
    ImageView mIvDemo1;
    @BindView(R2.id.iv_demo2)
    ImageView mIvDemo2;
    @BindView(R2.id.iv_demo3)
    ImageView mIvDemo3;
    @BindView(R2.id.txt_action_type)
    TextView mTxtActionType;
    @BindView(R2.id.txt_action_type_des)
    TextView mTxtActionTypeDes;
    @BindView(R2.id.edt_name)
    EditText mEdtName;
    @BindView(R2.id.txt_select_type)
    TextView mTxtSelectType;
    @BindView(R2.id.grid_actions_type)
    RecyclerView mGridActionsType;
    @BindView(R2.id.edt_disc)
    EditText mEdtDisc;
    @BindView(R2.id.lay_action_info)
    LinearLayout mLayActionInfo;
    RelativeLayout mLayHeadSel;
    private Unbinder mUnbinder;


    private SelectGridAdapter mSelectGridAdapter;

    private ActionTypeModel selectModel;

    private NewActionInfo mCurrentAction;//动作文件
    private String musicDir = "";
    public static String MUSIC_DIR = "music_dir";

    @Override
    public int getContentViewId() {
        return R.layout.activity_action_save;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mUnbinder = ButterKnife.bind(this);
        mPresenter.init(this);
        mCurrentAction = getIntent().getParcelableExtra(ActionsEditHelper.New_ActionInfo);//get parcelable object
        musicDir = getIntent().getStringExtra(MUSIC_DIR);
        initView();
    }


    /**
     * 初始化RecyleView Adapter
     */
    private void initView() {
        mGridActionsType.setLayoutManager(new GridLayoutManager(this, 5));
        mSelectGridAdapter = new SelectGridAdapter(R.layout.action_action_type_item, mPresenter.getTypeModelList());
        mGridActionsType.setAdapter(mSelectGridAdapter);
        mSelectGridAdapter.setOnItemClickListener(this);
        selectModel = mPresenter.getTypeModelList().get(0);
        setLeftImageShow();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mUnbinder.unbind();
    }

    /**
     * 点击动作类型事件
     *
     * @param adapter
     * @param view
     * @param position
     */
    @Override
    public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
        mPresenter.selectActionMode(position);
        selectModel = mPresenter.getTypeModelList().get(position);
        setLeftImageShow();
    }


    @OnClick({R2.id.iv_demo1, R2.id.iv_demo2, R2.id.iv_demo3})
    public void ClickView(View view) {
        int id = view.getId();
        if (id == R.id.iv_demo1) {
            selectModel.setLeftSelectedImage(selectModel.getImageTypeArray()[0]);
            mImgActionLogo.setImageResource(selectModel.getLeftSelectedImage());
        } else if (id == R.id.iv_demo2) {
            selectModel.setLeftSelectedImage(selectModel.getImageTypeArray()[1]);
            mImgActionLogo.setImageResource(selectModel.getLeftSelectedImage());
        } else if (id == R.id.iv_demo3) {
            selectModel.setLeftSelectedImage(selectModel.getImageTypeArray()[2]);
            mImgActionLogo.setImageResource(selectModel.getLeftSelectedImage());
        }
    }

    /**
     * 设置左边图片展示
     */
    private void setLeftImageShow() {
        mEdtDisc.setText(selectModel.getActionDescrion());
        mIvDemo1.setImageResource(selectModel.getImageTypeArray()[0]);
        mIvDemo2.setImageResource(selectModel.getImageTypeArray()[1]);
        mIvDemo3.setImageResource(selectModel.getImageTypeArray()[2]);
        mImgActionLogo.setImageResource(selectModel.getImageTypeArray()[0]);
        selectModel.setLeftSelectedImage(selectModel.getImageTypeArray()[0]);
    }

    /**
     * 刷新RecyleView
     */
    @Override
    public void notifyDataSetChanged() {
        mSelectGridAdapter.notifyDataSetChanged();
    }
}