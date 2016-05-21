package com.ferg.afergulator;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Environment;
import android.util.AttributeSet;

import java.io.*;
import java.nio.*;
import java.util.Arrays;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import com.google.common.io.ByteStreams;
import go.nesdroid.Nesdroid;
import timber.log.Timber;

import static android.opengl.GLES20.*;

public class GameView extends GLSurfaceView implements GLSurfaceView.Renderer {

    // @formatter:off
    static final String VERT_SHADER_SRC =
            "attribute vec4 vPosition;" +
            "attribute vec2 vTexCoord;" +
            "varying vec2 texCoord;" +
            "void main() {" +
            "    texCoord = vec2(vTexCoord.x * .9375 +.03125, -(vTexCoord.y * .875) -.09375);" +
            "    gl_Position = vec4((vPosition.xy * 2.0) - 1.0, vPosition.zw);" +
            "}";

    static final String FRAG_SHADER_SRC =
            "precision highp float;" +
            "varying vec2 texCoord;" +
            "uniform sampler2D texture;" +
            "uniform ivec3 palette[64];" +
            "void main() {" +
            "    vec4 t = texture2D(texture, texCoord);" +
            "    int i = int(t.b * 15.0) * 16 + int(t.a * 15.0);" +
            "    i = i - ((i / 64) * 64);" +
            "    vec3 color = vec3(palette[i]) / 256.0;" +
            "    gl_FragColor = vec4(color, 1);" +
            "    gl_FragColor = vec4(1, 0, 0, 1);" +
            "}";

    static final int[] NES_PALLETE = {
            0x666666, 0x002A88, 0x1412A7, 0x3B00A4, 0x5C007E,
            0x6E0040, 0x6C0600, 0x561D00, 0x333500, 0x0B4800,
            0x005200, 0x004F08, 0x00404D, 0x000000, 0x000000,
            0x000000, 0xADADAD, 0x155FD9, 0x4240FF, 0x7527FE,
            0xA01ACC, 0xB71E7B, 0xB53120, 0x994E00, 0x6B6D00,
            0x388700, 0x0C9300, 0x008F32, 0x007C8D, 0x000000,
            0x000000, 0x000000, 0xFFFEFF, 0x64B0FF, 0x9290FF,
            0xC676FF, 0xF36AFF, 0xFE6ECC, 0xFE8170, 0xEA9E22,
            0xBCBE00, 0x88D800, 0x5CE430, 0x45E082, 0x48CDDE,
            0x4F4F4F, 0x000000, 0x000000, 0xFFFEFF, 0xC0DFFF,
            0xD3D2FF, 0xE8C8FF, 0xFBC2FF, 0xFEC4EA, 0xFECCC5,
            0xF7D8A5, 0xE4E594, 0xCFEF96, 0xBDF4AB, 0xB3F3CC,
            0xB5EBF2, 0xB8B8B8, 0x000000, 0x000000};

    // @formatter:on

    static final int BYTES_PER_FLOAT = Float.SIZE / 8;
    static final int STRIDE_BYTES = 2 * BYTES_PER_FLOAT;

    private int mProgram;
    private int mTexture;
    private int mVertexHandle;
    private int mTexCoordHandle;

    private ByteBuffer mVertexBuffer;
    private ByteBuffer mTexCoordBuffer;

