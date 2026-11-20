package com.databurner.ui.components;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;

public class SpeedometerGaugeView extends View {

    private Paint trackPaint;
    private Paint progressPaint;
    private Paint tickPaint;
    private Paint activeTickPaint;
    private Paint dotPaint;
    private Paint dotGlowPaint;
    private Paint textSpeedPaint;
    private Paint textUnitPaint;

    private final RectF arcBounds = new RectF();
    private float currentSpeed = 0f;
    private float targetSpeed = 0f;

    // Smart Dynamic Max Speed Scaling
    private float currentMaxSpeed = 1.22f;
    private float targetMaxSpeed = 1.22f;
    private float lastSessionPeakSpeed = 0f;
    private boolean scaleInitialized = false;
    private float strokeWidth = 0f;
    private int currentGradientTier = -1;

    private static final float START_ANGLE = 150f;
    private static final float SWEEP_ANGLE = 240f;
    private static final float MAX_FILL_CAP = 0.82f;

    public SpeedometerGaugeView(Context context) {
        super(context);
        init();
    }

    public SpeedometerGaugeView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SpeedometerGaugeView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        trackPaint.setStyle(Paint.Style.STROKE);
        trackPaint.setStrokeCap(Paint.Cap.ROUND);
        trackPaint.setColor(Color.parseColor("#1C2234"));

        progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeCap(Paint.Cap.ROUND);

        tickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tickPaint.setColor(Color.parseColor("#323B54"));
        tickPaint.setStrokeWidth(3.5f);
        tickPaint.setStrokeCap(Paint.Cap.ROUND);

        activeTickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        activeTickPaint.setColor(Color.parseColor("#34D399"));
        activeTickPaint.setStrokeWidth(4.5f);
        activeTickPaint.setStrokeCap(Paint.Cap.ROUND);

        dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dotPaint.setColor(Color.WHITE);
        dotPaint.setStyle(Paint.Style.FILL);

        dotGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dotGlowPaint.setColor(Color.parseColor("#34D399"));
        dotGlowPaint.setStyle(Paint.Style.FILL);
        dotGlowPaint.setAlpha(150);

        textSpeedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textSpeedPaint.setColor(Color.parseColor("#34D399"));
        textSpeedPaint.setTextAlign(Paint.Align.CENTER);
        textSpeedPaint.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));

        textUnitPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textUnitPaint.setColor(Color.parseColor("#9CA3AF"));
        textUnitPaint.setTextAlign(Paint.Align.CENTER);
        textUnitPaint.setLetterSpacing(0.12f);
        textUnitPaint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL));
    }

    /**
     * Updates the gauge using the current speed and the highest speed recorded
     * during the active burning session. The session peak defines the scale,
     * while MAX_FILL_CAP keeps the arc from ever reaching 100%.
     */
    public void setSpeed(float speedMB, float sessionPeakSpeedMB) {
        this.targetSpeed = Math.max(0f, speedMB);

        float safePeak = Math.max(0f, sessionPeakSpeedMB);
        float newMaxSpeed = Math.max(1f, safePeak) / MAX_FILL_CAP;

        // A lower peak means a new session has started and its record was reset.
        if (scaleInitialized && safePeak + 0.001f < lastSessionPeakSpeed) {
            currentSpeed = targetSpeed;
            currentMaxSpeed = newMaxSpeed;
        }

        if (!scaleInitialized) {
            currentMaxSpeed = newMaxSpeed;
            scaleInitialized = true;
        }

        this.targetMaxSpeed = newMaxSpeed;
        this.lastSessionPeakSpeed = safePeak;
        postInvalidateOnAnimation();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        strokeWidth = Math.min(w, h) * 0.052f;
        trackPaint.setStrokeWidth(strokeWidth);
        progressPaint.setStrokeWidth(strokeWidth);

        float diameter = Math.min(w, h) - strokeWidth * 3.2f;
        float left = (w - diameter) / 2f;
        float top = (h - diameter) / 2f - h * 0.03f;

        arcBounds.set(left, top, left + diameter, top + diameter);

        textSpeedPaint.setTextSize(diameter * 0.28f);
        textUnitPaint.setTextSize(diameter * 0.058f);

        updateDynamicGradient(currentSpeed, true);
    }

    private void updateDynamicGradient(float speed, boolean force) {
        int tier;
        if (speed >= 50f) {
            tier = 3; // Ultra Fiery Flame
        } else if (speed >= 15f) {
            tier = 2; // High Speed Emerald Cyan
        } else {
            tier = 1; // Normal Cool Blue
        }

        if (tier != currentGradientTier || force) {
            currentGradientTier = tier;
            int[] colors;
            int glowColor;

            if (tier == 3) {
                colors = new int[]{Color.parseColor("#FF385C"), Color.parseColor("#FFB800"), Color.parseColor("#34D399")};
                glowColor = Color.parseColor("#FF385C");
            } else if (tier == 2) {
                colors = new int[]{Color.parseColor("#34D399"), Color.parseColor("#00F2FE"), Color.parseColor("#3B82F6")};
                glowColor = Color.parseColor("#34D399");
            } else {
                colors = new int[]{Color.parseColor("#06B6D4"), Color.parseColor("#3B82F6"), Color.parseColor("#6366F1")};
                glowColor = Color.parseColor("#06B6D4");
            }

            if (arcBounds.width() > 0) {
                LinearGradient gradient = new LinearGradient(
                        arcBounds.left, arcBounds.bottom,
                        arcBounds.right, arcBounds.top,
                        colors,
                        new float[]{0f, 0.5f, 1f},
                        Shader.TileMode.CLAMP
                );
                progressPaint.setShader(gradient);
            }

            dotGlowPaint.setColor(glowColor);
            activeTickPaint.setColor(glowColor);
            textSpeedPaint.setColor(glowColor);
        }
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        boolean animating = false;

        // Smooth speed interpolation
        if (Math.abs(currentSpeed - targetSpeed) > 0.01f) {
            currentSpeed += (targetSpeed - currentSpeed) * 0.15f;
            animating = true;
        } else {
            currentSpeed = targetSpeed;
        }

        // Smooth max speed scale interpolation
        if (Math.abs(currentMaxSpeed - targetMaxSpeed) > 0.1f) {
            currentMaxSpeed += (targetMaxSpeed - currentMaxSpeed) * 0.08f;
            animating = true;
        } else {
            currentMaxSpeed = targetMaxSpeed;
        }

        updateDynamicGradient(currentSpeed, false);

        if (animating) {
            postInvalidateOnAnimation();
        }

        float width = getWidth();
        float centerX = width / 2f;
        float centerY = arcBounds.centerY();
        float radius = arcBounds.width() / 2f;

        // 1. Background Arc Track
        canvas.drawArc(arcBounds, START_ANGLE, SWEEP_ANGLE, false, trackPaint);

        // 2. Speed Ticks (7 ticks around gauge)
        int numTicks = 7;
        for (int i = 0; i < numTicks; i++) {
            float frac = (float) i / (numTicks - 1);
            float angleDeg = START_ANGLE + frac * SWEEP_ANGLE;
            double angleRad = Math.toRadians(angleDeg);

            float innerR = radius - strokeWidth * 1.5f;
            float outerR = radius - strokeWidth * 0.8f;

            float x1 = (float) (centerX + innerR * Math.cos(angleRad));
            float y1 = (float) (centerY + innerR * Math.sin(angleRad));
            float x2 = (float) (centerX + outerR * Math.cos(angleRad));
            float y2 = (float) (centerY + outerR * Math.sin(angleRad));

            boolean isActive = (frac * currentMaxSpeed) <= currentSpeed && currentSpeed > 0f;
            canvas.drawLine(x1, y1, x2, y2, isActive ? activeTickPaint : tickPaint);
        }

        // 3. Active Speed Arc Sweep
        float rawRatio = currentMaxSpeed > 0 ? (currentSpeed / currentMaxSpeed) : 0f;
        float clampedRatio = Math.min(MAX_FILL_CAP, rawRatio);
        float currentSweep = clampedRatio * SWEEP_ANGLE;

        if (currentSweep > 0.5f) {
            canvas.drawArc(arcBounds, START_ANGLE, currentSweep, false, progressPaint);

            // 4. Glowing Tip Bulb
            float tipAngleDeg = START_ANGLE + currentSweep;
            double tipAngleRad = Math.toRadians(tipAngleDeg);
            float tipX = (float) (centerX + radius * Math.cos(tipAngleRad));
            float tipY = (float) (centerY + radius * Math.sin(tipAngleRad));

            canvas.drawCircle(tipX, tipY, strokeWidth * 0.85f, dotGlowPaint);
            canvas.drawCircle(tipX, tipY, strokeWidth * 0.42f, dotPaint);
        }

        // 5. Center Speed Text
        String speedStr = String.format(Locale.US, "%.2f", currentSpeed);
        float textY = centerY + (textSpeedPaint.getTextSize() * 0.28f);
        canvas.drawText(speedStr, centerX, textY, textSpeedPaint);

        // 6. Unit Label
        float unitY = textY + textUnitPaint.getTextSize() * 1.6f;
        canvas.drawText("MB / SECOND", centerX, unitY, textUnitPaint);
    }
}
