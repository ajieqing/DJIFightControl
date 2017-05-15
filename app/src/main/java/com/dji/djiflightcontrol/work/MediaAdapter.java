package com.dji.djiflightcontrol.work;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.dji.djiflightcontrol.R;
import com.dji.djiflightcontrol.common.MyMedio;

import java.io.File;
import java.util.ArrayList;

/**
 * 作者:姜帅杰
 * 版本:1.0
 * 创建日期:2016/10/11:18:12.
 */

public class MediaAdapter extends BaseAdapter {
    private ArrayList<MyMedio> mediaList;
    private Context context;
    private File flie;

    public MediaAdapter(Context context, ArrayList<MyMedio> mediaList) {
        this.context = context;
        this.mediaList = mediaList;
    }

    @Override
    public int getCount() {
        return mediaList.size();
    }

    @Override
    public Object getItem(int position) {
        return mediaList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public void setData(ArrayList<MyMedio> mediaList) {
        this.mediaList = mediaList;
        notifyDataSetChanged();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if (convertView == null) {
            viewHolder = new ViewHolder();
            convertView = LayoutInflater.from(context).inflate(R.layout.item_media, null);
            viewHolder.ivThumbnail = (ImageView) convertView.findViewById(R.id.ivThumbnail);
            convertView.setTag(viewHolder);
        }
        viewHolder = (ViewHolder) convertView.getTag();
        final ImageView ivThumbnail = viewHolder.ivThumbnail;
        if (position >= mediaList.size()) {
            ivThumbnail.setImageDrawable(context.getResources().getDrawable(R.drawable.bg));
            return convertView;
        }
        final MyMedio djiMedia = mediaList.get(position);
        flie = new File(MyMedio.dir + djiMedia.getName());
        djiMedia.setSuccess(new MyMedio.OnSuccess() {
            @Override
            public void go() {
                Glide.with(context).load(flie).into(ivThumbnail);
            }
        });
        if (flie.exists()) {
            Glide.with(context).load(flie).into(ivThumbnail);
        } else {
            djiMedia.fetchThumbnail();
        }
        return convertView;
    }

    public static class ViewHolder {
        ImageView ivThumbnail;
        int id;
    }
}
