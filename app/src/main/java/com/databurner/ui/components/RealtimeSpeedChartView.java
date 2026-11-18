package com.databurner.ui.components;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RealtimeSpeedChartView extends View {

    private static final int MAX_POINTS = 40;

    private final List<Float> speedHistory = new ArrayList<>();
    private final Path linePath = new Path();
    private final Path fillPath = new Path();

    private Paint linePaint;
    private Paint fillPaint;
    private Paint pointPaint;
    private Paint textPaint;

    private float maxSpeed = 20f;
    private float selectedX = -1f;
    private float selectedY = -1f;
    private String selectedText = "";

    public RealtimeSpeedChartView(Context context) {
        super(context);
        init();
    }

    public RealtimeSpeedChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public RealtimeSpeedChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(4.5f);
        linePaint.setColor(Color.parseColor("#34D399"));
        linePaint.setStrokeCap(Paint.Cap.ROUND);

        fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fillPaint.setStyle(Paint.Style.FILL);

        pointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pointPaint.setColor(Color.parseColor("#06B6D4"));

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(32f);
    }

    public void addSpeedSample(float speedMB) {
        if (speedHistory.size() >= MAX_POINTS) {
            speedHistory.remove(0);
        }
        speedHistory.add(speedMB);

        float currentMax = 20f;
        for (float val : speedHistory) {
            if (val > currentMax) currentMax = val;
        }
        maxSpeed = currentMax * 1.15f;

        postInvalidateOnAnimation();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w > 0 && h > 0) {
            LinearGradient fillGradient = new LinearGradient(
                    0, 0, 0, h,
                    new int[]{Color.parseColor("#3034D399"), Color.parseColor("#0506B6D4"), Color.TRANSPARENT},
                    new float[]{0f, 0.7f, 1f},
                    Shader.TileMode.CLAMP
            );
            fillPaint.setShader(fillGradient);
        }
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        int size = speedHistory.size();
        if (size < 2) return;

        float width = getWidth();
        float height = getHeight();

        linePath.reset();
        fillPath.reset();

        float dx = width / (MAX_POINTS - 1);

        float startX = 0f;
        float startY = height - (speedHistory.get(0) / maxSpeed) * height;

        linePath.moveTo(startX, startY);
        fillPath.moveTo(startX, height);
        fillPath.lineTo(startX, startY);

        for (int i = 1; i < size; i++) {
            float x1 = (i - 1) * dx;
            float y1 = height - (speedHistory.get(i - 1) / maxSpeed) * height;
            float x2 = i * dx;
            float y2 = height - (speedHistory.get(i) / maxSpeed) * height;

            float cx = (x1 + x2) / 2f;
            linePath.cubicTo(cx, y1, cx, y2, x2, y2);
            fillPath.cubicTo(cx, y1, cx, y2, x2, y2);
        }

        float lastX = (size - 1) * dx;
        fillPath.lineTo(lastX, height);
        fillPath.close();

        canvas.drawPath(fillPath, fillPaint);
        canvas.drawPath(linePath, linePaint);

        if (selectedX >= 0 && selectedY >= 0) {
            canvas.drawCircle(selectedX, selectedY, 10f, pointPaint);
            canvas.drawText(selectedText, Math.min(selectedX, width - 180f), Math.max(selectedY - 15f, 40f), textPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE) {
            int size = speedHistory.size();
            if (size > 0) {
                float dx = getWidth() / (MAX_POINTS - 1);
                int index = Math.min(size - 1, Math.max(0, Math.round(event.getX() / dx)));
                selectedX = index * dx;
                selectedY = getHeight() - (speedHistory.get(index) / maxSpeed) * getHeight();
                selectedText = String.format(Locale.US, "%.2f MB/s", speedHistory.get(index));
                invalidate();
                return true;
            }
        } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
            selectedX = -1f;
            selectedY = -1f;
            invalidate();
            return true;
        }
        return super.onTouchEvent(event);
    }
}
