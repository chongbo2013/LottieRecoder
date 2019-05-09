package com.yy.lottierecoder.encoders;

import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES30;
import android.view.Surface;

/**
 * @author ferrisXu
 * 创建日期：2019/5/8
 * 描述：
 */
public class LottieSurfaceTexture {
    private SurfaceTexture mSurfaceTexture;
    private Surface surface;
    int mTextureIn=-1;
    public LottieSurfaceTexture(int width,int height){
        int[] textures = new int[1];
        GLES30.glGenTextures(1, textures, 0);
        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textures[0]);
        GLES30.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);
        mTextureIn = textures[0];


        mSurfaceTexture = new SurfaceTexture(mTextureIn);
        mSurfaceTexture.setDefaultBufferSize(width,height);
        surface=new Surface(mSurfaceTexture);
    }

    public Surface getSurface() {
        return surface;
    }

    public int getmTextureIn() {
        return mTextureIn;
    }


    public SurfaceTexture getSurfaceTextrue() {
        return mSurfaceTexture;
    }

    public void release() {
        if (mTextureIn != 0) {
            int[] tex = new int[1];
            tex[0] = mTextureIn;
            GLES30.glDeleteTextures(1, tex, 0);
            mTextureIn = 0;
        }
        if(surface!=null){
            surface.release();
            surface=null;
        }
        if (mSurfaceTexture != null) {
            mSurfaceTexture.release();
            mSurfaceTexture = null;
        }

    }
}
