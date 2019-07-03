package com.ufotosoft.bzmedia.glutils;
/*
 * AudioVideoRecordingSample
 * Sample project to cature audio and video from internal mic/camera and save as MPEG4 file.
 *
 * Copyright (c) 2014-2015 saki t_saki@serenegiant.com
 *
 * File name: BaseProgram.java
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 * All files in the folder are under this Apache License, Version 2.0.
 */

import android.opengl.GLES20;

import com.ufotosoft.bzmedia.utils.BZLogUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Helper class to draw to whole view using specific texture and texture matrix
 */
public class BaseProgram {
    private static final String TAG = "bz_BaseProgram";

    private static final String vss
            = "attribute vec4 aPosition;\n"
            + "attribute vec2 aTextureCoord;\n"
            + "varying vec2 vTextureCoord;\n"
            + "void main() {\n"
            + "	gl_Position =aPosition;\n"
            + "	vTextureCoord = aTextureCoord;\n"
            + "}\n";
    private static final String fss
            = "precision mediump float;\n"
            + "uniform sampler2D sTexture;\n"
            + "varying vec2 vTextureCoord;\n"
            + "void main() {\n"
            + "  gl_FragColor = texture2D(sTexture, vTextureCoord);\n"
            + "}";

    protected int hProgram;

    private static final int FLOAT_SZ = Float.SIZE / 8;
    private static final int VERTEX_NUM = 4;
    private static final int VERTEX_SZ = VERTEX_NUM * 2;
    private int maPositionLoc;
    private int maTextureCoordLoc;
    private FloatBuffer pTexCoord;
    private FloatBuffer pVertex;

    private int[] coordinateBuffer = {-1};
    private int[] positionBuffer = {-1};
    private int rotation;
    private boolean flipHorizontal;
    private boolean flipVertical;
    private static final float TEXCOORD_FLIP_VERTICAL[] = {
            0.0f, 0.0f,
            1.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 1.0f
    };

    /**
     * Constructor
     * this should be called in GL context
     */
    public BaseProgram(boolean needFlipVertical) {
        createProgram(0, false, needFlipVertical);
    }

    public BaseProgram(int rotation, boolean flipHorizontal, boolean flipVertical) {
        createProgram(rotation, flipHorizontal, flipVertical);
    }

