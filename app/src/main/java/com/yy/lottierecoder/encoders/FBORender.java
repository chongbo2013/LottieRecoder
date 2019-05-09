package com.yy.lottierecoder.encoders;

import android.opengl.GLES30;

import java.util.ArrayList;
import java.util.List;

/**
 * 支持帧缓冲的渲染器
 * <p>
 * 会将图像渲染到一个输出纹理中。
 * FBORender需要与EndPointRender配合使用才能将帧缓冲中的内容显示出来。
 *
 * @author ferrisXu
 * @date 2019-02-27
 */
public class FBORender extends GLRender {
    protected boolean createFbo=true;
    protected int[] mFrameBuffer;
    protected int[] mTextureOut;
    protected int[] mDepthRenderBuffer;

    protected final List<OnTextureAcceptableListener> mTargets = new ArrayList<>();

    public FBORender() {
        super();
    }
    public FBORender(boolean createFbo) {
        super();
        this.createFbo=createFbo;
    }
    @Override
    public void destroy() {
        super.destroy();
        if (mFrameBuffer != null) {
            GLES30.glDeleteFramebuffers(1, mFrameBuffer, 0);
            mFrameBuffer = null;
        }
        if (mDepthRenderBuffer != null) {
            GLES30.glDeleteRenderbuffers(1, mDepthRenderBuffer, 0);
            mDepthRenderBuffer = null;
        }
        if (mTextureOut != null) {
            GLES30.glDeleteTextures(1, mTextureOut, 0);
            mTextureOut = null;
        }
    }

    @Override
    public String toString() {
        return super.toString() + " Targets:" + mTargets.size();
    }

    @Override
    protected void drawFrame() {
        if(mFrameBuffer==null)
            return;

        if (mTextureOut == null) {
            if (mWidth != 0 && mHeight != 0) {
                initFBO();
            } else {
                return;
            }
        }
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, mFrameBuffer[0]);

        onDraw();

        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0);

        synchronized (mTargets) {
            for (OnTextureAcceptableListener target : mTargets) {
                if (null != target && null != mTextureOut && mTextureOut.length > 0) {
                    target.onTextureAcceptable(mTextureOut[0], this);
                }
            }
        }
    }

    /**
     * 为了方便子类覆盖，这里独立为一个方法
     */
    protected void onDraw() {
        super.drawFrame();
    }

    @Override
    protected void onRenderSizeChanged() {
        initFBO();
    }

    protected void initFBO() {
        if(!createFbo)
            return;
        // 初始化输出纹理
        if (mTextureOut != null) {
            GLES30.glDeleteTextures(1, mTextureOut, 0);
            mTextureOut = null;
        }
        mTextureOut = new int[1];
        GLES30.glGenTextures(1, mTextureOut, 0);
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, mTextureOut[0]);

        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D,
                GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D,
                GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S,
                GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T,
                GLES30.GL_CLAMP_TO_EDGE);

        GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA,
                getWidth(), getHeight(), 0, GLES30.GL_RGBA,
                GLES30.GL_UNSIGNED_BYTE, null);

        // 初始化帧缓冲和深度缓冲
        if (mFrameBuffer != null) {
            GLES30.glDeleteFramebuffers(1, mFrameBuffer, 0);
            mFrameBuffer = null;
        }
        if (mDepthRenderBuffer != null) {
            GLES30.glDeleteRenderbuffers(1, mDepthRenderBuffer, 0);
            mDepthRenderBuffer = null;
        }
        mFrameBuffer = new int[1];
        mDepthRenderBuffer = new int[1];
        GLES30.glGenFramebuffers(1, mFrameBuffer, 0);
        GLES30.glGenRenderbuffers(1, mDepthRenderBuffer, 0);
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, mFrameBuffer[0]);
        GLES30.glFramebufferTexture2D(GLES30.GL_FRAMEBUFFER,
                GLES30.GL_COLOR_ATTACHMENT0, GLES30.GL_TEXTURE_2D,
                mTextureOut[0], 0);
        GLES30.glBindRenderbuffer(GLES30.GL_RENDERBUFFER, mDepthRenderBuffer[0]);
        GLES30.glRenderbufferStorage(GLES30.GL_RENDERBUFFER,
                GLES30.GL_DEPTH_COMPONENT16, getWidth(), getHeight());
        GLES30.glFramebufferRenderbuffer(GLES30.GL_FRAMEBUFFER,
                GLES30.GL_DEPTH_ATTACHMENT, GLES30.GL_RENDERBUFFER,
                mDepthRenderBuffer[0]);
    }

    public List<OnTextureAcceptableListener> getTargets() {
        return mTargets;
    }

    public void addTarget(OnTextureAcceptableListener target) {
        synchronized (mTargets) {
            if (!mTargets.contains(target) && target != null) {
                mTargets.add(target);
            }
        }
    }

    public void removeTarget(OnTextureAcceptableListener target) {
        synchronized (mTargets) {
            mTargets.remove(target);
        }
    }

    public void clearTargets() {
        synchronized (mTargets) {
            mTargets.clear();
        }
    }
}
