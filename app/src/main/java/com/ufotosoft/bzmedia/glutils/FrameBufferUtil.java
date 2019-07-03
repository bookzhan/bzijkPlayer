package com.ufotosoft.bzmedia.glutils;

import com.ufotosoft.bzmedia.utils.BZLogUtil;

import static android.opengl.GLES20.GL_CLAMP_TO_EDGE;
import static android.opengl.GLES20.GL_COLOR_ATTACHMENT0;
import static android.opengl.GLES20.GL_FRAMEBUFFER;
import static android.opengl.GLES20.GL_FRAMEBUFFER_BINDING;
import static android.opengl.GLES20.GL_LINEAR;
import static android.opengl.GLES20.GL_RGBA;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.GL_TEXTURE_MAG_FILTER;
import static android.opengl.GLES20.GL_TEXTURE_MIN_FILTER;
import static android.opengl.GLES20.GL_TEXTURE_WRAP_S;
import static android.opengl.GLES20.GL_TEXTURE_WRAP_T;
import static android.opengl.GLES20.GL_UNSIGNED_BYTE;
import static android.opengl.GLES20.glBindFramebuffer;
import static android.opengl.GLES20.glBindTexture;
import static android.opengl.GLES20.glDeleteFramebuffers;
import static android.opengl.GLES20.glDeleteTextures;
import static android.opengl.GLES20.glFramebufferTexture2D;
import static android.opengl.GLES20.glGenFramebuffers;
import static android.opengl.GLES20.glGenTextures;
import static android.opengl.GLES20.glGetIntegerv;
import static android.opengl.GLES20.glIsFramebuffer;
import static android.opengl.GLES20.glIsTexture;
import static android.opengl.GLES20.glTexImage2D;
import static android.opengl.GLES20.glTexParameterf;

/**
 * Created by zhandalin on 2017-12-08 09:11.
 * 说明:
 */

public class FrameBufferUtil {

    private int[] frameBuffer = new int[1];
    private int[] frameBufferTextureId = new int[1];
    private static final String TAG = "bz_FrameBufferUtil";
    private int width;
    private int height;
    private int[] lastBindFrameBuffer = new int[]{0};

    public FrameBufferUtil(int width, int height) {
        this.width = width;
        this.height = height;
        initFrameBuffer(width, height);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    private void initFrameBuffer(int width, int height) {
//        BZLogUtil.d(TAG, "initFrameBuffer width=" + width + " height=" + height);

        BZOpenGlUtils.checkEglError("bz_FrameBufferUtil initFrameBuffer start");

        glGenFramebuffers(1, frameBuffer, 0);
        glBindFramebuffer(GL_FRAMEBUFFER, frameBuffer[0]);

        glGenTextures(1, frameBufferTextureId, 0);
        glBindTexture(GL_TEXTURE_2D, frameBufferTextureId[0]);

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0,
                GL_RGBA, GL_UNSIGNED_BYTE, null);
        glTexParameterf(GL_TEXTURE_2D,
                GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameterf(GL_TEXTURE_2D,
                GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameterf(GL_TEXTURE_2D,
                GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameterf(GL_TEXTURE_2D,
                GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0,
                GL_TEXTURE_2D, frameBufferTextureId[0], 0);
        glBindTexture(GL_TEXTURE_2D, 0);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);

        BZOpenGlUtils.checkEglError("bz_FrameBufferUtil initFrameBuffer end");
    }

    public void bindFrameBuffer() {
//        GLUtil.checkGlError("bindFrameBuffer start");
        //找出当前绑定的FrameBuffer,最后需要还原的
        lastBindFrameBuffer[0] = 0;
        glGetIntegerv(GL_FRAMEBUFFER_BINDING, lastBindFrameBuffer, 0);
        if (glIsFramebuffer(frameBuffer[0])) {
            glBindFramebuffer(GL_FRAMEBUFFER, frameBuffer[0]);
        } else {
            BZLogUtil.e(TAG, "Framebuffer unavailable recreate");
            release();
            initFrameBuffer(width, height);
            glBindFramebuffer(GL_FRAMEBUFFER, frameBuffer[0]);
        }
//        GLUtil.checkGlError("bindFrameBuffer end");
    }

    public void unbindFrameBuffer() {
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        if (lastBindFrameBuffer[0] > 0) {
            glBindFramebuffer(GL_FRAMEBUFFER, lastBindFrameBuffer[0]);
        }
    }

    public int getFrameBufferTextureID() {
        return frameBufferTextureId[0];
    }

    public void release() {
        GLUtil.checkGlError("FrameBufferUtil release start");
        if (frameBuffer[0] > 0 && glIsFramebuffer(frameBuffer[0])) {
            glDeleteFramebuffers(1, frameBuffer, 0);
            frameBuffer[0] = 0;
        }
        if (frameBufferTextureId[0] > 0 && glIsTexture(frameBufferTextureId[0])) {
            glDeleteTextures(1, frameBufferTextureId, 0);
            frameBufferTextureId[0] = 0;
        }
//        BZLogUtil.d(TAG, "release finish");
        GLUtil.checkGlError("FrameBufferUtil release end");
    }
}
