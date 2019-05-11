package com.airbnb.lottie;

import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.text.TextUtils;


/**
 * Data class describing an image asset exported by bodymovin.
 */
public class LottieVideoAsset {
    private int width;
    private int height;
    private final String id;
    private final String fileName;
    private String dirName;
    //视频的时长
    private long duration;
    //视频的旋转角度
    private int rotate;

    /**
     * Pre-set a bitmap for this asset
     */


    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public LottieVideoAsset(int width, int height, String id, String fileName, String dirName, long duration, int rotate) {
        this.width = width;
        this.height = height;
        this.id = id;
        this.fileName = fileName;
        this.dirName = dirName;
        this.duration = duration;
        this.rotate = rotate;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public String getId() {
        return id;
    }

    public String getFileName() {
        return fileName;
    }

    public String getDirName() {
        return dirName;
    }

    public void setDirName(String dirName) {
        this.dirName = dirName;
    }

    /**
     * 初始化视频信息
     * @param absolutePath
     */
    public void init(String absolutePath) {
        if (width <= 0 || height <= 0) {
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            try {
                retriever.setDataSource(absolutePath);
                String duration2 = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                String width2 = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
                String height2 = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
                String rotation2 = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);
                duration = parseLong(duration2);
                width = parseInt(width2);
                height = parseInt(height2);
                rotate = parseInt(rotation2);
            } catch (RuntimeException e) {
                e.printStackTrace();
            } finally {
                retriever.release();
            }
        }
    }

    public int parseInt(String val) {
        return parseInt(val, 0);
    }

    public int parseInt(String val, int def) {
        if (TextUtils.isEmpty(val)) return def;
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        return def;
    }

    public long parseLong(String val) {
        return parseLong(val, 0);
    }

    public long parseLong(String val, long def) {
        if (TextUtils.isEmpty(val)) return def;
        try {
            return Long.parseLong(val);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return def;
    }
}
