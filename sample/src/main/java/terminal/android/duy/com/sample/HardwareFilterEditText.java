package terminal.android.duy.com.sample;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.Selection;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.Toast;

/**
 * Created by Duy on 26-Mar-17.
 */

public class HardwareFilterEditText extends android.support.v7.widget.AppCompatEditText {
    private static final String TAG = HardwareFilterEditText.class.getSimpleName();
    private static final boolean LOG_KEY_EVENTS = true;
    private TermSettings mSettings;
    private SharedPreferences mPrefs;
    private KeyListener mKeyListener;
    private String mImeBuffer = "";

    public HardwareFilterEditText(Context context) {
        super(context);
        init();
    }

    public HardwareFilterEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public HardwareFilterEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public void init() {
        mPrefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        mSettings = new TermSettings(mPrefs);
        mKeyListener = new KeyListener();
        updatePrefs(mSettings);

    }

    public void updatePrefs(TermSettings settings) {
        mSettings = settings;
    }


    /**
     * @param keyCode - key code event
     * @param down    - is down
     * @return - <code>true</code> if is ctrl key
     */
    private boolean handleControlKey(int keyCode, boolean down) {
        if (keyCode == mSettings.getControlKeyCode()) {
            Log.w(TAG, "handler control key: ");
            mKeyListener.handleControlKey(down);
            return true;
        }
        return false;
    }

    /**
     * CTRL + C copy
     * CTRL + V paste
     * CTRL + B: compile
     * CTRL + R run
     * CTRL + X cut
     * CTRL + Z undo
     * CTRL + Y redo
     * CTRL + Q quit
     * CTRL + S save
     * CTRL + O open
     * CTRL + F find
     * CTRL + H find and replace
     * CTRL + L format code
     * CTRL + G: goto line
     */
    @Override
    public boolean onKeyDown(int zKeyCode, KeyEvent event) {
        if (TermDebug.LOG_IME) Log.w(TAG, "onKeyDown: " + zKeyCode + " " + event);

        //The new Key Code
        int keyCode = event.getKeyCode();

        if (handleControlKey(keyCode, true)) {
            return true;
        } else if (event.isSystem()) {
            // Don't intercept the system keys And the HOME /END / PGUP / PGDOWN KEYS
            return super.onKeyDown(keyCode, event);
        }

        int state = mKeyListener.keyDown(keyCode, event);
        if (state != -1) {
            Log.d(TAG, "onKeyDown: " + state);
            switch (state) {
                case KeyListener.ACTION_SELECT_ALL:
                    Selection.selectAll(getEditableText());
                    break;
                case KeyListener.ACTION_COPY:
                    Toast.makeText(getContext(), "Copy", Toast.LENGTH_SHORT).show();
                    break;
                case KeyListener.ACTION_PASTE:
                    Toast.makeText(getContext(), "Paste", Toast.LENGTH_SHORT).show();
                    break;
                case KeyListener.ACTION_CUT:
                    Toast.makeText(getContext(), "cut", Toast.LENGTH_SHORT).show();
                    break;
                default:
                    return true;
            }
            return true;
        }
        return super.onKeyDown(zKeyCode, event);
    }

    @Override
    public boolean onKeyUp(int zKeyCode, KeyEvent event) {
        if (LOG_KEY_EVENTS) Log.w(TAG, "onKeyUp " + zKeyCode);

        //The new Key Code
        int keyCode = event.getKeyCode();
        if (handleControlKey(keyCode, false)) {
            return true;
        } else if (event.isSystem()) {
            // Don't intercept the system keys
            return super.onKeyUp(keyCode, event);
        }
        mKeyListener.keyUp(keyCode);
        return true;
    }


    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        return new BaseInputConnection(this, false);
    }
}