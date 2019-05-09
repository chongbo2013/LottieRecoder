package com.yy.lottierecoder.encoders;

import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES30;


/**
 * 支持帧缓冲的视频渲染器
 * <p>
 * 支持加滤镜特效
 *
 * @author ferrisXu
 * @date 2019-02-27
 */
public class VideoFBORender extends FBORender implements IVideoRender {

    private static final String UNIFORM_CAM_MATRIX = "u_Matrix";

    private int mMatrixHandle;
    private float[] mMatrix = new float[16];

    private SurfaceTexture mSurfaceTexture;

    public VideoFBORender() {
        this(MaskType.mask,false);
    }
    MaskType isPhotoAlbum;
    boolean isVertical;
    public VideoFBORender(MaskType isPhotoAlbum, boolean isVertical) {
        setVertexShader("uniform mat4 " + UNIFORM_CAM_MATRIX + ";\n"
                + "attribute vec4 " + ATTRIBUTE_POSITION + ";\n"
                + "attribute vec2 " + ATTRIBUTE_TEXTURE_COORD + ";\n"
                + "varying vec2 " + VARYING_TEXTURE_COORD + ";\n"
                + "void main() {\n"
                + "   vec4 texPos = " + UNIFORM_CAM_MATRIX + " * vec4(" + ATTRIBUTE_TEXTURE_COORD + "," + " 1, 1);\n"
                + "   " + VARYING_TEXTURE_COORD + " = texPos.xy;\n"
                + "   gl_Position = " + ATTRIBUTE_POSITION + ";\n"
                + "}\n");
        this.isPhotoAlbum=isPhotoAlbum;
        this.isVertical=isVertical;
        setFragmentShader("#extension GL_OES_EGL_image_external : require\n"
                    + "precision mediump float;\n"
                    + "uniform samplerExternalOES " + UNIFORM_TEXTURE_0 + ";\n"
                    + "varying vec2 " + VARYING_TEXTURE_COORD + ";\n"
                    + "void main() {\n"
                    + "   gl_FragColor = texture2D(" + UNIFORM_TEXTURE_0 + ", " + VARYING_TEXTURE_COORD + ");\n"
                    + "}\n");


    }

    @Override
    public void drawFrame(long time,long duration) {
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

        if (mTextureIn != 0) {
            int[] tex = new int[1];
            tex[0] = mTextureIn;
            GLES30.glDeleteTextures(1, tex, 0);
            mTextureIn = 0;
        }
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

        if (mSurfaceTexture != null) {
            mSurfaceTexture.release();
            mSurfaceTexture = null;
        }
        mSurfaceTexture = new SurfaceTexture(mTextureIn);
    }

    @Override
    protected void bindShaderValues() {
        resetTextureVertices();
        super.bindShaderVertices();

        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextureIn);
        GLES30.glUniform1i(mTextureHandle, 0);

