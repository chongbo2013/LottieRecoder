//
// Created by cain on 2018/9/7.
//

#include <jni.h>


/**
 * 马赛克处理
 */
extern "C"
JNIEXPORT void JNICALL
Java_com_airbnb_lottie_surfaces_NativeCreateSurface_createNativeWindow(JNIEnv *env, jobject textureView,
        jobject surface) {

    sp<IGraphicBufferProducer> producer(SurfaceTexture_getProducer(env, surface));
       sp<ANativeWindow> window = new Surface(producer, true);

       window->incStrong((void*)android_view_TextureView_createNativeWindow);
       SET_LONG(textureView, gTextureViewClassInfo.nativeWindow, jlong(window.get()));


}

