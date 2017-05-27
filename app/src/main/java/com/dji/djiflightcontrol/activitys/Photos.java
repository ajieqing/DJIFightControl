package com.dji.djiflightcontrol.activitys;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Gallery.LayoutParams;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.ViewSwitcher.ViewFactory;

import com.bumptech.glide.Glide;
import com.dji.djiflightcontrol.R;
import com.dji.djiflightcontrol.common.GestureListener;
import com.dji.djiflightcontrol.common.MyMedio;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;

import dji.common.camera.DJICameraSettingsDef;
import dji.common.error.DJICameraError;
import dji.common.error.DJIError;
import dji.common.util.DJICommonCallbacks;
import dji.sdk.camera.DJIMedia;
import dji.sdk.camera.DJIMediaManager;

import static com.dji.djiflightcontrol.common.DJISampleApplication.NAME;
import static com.dji.djiflightcontrol.common.DJISampleApplication.getCameraInstance;
import static com.dji.djiflightcontrol.common.DJISampleApplication.getProductInstance;
import static com.dji.djiflightcontrol.common.DJISampleApplication.util;

public class Photos extends Activity {
    ImageSwitcher imageSwitcher; //声明ImageSwitcher对象，图片显示区域
    ListView listView;
    private ArrayList<MyMedio> mediaList;
    private int position;
    private MediaAdapter mediaAdapter;
    private MessageHandler messageHandler;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.photos);
        mediaList = new ArrayList<>();
        //通过控件的ID获得imageSwitcher的对象
        imageSwitcher = (ImageSwitcher) findViewById(R.id.switcher);
        //设置自定义的图片显示工厂类
        imageSwitcher.setFactory(new MyViewFactory(Photos.this));
        imageSwitcher.setLongClickable(true);
        imageSwitcher.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                dialog();
                return true;
            }
        });
        imageSwitcher.setOnTouchListener(new GestureListener(this) {
            @Override
            public boolean left() {
                if (position == mediaList.size() - 1)
                    position = -1;
                position++;
                showNext();
                return true;
            }

            @Override
            public boolean right() {
                if (position == 0)
                    position = mediaList.size() - 1;
                position--;
                showNext();
                return true;
            }
        });

        //通过控件的ID获得gallery的对象
        listView = (ListView) findViewById(R.id.listview);
        //设置自定义的图片适配器
        mediaAdapter = new MediaAdapter(this, mediaList);
        listView.setAdapter(mediaAdapter);
        //实现被选中的事件监听器
        listView.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       final int position, long id) {
                Photos.this.position = position;
                showNext();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // TODO Auto-generated method stub
            }
        });
        Looper looper = Looper.myLooper();
        messageHandler = new MessageHandler(looper);
        if (getCameraInstance() != null) {
            getCameraInstance().setCameraMode(
                    DJICameraSettingsDef.CameraMode.MediaDownload,
                    new DJICommonCallbacks.DJICompletionCallback() {
                        @Override
                        public void onResult(DJIError djiError) {
                            if (null == djiError)
                                fetchMediaList();
                        }
                    }
            );
        }
        /*
        for (int i = 0; i < util.getFailureMedios().size(); i++) {
            listView.addView(mediaAdapter.getView(mediaList.size() + i, new View(this), null));
        }
        */
    }

    private void showNext() {
        MyMedio myMedio = mediaList.get(position);
        ImageView image = (ImageView) imageSwitcher.getNextView();
        Glide.with(Photos.this).load(new File(MyMedio.dir + myMedio.getName())).centerCrop().into(image);
        imageSwitcher.showNext();
    }

    protected void dialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(Photos.this);
        /*
        if (position >= mediaList.size()) {
            if (!util.getFailureMedios().isEmpty())
                builder.setMessage(util.getFailureMedios().get(position - mediaList.size()).toString());
        } else {
            int i = util.getMedios().size() - mediaList.size() + position;
            if (i >= 0 && i < util.getMedios().size())
                builder.setMessage(util.getMedios().get(i).toString() + "创建时间" + mediaList.get(position).getCreatedDate());
            else
                builder.setMessage("名字" + mediaList.get(position).getFileName() + "创建时间" + mediaList.get(position).getCreatedDate());
        }
        */
        String[] items = new String[]{"下载", "删除"};
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        util.setCameraMode(DJICameraSettingsDef.CameraMode.MediaDownload);
                        String path = getApplicationContext().getFilesDir().getAbsolutePath() + "/picture/" + NAME;
                        File dir = new File(path);
                        if (!dir.exists()) {
                            dir.mkdirs();
                        }
                        DJIMedia downloadMedia = mediaList.get(position).getDjiMedia();
                        downloadMedia.fetchMediaData(dir, downloadMedia.getFileNameWithoutExtension(), new DJIMediaManager.CameraDownloadListener<String>() {
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
                            public void onSuccess(String s) {
                                Toast.makeText(getApplicationContext(), "下载成功", Toast.LENGTH_SHORT).show();
                                util.setCameraMode(DJICameraSettingsDef.CameraMode.RecordVideo);
                            }

                            @Override
                            public void onFailure(DJIError djiError) {
                                Toast.makeText(getApplicationContext(), "下载失败:" + djiError.getDescription(), Toast.LENGTH_SHORT).show();
                                util.setCameraMode(DJICameraSettingsDef.CameraMode.RecordVideo);
                            }
                        });
                        break;
                    case 1:
                        util.setCameraMode(DJICameraSettingsDef.CameraMode.MediaDownload);
                        final DJIMedia delMedia = mediaList.get(position).getDjiMedia();
                        ArrayList<DJIMedia> delMediaList = new ArrayList<>();
                        delMediaList.add(delMedia);
                        getProductInstance().getCamera().getMediaManager().deleteMedia(delMediaList, new DJICommonCallbacks.DJICompletionCallbackWithTwoParam<ArrayList<DJIMedia>, DJICameraError>() {
                            @Override
                            public void onSuccess(ArrayList<DJIMedia> djiMedias, DJICameraError djiCameraError) {
                                Toast.makeText(getApplicationContext(), "删除成功", Toast.LENGTH_SHORT).show();
                                fetchMediaList();
                                util.setCameraMode(DJICameraSettingsDef.CameraMode.RecordVideo);
                                File file = new File(MyMedio.dir + delMedia.getFileName());
                                if (file.exists())
                                    file.delete();
                            }

                            @Override
                            public void onFailure(DJIError djiError) {
                                Toast.makeText(getApplicationContext(), "删除失败:" + djiError.getDescription(), Toast.LENGTH_SHORT).show();
                                util.setCameraMode(DJICameraSettingsDef.CameraMode.RecordVideo);
                            }
                        });
                        break;
                }
            }
        });
        builder.show();
    }

    private void fetchMediaList() {
        if (getCameraInstance() == null) return;
        if (getCameraInstance().getMediaManager() != null) {
            getProductInstance().getCamera().getMediaManager().fetchMediaList(
                    new DJIMediaManager.CameraDownloadListener<ArrayList<DJIMedia>>() {

                        @Override
                        public void onStart() {
                        }

                        @Override
                        public void onRateUpdate(long total, long current, long persize) {
                        }

                        @Override
                        public void onProgress(long l, long l1) {

                        }

                        @Override
                        public void onSuccess(ArrayList<DJIMedia> djiMedias) {
                            if (null != djiMedias) {
                                if (!djiMedias.isEmpty()) {
                                    initMediaList(djiMedias);
                                    Message message = Message.obtain();
                                    message.what = 100;
                                    messageHandler.sendMessage(message);
                                } else {
                                    Message message = Message.obtain();
                                    message.what = 200;
                                    messageHandler.sendMessage(message);
                                }

                            }
                        }

                        @Override
                        public void onFailure(DJIError djiError) {
                            Message message = Message.obtain();
                            message.obj = djiError.getDescription();
                            messageHandler.sendMessage(message);
                        }
                    }
            );
        }
    }

    private void initMediaList(ArrayList<DJIMedia> djiMedias) {
        Iterator<DJIMedia> iterator = djiMedias.iterator();
        mediaList.clear();
        while (iterator.hasNext()) {
            DJIMedia media = iterator.next();
            mediaList.add(new MyMedio(media));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        messageHandler.removeCallbacksAndMessages(null);
        imageSwitcher.removeAllViews();
    }

    private class MessageHandler extends Handler {
        MessageHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            if (msg.what == 100) {
                mediaAdapter.setData(mediaList);
            } else if (msg.what == 200) {
                mediaList.clear();
                mediaAdapter.setData(mediaList);
            }
        }
    }

    //自定义图片显示工厂类，继承ViewFactory
    private class MyViewFactory implements ViewFactory {
        private Context context; //定义上下文

        //参数为上下文的构造方法
        MyViewFactory(Context context) {
            this.context = context;
        }

        //显示图标区域
        @Override
        public View makeView() {
            ImageView iv = new ImageView(context); //创建ImageView对象
            iv.setScaleType(ImageView.ScaleType.FIT_CENTER); //图片自动居中显示
            //设置图片的宽和高
            iv.setLayoutParams(new ImageSwitcher.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
            return iv; //返回ImageView对象
        }
    }
}