package com.dlbarron.dlbwatchface;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.Time;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.WindowInsets;

import java.util.TimeZone;
import java.util.concurrent.TimeUnit;


public class DLBSquareWatchFace extends CanvasWatchFaceService   {
    private static final String TAG = "DLBSquareWatchFace";

    int batteryLevel;
    @Override
    public void onCreate() {
        super.onCreate();
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(batteryReceiver, intentFilter);
    }
    private BroadcastReceiver batteryReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL;
            batteryLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            if(isCharging) {
                Intent i = new Intent();
                i.setClassName("com.dlbarron.dlbwatchface", "com.dlbarron.dlbwatchface.DarkActivity");
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(i);
            }
            else {
                Intent startMain = new Intent(Intent.ACTION_MAIN);
                startMain.addCategory(Intent.CATEGORY_HOME);
                startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(startMain);
            }
        }
    };
    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(batteryReceiver);
    }
    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }
    private class Engine extends CanvasWatchFaceService.Engine {
        String[] daysOfWeek = {"SUN","MON","TUE","WED","THR","FRI","SAT"};
        static final int MSG_UPDATE_TIME = 0;
        Bitmap mHourBitmap, mHourScaledBitmap;
        Bitmap mMinuteBitmap, mMinuteScaledBitmap;
        Bitmap mSecondBitmap, mSecondScaledBitmap;
        Bitmap mBackgroundBitmap, mBackgroundScaledBitmap;
        Paint mTextPaint;
        boolean mMute;
        Time mTime;
        boolean mLowBitAmbient;
        boolean mRegisteredTimeZoneReceiver = false;
        float mDateXOffset;
        float mDateYOffset;
        float mDayXOffset;
        float mDayYOffset;
        Resources resources = DLBSquareWatchFace.this.getResources();
        final Handler mUpdateTimeHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case MSG_UPDATE_TIME:
                        invalidate();
                        if (shouldTimerBeRunning()) {
                            long timeMs = System.currentTimeMillis();
                            long delayMs = TimeUnit.SECONDS.toMillis(1) - (timeMs % TimeUnit.SECONDS.toMillis(1));
                            mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
                        }
                        break;
                }
            }
        };

        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mTime.clear(intent.getStringExtra("time-zone"));
                mTime.setToNow();
            }
        };
        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            super.onApplyWindowInsets(insets);

            // Load resources that have alternate values for round watches.

            mDateYOffset = resources.getDimension(R.dimen.y_date_offset_square_face);
            mDateXOffset = resources.getDimension(R.dimen.x_date_offset_square_face);
            mDayYOffset = resources.getDimension(R.dimen.y_day_offset_square_face);
            mDayXOffset = resources.getDimension(R.dimen.x_day_offset_square_face);
            // Load the 3 hand images
            Drawable drawable = resources.getDrawable(R.drawable.img195);
            mHourBitmap = ((BitmapDrawable) drawable).getBitmap();

            drawable = resources.getDrawable(R.drawable.img196);
            mMinuteBitmap = ((BitmapDrawable) drawable).getBitmap();

            drawable = resources.getDrawable(R.drawable.img199);
            mSecondBitmap = ((BitmapDrawable) drawable).getBitmap();
            //Load the background
            drawable = resources.getDrawable(R.drawable.square_bg);
            mBackgroundBitmap = ((BitmapDrawable) drawable).getBitmap();

        }
        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            setWatchFaceStyle(new WatchFaceStyle.Builder(DLBSquareWatchFace.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .setStatusBarGravity(Gravity.CENTER | Gravity.TOP)
                    .setHotwordIndicatorGravity(Gravity.CENTER | Gravity.TOP)
                    .build());

            float textSize = resources.getDimension(R.dimen.text_size);

            mTextPaint = new Paint();
            mTextPaint.setARGB(255, 200, 200, 200);
            mTextPaint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
            mTextPaint.setAntiAlias(true);
            mTextPaint.setTextSize(textSize);
            mTextPaint.setTextAlign(Paint.Align.CENTER);
            mTime = new Time();
        }

        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            if(inAmbientMode) {
                mTextPaint.setARGB(0,0,0,0);
            }
            else {
                mTextPaint.setARGB(255, 200, 200, 200);
            }
            if (mLowBitAmbient) {
                boolean antiAlias = !inAmbientMode;
                mTextPaint.setAntiAlias(antiAlias);
            }
            invalidate();
            updateTimer();
        }

        @Override
        public void onInterruptionFilterChanged(int interruptionFilter) {
            super.onInterruptionFilterChanged(interruptionFilter);
            boolean inMuteMode = (interruptionFilter == WatchFaceService.INTERRUPTION_FILTER_NONE);
            if (mMute != inMuteMode) {
                mMute = inMuteMode;
                mTextPaint.setAlpha(inMuteMode ? 80 : 255);
                invalidate();
            }
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            mTime.setToNow();
            // Draw the background
            if (mBackgroundScaledBitmap == null || mBackgroundScaledBitmap.getWidth() != bounds.width() || mBackgroundScaledBitmap.getHeight() != bounds.height()) {
                mBackgroundScaledBitmap = Bitmap.createScaledBitmap(mBackgroundBitmap,bounds.width(), bounds.height(), true);
            }
            canvas.drawBitmap(mBackgroundScaledBitmap, 0, 0, null);
            // Draw day of week
            String str = daysOfWeek[mTime.weekDay];
            canvas.drawText(str, mDayXOffset, mDayYOffset, mTextPaint);
            // Draw the date
            canvas.drawText(String.format("%2d", mTime.monthDay), mDateXOffset, mDateYOffset, mTextPaint);
            // Calculate hand rotations
            float secRot = mTime.second / 30f * 180f;
            float minRot = mTime.minute / 30f * 180f;
            float hrRot = ((mTime.hour + (mTime.minute / 60f)) / 6f * 180f);

            if (mHourScaledBitmap == null || mHourScaledBitmap.getWidth() != bounds.width() || mHourScaledBitmap.getHeight() != bounds.height()) {
                mHourScaledBitmap = Bitmap.createScaledBitmap(mHourBitmap,bounds.width(), bounds.height(), true);
            }
            canvas.save();
            canvas.rotate(hrRot,canvas.getWidth()/2,canvas.getHeight()/2);
            canvas.drawBitmap(mHourScaledBitmap, 0, 0, null);

            canvas.restore();
            if (mMinuteScaledBitmap == null || mMinuteScaledBitmap.getWidth() != bounds.width() || mMinuteScaledBitmap.getHeight() != bounds.height()) {
                mMinuteScaledBitmap = Bitmap.createScaledBitmap(mMinuteBitmap, bounds.width(), bounds.height(), true );
            }
            canvas.save();
            canvas.rotate(minRot,canvas.getWidth()/2,canvas.getHeight()/2);
            canvas.drawBitmap(mMinuteScaledBitmap, 0, 0, null);
            canvas.restore();

            if (!isInAmbientMode()) {
                if (mSecondScaledBitmap == null || mSecondScaledBitmap.getWidth() != bounds.width() || mSecondScaledBitmap.getHeight() != bounds.height()) {
                    mSecondScaledBitmap = Bitmap.createScaledBitmap(mSecondBitmap, bounds.width(), bounds.height(), true);
                }
                canvas.save();
                canvas.rotate(secRot,canvas.getWidth()/2,canvas.getHeight()/2);
                canvas.drawBitmap(mSecondScaledBitmap, 0, 0, null);
                canvas.restore();
            }
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);
            if (visible) {
                registerReceiver();

                mTime.clear(TimeZone.getDefault().getID());
                mTime.setToNow();
            }
            else {
                unregisterReceiver();
            }
            updateTimer();
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            DLBSquareWatchFace.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            DLBSquareWatchFace.this.unregisterReceiver(mTimeZoneReceiver);
        }

        private void updateTimer() {
            //Log.i(TAG, "updateTimer");
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }

    }

}
