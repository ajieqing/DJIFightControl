package com.dji.djiflightcontrol.common.media;

import android.graphics.Bitmap;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.support.annotation.RequiresApi;

import com.dji.djiflightcontrol.common.util.ImageInfo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import dji.common.error.DJIError;
import dji.sdk.camera.DJIMedia;
import dji.sdk.camera.DJIMediaManager;

import static com.dji.djiflightcontrol.common.DJISampleApplication.SDCARDAVALIABLE;
import static com.dji.djiflightcontrol.common.DJISampleApplication.dir;
import static com.dji.djiflightcontrol.common.DJISampleApplication.util;


/**
 * Created by 杰 on 2016/12/13.
 */

public class MyMedia {

    private double h;
    private String name, b_name;
    private DJIMedia djiMedia;
    private OnSuccess success;

    public MyMedia(DJIMedia djiMedia) {
        createIntance(-1, djiMedia, null);
    }

    private void createIntance(double h, DJIMedia djiMedia, String name) {
        this.djiMedia = djiMedia;
        this.h = h;
        if (djiMedia == null) {
            this.name = null;
        } else {
            this.name = dir+ djiMedia.getFileName() + ".jpeg";
        }
        b_name = name;
    }

    public MyMedia(double h, DJIMedia djiMedia, String name) {
        createIntance(h, djiMedia, name);
    }

    public MyMedia(double h, String s) {
        createIntance(h, null, s);
    }

    public String getTime() {
        if (djiMedia != null)
            return djiMedia.getCreatedDate();
        else return "";
    }

    public DJIMedia getDjiMedia() {
        return djiMedia;
    }

    void setSuccess(OnSuccess success) {
        this.success = success;
    }

    void fetchThumbnail() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (djiMedia != null)
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

                        @RequiresApi(api = Build.VERSION_CODES.N)
                        @Override
                        public void onSuccess(Bitmap bitmap) {
                            try {
                                long size;
                                if (SDCARDAVALIABLE){
                                    size=getAvailableExternalMemorySize();
                                }else {
                                    size=getAvailableInternalMemorySize();
                                }
                                if (size<100*1024*1024){
                                    util.showToast("手机内存不足，请清理内存");
                                    return;
                                }
                                File file = new File(name);
                                file.getParentFile().mkdirs();
                                file.createNewFile();
                                FileOutputStream stream = new FileOutputStream(file);
                                ObjectOutputStream objectOutput = new ObjectOutputStream(stream);
                                bitmap.compress(Bitmap.CompressFormat.JPEG,100,objectOutput);
                                ImageInfo imageInfo = new ImageInfo(file.getPath());
                                imageInfo.setAttribute(ImageInfo.H, String.valueOf(h));
                                imageInfo.setAttribute(ImageInfo.NAME,b_name);
                                objectOutput.close();
                                stream.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            if (success != null)
                                success.go();
                        }

                        @Override
                        public void onFailure(DJIError djiError) {

                        }
                    });
            }
        }).start();
    }

    public double getH() {
        return h;
    }

    public String getName() {
        return name;
    }

    public String getB_name() {
        return b_name;
    }

    interface OnSuccess {
        void go();
    }
    private static long getAvailableExternalMemorySize() {
            File path = Environment.getExternalStorageDirectory();
            StatFs stat = new StatFs(path.getPath());
            long blockSize = stat.getBlockSize();
            long availableBlocks = stat.getAvailableBlocks();
            return availableBlocks * blockSize;
    }
    private static long getAvailableInternalMemorySize() {
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSize();
        long availableBlocks = stat.getAvailableBlocks();
        return availableBlocks * blockSize;
    }

}
