package com.dji.djiflightcontrol.work;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import com.dji.djiflightcontrol.R;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

import dji.common.error.DJIError;
import dji.sdk.camera.DJIMedia;
import dji.sdk.camera.DJIMediaManager;

/**
 * 作者:姜帅杰
 * 版本:1.0
 * 创建日期:2016/10/11:18:12.
 */

public class MediaAdapter extends BaseAdapter {
    private ArrayList<DJIMedia> mediaList;
    private Context context;
    private File cacheDir;

    public MediaAdapter(Context context, ArrayList<DJIMedia> mediaList) {
        this.context = context;
        this.mediaList = mediaList;
        cacheDir = context.getCacheDir();
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

    public void setData(ArrayList<DJIMedia> mediaList) {
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
        final DJIMedia djiMedia = mediaList.get(position);
        viewHolder.id = djiMedia.getId();
        final Handler handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                Bitmap bm = (Bitmap) msg.obj;
                ivThumbnail.setImageBitmap(bm);
            }
        };
        new Thread(new Runnable() {
            @Override
            public void run() {
                djiMedia.fetchThumbnail(new DJIMediaManager.CameraDownloadListener<Bitmap>() {
                    @Override
                    public void onStart() {

                    }

                    @Override
                    public void onRateUpdate(long l, long l1, long l2) {

                    }

                    @Override
                    public void onProgress(long l, long l1) {

                    }

                    @Override
                    public void onSuccess(Bitmap bitmap) {
                        Message message = Message.obtain();
                        message.obj = bitmap;
                        handler.sendMessage(message);
                        try {
                            FileOutputStream outputStream = new FileOutputStream(cacheDir);
                            ObjectOutputStream stream= new ObjectOutputStream(outputStream);
                            stream.writeObject(bitmap);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onFailure(DJIError djiError) {

                    }
                });
            }
        }).start();
        return convertView;
    }

    public static class ViewHolder {
        ImageView ivThumbnail;
        int id;
    }
}
