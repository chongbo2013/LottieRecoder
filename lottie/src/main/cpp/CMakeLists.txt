
cmake_minimum_required(VERSION 3.4.1)

add_library(
             nCreateSurace

             SHARED

             # jni interface
             NativeCreateSurface.cpp )


find_library( log-lib log )

target_link_libraries(
                       nCreateSurace
                       -landroid
                       ${log-lib} )