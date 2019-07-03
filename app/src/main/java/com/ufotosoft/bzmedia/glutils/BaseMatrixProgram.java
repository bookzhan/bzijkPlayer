package com.ufotosoft.bzmedia.glutils;

import com.ufotosoft.bzmedia.utils.BZLogUtil;

import static android.opengl.GLES20.glGetUniformLocation;
import static android.opengl.GLES20.glUniformMatrix4fv;

/**
 * Created by zhandalin on 2018-12-03 10:42.
 * 说明:
 */
public class BaseMatrixProgram extends BaseProgram {
    private static final String TAG = "bz_BaseMatrixProgram";
    private static final String vss
            = "attribute vec4 aPosition;\n"
            + "attribute vec2 aTextureCoord;\n"
            + "varying vec2 vTextureCoord;\n"
            + "uniform mat4 vMatrix;\n"
            + "void main() {\n"
            + "	gl_Position =vMatrix*aPosition;\n"
            + "	vTextureCoord = aTextureCoord;\n"
            + "}\n";
    private float matrix[] = {1, 0, 0, 0,
            0, 1, 0, 0,
            0, 0, 1, 0,
            0, 0, 0, 1};
    private int vMatrixLocation = -1;

    public BaseMatrixProgram(boolean needFlipVertical) {
        super(needFlipVertical);
    }

    public BaseMatrixProgram(int rotation, boolean flipHorizontal, boolean flipVertical) {
        super(rotation, flipHorizontal, flipVertical);
    }

    @Override
    public void onDrawBefore() {
        super.onDrawBefore();
        if (vMatrixLocation < 0) {
            vMatrixLocation = glGetUniformLocation(hProgram, "vMatrix");
        }
        if (null != matrix && vMatrixLocation >= 0) {
            glUniformMatrix4fv(vMatrixLocation, 1, false, matrix, 0);
        }
    }

    @Override
    public int loadShader(String vss, String fss) {
        return super.loadShader(BaseMatrixProgram.vss, fss);
    }

    public void setMatrix(float matrix[]) {
        if (null == matrix || matrix.length != 16) {
            BZLogUtil.e(TAG, "setMatrix null==matrix||matrix.length!=16");
            return;
        }
        this.matrix = matrix;
    }
}
