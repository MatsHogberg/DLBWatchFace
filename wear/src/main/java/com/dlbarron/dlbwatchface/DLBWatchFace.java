package com.dlbarron.dlbwatchface;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
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
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.WindowInsets;

import java.util.TimeZone;
import java.util.concurrent.TimeUnit;


public class DLBWatchFace extends CanvasWatchFaceService   {
    private static final String TAG = "DLBWatchFace";

    float mDateYOffset;
    float mDayYOffset;
    String[] daysOfWeek = {"Sunday","Monday","Tuesday","Wednesday","Thursday","Friday","Saturday"};
    String[] monthsOfYear = {"January","February","March","April","May","June","July","August","September","October","November","December"};
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
            Log.i(TAG,"Charging: " + isCharging);
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
        static final int MSG_UPDATE_TIME = 0;
        Paint mBackgroundPaint;
        Paint mSecondPaint;
        Bitmap mHourBitmap, mHourScaledBitmap;
        Bitmap mMinuteBitmap, mMinuteScaledBitmap;
        Paint mTickPaint;
        Paint mSmallTickPaint;
        Paint mTextPaint;
        boolean mMute;
        Time mTime;
        boolean mLowBitAmbient;
        boolean mRegisteredTimeZoneReceiver = false;
        Resources resources = DLBWatchFace.this.getResources();
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

            boolean isRound = insets.isRound();
            if(isRound) {
                mDateYOffset = resources.getDimension(R.dimen.y_date_offset_round);
                mDayYOffset = resources.getDimension(R.dimen.y_day_offset_round);
            }
            else {
                mDateYOffset = resources.getDimension(R.dimen.y_date_offset_square);
                mDayYOffset = resources.getDimension(R.dimen.y_day_offset_square);

            }
            Drawable drawable = resources.getDrawable(R.drawable.hour);
            mHourBitmap = ((BitmapDrawable) drawable).getBitmap();

            drawable = resources.getDrawable(R.drawable.minute);
            mMinuteBitmap = ((BitmapDrawable) drawable).getBitmap();

        }
        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            setWatchFaceStyle(new WatchFaceStyle.Builder(DLBWatchFace.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .setStatusBarGravity(Gravity.CENTER | Gravity.TOP)
                    .setHotwordIndicatorGravity(Gravity.CENTER | Gravity.TOP)
                    .build());

            float textSize = resources.getDimension(R.dimen.text_size);
            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(Color.parseColor("black"));

            mSecondPaint = new Paint();
            mSecondPaint.setARGB(255, 255, 0, 0);
            mSecondPaint.setStrokeWidth(2.f);
            mSecondPaint.setAntiAlias(true);
            mSecondPaint.setStrokeCap(Paint.Cap.BUTT);

            mTickPaint = new Paint();
            mTickPaint.setARGB(255, 200, 200, 200);
            mTickPaint.setStrokeWidth(3.f);
            mTickPaint.setAntiAlias(true);

            mSmallTickPaint = new Paint();
            mSmallTickPaint.setARGB(255, 200, 200, 200);
            mSmallTickPaint.setStrokeWidth(1.f);
            mSmallTickPaint.setAntiAlias(true);

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
                mTickPaint.setARGB(0,0,0,0);
                mSmallTickPaint.setARGB(0,0,0,0);
            }
            else {
                mTextPaint.setARGB(255, 200, 200, 200);
                mTickPaint.setARGB(255, 200, 200, 200);
                mSmallTickPaint.setARGB(255, 200, 200, 200);
            }
            if (mLowBitAmbient) {
                boolean antiAlias = !inAmbientMode;
                mSecondPaint.setAntiAlias(antiAlias);
                mTickPaint.setAntiAlias(antiAlias);
                mSmallTickPaint.setAntiAlias(antiAlias);
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
                mSecondPaint.setAlpha(inMuteMode ? 80 : 255);
                mTextPaint.setAlpha(inMuteMode ? 80 : 255);
                invalidate();
            }
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            mTime.setToNow();

            float centerX = bounds.width() / 2f;
            float centerY = bounds.height() / 2f;
            // Draw the background
            canvas.drawRect(0, 0, bounds.width(), bounds.height(), mBackgroundPaint);
            // Draw the date.
            String str = daysOfWeek[mTime.weekDay];
            canvas.drawText(str, centerX, mDayYOffset, mTextPaint);
            str = monthsOfYear[mTime.month] + " " + mTime.monthDay + ", " + mTime.year;
            //Draw the day
            canvas.drawText(str, centerX, mDateYOffset, mTextPaint);
            // Draw the ticks.
            float innerTickRadius = centerX - 15;
            for (int tickIndex = 0; tickIndex < 12; tickIndex++) {
                float tickRot = (float) (tickIndex * Math.PI * 2 / 12);
                float innerX = (float) Math.sin(tickRot) * innerTickRadius;
                float innerY = (float) -Math.cos(tickRot) * innerTickRadius;
                float outerX = (float) Math.sin(tickRot) * centerX;
                float outerY = (float) -Math.cos(tickRot) * centerX;
                canvas.drawLine(centerX + innerX, centerY + innerY, centerX + outerX, centerY + outerY, mTickPaint);
            }

            innerTickRadius = centerX - 10;
            for (int tickIndex = 0; tickIndex < 60; tickIndex++) {
                float tickRot = (float) (tickIndex * Math.PI * 2 / 60);
                float innerX = (float) Math.sin(tickRot) * innerTickRadius;
                float innerY = (float) -Math.cos(tickRot) * innerTickRadius;
                float outerX = (float) Math.sin(tickRot) * centerX;
                float outerY = (float) -Math.cos(tickRot) * centerX;
                canvas.drawLine(centerX + innerX, centerY + innerY, centerX + outerX, centerY + outerY, mSmallTickPaint);
            }
            for (int i = 1; i <= 12; i++) {
                float x = (float) Math.sin(Math.PI * 2 * (i / (float) 12)) * 110;
                float y = -(float) Math.cos(Math.PI * 2 * (i / (float) 12)) * 110;
                canvas.drawText(String.format("%d", i), centerX + x, centerY + y - vCenter(), mTextPaint);
            }
            float secRot = mTime.second / 30f * (float) Math.PI;
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
                float secLength = centerX - 20;
                float secX = (float) Math.sin(secRot) * secLength;
                float secY = (float) -Math.cos(secRot) * secLength;
                canvas.drawLine(centerX, centerY, centerX + secX, centerY + secY, mSecondPaint);
                canvas.drawCircle(centerX,centerY, 4, mSecondPaint);
            }
        }
        private float vCenter() {
            return (mTextPaint.ascent() + mTextPaint.descent()) / 2;
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
            DLBWatchFace.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            DLBWatchFace.this.unregisterReceiver(mTimeZoneReceiver);
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
