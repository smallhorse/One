package com.ubt.en.alpha1e.action.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.ubt.en.alpha1e.action.R;
import com.vise.log.ViseLog;

import java.util.List;
import java.util.Map;

/**
 * @author wmma
 * @className
 * @description
 * @date
 * @update
 */


public class TimesHideRecycleViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = "FrameRecycleViewAdapter";
    private Context context;
    private List<Map<String, Object>> timeDatas;
    private FrameViewHolder viewHolder;

    private int defItem = -1;
    private OnItemListener onItemListener;

    private int zoom = 1;


    public TimesHideRecycleViewAdapter(Context context, List<Map<String, Object>> datas) {
        this.context = context;
        this.timeDatas = datas;
    }


    //define interface
    public  interface OnItemListener {
        void onItemClick(View view, int pos, Map<String, Object> data);
    }

    public void setOnItemListener(OnItemListener onItemListener) {
        this.onItemListener = onItemListener;
    }

    public void setDefSelect(int position) {
        this.defItem = position;
        notifyDataSetChanged();
    }

    public void scaleItem(int scale){
        zoom = scale;
        notifyDataSetChanged();
    }



    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        ViseLog.d(TAG, "onCreateViewHolder");

//        changeViewLen(orignalView, (Integer.valueOf(viewType) + 0f) / 100f);

        View view = LayoutInflater.from(context).inflate(R.layout.layout_new_action_item_time_hide, parent, false);

        viewHolder = new FrameViewHolder(view);

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        ViseLog.d(TAG, "onBindViewHolder");

        FrameViewHolder viewHolder = (FrameViewHolder)holder;
        String time = (String)timeDatas.get(position).get("time");

        changeViewLen(viewHolder.itemView,zoom, time);

        int state = Integer.valueOf((String)timeDatas.get(position).get("show"));
        if(state == 0){
            viewHolder.ivThumb.setVisibility(View.INVISIBLE);
        }else if(state == 1){
            ViseLog.d(TAG, "onBindViewHolder:" + state);
            viewHolder.ivThumb.setVisibility(View.VISIBLE);
        }


    }

    @Override
    public int getItemCount() {
        ViseLog.d(TAG, "size:" + timeDatas.size());
        return timeDatas.size();
    }

    @Override
    public int getItemViewType(int position) {


        return Integer.valueOf((String)(timeDatas.get(position).get("time")));
    }



    public class FrameViewHolder extends RecyclerView.ViewHolder{

       ImageView ivThumb;


        public FrameViewHolder(final View view) {
            super(view);

            ivThumb = (ImageView) view.findViewById(R.id.iv_thumb);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (onItemListener != null) {
                        onItemListener.onItemClick(v,getLayoutPosition(),timeDatas.get(getLayoutPosition()));
                    }
                }
            });


        }
    }

    private void changeViewLen(View view, int weight, String time) {
        changeViewLen(view, 0, weight, time);
    }

    private void changeViewLen(View view, float start_weight, int weight, String time) {
        ViseLog.d(TAG, "view:" + view.getId() + "weight:" + weight);

        int width = 0;
        int times = Integer.valueOf(time);
        width = times;

        if(weight == 1){
            width = width*1;
        }else if(weight == 2){
            width = width*2;
        }else if(weight == 3){
            width = width*3;
        }else if(weight == -1){
            width = width/2;
        }else if(weight == -2){
            width = width/4;
        }



//        int width = 50;
//        if(weight == 1.0){
//            width = 100;
//        }else if(weight == 2.0){
//            width = 150;
//        }else if(weight == 3.0){
//            width = 200;
//        }else if(weight == 1/2){
//            width = 25;
//        }else if(weight == 1/3){
//            width = 13;
//        }
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(width, 100);
        view.setLayoutParams(params);
    }


}
