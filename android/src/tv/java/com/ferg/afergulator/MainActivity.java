package com.ferg.afergulator;

import android.app.Activity;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;

import butterknife.ButterKnife;
import butterknife.InjectView;
import com.ferg.afergulator.widget.ButtonNES;
import com.ferg.afergulator.widget.ButtonNES.Key;
import timber.log.Timber;

public class MainActivity extends Activity {

    @InjectView(R.id.gameView) GameView mGameView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        ButterKnife.inject(this);

        String rom = "Super Mario Bros.nes";

        try {
            mGameView.loadGame(getAssets().open("roms/" + rom), displayRomName(rom));
        } catch (IOException e) {
            Timber.w(e, "Couldn't load ROM (%s) !!!!", rom);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        mGameView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();

        Engine.pauseEmulator();
        Engine.saveBatteryRam();
        mGameView.onPause();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Key nesKey = ButtonNES.keyFromKeyCode(keyCode);
        if (nesKey != null) {
            Engine.buttonDown(nesKey);
            return true;
        }

        return false;
    }
     
     @Override
     public boolean onKeyUp(int keyCode, KeyEvent event) {
         Key nesKey = ButtonNES.keyFromKeyCode(keyCode);
         if (nesKey != null) {
             Engine.buttonUp(nesKey);
             return true;
         }

         return false;
     }

    private String displayRomName(String rom) {
        if (rom.endsWith(".nes")) {
            return rom.substring(0, rom.length() - 4);
        }
        return rom;
    }

}
