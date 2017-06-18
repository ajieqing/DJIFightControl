package com.dji.djiflightcontrol.common.media;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.dji.djiflightcontrol.R;

import java.io.File;
import java.util.ArrayList;

public class MediaAdapter extends BaseAdapter {
    private ArrayList<MyMedia> mediaList;
    private Context context;
    private File flie;

    public MediaAdapter(Context context, ArrayList<MyMedia> mediaList) {
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

    public void setData(ArrayList<MyMedia> mediaList) {
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
            ivThumbnail.setImageDrawable(context.getResources().getDrawable(R.mipmap.bg));
            return convertView;
        }
        MyMedia djiMedia = mediaList.get(position);
        flie = new File(djiMedia.getName());
        djiMedia.setSuccess(new MyMedia.OnSuccess() {
            @Override
            public void go() {
                Glide.with(context).load(flie).into(ivThumbnail);
            }
        });
        if (flie.exists()) {
            Glide.with(context).load(flie).into(ivThumbnail);
        } else {
            Glide.with(context).load(R.mipmap.logo).into(ivThumbnail);
            djiMedia.fetchThumbnail();
        }
        return convertView;
    }

    public static class ViewHolder {
        ImageView ivThumbnail;
    }
}
