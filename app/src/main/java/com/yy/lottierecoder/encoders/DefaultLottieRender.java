package com.yy.lottierecoder.encoders;

import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES30;


/**
 * 默认的视频渲染器，不加任何特效
 *
 * @author ferrisXu
 * @date 2019-02-27
 */
public class DefaultLottieRender extends GLRender implements IVideoRender {

    private static final String UNIFORM_CAM_MATRIX = "u_Matrix";

    private int mMatrixHandle;
    private float[] mMatrix = new float[16];

    SurfaceTexture mSurfaceTexture;

    public DefaultLottieRender(SurfaceTexture mSurfaceTexture,int mTextureIn) {
        this.mTextureIn=mTextureIn;
        this.mSurfaceTexture=mSurfaceTexture;
        setVertexShader("uniform mat4 " + UNIFORM_CAM_MATRIX + ";\n"
                + "attribute vec4 " + ATTRIBUTE_POSITION + ";\n"
                + "attribute vec2 " + ATTRIBUTE_TEXTURE_COORD + ";\n"
                + "varying vec2 " + VARYING_TEXTURE_COORD + ";\n"
                + "void main() {\n"
                + "   vec4 texPos = " + UNIFORM_CAM_MATRIX + " * vec4(" + ATTRIBUTE_TEXTURE_COORD + "," + " 1, 1);\n"
                + "   " + VARYING_TEXTURE_COORD + " = texPos.xy;\n"
                + "   gl_Position = " + ATTRIBUTE_POSITION + ";\n"
                + "}\n");
        setFragmentShader("#extension GL_OES_EGL_image_external : require\n"
                + "precision mediump float;\n"
                + "uniform samplerExternalOES " + UNIFORM_TEXTURE_0 + ";\n"
                + "varying vec2 " + VARYING_TEXTURE_COORD + ";\n"
                + "void main() {\n"
                + "   gl_FragColor = texture2D(" + UNIFORM_TEXTURE_0 + ", " + VARYING_TEXTURE_COORD + ");\n"
                + "}\n");
    }

    @Override
    public void drawFrame(long time,long duation) {
        try {
            //更新texImage
            if(mSurfaceTexture!=null)
            mSurfaceTexture.updateTexImage();
        } catch (Exception e) {
            e.printStackTrace();
        }
        onDrawFrame();
    }

    @Override
    protected void initShaderHandles() {
        super.initShaderHandles();
        mMatrixHandle = GLES30.glGetUniformLocation(mProgramHandle, UNIFORM_CAM_MATRIX);
    }

    @Override
    protected void initGLContext() {
        super.initGLContext();
    }

    @Override
    protected void bindShaderValues() {
        super.bindShaderVertices();
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextureIn);
        GLES30.glUniform1i(mTextureHandle, 0);
        if(mSurfaceTexture!=null)
        mSurfaceTexture.getTransformMatrix(mMatrix);
        GLES30.glUniformMatrix4fv(mMatrixHandle, 1, false, mMatrix, 0);
    }

    @Override
    public SurfaceTexture getSurfaceTexture() {
        onDrawFrame();
        return mSurfaceTexture;
    }

    @Override
    public void onFrameDrawKey(int key,int count,int frameRate) {

    }

    @Override
    public void cropRect(RectF cropRectF) {

    }

    @Override
    public void destroy() {
        super.destroy();
        mSurfaceTexture=null;
    }
}
