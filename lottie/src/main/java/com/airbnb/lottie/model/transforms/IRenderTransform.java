package com.airbnb.lottie.model.transforms;

import android.graphics.Bitmap;

/**
 * @author ferrisXu
 * 创建日期：2019/5/10
 * 描述：渲染转换 ,将某种效果应用并转换成新的BITMAP
 * 作用是将图片，或者视频解析成BITMAP或者通过GLSL渲染具体的效果后，进行转换成BITMAP
 */
public interface IRenderTransform<T> {

    //获取转换后的图片，或者视频
    Bitmap transformBitmap(T source);
}
