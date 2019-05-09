package com.yy.lottierecoder;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.view.Surface;
import android.view.View;

import com.airbnb.lottie.LottieComposition;
import com.airbnb.lottie.LottieCompositionFactory;
import com.airbnb.lottie.LottieDrawable;
import com.airbnb.lottie.LottieResult;
import com.yy.lottierecoder.encoders.DefaultLottieRender;
import com.yy.lottierecoder.encoders.LottieSurfaceTexture;

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
    private DefaultLottieRender mSurfaceRender;
    LottieSurfaceTexture lottieSurfaceTexture;
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

    public int getLottieWidth() {
        if(lottieDrawable!=null) {
          return   lottieDrawable.getIntrinsicWidth();
        }
        return 0;
    }

    public int getLottieHeight() {
        if(lottieDrawable!=null) {
            return   lottieDrawable.getIntrinsicHeight();
        }
        return 0;
    }

    public LottieDrawable getDrawable() {
        return lottieDrawable;
    }

    //将surface 绘制
    public void generateSurfaceFrame(int frameIndex) {
        float progress=frameIndex/lottieDrawable.getComposition().getEndFrame();
        //将画面绘制到surface中
        setProgress(progress);
        //绘制纹理
        mSurfaceRender.drawFrame(0,0);
    }
    //初始化GL相关
    public void initGL() {
        lottieSurfaceTexture=new LottieSurfaceTexture(getLottieWidth(),getLottieHeight());
        setSurface(lottieSurfaceTexture.getSurface());
        //初始化绘制
        mSurfaceRender = new DefaultLottieRender(lottieSurfaceTexture.getSurfaceTextrue(),lottieSurfaceTexture.getmTextureIn());
        mSurfaceRender.setRenderSize(getLottieWidth(),getLottieHeight());
    }
    Handler handler=new Handler(Looper.getMainLooper());
    public void release(){
        mSurfaceRender.destroy();
        lottieSurfaceTexture.release();

        handler.post(new Runnable() {
            @Override
            public void run() {
                lottieDrawable.clearComposition();
                lottieDrawable.cancelAnimation();
            }
        });

        handler.removeCallbacksAndMessages(null);
        handler=null;
    }
}