    private void createProgram(int rotation, boolean flipHorizontal, boolean flipVertical) {
        this.rotation = rotation;
        this.flipHorizontal = flipHorizontal;
        this.flipVertical = flipVertical;

        pVertex = ByteBuffer.allocateDirect(VERTEX_SZ * FLOAT_SZ)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        pVertex.put(GLTexturesUtil.CUBE);
        pVertex.position(0);

        pTexCoord = ByteBuffer.allocateDirect(VERTEX_SZ * FLOAT_SZ)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();

        pTexCoord.put(GLTexturesUtil.getRotationTexture(rotation, flipHorizontal, flipVertical));
        pTexCoord.position(0);

        hProgram = loadShader(vss, fss);
        maPositionLoc = GLES20.glGetAttribLocation(hProgram, "aPosition");
        maTextureCoordLoc = GLES20.glGetAttribLocation(hProgram, "aTextureCoord");

        int sTextureLoc = GLES20.glGetUniformLocation(hProgram, "sTexture");

        GLES20.glUseProgram(hProgram);
        GLES20.glUniform1i(sTextureLoc, 5);
        GLES20.glUseProgram(0);

        //coordinateBuffer
        GLES20.glGenBuffers(1, coordinateBuffer, 0);
        //存放顶点位置数据
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, coordinateBuffer[0]);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, VERTEX_SZ * 4, pTexCoord,
                GLES20.GL_STATIC_DRAW);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

        //positionBuffer
        GLES20.glGenBuffers(1, positionBuffer, 0);
        //存放顶点位置数据
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, positionBuffer[0]);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, VERTEX_SZ * 4,
                pVertex, GLES20.GL_STATIC_DRAW);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
    }

    public void setRotation(int rotation) {
        this.rotation = rotation;
        updateTexCoord();
    }

    public void setFlip(boolean flipHorizontal, boolean flipVertical) {
        this.flipHorizontal = flipHorizontal;
        this.flipVertical = flipVertical;
        updateTexCoord();
    }

    private void updateTexCoord() {
        if (null == pTexCoord) return;

        pTexCoord.put(GLTexturesUtil.getRotationTexture(rotation, flipHorizontal, flipVertical));
        pTexCoord.position(0);

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, coordinateBuffer[0]);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, VERTEX_SZ * 4, pTexCoord,
                GLES20.GL_STATIC_DRAW);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
    }

    /**
     * terminatinng, this should be called in GL context
     */
    public void release() {
        GLUtil.checkGlError("BaseProgram release start");
        BZLogUtil.d(TAG, "release");
        if (hProgram > 0 && GLES20.glIsProgram(hProgram)) {
            GLES20.glDeleteProgram(hProgram);
            hProgram = -1;
        }
        if (coordinateBuffer[0] > 0 && GLES20.glIsBuffer(coordinateBuffer[0])) {
            GLES20.glDeleteBuffers(1, coordinateBuffer, 0);
            coordinateBuffer[0] = -1;
        }
        if (positionBuffer[0] > 0 && GLES20.glIsBuffer(positionBuffer[0])) {
            GLES20.glDeleteBuffers(1, positionBuffer, 0);
            positionBuffer[0] = -1;
        }
        GLUtil.checkGlError("BaseProgram release end");
    }

    /**
     * draw specific texture with specific texture matrix
     *
     * @param tex_id texture ID
     */
    public void draw(final int tex_id) {
        if (!GLES20.glIsProgram(hProgram)) {
            BZLogUtil.e(TAG, "Program is not enable create a new");
            release();
            createProgram(rotation, flipHorizontal, flipVertical);
        }
        GLES20.glUseProgram(hProgram);

        GLES20.glEnableVertexAttribArray(maPositionLoc);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, positionBuffer[0]);
        GLES20.glVertexAttribPointer(maPositionLoc, 2, GLES20.GL_FLOAT, false, VERTEX_SZ, 0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

        GLES20.glEnableVertexAttribArray(maTextureCoordLoc);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, coordinateBuffer[0]);
        GLES20.glVertexAttribPointer(maTextureCoordLoc, 2, GLES20.GL_FLOAT, false, VERTEX_SZ, 0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE5);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, tex_id);

        onDrawBefore();
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, VERTEX_NUM);

        GLES20.glDisableVertexAttribArray(maPositionLoc);
        GLES20.glDisableVertexAttribArray(maTextureCoordLoc);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glUseProgram(0);

        BZOpenGlUtils.checkEglError(TAG + "draw tex");
    }

    public void onDrawBefore() {

    }

    /**
     * delete specific texture
     */
    public static void deleteTex(final int hTex) {
        BZLogUtil.d(TAG, "deleteTex:");
        final int[] tex = new int[]{hTex};
        GLES20.glDeleteTextures(1, tex, 0);
    }

    /**
     * load, compile and link shader
     *
     * @param vss source of vertex shader
     * @param fss source of fragment shader
     * @return
     */
    protected int loadShader(final String vss, final String fss) {
        BZLogUtil.d(TAG, "loadShader:");
        BZOpenGlUtils.checkEglError("bz_BaseProgram loadShader start");
        int vs = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER);
        GLES20.glShaderSource(vs, vss);
        GLES20.glCompileShader(vs);
        final int[] compiled = new int[1];
        GLES20.glGetShaderiv(vs, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            BZLogUtil.e(TAG, "Failed to compile vertex shader:"
                    + GLES20.glGetShaderInfoLog(vs));
            GLES20.glDeleteShader(vs);
            vs = 0;
        }

        int fs = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);
        GLES20.glShaderSource(fs, fss);
        GLES20.glCompileShader(fs);
        GLES20.glGetShaderiv(fs, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            BZLogUtil.d(TAG, "Failed to compile fragment shader:"
                    + GLES20.glGetShaderInfoLog(fs));
            GLES20.glDeleteShader(fs);
            fs = 0;
        }

        final int program = GLES20.glCreateProgram();
        GLES20.glAttachShader(program, vs);
        GLES20.glAttachShader(program, fs);
        GLES20.glLinkProgram(program);

        BZOpenGlUtils.checkEglError("bz_BaseProgram loadShader end");
        return program;
    }

}
