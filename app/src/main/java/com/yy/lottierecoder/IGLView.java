package com.yy.lottierecoder;

/**
 * @author ferrisXu
 * 创建日期：2019/5/7
 * 描述：
 */

import android.view.Surface;

/**
 * 支持OpenGL渲染的View的接口
 *
 * @author like
 * @date 2017-09-20
 */
public interface IGLView {

    /**
     * 获取高度
     *
     * @return
     */
    int getWidth();

    /**
     * 获取宽度
     *
     * @return
     */
    int getHeight();

    /**
     * 设置要渲染到的Surface
     *
     * @param surface
     */
    void setSurface(Surface surface);

    /**
     * 设置GL环境
     *
     * @param render
     */
    void setGLEnvironment(IGLEnvironment render);

    /**
     * 设置刷新模式，默认为RENDERMODE_WHEN_DIRTY
     * <p>
     * RENDERMODE_WHEN_DIRTY 或 RENDERMODE_CONTINUOUSLY
     *
     * @param model
     */
    void setRenderMode(int model);
}