# 设置cmake最低版本
cmake_minimum_required(VERSION 3.4.1)

# 设置GCC编译器的编译选项
if(CMAKE_COMPILER_IS_GNUCC)
    set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Wmissing-declarations -Wmissing-prototypes")
endif(CMAKE_COMPILER_IS_GNUCC)

# 设置clang编译器的编译选项
if(CMAKE_COMPILER_IS_CLANG)
    set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Wmissing-declarations -Wmissing-prototypes -Wdocumentation -Wunreachable-code")
endif(CMAKE_COMPILER_IS_CLANG)


# 根据API版本判断使用哪个版本的OpenGLES
if (${ANDROID_PLATFORM_LEVEL} LESS 12)
    message(FATAL_ERROR "OpenGL 2 is not supported before API level 11 (currently using ${ANDROID_PLATFORM_LEVEL}).")
    return()
elseif (${ANDROID_PLATFORM_LEVEL} LESS 18)
    add_definitions("-DDYNAMIC_ES3")
    set(GLES-lib GLESv2)
else ()
    set(GLES-lib GLESv3)
endif (${ANDROID_PLATFORM_LEVEL} LESS 11)

# 添加include目录路径
include_directories(

        modules/skottie/include/android
        modules/skottie/include/c
        modules/skottie/include/codec
        modules/skottie/include/config
        modules/skottie/include/core
        modules/skottie/include/docs
        modules/skottie/include/effects
        modules/skottie/include/encode
        modules/skottie/include/gpu
        modules/skottie/include/gpu/d3d
        modules/skottie/include/gpu/dawn
        modules/skottie/include/gpu/gl
        modules/skottie/include/gpu/mock
        modules/skottie/include/gpu/mtl
        modules/skottie/include/gpu/vk
        modules/skottie/include/pathops
        modules/skottie/include/ports
        modules/skottie/include/private
        modules/skottie/include/svg
        modules/skottie/include/third_party
        modules/skottie/include/third_party/skcms
        modules/skottie/include/third_party/vulkan/vulkan
        modules/skottie/include/utils
        modules/skottie/include/utils/mac
        modules/skottie/include/flags
        #modules include
        modules/skottie/include
        modules/skottie/src
        modules/skottie/src/utils
        modules/skottie/src/animator
        modules/skottie/src/effects
        modules/skottie/src/layers
        modules/skottie/src/layers/shapelayer
        modules/skottie/src/text

        #utils
        modules/skottie/utils


        #out
        )

# 添加skia库
add_library( libskia SHARED IMPORTED )
set_target_properties( libskia
        PROPERTIES IMPORTED_LOCATION
        ${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libskia.so )

# 添加sksg库
add_library( libsksg SHARED IMPORTED )
set_target_properties( libsksg
        PROPERTIES IMPORTED_LOCATION
        ${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libsksg.so )

# 添加skia库
add_library( libskshaper SHARED IMPORTED )
set_target_properties( libskshaper
        PROPERTIES IMPORTED_LOCATION
        ${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libskshaper.so )



# FFmpeg命令行库
# 添加源文件
add_library( ffcommand

        SHARED
        # library
        # skottie
        modules/skottie/src/animator/Animator.cpp
        modules/skottie/src/animator/KeyframeAnimator.cpp
        modules/skottie/src/animator/ScalarKeyframeAnimator.cpp
        modules/skottie/src/animator/ShapeKeyframeAnimator.cpp
        modules/skottie/src/animator/TextKeyframeAnimator.cpp
        modules/skottie/src/animator/Vec2KeyframeAnimator.cpp
        modules/skottie/src/animator/VectorKeyframeAnimator.cpp
        # effects
        modules/skottie/src/effects/BrightnessContrastEffect.cpp
        modules/skottie/src/effects/CornerPinEffect.cpp
        modules/skottie/src/effects/DropShadowEffect.cpp
        modules/skottie/src/effects/Effects.cpp
        modules/skottie/src/effects/FillEffect.cpp
        modules/skottie/src/effects/GaussianBlurEffect.cpp
        modules/skottie/src/effects/GlowStyles.cpp
        modules/skottie/src/effects/GradientEffect.cpp
        modules/skottie/src/effects/HueSaturationEffect.cpp
        modules/skottie/src/effects/InvertEffect.cpp
        modules/skottie/src/effects/LevelsEffect.cpp
        modules/skottie/src/effects/LinearWipeEffect.cpp
        modules/skottie/src/effects/MotionBlurEffect.cpp
        modules/skottie/src/effects/MotionTileEffect.cpp
        modules/skottie/src/effects/RadialWipeEffect.cpp
        modules/skottie/src/effects/ShadowStyles.cpp
        modules/skottie/src/effects/ShiftChannelsEffect.cpp
        modules/skottie/src/effects/TintEffect.cpp
        modules/skottie/src/effects/TransformEffect.cpp
        modules/skottie/src/effects/TritoneEffect.cpp
        modules/skottie/src/effects/VenetianBlindsEffect.cpp
        #layers
        modules/skottie/src/layers/shapelayer/Ellipse.cpp
        modules/skottie/src/layers/shapelayer/FillStroke.cpp
        modules/skottie/src/layers/shapelayer/Gradient.cpp
        modules/skottie/src/layers/shapelayer/MergePaths.cpp
        modules/skottie/src/layers/shapelayer/OffsetPaths.cpp
        modules/skottie/src/layers/shapelayer/Polystar.cpp
        modules/skottie/src/layers/shapelayer/PuckerBloat.cpp
        modules/skottie/src/layers/shapelayer/Rectangle.cpp
        modules/skottie/src/layers/shapelayer/Repeater.cpp
        modules/skottie/src/layers/shapelayer/RoundCorners.cpp
        modules/skottie/src/layers/shapelayer/ShapeLayer.cpp
        modules/skottie/src/layers/shapelayer/TrimPaths.cpp
        modules/skottie/src/layers/FootageLayer.cpp
        modules/skottie/src/layers/NullLayer.cpp
        modules/skottie/src/layers/PrecompLayer.cpp
        modules/skottie/src/layers/SolidLayer.cpp
        modules/skottie/src/layers/TextLayer.cpp
        #text
        modules/skottie/src/text/RangeSelector.cpp
        modules/skottie/src/text/SkottieShaper.cpp
        modules/skottie/src/text/TextAdapter.cpp
        modules/skottie/src/text/TextAnimator.cpp
        modules/skottie/src/text/TextValue.cpp
        #
        modules/skottie/src/Camera.cpp
        modules/skottie/src/Composition.cpp
        modules/skottie/src/Layer.cpp
        modules/skottie/src/Path.cpp
        modules/skottie/src/Skottie.cpp
        modules/skottie/src/SkottieJson.cpp
        modules/skottie/src/SkottieProperty.cpp
        modules/skottie/src/SkottieTool.cpp
        modules/skottie/src/Transform.cpp
        #utils
        modules/skottie/utils/SkottieUtils.cpp
        modules/skottie/utils/SkOSPath2.cpp
        #source
        modules/medias/native-lib.cpp


        )


find_library( # Sets the name of the path variable.
        log-lib

        # Specifies the name of the NDK library that
        # you want CMake to locate.
        log)
# 链接静态库
target_link_libraries( ffcommand
        jnigraphics
        EGL
        ${log-lib}
        ${GLES-lib}
        libskia
        libsksg
        libskshaper
        )


