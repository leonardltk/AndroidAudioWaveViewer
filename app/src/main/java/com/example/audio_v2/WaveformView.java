package com.example.audio_v2;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.Nullable;
import java.util.ArrayList;

public class WaveformView extends View {
    /* Custom Canvas */
    // https://stackoverflow.com/questions/45162798/audio-visualization-android-code-with-the-moving-line
    // https://www.youtube.com/watch?v=sb9OEl4k9Dk
    /* Scrolling Canvas */
    // https://softwarehealthclub.wordpress.com/programming/android-development/how-to-add-a-scrolling-canvas-using-scrollview/
    // https://stackoverflow.com/questions/6674341/how-to-use-scrollview-in-android

    /** Init Variables */
    /* Canvas setup */
    private static final int LINE_WIDTH = 1; //width of visualiser lines, default=5
    private static final int LINE_SCALE = 150; //scales visualiser lines, default=150
    private int viewWidth;
    private int viewHeight;
    /* Waveform */
    private ArrayList<Float> amplitudes;
    Integer hop_length = 128;
    Integer samplingRate = 16000;
    private Paint linePaint; // waveform line characteristics
    float scaledHeight;
    private int SkipSample = 30; // Resolution for display
    /* Beats */
    ArrayList<Integer> Beats;
    float SampleIdx;
    int QuadShift = 0;
    private Paint beatPaint; // beats line characteristics
    private Paint MinorBeatsPaint; // For the in betweens beats. Default to quads
    /* debug */
    private Paint debugPaint;

    /** Constructors */
    public WaveformView(Context context) {
        super(context);
        init(null);
    }
    public WaveformView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }
    public void init(@Nullable AttributeSet set){
        /* Waveform */
        linePaint = new Paint ();
        linePaint.setColor(Color.GREEN);
        linePaint.setStrokeWidth(LINE_WIDTH);
        amplitudes = initAmplitudes();

        /* Beats */
        beatPaint = new Paint ();
        beatPaint.setColor(Color.RED);
        beatPaint.setStrokeWidth(5);
        Beats = initBeats();

        /* debug */
        MinorBeatsPaint = new Paint ();
        MinorBeatsPaint.setColor(Color.WHITE);
        MinorBeatsPaint.setStrokeWidth(5);
    }
    public ArrayList<Float> initAmplitudes(){
        amplitudes = new ArrayList<>();
        for (Integer idx=0; idx<1024; idx++){ // viewWidth
            amplitudes.add( (float) LINE_SCALE*5 );
        }
        return amplitudes;
    }
    public ArrayList<Integer> initBeats(){
        Beats = new ArrayList<>();
        /*for (Integer idx=0; idx<20; idx++){
            Beats.add( idx );
        }*/
        Beats.add( 0 );
        return Beats;
    }

    /** Overides */
    @Override
    public void onSizeChanged (int w, int h, int oldW, int newH){
        /* when dimensions of view changes */
        viewWidth = w;
        viewHeight = h;
    }
    @Override
    public void onDraw (Canvas canvas){
        super.onDraw(canvas);

        /* bg color */
        canvas.drawColor(Color.BLACK);

        /** Plot Waveform */
        int middle = viewHeight/2; //middle of view
        int curX = 0;
        int npts = -1;
        for (float power : amplitudes) {
            npts++;
            if (npts%SkipSample > 0){
                continue;
            }

            scaledHeight = power/LINE_SCALE; //scale the power
            curX += LINE_WIDTH ; //increase by line width

            // draw a line representing this item in the amplitudes ArrayList
            canvas.drawLine(
                    curX, middle + scaledHeight/2,
                    curX, middle - scaledHeight/2,
                    linePaint);
        }
        /* Determine the end of the waveform */
        for (int idx=0; idx<100; idx++) {
            curX += LINE_WIDTH ; //increase by line width
            canvas.drawLine(
                    curX, 0,
                    curX, viewHeight,
                    linePaint);
        }

        /** Plot Beats */
        if (Beats.size()>1){
            QuadShift = (Beats.get(1) - Beats.get(0))*hop_length/4;
            QuadShift /= SkipSample;
        }
        for (float FrameIdx : Beats){
            SampleIdx = FrameIdx*hop_length/SkipSample;

            /* draw Major beats */
            canvas.drawLine(
                    SampleIdx, 0,
                    SampleIdx, viewHeight,
                    beatPaint);

            /* draw Minor beats */
            for (int jdx=1; jdx<4; jdx++){
                canvas.drawLine(
                        SampleIdx + jdx*QuadShift, 0,
                        SampleIdx + jdx*QuadShift, viewHeight,
                        MinorBeatsPaint);
            }
        }
    }
    /* Expand the canvas to desired width.
    MUST be included otherwise one or both scroll views will be compressed to zero pixels
     and the scrollview will then be invisible */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = 50000; // To cater to a song that is as long as 50000/16000 seconds
        int height = heightMeasureSpec;
        setMeasuredDimension(width, height);
    }

    /** Waveform */
    public void drawAmplitudes(ArrayList<Float> amplitudesIN){
        amplitudes = amplitudesIN;
        postInvalidate();
    }
    public void clearAmplitudes(){
        amplitudes.clear();
    }

    /** Beats */
    public void drawBeats(ArrayList<Integer> BeatsIN){
        Beats = BeatsIN;
        postInvalidate();
    }
    public void clearBeats(){
        Beats.clear();
    }
}
