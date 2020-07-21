# 添加头文件路径
include_directories( source )
# 添加源文件
add_library( ffcommand

        SHARED

        # library
        cmdutils.c
        ffmpeg.c
        ffmpeg_filter.c
        ffmpeg_opt.c
        FFmpegUtils.cpp )

# 链接静态库
target_link_libraries( ffcommand
                       ffmpeg

                       android
                       jnigraphics
                       log )

