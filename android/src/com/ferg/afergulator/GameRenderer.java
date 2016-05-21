package com.ferg.afergulator;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.*;
import android.opengl.GLSurfaceView.Renderer;

import java.nio.*;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import go.nesdroid.Nesdroid;

import static android.opengl.GLES20.*;

class GameRenderer implements Renderer {

    // @formatter:off
    private final String pixelShader =
            "attribute vec4 vPosition;" +
            "attribute vec2 a_texCoord;" +
            "varying vec2 v_texCoord;" +
            "void main() {" +
            "  gl_Position = vPosition;" +
            "  v_texCoord = a_texCoord;" +
            "}";

    private final String fragmentShader =
            "precision mediump float;" +
            "varying vec2 v_texCoord;" +
            "uniform sampler2D s_texture;" +
            "void main() {" +
//          "    gl_FragColor = texture2D(s_texture, v_texCoord);" +
            "    vec4 c = texture2D(s_texture, v_texCoord);" +
            "    gl_FragColor = vec4(c.a, c.b, c.g, 1.0);" +
            "}";
    // @formatter:on

    private short indices[];

    private FloatBuffer vertexBuffer;
    private ShortBuffer drawListBuffer;
    private FloatBuffer uvBuffer;

    private Context mContext;
    private int     mProgram;

    private int mTexture;
    private int mPositionLoc;
    private int mTexCoordLoc;
    private int mSamplerLoc;

    GameRenderer(Context c) {
        mContext = c;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        setupSquare();
        setupTexture();

        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1);

        int vertexShader = GameView.loadShader(GLES20.GL_VERTEX_SHADER, pixelShader);
        int fragmentShader = GameView.loadShader(GLES20.GL_FRAGMENT_SHADER, this.fragmentShader);

        mProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(mProgram, vertexShader);
        GLES20.glAttachShader(mProgram, fragmentShader);
        GLES20.glLinkProgram(mProgram);
        GLES20.glUseProgram(mProgram);
    }

    private void setupTexture() {
        float[] uvs = new float[]{
                0.0f, 0.0f,
                0.0f, 1.0f,
                1.0f, 1.0f,
                1.0f, 0.0f
        };

        ByteBuffer bb = ByteBuffer.allocateDirect(uvs.length * 4);
        bb.order(ByteOrder.nativeOrder());
        uvBuffer = bb.asFloatBuffer();
        uvBuffer.put(uvs);
        uvBuffer.position(0);

        int[] texturenames = new int[1];
        GLES20.glGenTextures(1, texturenames, 0);
        mTexture = texturenames[0];

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTexture);

        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

        Bitmap bmp = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ic_launcher);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bmp, 0);
        bmp.recycle();
    }

    private void setupSquare() {
        float[] vertices = new float[]{
                -1.0f, 1.0f, 0.0f,
                -1.0f, -1.0f, 0.0f,
                1.0f, -1.0f, 0.0f,
                1.0f, 1.0f, 0.0f,
        };

        indices = new short[]{0, 1, 2, 0, 2, 3};

        ByteBuffer bb = ByteBuffer.allocateDirect(vertices.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(vertices);
        vertexBuffer.position(0);

        ByteBuffer dlb = ByteBuffer.allocateDirect(indices.length * 2);
        dlb.order(ByteOrder.nativeOrder());
        drawListBuffer = dlb.asShortBuffer();
        drawListBuffer.put(indices);
        drawListBuffer.position(0);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
    }

    @Override
    public void onDrawFrame(GL10 unused) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        mPositionLoc = GLES20.glGetAttribLocation(mProgram, "vPosition");
        GLES20.glEnableVertexAttribArray(mPositionLoc);
        GLES20.glVertexAttribPointer(mPositionLoc, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);

        mTexCoordLoc = GLES20.glGetAttribLocation(mProgram, "a_texCoord");
        GLES20.glEnableVertexAttribArray(mTexCoordLoc);
        GLES20.glVertexAttribPointer(mTexCoordLoc, 2, GLES20.GL_FLOAT, false, 0, uvBuffer);

        mSamplerLoc = GLES20.glGetUniformLocation(mProgram, "s_texture");
        GLES20.glUniform1i(mSamplerLoc, 0);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTexture);

        byte[] frame = Nesdroid.GetPixelBuffer();
        if (frame != null) {
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, 240, 256, 0, GL_RGBA,
                         GL_UNSIGNED_BYTE, ByteBuffer.wrap(frame));
        }

        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indices.length,
                              GLES20.GL_UNSIGNED_SHORT, drawListBuffer);

        GLES20.glDisableVertexAttribArray(mPositionLoc);
        GLES20.glDisableVertexAttribArray(mTexCoordLoc);

    }
}
