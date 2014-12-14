package me.everything.jittlib;

import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewRootImpl;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;

import java.lang.ref.WeakReference;

/**
 * Created by adam on 12/11/14.
 */
public class JittService extends Service {

    private WindowManager windowManager;
    private WeakReference<Activity> mActivity;
    private ImageView button;
    WindowManager.LayoutParams params;

    private final IBinder mBinder = new JittLocalBinder();
    private boolean mEnabled = true;

    @Override public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override public void onCreate() {
        super.onCreate();

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        button = new ImageView(this);
        button.setImageResource(R.drawable.translate_icon_2);
        button.setAdjustViewBounds(true);
        final float scale = getResources().getDisplayMetrics().density;
        button.setMaxWidth((int) (48*scale));
        button.setMaxHeight((int) (48*scale));
        button.setScaleType(ImageView.ScaleType.FIT_CENTER);

        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.TOP | Gravity.LEFT;
        params.x = 630;
        params.y = 0;

        button.setOnTouchListener(new View.OnTouchListener() {
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;
            private long initialTouchTS;

            @Override public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        initialTouchTS = System.currentTimeMillis();
                        return true;
                    case MotionEvent.ACTION_UP:
                        if ( System.currentTimeMillis() - initialTouchTS < 250 ) {
                            initialTouchTS = 0;
                            Activity a = null;
                            if (mActivity != null) {
                                a = mActivity.get();
                            }
                            if ( a != null ) {
                                button.setVisibility(View.GONE);
                                Jitt.getInstance().openTranslationWindow((ViewGroup) a.getWindow().getDecorView());
                            }
                        }
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        params.x = initialX + (int) (event.getRawX() - initialTouchX);
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);
//                        android.util.Log.e("XXX","XYXYXY "+params.x+" "+params.y);
                        windowManager.updateViewLayout(button, params);
                        return true;
                }
                return false;
            }
        });

        windowManager.addView(button, params);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (button != null) windowManager.removeView(button);
    }

    public void setActivity(Activity activity) {
        if ( activity == null ) {
            button.setVisibility(View.GONE);
        } else if ( activity.getClass().getPackage().getName().equals("me.everything.jittlib") ) {
            button.setVisibility(View.GONE);
        } else {
            if ( mEnabled ) {
                button.setVisibility(View.VISIBLE);
            }
            mActivity = new WeakReference<Activity>(activity);
        }
    }

    public void setEnabled(final boolean enabled) {
        android.util.Log.e("XXX","setEnabled="+enabled);
        new Handler().post( new Runnable() {
            @Override
            public void run() {
                mEnabled = enabled;
                android.util.Log.e("XXX","actual setEnabled="+enabled);
                if ( button != null ) {
                    if ( enabled ) {
                        button.setVisibility(View.VISIBLE);
                    } else {
                        button.setVisibility(View.GONE);
                    }
                }
            }
        });
    }

    public class JittLocalBinder extends Binder {
        JittService getService() {
            return JittService.this;
        }
    }

}
