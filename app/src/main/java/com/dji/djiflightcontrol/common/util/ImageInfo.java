package com.dji.djiflightcontrol.common.util;

import android.media.ExifInterface;
import android.os.Build;
import android.support.annotation.RequiresApi;

import java.io.IOException;


@RequiresApi(api = Build.VERSION_CODES.N)
public class ImageInfo extends ExifInterface {
    public static String H = ExifInterface.TAG_BITS_PER_SAMPLE;

    public static String NAME = ExifInterface.TAG_ARTIST;

    public ImageInfo(String filename) throws IOException {
        super(filename);
    }
}
