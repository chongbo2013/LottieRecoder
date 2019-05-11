package com.airbnb.lottie.model.transforms;

import android.graphics.Bitmap;

/**
 * @author ferrisXu
 * 创建日期：2019/5/10
 * 描述：
 */
public abstract class BaseEffectTransformFilter {
    public EffectType effectType;
    public BaseEffectTransformFilter(EffectType effectType){
        this.effectType=effectType;
    }

    protected void init(){

    }

    /**
     * 是否需要回收源文件
     * @param source
     */
    public void draw(Bitmap source){
        Bitmap transformbBitmap=transform(source);
        if(nextTransfromFilter!=null)
            nextTransfromFilter.draw(transformbBitmap);
    }

    /**
     * 转换
     * @param source
     * @return
     */
    public abstract Bitmap transform(Bitmap source);

    //下一个渲染节点
    public BaseEffectTransformFilter nextTransfromFilter;

}