    public GameView(Context context) {
        super(context);
        init(context);
    }

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context ctx) {
        setupContextPreserve();
        setEGLContextClientVersion(2);
        setDebugFlags(DEBUG_CHECK_GL_ERROR | DEBUG_CHECK_GL_ERROR);

//        setRenderer(this);
        setRenderer(new GameRenderer(ctx));

        if (!isInEditMode()) {
            File cache = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
//            Nesdroid.SetFilePath(cache.getAbsolutePath());
        }
    }

    public boolean loadGame(InputStream is, String name) throws IOException {
        byte[] rom = ByteStreams.toByteArray(is);
        byte[] start = Arrays.copyOfRange(rom, 0, 3);
        Timber.d("%s ROM: %s (%dk)", new String(start), name, rom.length / 1024);

        return Nesdroid.LoadRom(rom, name);
    }

    private void setupContextPreserve() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            setPreserveEGLContextOnPause(true);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = resolveSize(0, widthMeasureSpec);
        int h = getDefaultSize(0, heightMeasureSpec);
        if (w == 0) w = Math.round(h * 240f / 224f);
        setMeasuredDimension(w, h);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        Timber.i("onSurfaceCreated(%s, %s)...", gl.glGetString(GL_VERSION), config);

        glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        glEnable(GL_CULL_FACE);
        glEnable(GL_DEPTH_TEST);

        mProgram = createProgram(VERT_SHADER_SRC, FRAG_SHADER_SRC);

        mVertexHandle = glGetAttribLocation(mProgram, "vPosition");
        mTexCoordHandle = glGetAttribLocation(mProgram, "vTexCoord");

        int paletteLoc = glGetUniformLocation(mProgram, "palette");
        int textureUni = glGetUniformLocation(mProgram, "texture"); // TODO connect to Go buffer?

        int[] textures = new int[1];
        mTexture = textures[0];
        glGenTextures(1, textures, 0);
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, mTexture);

        gl.glTexParameterx(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        gl.glTexParameterx(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

        glUseProgram(mProgram);

        glEnableVertexAttribArray(mVertexHandle);
        glEnableVertexAttribArray(mTexCoordHandle);

        glUniform1iv(paletteLoc, NES_PALLETE.length, NES_PALLETE, 0);

        int[] buffers = new int[2];
        glGenBuffers(2, buffers, 0);

        float[] vertices = new float[]{-1, 1, -1, -1, 1, -1, 1, -1, 1, 1, -1, 1};
        float[] textureCoords = new float[]{0, 1, 0, 0, 1, 0, 1, 0, 1, 1, 0, 1};

        glBindBuffer(GL_ARRAY_BUFFER, buffers[0]);
        mVertexBuffer = ByteBuffer.allocateDirect(vertices.length * BYTES_PER_FLOAT);
        mVertexBuffer.order(ByteOrder.nativeOrder()).asFloatBuffer().put(vertices).position(0);
        glBufferData(GL_ARRAY_BUFFER, vertices.length * BYTES_PER_FLOAT, mVertexBuffer, GL_STATIC_DRAW);

        glBindBuffer(GL_ARRAY_BUFFER, buffers[1]);
        mTexCoordBuffer = ByteBuffer.allocateDirect(textureCoords.length * BYTES_PER_FLOAT);
        mTexCoordBuffer.order(ByteOrder.nativeOrder()).asFloatBuffer().put(textureCoords).position(0);
        glBufferData(GL_ARRAY_BUFFER, textureCoords.length * BYTES_PER_FLOAT, mTexCoordBuffer, GL_STATIC_DRAW);

//        glVertexAttribPointer(mVertexHandle, 2, GL_FLOAT, false, 2, mVertexBuffer);
//        glEnableVertexAttribArray(mVertexHandle);

//        glVertexAttribPointer(mTexCoordHandle, 2, GL_FLOAT, false, 2, mTexCoordBuffer);
//        glEnableVertexAttribArray(mTexCoordHandle);

        Timber.d("Surface Created…");
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        byte[] frame = Nesdroid.GetPixelBuffer();
        if (frame != null) {
            Timber.d("frame: %,d bytes", frame.length);

            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            glUseProgram(mProgram);

            glEnableVertexAttribArray(mVertexHandle);
            glVertexAttribPointer(mVertexHandle, 2, GL_FLOAT, false, 2 * BYTES_PER_FLOAT, mVertexBuffer);

            glEnableVertexAttribArray(mTexCoordHandle);
            glVertexAttribPointer(mTexCoordHandle, 2, GL_FLOAT, false, 2 * BYTES_PER_FLOAT, mTexCoordBuffer);

            glActiveTexture(GL_TEXTURE0);
            glBindTexture(GL_TEXTURE_2D, mTexture);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, 256, 256, 0, GL_RGBA,
                         GL_UNSIGNED_BYTE, ByteBuffer.wrap(frame));

            glDrawArrays(GL_TRIANGLES, 0, 6);

            glDisableVertexAttribArray(mVertexHandle);
            glDisableVertexAttribArray(mTexCoordHandle);
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        Timber.i("Surface Changed (%d, %d)…", width, height);
        glViewport(0, 0, width, height);
    }

    public static int createProgram(String vertShaderSrc, String fragShaderSrc) {
        int vertShader = loadShader(GL_VERTEX_SHADER, vertShaderSrc);
        int fragShader = loadShader(GL_FRAGMENT_SHADER, fragShaderSrc);

        int program = glCreateProgram();

        glAttachShader(program, vertShader);
        glAttachShader(program, fragShader);
        glLinkProgram(program);

        return program;
    }

    public static int loadShader(int type, String shaderCode) {
        int shader = glCreateShader(type);
        glShaderSource(shader, shaderCode);
        glCompileShader(shader);
        return shader;
    }

}
