package com.softaai.androidassignmentweel;

import android.animation.Animator;
import android.animation.PropertyValuesHolder;
import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.os.Build;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;

import androidx.annotation.ColorInt;
import androidx.annotation.Dimension;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@SuppressWarnings("FieldCanBeLocal")
public class CircularProgress extends View {

    public static final int DIRECTION_CLOCKWISE = 0;
    public static final int DIRECTION_COUNTERCLOCKWISE = 1;

    public static final int CAP_ROUND = 0;
    public static final int CAP_BUTT = 1;

    public static final int NO_GRADIENT = 0;
    public static final int LINEAR_GRADIENT = 1;
    public static final int RADIAL_GRADIENT = 2;
    public static final int SWEEP_GRADIENT = 3;

    private static final int DEFAULT_PROGRESS_START_ANGLE = 270;
    private static final int ANGLE_START_PROGRESS_BACKGROUND = 0;
    private static final int ANGLE_END_PROGRESS_BACKGROUND = 360;

    private static final int DESIRED_WIDTH_DP = 150;

    private static final String DEFAULT_PROGRESS_COLOR = "#37CCA9";
    private static final int DEFAULT_TEXT_SIZE_SP = 24;
    private static final int DEFAULT_STROKE_WIDTH_DP = 8;
    private static final String DEFAULT_PROGRESS_BACKGROUND_COLOR = "#e0e0e0";

    private static final int DEFAULT_ANIMATION_DURATION = 1_000;

    private static final String PROPERTY_ANGLE = "angle";


    private Paint progressPaint;
    private Paint progressBackgroundPaint;
    private Paint dotPaint;
    private Paint textPaint;

    private int startAngle = DEFAULT_PROGRESS_START_ANGLE;
    private int sweepAngle = 0;

    private RectF circleBounds;

    private float radius;

    private boolean shouldDrawDot;

    private double maxProgressValue = 100.0;
    private double progressValue = 0.0;

    private boolean isAnimationEnabled;

    private boolean isFillBackgroundEnabled;

    //    @Direction
    private int direction = DIRECTION_CLOCKWISE;

    private ValueAnimator progressAnimator;

    @NonNull
    private Interpolator animationInterpolator = new AccelerateDecelerateInterpolator();

    public CircularProgress(Context context) {
        super(context);
        init(context, null);
    }

