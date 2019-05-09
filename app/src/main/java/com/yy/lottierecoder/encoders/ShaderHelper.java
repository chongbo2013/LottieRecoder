package com.yy.lottierecoder.encoders;

import android.opengl.GLES30;


/**
 * ShaderHelper
 *
 * @author ferrisXu
 * @date 2019-02-27
 */
public class ShaderHelper {

    public static int compileShader(String shaderSource, int shaderType) {
        String errorInfo = "none";

        int shaderHandle = GLES30.glCreateShader(shaderType);
        if (shaderHandle != 0) {
            GLES30.glShaderSource(shaderHandle, shaderSource);
            GLES30.glCompileShader(shaderHandle);
            final int[] compileStatus = new int[1];
            GLES30.glGetShaderiv(shaderHandle, GLES30.GL_COMPILE_STATUS, compileStatus, 0);
            if (compileStatus[0] == 0) {
                errorInfo = GLES30.glGetShaderInfoLog(shaderHandle);
                GLES30.glDeleteShader(shaderHandle);
                shaderHandle = 0;
            }
        }
        if (shaderHandle == 0) {
            throw new RuntimeException("failed to compile shader. Reason: " + errorInfo);
        }

        return shaderHandle;
    }

    public static int linkProgram(int vertexShaderHandle, int fragmentShaderHandle, String attributes[]) {
        int programHandle = GLES30.glCreateProgram();
        if (programHandle != 0) {
            GLES30.glAttachShader(programHandle, vertexShaderHandle);
            GLES30.glAttachShader(programHandle, fragmentShaderHandle);

            for (int i = 0; i < attributes.length; i++) {
                GLES30.glBindAttribLocation(programHandle, i, attributes[i]);
            }

            GLES30.glLinkProgram(programHandle);
            final int[] linkStatus = new int[1];
            GLES30.glGetProgramiv(programHandle, GLES30.GL_LINK_STATUS, linkStatus, 0);
            if (linkStatus[0] == 0) {
                GLES30.glDeleteProgram(programHandle);
                programHandle = 0;
            }
        }
        if (programHandle == 0) {
            throw new RuntimeException("failed to link program.");
        }

        return programHandle;
    }
}
