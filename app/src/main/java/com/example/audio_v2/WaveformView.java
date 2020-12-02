package com.example.audio_v2;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
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
    /* Axis */
    private float TenthSec = (float) 53.325;
    private ArrayList<Integer> xAxisMinor, xAxisMajor;
    private Paint AxisPaint;
    /* Waveform */
    private ArrayList<Float> amplitudes;
    private float power;
    private Integer hop_length = 128;
    private Integer samplingRate = 16000;
    private int SkipSample = 30; // Resolution for display
    private float scaledHeight;
    private int middle;
    private Paint linePaint; // waveform line characteristics
    /* Beats */
    private ArrayList<Integer> Beats;
    private float SampleIdx;
    private int QuadShift = 0;
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
        /** Axis */
        AxisPaint = new Paint ();
        AxisPaint.setColor(Color.WHITE);
        AxisPaint.setStrokeWidth(5);
        AxisPaint.setTextSize(20);
        xAxisMinor = initAxisMinor();
        xAxisMajor = initAxisMajor();

        /** Waveform */
        linePaint = new Paint ();
        linePaint.setColor(Color.GREEN);
        linePaint.setStrokeWidth(1);
        amplitudes = initAmplitudes();

        /** Beats */
        /* Major Beats */
        beatPaint = new Paint ();
        beatPaint.setColor(Color.RED);
        beatPaint.setStrokeWidth(4);
        Beats = initBeats();
        /* Beats */
        MinorBeatsPaint = new Paint ();
        MinorBeatsPaint.setColor(Color.WHITE);
        MinorBeatsPaint.setStrokeWidth(2);
    }
    public ArrayList<Integer> initAxisMinor(){
        xAxisMinor = new ArrayList<>();
        for (Integer idx=0; idx<50000/TenthSec; idx++){
            xAxisMinor.add( (int) (idx*TenthSec) );
        }
        return xAxisMinor;
    }
    public ArrayList<Integer> initAxisMajor(){
        xAxisMajor = new ArrayList<>();
        for (Integer idx=0; idx<50000/(TenthSec*10); idx++){
            xAxisMajor.add( (int) (idx*TenthSec*10) );
        }
        return xAxisMajor;
    }
    public ArrayList<Float> initAmplitudes(){
        amplitudes = new ArrayList<>();
        for (Integer idx=0; idx<2*samplingRate; idx++){ // viewWidth
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
        /** Background color */
        canvas.drawColor(Color.BLACK);


        /** Plot Axis */
        // Horizontal
        canvas.drawLine(
                0, (float) viewHeight*9/10,
                viewWidth, (float) viewHeight*9/10,
                AxisPaint);
        // Minor Verticals & Duration
        int AxisSeconds=0;
        for (int currAxis : xAxisMinor){
            canvas.drawLine(
                    currAxis, (float) viewHeight*9/10,
                    currAxis, (float) viewHeight*925/1000,
                    AxisPaint);
            if (AxisSeconds>0) {
                canvas.drawText(
                        String.format("%.1f", AxisSeconds*0.1),
                        currAxis-10,
                        viewHeight*975/1000, AxisPaint);
            }
            AxisSeconds++;
        }
        // Major Verticals
        for (int currAxis : xAxisMajor){
            canvas.drawLine(
                    currAxis, (float) viewHeight*9/10,
                    currAxis, (float) viewHeight*95/100,
                    AxisPaint);
        }

        /** Plot Waveform */
        middle = viewHeight/2; //middle of view
        for (int idx=0; idx<amplitudes.size(); idx+=SkipSample ) {
            power = amplitudes.get(idx);
            scaledHeight = power/(2*LINE_SCALE); //scale the power
            // draw a line representing this item in the amplitudes ArrayList
            canvas.drawLine(
                    idx/SkipSample, middle + scaledHeight,
                    idx/SkipSample, middle - scaledHeight,
                    linePaint);
        }
        /* Determine the end of the waveform */
//        for (int idx=amplitudes.size()/SkipSample; idx<amplitudes.size()/SkipSample+100; idx++) {
//            canvas.drawLine(
//                    idx, 0,
//                    idx, viewHeight*9/10,
//                    linePaint);
//        }

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
                    SampleIdx, viewHeight*9/10,
                    beatPaint);

            /* draw Minor beats */
            for (int jdx=1; jdx<4; jdx++){
                canvas.drawLine(
                        SampleIdx + jdx*QuadShift, 0,
                        SampleIdx + jdx*QuadShift, viewHeight*9/10,
                        MinorBeatsPaint);
            }
        }
    }
    /* Expand the canvas to desired width.
    MUST be included otherwise one or both scroll views will be compressed to zero pixels
     and the scrollview will then be invisible */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = 20000; // To cater to a song that is as long as 20000/16000 seconds
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