    public CircularProgress(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public CircularProgress(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public CircularProgress(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }

    private void init(@NonNull Context context, @Nullable AttributeSet attrs) {

        int progressColor = Color.parseColor(DEFAULT_PROGRESS_COLOR);
        int progressBackgroundColor = Color.parseColor(DEFAULT_PROGRESS_BACKGROUND_COLOR);
        int progressStrokeWidth = dp2px(DEFAULT_STROKE_WIDTH_DP);
        int progressBackgroundStrokeWidth = progressStrokeWidth;
        int textColor = progressColor;
        int textSize = sp2px(DEFAULT_TEXT_SIZE_SP);

        shouldDrawDot = true;
        int dotColor = progressColor;
        int dotWidth = progressStrokeWidth;

        Paint.Cap progressStrokeCap = Paint.Cap.ROUND;

        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CircularProgress);

            progressColor = a.getColor(R.styleable.CircularProgress_progressColor, progressColor);
            progressBackgroundColor = a.getColor(R.styleable.CircularProgress_progressBackgroundColor, progressBackgroundColor);
            progressStrokeWidth = a.getDimensionPixelSize(R.styleable.CircularProgress_progressStrokeWidth, progressStrokeWidth);
            progressBackgroundStrokeWidth = a.getDimensionPixelSize(R.styleable.CircularProgress_progressBackgroundStrokeWidth, progressStrokeWidth);
            textColor = a.getColor(R.styleable.CircularProgress_textColor, progressColor);
            textSize = a.getDimensionPixelSize(R.styleable.CircularProgress_textSize, textSize);

            shouldDrawDot = a.getBoolean(R.styleable.CircularProgress_drawDot, true);
            dotColor = a.getColor(R.styleable.CircularProgress_dotColor, progressColor);
            dotWidth = a.getDimensionPixelSize(R.styleable.CircularProgress_dotWidth, progressStrokeWidth);

            startAngle = a.getInt(R.styleable.CircularProgress_startAngle, DEFAULT_PROGRESS_START_ANGLE);
            if (startAngle < 0 || startAngle > 360) {
                startAngle = DEFAULT_PROGRESS_START_ANGLE;
            }

            isAnimationEnabled = a.getBoolean(R.styleable.CircularProgress_enableProgressAnimation, true);
            isFillBackgroundEnabled = a.getBoolean(R.styleable.CircularProgress_fillBackground, false);

            direction = a.getInt(R.styleable.CircularProgress_direction, DIRECTION_COUNTERCLOCKWISE);

            int cap = a.getInt(R.styleable.CircularProgress_progressCap, CAP_ROUND);
            progressStrokeCap = (cap == CAP_ROUND) ? Paint.Cap.ROUND : Paint.Cap.BUTT;

            final int gradientType = a.getColor(R.styleable.CircularProgress_gradientType, 0);
            if (gradientType != NO_GRADIENT) {
                final int gradientColorEnd = a.getColor(R.styleable.CircularProgress_gradientEndColor, -1);

                if (gradientColorEnd == -1) {
                    throw new IllegalArgumentException("did you forget to specify gradientColorEnd?");
                }

                post(new Runnable() {
                    @Override
                    public void run() {
                        setGradient(gradientType, gradientColorEnd);
                    }
                });
            }

            a.recycle();
        }

        progressPaint = new Paint();
        progressPaint.setStrokeCap(progressStrokeCap);
        progressPaint.setStrokeWidth(progressStrokeWidth);
        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setColor(progressColor);
        progressPaint.setAntiAlias(true);

        Paint.Style progressBackgroundStyle = isFillBackgroundEnabled ? Paint.Style.FILL_AND_STROKE : Paint.Style.STROKE;
        progressBackgroundPaint = new Paint();
        progressBackgroundPaint.setStyle(progressBackgroundStyle);
        progressBackgroundPaint.setStrokeWidth(progressBackgroundStrokeWidth);
        progressBackgroundPaint.setColor(progressBackgroundColor);
        progressBackgroundPaint.setAntiAlias(true);

        dotPaint = new Paint();
        dotPaint.setStrokeCap(Paint.Cap.ROUND);
        dotPaint.setStrokeWidth(dotWidth);
        dotPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        dotPaint.setColor(dotColor);
        dotPaint.setAntiAlias(true);

        textPaint = new TextPaint();
        textPaint.setStrokeCap(Paint.Cap.ROUND);
        textPaint.setColor(textColor);
        textPaint.setAntiAlias(true);
        textPaint.setTextSize(textSize);
        textPaint.setColor(Color.parseColor("#ffffff"));

        circleBounds = new RectF();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int paddingLeft = getPaddingLeft();
        int paddingRight = getPaddingRight();
        int paddingTop = getPaddingTop();
        int paddingBottom = getPaddingBottom();

        int measuredWidth = MeasureSpec.getSize(widthMeasureSpec);
        int measuredHeight = MeasureSpec.getSize(heightMeasureSpec);

        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);

        Rect textBoundsRect = new Rect();
        //  textPaint.getTextBounds(progressText, 0, progressText.length(), textBoundsRect);


        float dotWidth = dotPaint.getStrokeWidth();
        float progressWidth = progressPaint.getStrokeWidth();
        float progressBackgroundWidth = progressBackgroundPaint.getStrokeWidth();
        float strokeSizeOffset = (shouldDrawDot) ? Math.max(dotWidth, Math.max(progressWidth, progressBackgroundWidth)) : Math.max(progressWidth, progressBackgroundWidth);

        int desiredSize = ((int) strokeSizeOffset) + dp2px(DESIRED_WIDTH_DP) +
                Math.max(paddingBottom + paddingTop, paddingLeft + paddingRight);

