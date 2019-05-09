package com.yy.lottierecoder.encoders;

import android.graphics.RectF;
import android.graphics.SurfaceTexture;

/**
 * 纹理渲染接口
 *
 * @author ferrisXu
 * @date 2019-02-27
 */
public interface IVideoRender {

    /**
     * 绘制当前帧
     *
     * @param time 当前帧时间（单位纳秒）
     */
    void drawFrame(long time, long duration);

    SurfaceTexture getSurfaceTexture();

    void onFrameDrawKey(int key, int count, int frameRate);

    void cropRect(RectF cropRectF);
}
