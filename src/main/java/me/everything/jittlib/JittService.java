package me.everything.jittlib;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Binder;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.BounceInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;

import com.melnykov.fab.FloatingActionButton;

import java.lang.ref.WeakReference;

/**
 * Created by adam on 12/11/14.
 */
public class JittService extends Service {

    private WindowManager mWindowManager;
    private WeakReference<Activity> mActivity;
    private ViewGroup mParentView;
    private FloatingActionButton mOpenJittButton;
    WindowManager.LayoutParams mWindowParams;
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
        mOpenJittButton.setImageResource(R.drawable.translate_icon);
        mOpenJittButton.setColorNormalResId(R.color.translate_icon_normal);
        mOpenJittButton.setColorPressedResId(R.color.translate_icon_pressed);
        mOpenJittButton.setColorRippleResId(R.color.translate_icon_ripple);
        mOpenJittButton.setScaleX(0f);
        mOpenJittButton.setScaleY(0f);

        FrameLayout.LayoutParams params =  new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);

        mParentView = new FrameLayout(this);
        // Allow shadow to be fe fully drawn
        mParentView.setClipToPadding(false);
        mParentView.setPadding(10,10,10,20);
        mParentView.addView(mOpenJittButton, params);

        mWindowParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        mWindowParams.gravity = Gravity.TOP | Gravity.LEFT;
        mWindowParams.x = 630;
        mWindowParams.y = 0;

        mOpenJittButton.setOnTouchListener(new View.OnTouchListener() {
            private float initialX;
            private float initialY;
            private float initialTouchX;
            private float initialTouchY;
            private long initialTouchTS;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getActionMasked();
                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                        mOpenJittButton.setPressed(true);
                        initialX = mWindowParams.x;
                        initialY = mWindowParams.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        initialTouchTS = System.currentTimeMillis();

                        return true;
                    case MotionEvent.ACTION_UP:
                        mOpenJittButton.setPressed(false);
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
                        mWindowParams.x = (int) (initialX + (event.getRawX() - initialTouchX));
                        mWindowParams.y = (int) (initialY + (event.getRawY() - initialTouchY));
                        updateWindow();
                        return true;
                }
                return false;
            }
        });
    }

    private void updateWindow() {
        try {
            if (mEnabled) {
                mWindowManager.updateViewLayout(mParentView, mWindowParams);
            }
        } catch (IllegalArgumentException e) {
            // View not attached to window manager
            // do nothing
        }
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
        if (mEnabled == enabled || mOpenJittButton == null || mWindowParams == null) {
            return;
        }

        if (mOpenJittButtonScaleInAnimation != null) {
            mOpenJittButtonScaleInAnimation.cancel();
            mOpenJittButtonScaleInAnimation = null;
        }

        if (mOpenJittButtonScaleOutAnimation != null) {
            mOpenJittButtonScaleOutAnimation.cancel();
            mOpenJittButtonScaleOutAnimation = null;
        }

        if (enabled) {
            mWindowManager.addView(mParentView, mWindowParams);
            // Animate inner button
            ObjectAnimator scaleInX = ObjectAnimator.ofFloat(mOpenJittButton,"scaleX",0f, 1f);
            ObjectAnimator scaleInY = ObjectAnimator.ofFloat(mOpenJittButton,"scaleY",0f, 1f);
            mOpenJittButtonScaleInAnimation = new AnimatorSet();
            mOpenJittButtonScaleInAnimation.playTogether(scaleInX, scaleInY);
            mOpenJittButtonScaleInAnimation.setDuration(500);
            mOpenJittButtonScaleInAnimation.setStartDelay(300);
            mOpenJittButtonScaleInAnimation.setInterpolator(new BounceInterpolator());
            mOpenJittButtonScaleInAnimation.start();

        } else {
            // Animate inner button
            ObjectAnimator scaleInX = ObjectAnimator.ofFloat(mOpenJittButton,"scaleX",1f, 0f);
            ObjectAnimator scaleInY = ObjectAnimator.ofFloat(mOpenJittButton,"scaleY",1f, 0f);
            mOpenJittButtonScaleOutAnimation = new AnimatorSet();
            mOpenJittButtonScaleOutAnimation.playTogether(scaleInX, scaleInY);
            mOpenJittButtonScaleOutAnimation.setDuration(200);
            mOpenJittButtonScaleOutAnimation.setInterpolator(new DecelerateInterpolator());
            mOpenJittButtonScaleOutAnimation.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {

                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    mWindowManager.removeView(mParentView);
                }

                @Override
                public void onAnimationCancel(Animator animation) {

                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }
            });
            mOpenJittButtonScaleOutAnimation.start();
        }

        mEnabled = enabled;
    }

    public class JittLocalBinder extends Binder {
        JittService getService() {
            return JittService.this;
        }
    }
}
