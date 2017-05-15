package com.dji.djiflightcontrol.common;

import android.graphics.Bitmap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import dji.common.error.DJIError;
import dji.sdk.camera.DJIMedia;
import dji.sdk.camera.DJIMediaManager;

/**
 * Created by 杰 on 2016/12/13.
 */

public class MyMedio {
    public static String dir = "com/dji/djiflightcontrol/picture";
    private double h;
    private double la;
    private double lo;
    private String name;
    private DJIMedia djiMedia;
    private OnSuccess success;

    public MyMedio(DJIMedia djiMedia) {
        this.djiMedia = djiMedia;
        h = -1;
        la = lo = 0;
        name = djiMedia.getFileName();
    }

    public MyMedio(double h, double la, double lo, DJIMedia djiMedia) {
        this.djiMedia = djiMedia;
        this.h = h;
        this.la = la;
        this.lo = lo;
        name = djiMedia.getFileName();
    }

    public DJIMedia getDjiMedia() {
        return djiMedia;
    }

    public void setDjiMedia(DJIMedia djiMedia) {
        this.djiMedia = djiMedia;
    }

    public void setSuccess(OnSuccess success) {
        this.success = success;
    }

    public void fetchThumbnail() {
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
                        try {
                            File file = new File(dir + name);
                            file.getParentFile().mkdirs();
                            file.createNewFile();
                            /*    内存卡不足shi   */
                            FileOutputStream stream = new FileOutputStream(file);
                            ObjectOutputStream objectOutput = new ObjectOutputStream(stream);
                            objectOutput.writeObject(bitmap);
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

    public void setH(float h) {
        this.h = h;
    }

    public double getLa() {
        return la;
    }

    public void setLa(float la) {
        this.la = la;
    }

    public double getLo() {
        return lo;
    }

    public void setLo(float lo) {
        this.lo = lo;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "名称" + name + "\n高度" + h + "\n经度" + lo + "\n纬度" + la + "\n";
    }

    public interface OnSuccess {
        void go();
    }
}
