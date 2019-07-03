package com.ufotosoft.bzmedia.glutils;

import static android.opengl.GLES10.glDeleteTextures;
import static android.opengl.GLES20.GL_CLAMP_TO_EDGE;
import static android.opengl.GLES20.GL_LINEAR;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.GL_TEXTURE_MAG_FILTER;
import static android.opengl.GLES20.GL_TEXTURE_MIN_FILTER;
import static android.opengl.GLES20.GL_TEXTURE_WRAP_S;
import static android.opengl.GLES20.GL_TEXTURE_WRAP_T;
import static android.opengl.GLES20.glBindTexture;
import static android.opengl.GLES20.glGenTextures;
import static android.opengl.GLES20.glIsTexture;
import static android.opengl.GLES20.glTexParameteri;

/**
 * Created by zhandalin on 2018-04-04 17:12.
 * 说明:
 */
public class GLTexturesUtil {
    private final static float[] TEXTURE_NO_ROTATION = new float[]{
            0.0f, 0.0f,
            1.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 1.0f};
    private final static float[] TEXTURE_ROTATED_90 = new float[]{
            1.0f, 0.0f,
            1.0f, 1.0f,
            0.0f, 0.0f,
            0.0f, 1.0f};
    private final static float[] TEXTURE_ROTATED_180 = new float[]{
            1.0f, 1.0f,
            0.0f, 1.0f,
            1.0f, 0.0f,
            0.0f, 0.0f,};
    private final static float[] TEXTURE_ROTATED_270 = new float[]{
            0.0f, 1.0f,
            0.0f, 0.0f,
            1.0f, 1.0f,
            1.0f, 0.0f};
    public final static float[] CUBE = new float[]{
            -1.0f, -1.0f,
            1.0f, -1.0f,
            -1.0f, 1.0f,
            1.0f, 1.0f};

    public static int genTextures() {
        int textures[] = new int[1];
        glGenTextures(1, textures, 0);
        glBindTexture(GL_TEXTURE_2D, textures[0]);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        return textures[0];
    }

    public static void releaseTextures(int texturesId) {
        if (texturesId > 0 && glIsTexture(texturesId)) {
            glDeleteTextures(1, new int[]{texturesId}, 0);
        }
    }

    public static float[] getRotationTexture(int rotation, boolean flipHorizontal, boolean flipVertical) {
        float[] rotatedTex;
        switch (rotation) {
            case 90:
                rotatedTex = new float[]{
                        TEXTURE_ROTATED_90[0], TEXTURE_ROTATED_90[1],
                        TEXTURE_ROTATED_90[2], TEXTURE_ROTATED_90[3],
                        TEXTURE_ROTATED_90[4], TEXTURE_ROTATED_90[5],
                        TEXTURE_ROTATED_90[6], TEXTURE_ROTATED_90[7],
                };
                break;
            case 180:
                rotatedTex = new float[]{
                        TEXTURE_ROTATED_180[0], TEXTURE_ROTATED_180[1],
                        TEXTURE_ROTATED_180[2], TEXTURE_ROTATED_180[3],
                        TEXTURE_ROTATED_180[4], TEXTURE_ROTATED_180[5],
                        TEXTURE_ROTATED_180[6], TEXTURE_ROTATED_180[7],
                };
                break;
            case 270:
                rotatedTex = new float[]{
                        TEXTURE_ROTATED_270[0], TEXTURE_ROTATED_270[1],
                        TEXTURE_ROTATED_270[2], TEXTURE_ROTATED_270[3],
                        TEXTURE_ROTATED_270[4], TEXTURE_ROTATED_270[5],
                        TEXTURE_ROTATED_270[6], TEXTURE_ROTATED_270[7],
                };
                break;
            default:
                rotatedTex = new float[]{
                        TEXTURE_NO_ROTATION[0], TEXTURE_NO_ROTATION[1],
                        TEXTURE_NO_ROTATION[2], TEXTURE_NO_ROTATION[3],
                        TEXTURE_NO_ROTATION[4], TEXTURE_NO_ROTATION[5],
                        TEXTURE_NO_ROTATION[6], TEXTURE_NO_ROTATION[7],
                };
                break;
        }
        if (flipHorizontal) {
            rotatedTex[0] = flip(rotatedTex[0]);
            rotatedTex[2] = flip(rotatedTex[2]);
            rotatedTex[4] = flip(rotatedTex[4]);
            rotatedTex[6] = flip(rotatedTex[6]);
        }
        if (flipVertical) {
            rotatedTex[1] = flip(rotatedTex[1]);
            rotatedTex[3] = flip(rotatedTex[3]);
            rotatedTex[5] = flip(rotatedTex[5]);
            rotatedTex[7] = flip(rotatedTex[7]);
        }
        return rotatedTex;
    }

    private static float flip(float i) {
        if (i == 0.0f) {
            return 1.0f;
        }
        return 0.0f;
    }
}
