package com.github.ashutoshgngwr.tenbitclockwidget;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.Log;
import android.util.TypedValue;

import androidx.annotation.DimenRes;

import java.util.Calendar;

class ClockWidgetRenderer {

	private static final String TAG = ClockWidgetRenderer.class.getSimpleName();

	private static final int SEPARATOR_LINE_ALPHA = 0x75;

	private static ClockWidgetRenderer mInstance;

	private final int width = getDimen(R.dimen.widget_width);
	private final int height = getDimen(R.dimen.widget_height);
	private final int padding = px(10);
	private final Paint mPaint;
	private final Bitmap clockBitmap;
	private final Canvas canvas;

	private ClockWidgetRenderer() {
		clockBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		canvas = new Canvas(clockBitmap);
		mPaint = new Paint();
	}

	static Bitmap renderBitmap() {
		if (mInstance == null) {
			Log.d(TAG, "Creating a new renderer instance...");
			mInstance = new ClockWidgetRenderer();
		}

		return mInstance.render();
	}

	private int getDimen(@DimenRes int resId) {
		return Math.round(ClockWidgetApplication.getContext().getResources().getDimension(resId));
	}

	private void clearClockBitmap() {
		clockBitmap.eraseColor(Color.TRANSPARENT);
	}

	private void resetPaint() {
		mPaint.reset();
		mPaint.setAntiAlias(true);
		mPaint.setStyle(Paint.Style.FILL);
	}

	private Bitmap render() {
		// clear and reuse previously allocated bitmap
		clearClockBitmap();
		resetPaint();

		Calendar calendar = Calendar.getInstance();
		final boolean is24Hour = ClockWidgetSettings.shouldUse24HourFormat();
		final int nHourBits = is24Hour ? ClockWidgetSettings.shouldUse6bitsForHour() ? 6 : 5 : 4;
		final int hour = calendar.get(is24Hour ? Calendar.HOUR_OF_DAY : Calendar.HOUR);
		final int minute = calendar.get(Calendar.MINUTE);
		final int onBitColor = is24Hour || calendar.get(Calendar.AM_PM) == Calendar.AM
			? ClockWidgetSettings.getClockAMOnColor() : ClockWidgetSettings.getClockPMOnColor();

		final int offBitColor = is24Hour || calendar.get(Calendar.AM_PM) == Calendar.AM
			? ClockWidgetSettings.getClockAMOffColor() : ClockWidgetSettings.getClockPMOffColor();

		final float sx = width * (is24Hour ? 0.5f : 0.4f);
		final float sp = px(5);

		// set clock's background color.
		mPaint.setColor(ClockWidgetSettings.getClockBackgroundColor());
		canvas.drawRoundRect(new RectF(0, 0, width, height), px(5), px(5), mPaint);

		RectF bounds = new RectF(padding, padding, sx - sp, height - padding);
		renderBits(onBitColor, offBitColor, bounds, 2, is24Hour ? 3 : 2, nHourBits, hour, false);

		bounds.set(sx + sp, padding, width - padding, height - padding);
		renderBits(onBitColor, offBitColor, bounds, 2, 3, 6, minute, true);

		if (ClockWidgetSettings.shouldDisplaySeparator()) {
			mPaint.setColor(onBitColor);
			mPaint.setAlpha(SEPARATOR_LINE_ALPHA);
			mPaint.setStrokeWidth(px(1));
			canvas.drawLine(sx, height * 0.35f, sx, height * 0.65f, mPaint);
		}

		return clockBitmap;
	}
	@SuppressWarnings("SameParameterValue")
	private void renderBits(int onColor, int offColor, RectF bounds, int rows, int cols, int nBits, int value, boolean isMinutes) {
		final float dr = px(ClockWidgetSettings.getDotSize());
		final float cw = bounds.width() / cols;
		final float ch = bounds.height() / rows;
		final float cpx = (cw - (dr * 2)) / 2;
		final float cpy = (ch - (dr * 2)) / 2;
		final int remMinutes = (value % 15) ;
		final int quarterHours = (value / 15);
		float x = bounds.right;
		float y = bounds.bottom;

		for (int i = 0; i < rows; i++) {
			for (int j = 0; j < cols; j++) {
				if (--nBits < 0) {
					continue;
				}
				// TODO: make the quarter hour tally configurable
				// if (isMinutes && tallyQuarterHours) ...
				if (isMinutes){
					setMinuteColor(onColor, offColor, cols, quarterHours, remMinutes, i, j);
				} else {
					setColor(onColor, offColor, cols, value, i, j);
				}
				canvas.drawCircle(x - cpx - dr, y - cpy - dr, dr, mPaint);
				x -= cw;
			}
			x = bounds.right;
			y -= ch;
		}
	}

	private void setColor(int onColor, int offColor, int cols, int value, int row, int col) {
		if ((value >> ((row * cols) + col) & 1) == 1) {
			mPaint.setColor(onColor);
		} else {
			mPaint.setColor(offColor);
		}
	}
	private void setMinuteColor(int onColor, int offColor, int cols, int quarterHours, int remMinutes, int row, int col) {
		if (col == cols -1){
			// separate grid 1 column wide, zeroth column
			setColor(onColor, offColor, 1, quarterHours, row, 0);
		}
		else {
			setColor(onColor, offColor, cols -1, remMinutes, row, col);
		}
	}

	private int px(int dp) {
		return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
			ClockWidgetApplication.getContext().getResources().getDisplayMetrics()));
	}
}
