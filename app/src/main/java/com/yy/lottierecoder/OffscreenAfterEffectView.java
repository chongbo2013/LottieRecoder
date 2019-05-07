package com.yy.lottierecoder;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.view.Surface;
import android.view.View;

import com.airbnb.lottie.LottieAnimationView;
import com.airbnb.lottie.LottieComposition;
import com.airbnb.lottie.LottieCompositionFactory;
import com.airbnb.lottie.LottieDrawable;
import com.airbnb.lottie.LottieResult;

/**
 * @author ferrisXu
 * 创建日期：2019/5/7
 * 描述：
 */
public class OffscreenAfterEffectView extends View implements IGLView, Drawable.Callback {
    private final LottieDrawable lottieDrawable = new LottieDrawable();
    Context context;
    public OffscreenAfterEffectView(Context context) {
        super(context);
        this.context=context;
    }
    private GLViewHelper mGLViewHelper = new GLViewHelper();
    public boolean load() {
        clearComposition();
        lottieDrawable.setImagesAssetsFolder("images");
        lottieDrawable.setCallback(this);
        LottieResult<LottieComposition> result = LottieCompositionFactory.fromAssetSync(context, "kuaijian.json");
        if (result.getValue() != null) {
            lottieDrawable.setComposition(result.getValue());
            lottieDrawable.setBounds(0,0,result.getValue().getBounds().width(),result.getValue().getBounds().height());
        }
        return result.getValue() != null;
    }

    public Bitmap updateBitmap(String id, @Nullable Bitmap bitmap) {
        return lottieDrawable.updateBitmap(id, bitmap);
    }

    public void setSize(int w,int h){
        if(lottieDrawable!=null)
        lottieDrawable.setBounds(0,0,w,h);
    }
    public void draw() {
        Canvas surfaceCanvas = mGLViewHelper.drawStart(null);
        if (surfaceCanvas != null) {
            if(lottieDrawable!=null) {
                lottieDrawable.draw(surfaceCanvas);
            }
            mGLViewHelper.drawEnd(surfaceCanvas);
        }
    }

    private void clearComposition() {
        lottieDrawable.clearComposition();
    }



    @Override
    public void setSurface(Surface surface) {
        mGLViewHelper.setSurface(surface);
    }

    /**
     * 设置surface大小
     * @param surfaceTexture
     */
    public void setDefaultBufferSize(SurfaceTexture surfaceTexture){
        if(lottieDrawable==null)
            return;
        surfaceTexture.setDefaultBufferSize(getWidth(), getHeight());
    }
    @Override
    public void setGLEnvironment(IGLEnvironment render) {
        mGLViewHelper.setGLEnvironment(render);
    }

    @Override
    public void setRenderMode(int model) {

    }

    public void setProgress(float progress) {
        if(lottieDrawable!=null) {
            lottieDrawable.setProgress(progress);
        }
        draw();
    }

    @Override
    public void invalidateDrawable( Drawable who) {

    }

    @Override
    public void scheduleDrawable( Drawable who, Runnable what, long when) {

    }

    @Override
    public void unscheduleDrawable( Drawable who,  Runnable what) {

    }
}
