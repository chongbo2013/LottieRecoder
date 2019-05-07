package com.yy.lottierecoder;
import android.content.Context;
import android.graphics.Canvas;
import android.view.Surface;
import com.airbnb.lottie.LottieComposition;
import com.airbnb.lottie.LottieCompositionFactory;
import com.airbnb.lottie.LottieDrawable;
import com.airbnb.lottie.LottieResult;

/**
 * @author ferrisXu
 * 创建日期：2019/5/7
 * 描述：
 */
public class OffscreenAfterEffectView implements IGLView{
    private final LottieDrawable lottieDrawable = new LottieDrawable();
    Context context;
    public OffscreenAfterEffectView(Context context) {
        this.context=context;
    }
    private GLViewHelper mGLViewHelper = new GLViewHelper();
    public boolean load() {
        clearComposition();
        LottieResult<LottieComposition> result = LottieCompositionFactory.fromAssetSync(context, "biao.json");
        if (result.getValue() != null) {
            lottieDrawable.setComposition(result.getValue());
            lottieDrawable.setBounds(0,0,result.getValue().getBounds().width(),result.getValue().getBounds().height());
            setProgress(0.0f);
        }
        return result.getValue() != null;
    }


    protected void onDraw(Canvas canvas) {
        if(lottieDrawable!=null) {
            lottieDrawable.draw(canvas);
        }
    }

    public void draw() {
        Canvas surfaceCanvas = mGLViewHelper.drawStart(null);
        if (surfaceCanvas != null) {
            onDraw(surfaceCanvas);
            mGLViewHelper.drawEnd(surfaceCanvas);
        }
    }

    private void clearComposition() {
        lottieDrawable.clearComposition();
    }

    @Override
    public int getWidth() {
        if(lottieDrawable!=null)
        return lottieDrawable.getIntrinsicWidth();
        return 0;
    }

    @Override
    public int getHeight() {
        if(lottieDrawable!=null)
        return lottieDrawable.getIntrinsicHeight();
        return 0;
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
            draw();
        }
    }
}