        mSurfaceTexture.getTransformMatrix(mMatrix);
        GLES30.glUniformMatrix4fv(mMatrixHandle, 1, false, mMatrix, 0);
    }

    @Override
    public SurfaceTexture getSurfaceTexture() {
        onDrawFrame();
        return mSurfaceTexture;
    }

    @Override
    public void onFrameDrawKey(int key,int count,int frateRate) {

    }
    RectF cropRectF=new RectF(0,0,1,1);
    @Override
    public void cropRect(RectF cropRectF) {
        this.cropRectF.set(cropRectF);

    }
    private void resetTextureVertices() {
        // 正向纹理坐标
        if(mCurrentRotation==0) {
            if (isPhotoAlbum == MaskType.mask_photo || isPhotoAlbum == MaskType.mask_bg) {
                mTextureVertices[0].clear();
                mTextureVertices[0].position(0);
                if (isVertical) {//x
                    float u1 = cropRectF.left;
                    float v1 = cropRectF.top;
                    float u2 = u1 + cropRectF.width() / 2;
                    float v2 = v1 + cropRectF.height();
                    float[] texData0 = new float[]{u1, v1, u2, v1, u1,
                            v2, u2, v2};
                    mTextureVertices[0].put(texData0).position(0);
                } else {//y
                    float u1 = cropRectF.left;
                    float v1 = cropRectF.top+cropRectF.height()/2;
                    float u2 = u1 + cropRectF.width();
                    float v2 = v1 + cropRectF.height() / 2;
                    float[] texData0 = new float[]{u1, v1, u2, v1, u1,
                            v2, u2, v2};
                    mTextureVertices[0].put(texData0).position(0);
                }
            }
        }
        if(mCurrentRotation==1) {
            // 顺时针旋转90°的纹理坐标
            if (isPhotoAlbum == MaskType.mask_photo || isPhotoAlbum == MaskType.mask_bg) {
                mTextureVertices[1].clear();
                mTextureVertices[1].position(0);
                if (isVertical) {//x
                    float u1 = cropRectF.left;
                    float v1 = cropRectF.top;
                    float u2 = u1 + cropRectF.width() / 2;
                    float v2 = v1 + cropRectF.height();
                    float[] texData1 = new float[]{u2, v1, u2, v2, u1, v1, u1, v2};
                    mTextureVertices[1].put(texData1).position(0);
                } else {//y
                    float u1 = cropRectF.left;
                    float v1 = cropRectF.top+cropRectF.height()/2;
                    float u2 = u1 + cropRectF.width();
                    float v2 = v1 + cropRectF.height() / 2;
                    float[] texData1 = new float[]{u2, v1, u2, v2, u1, v1, u1, v2};
                    mTextureVertices[1].put(texData1).position(0);
                }
            }
        }

        if(mCurrentRotation==2) {

            // 顺时针旋转180°的纹理坐标
            if (isPhotoAlbum == MaskType.mask_photo || isPhotoAlbum == MaskType.mask_bg) {
                mTextureVertices[2].clear();
                mTextureVertices[2].position(0);
                if (isVertical) {//x
                    float u1 = cropRectF.left;
                    float v1 = cropRectF.top;
                    float u2 = u1 + cropRectF.width() / 2;
                    float v2 = v1 + cropRectF.height();
                    float[] texData2 = new float[]{u2, v2, u1, v2, u2, v1, u1, v1};
                    mTextureVertices[2].put(texData2).position(0);
                } else {//y
                    float u1 = cropRectF.left;
                    float v1 = cropRectF.top+cropRectF.height()/2;
                    float u2 = u1 + cropRectF.width();
                    float v2 = v1 + cropRectF.height() / 2;
                    float[] texData2 = new float[]{u2, v2, u1, v2, u2, v1, u1, v1};
                    mTextureVertices[2].put(texData2).position(0);
                }
            }
        }
        if(mCurrentRotation==3) {
            // 顺时针旋转270°的纹理坐标
            if (isPhotoAlbum == MaskType.mask_photo || isPhotoAlbum == MaskType.mask_bg) {
                mTextureVertices[3].clear();
                mTextureVertices[3].position(0);
                if (isVertical) {//x
                    float u1 = cropRectF.left;
                    float v1 = cropRectF.top;
                    float u2 = u1 + cropRectF.width() / 2;
                    float v2 = v1 + cropRectF.height();
                    float[] texData3 = new float[]{u1, v2, u1, v1, u2, v2, u2, v1};
                    mTextureVertices[3].put(texData3).position(0);
                } else {//y
                    float u1 = cropRectF.left;
                    float v1 = cropRectF.top+cropRectF.height()/2;
                    float u2 = u1 + cropRectF.width();
                    float v2 = v1 + cropRectF.height() / 2;
                    float[] texData3 = new float[]{u1, v2, u1, v1, u2, v2, u2, v1};
                    mTextureVertices[3].put(texData3).position(0);
                }
            }
        }

    }

    @Override
    public void destroy() {
        super.destroy();
        if (mSurfaceTexture != null) {
            mSurfaceTexture.release();
            mSurfaceTexture = null;
        }
        if (mTextureIn != 0) {
            int[] tex = new int[1];
            tex[0] = mTextureIn;
            GLES30.glDeleteTextures(1, tex, 0);
            mTextureIn = 0;
        }
    }

    public int getVideoTexId() {
        return mTextureIn;
    }

    public RectF getCropRect() {
      return  cropRectF;
    }
}