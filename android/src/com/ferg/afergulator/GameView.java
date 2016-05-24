package com.ferg.afergulator;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.os.Environment;
import android.util.AttributeSet;

import java.io.*;

import go.nesdroid.Nesdroid;

public class GameView extends GLSurfaceView {

    public GameView(Context context) {
        super(context);
        init(context);
    }

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context ctx) {
        setEGLContextClientVersion(2);
        setPreserveEGLContextOnPause(true);
        setDebugFlags(DEBUG_CHECK_GL_ERROR | DEBUG_CHECK_GL_ERROR);

        setRenderer(new GameRenderer(ctx));

        if (!isInEditMode()) {
            File cache = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
//            Nesdroid.SetFilePath(cache.getAbsolutePath());
        }
    }

    public boolean loadGame(InputStream is, String name) throws IOException {
        return Nesdroid.LoadRom(toByteArray(is), name);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = resolveSize(0, widthMeasureSpec);
        int h = getDefaultSize(0, heightMeasureSpec);
        if (w == 0) w = Math.round(h * 240f / 224f);
        setMeasuredDimension(w, h);
    }

    public static byte[] toByteArray(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        copy(in, out);
        return out.toByteArray();
    }

    public static long copy(InputStream from, OutputStream to) throws IOException {
        if (from == null || to == null) return -1;

        byte[] buf = new byte[0x1000]; // 4K
        long total = 0;
        while (true) {
            int r = from.read(buf);
            if (r == -1) {
                break;
            }
            to.write(buf, 0, r);
            total += r;
        }

        return total;
    }

}
