package me.everything.jittlib;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.BounceInterpolator;

import com.melnykov.fab.FloatingActionButton;

import java.lang.ref.WeakReference;

/**
 * Created by adam on 12/11/14.
 */
public class JittService extends Service {

    private WindowManager mWindowManager;
    private WeakReference<Activity> mActivity;
    private FloatingActionButton mOpenJittButton;
    WindowManager.LayoutParams mOpenJittButtonparams;
    private boolean mEnabled = false;

    private AnimatorSet mOpenJittButtonScaleInAnimation = null;
    private AnimatorSet mOpenJittButtonScaleOutAnimation = null;

    private final IBinder mBinder = new JittLocalBinder();

    @Override public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override public void onCreate() {
        super.onCreate();
        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        mOpenJittButton = new FloatingActionButton(this);
        mOpenJittButton.setImageResource(R.drawable.translate_icon_2);
        mOpenJittButton.setAdjustViewBounds(true);

        mOpenJittButtonparams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        mOpenJittButtonparams.gravity = Gravity.TOP | Gravity.LEFT;
        mOpenJittButtonparams.x = 630;
        mOpenJittButtonparams.y = 0;

        mOpenJittButton.setOnTouchListener(new View.OnTouchListener() {
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;
            private long initialTouchTS;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = mOpenJittButtonparams.x;
                        initialY = mOpenJittButtonparams.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        initialTouchTS = System.currentTimeMillis();
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (System.currentTimeMillis() - initialTouchTS < 250) {
                            initialTouchTS = 0;
                            Activity activity = null;
                            if (mActivity != null) {
                                activity = mActivity.get();
                            }
                            if (activity != null) {
                                Jitt.getInstance().openTranslationWindow((ViewGroup) activity.getWindow().getDecorView());
                            }
                        }
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        mOpenJittButtonparams.x = initialX + (int) (event.getRawX() - initialTouchX);
                        mOpenJittButtonparams.y = initialY + (int) (event.getRawY() - initialTouchY);
                        mWindowManager.updateViewLayout(mOpenJittButton, mOpenJittButtonparams);
                        return true;
                }
                return false;
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        setEnabled(false);
    }

    public void setActivity(Activity activity) {
        if (activity == null) {
            setEnabled(false);
        } else if ( activity.getClass().getPackage().getName().equals("me.everything.jittlib") ) {
            setEnabled(false);
        } else {
            mActivity = new WeakReference<Activity>(activity);
            setEnabled(true);
        }
    }

    public synchronized void setEnabled(final boolean enabled) {
        if (mEnabled == enabled || mOpenJittButton == null) {
            return;
        }

        if (enabled) {
            mWindowManager.addView(mOpenJittButton, mOpenJittButtonparams);
        } else {
            mWindowManager.removeView(mOpenJittButton);
        }

        mEnabled = enabled;
    }

    public class JittLocalBinder extends Binder {
        JittService getService() {
            return JittService.this;
        }
    }

}
