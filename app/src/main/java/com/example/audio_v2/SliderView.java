package com.example.audio_v2;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.graphics.Path;
import androidx.annotation.Nullable;

public class SliderView extends View {
    // https://stackoverflow.com/questions/5367950/android-drawing-an-animated-line
    // https://developer.android.com/codelabs/advanced-android-kotlin-training-property-animation#4
    // https://medium.com/@quiro91/custom-view-mastering-onmeasure-a0a0bb11784d

    Path path;
    Paint paint;
    float length;
    private int samplingRate = 16000;
    private int SkipSample = 30; // Resolution for display

    public SliderView(Context context){
        super(context);
        init(null);
    }
    public SliderView(Context context, AttributeSet attrs){
        super(context, attrs);
        init(attrs);
    }
    public SliderView(Context context, AttributeSet attrs, int defStyleAttr){
        super(context, attrs, defStyleAttr);
        init(attrs);
    }
    public void init(@Nullable AttributeSet set) {
        paint = new Paint();
        paint.setColor(Color.WHITE);
        paint.setStrokeWidth(10);
        paint.setStyle(Paint.Style.STROKE);

        path = new Path();
        path.moveTo(0, 0);
        path.lineTo(0, 1200);
    }

    @Override
    public void onDraw(Canvas c){
        super.onDraw(c);

        /* bg color */
        c.drawColor(Color.TRANSPARENT);

        c.drawPath(path, paint);
    }
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = 50000; // To cater to a song that is as long as 50000/16000 seconds
        int height = heightMeasureSpec; // heightMeasureSpec MeasureSpec.getSize(heightMeasureSpec)
        setMeasuredDimension(width, height);
    }

    public void Draw1(int NumSamples){
        ObjectAnimator animator = ObjectAnimator.ofFloat(
                SliderView.this, "translationX",
                0, NumSamples/SkipSample);
        animator.setDuration(NumSamples*1000/samplingRate);
        animator.start();

        postInvalidate();
    }
}