        // multiply by .1f to have an extra space for small padding between text and circle
        desiredSize += Math.max(textBoundsRect.width(), textBoundsRect.height()) + desiredSize * .1f;

        int finalWidth;
        switch (widthMode) {
            case MeasureSpec.EXACTLY:
                finalWidth = measuredWidth;
                break;
            case MeasureSpec.AT_MOST:
                finalWidth = Math.min(desiredSize, measuredWidth);
                break;
            default:
                finalWidth = desiredSize;
                break;
        }

        int finalHeight;
        switch (heightMode) {
            case MeasureSpec.EXACTLY:
                finalHeight = measuredHeight;
                break;
            case MeasureSpec.AT_MOST:
                finalHeight = Math.min(desiredSize, measuredHeight);
                break;
            default:
                finalHeight = desiredSize;
                break;
        }

        int widthWithoutPadding = finalWidth - paddingLeft - paddingRight;
        int heightWithoutPadding = finalHeight - paddingTop - paddingBottom;

        int smallestSide = Math.min(heightWithoutPadding, widthWithoutPadding);
        setMeasuredDimension(smallestSide, smallestSide);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        calculateBounds(w, h);

        Shader shader = progressPaint.getShader();
        if (shader instanceof RadialGradient) {
            RadialGradient gradient = (RadialGradient) shader;
        }
    }

    private void calculateBounds(int w, int h) {
        radius = w / 2f;

        float dotWidth = dotPaint.getStrokeWidth();
        float progressWidth = progressPaint.getStrokeWidth();
        float progressBackgroundWidth = progressBackgroundPaint.getStrokeWidth();
        float strokeSizeOffset = (shouldDrawDot) ? Math.max(dotWidth, Math.max(progressWidth, progressBackgroundWidth)) : Math.max(progressWidth, progressBackgroundWidth); // to prevent progress or dot from drawing over the bounds
        float halfOffset = strokeSizeOffset / 2f;

        circleBounds.left = halfOffset;
        circleBounds.top = halfOffset;
        circleBounds.right = w - halfOffset;
        circleBounds.bottom = h - halfOffset;

        radius = circleBounds.width() / 2f;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        if (progressAnimator != null) {
            progressAnimator.cancel();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        drawProgressBackground(canvas);
        drawProgress(canvas);
        if (shouldDrawDot) drawDot(canvas);
    }

    private void drawProgressBackground(Canvas canvas) {
        canvas.drawArc(circleBounds, ANGLE_START_PROGRESS_BACKGROUND, ANGLE_END_PROGRESS_BACKGROUND,
                false, progressBackgroundPaint);
    }

    private void drawProgress(Canvas canvas) {
        canvas.drawArc(circleBounds, startAngle, sweepAngle, false, progressPaint);
    }

    private void drawDot(Canvas canvas) {
        double angleRadians = Math.toRadians(startAngle + sweepAngle + 180);
        float cos = (float) Math.cos(angleRadians);
        float sin = (float) Math.sin(angleRadians);
        float x = circleBounds.centerX() - radius * cos;
        float y = circleBounds.centerY() - radius * sin;

        canvas.drawPoint(x, y, dotPaint);
    }

    public void setMaxProgress(double maxProgress) {
        maxProgressValue = maxProgress;
        if (maxProgressValue < progressValue) {
            setCurrentProgress(maxProgress);
        }
        invalidate();
    }

    public void setCurrentProgress(double currentProgress) {
        if (currentProgress > maxProgressValue) {
            maxProgressValue = currentProgress;
        }

        setProgress(currentProgress, maxProgressValue);
    }

    public void setProgress(double current, double max) {
        final double finalAngle;

        if (direction == DIRECTION_COUNTERCLOCKWISE) {
            finalAngle = -(current / max * 360);
        } else {
            finalAngle = current / max * 360;
        }

        double oldCurrentProgress = progressValue;

        maxProgressValue = max;
        progressValue = Math.min(current, max);

        stopProgressAnimation();

        if (isAnimationEnabled) {
            startProgressAnimation(oldCurrentProgress, finalAngle);
        } else {
            sweepAngle = (int) finalAngle;
            invalidate();
        }
    }

    private void startProgressAnimation(double oldCurrentProgress, final double finalAngle) {
        final PropertyValuesHolder angleProperty = PropertyValuesHolder.ofInt(PROPERTY_ANGLE, sweepAngle, (int) finalAngle);

        progressAnimator = ValueAnimator.ofObject(new TypeEvaluator<Double>() {
            @Override
            public Double evaluate(float fraction, Double startValue, Double endValue) {
                return (startValue + (endValue - startValue) * fraction);
            }
        }, oldCurrentProgress, progressValue);
        progressAnimator.setDuration(DEFAULT_ANIMATION_DURATION);
        progressAnimator.setValues(angleProperty);
        progressAnimator.setInterpolator(animationInterpolator);
        progressAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                sweepAngle = (int) animation.getAnimatedValue(PROPERTY_ANGLE);
                invalidate();
            }
        });
        progressAnimator.addListener(new DefaultAnimatorListener() {
            @Override
            public void onAnimationCancel(Animator animation) {
                sweepAngle = (int) finalAngle;
                invalidate();
                progressAnimator = null;
            }
        });
        progressAnimator.start();
    }

    private void stopProgressAnimation() {
        if (progressAnimator != null) {
            progressAnimator.cancel();
        }
    }

    private int dp2px(float dp) {
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, metrics);
    }

    private int sp2px(float sp) {
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, metrics);
    }

    // calculates circle bounds, view size and requests invalidation
    private void invalidateEverything() {
        calculateBounds(getWidth(), getHeight());
        requestLayout();
        invalidate();
    }

    public void setShouldDrawDot(boolean shouldDrawDot) {
        this.shouldDrawDot = shouldDrawDot;

        if (dotPaint.getStrokeWidth() > progressPaint.getStrokeWidth()) {
            requestLayout();
            return;
        }

        invalidate();
    }

    public void setDotColor(@ColorInt int color) {
        dotPaint.setColor(color);

        invalidate();
    }

    @ColorInt
    public int getDotColor() {
        return dotPaint.getColor();
    }


    public void setDotWidthDp(@Dimension int width) {
        setDotWidthPx(dp2px(width));
    }

    public void setDotWidthPx(@Dimension int width) {
        dotPaint.setStrokeWidth(width);

        invalidateEverything();
    }

    public void setAnimationEnabled(boolean enableAnimation) {
        isAnimationEnabled = enableAnimation;

        if (!enableAnimation) stopProgressAnimation();
    }

    public void setFillBackgroundEnabled(boolean fillBackgroundEnabled) {
        if (fillBackgroundEnabled == isFillBackgroundEnabled) return;

        isFillBackgroundEnabled = fillBackgroundEnabled;

        Paint.Style style = fillBackgroundEnabled ? Paint.Style.FILL_AND_STROKE : Paint.Style.STROKE;
        progressBackgroundPaint.setStyle(style);

        invalidate();
    }

    public void setGradient(@GradientType int type, @ColorInt int endColor) {
        Shader gradient = null;

        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;

        int startColor = progressPaint.getColor();

        switch (type) {
            case LINEAR_GRADIENT:
                gradient = new LinearGradient(0f, 0f, getWidth(), getHeight(), startColor, endColor, Shader.TileMode.CLAMP);
                break;
            case RADIAL_GRADIENT:
                gradient = new RadialGradient(cx, cy, cx, startColor, endColor, Shader.TileMode.MIRROR);
                break;
            case SWEEP_GRADIENT:
                gradient = new SweepGradient(cx, cy, new int[]{startColor, endColor}, null);
                break;
        }

        if (gradient != null) {
            Matrix matrix = new Matrix();
            matrix.postRotate(startAngle, cx, cy);
            gradient.setLocalMatrix(matrix);
        }

        progressPaint.setShader(gradient);

        invalidate();
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({NO_GRADIENT, LINEAR_GRADIENT, RADIAL_GRADIENT, SWEEP_GRADIENT})
    public @interface GradientType {
    }

}
