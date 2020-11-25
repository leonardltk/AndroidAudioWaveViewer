package com.example.audio_v2;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import android.graphics.Canvas;

import com.jjoe64.graphview.DefaultLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.LineGraphSeries;

import net.galmiza.android.engine.sound.SoundEngine;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

public class MainActivity extends AppCompatActivity {
    /** Variables */
    /* Template song */
    MediaPlayer mySong;
    /* Attributes */
    private SoundEngine nativeLib;
    private Menu menu;
    private Canvas canvas;
    private int samplingRate = 16000;
    private int fftResolution = 512;
    /* Attributes added by LL */
    private int hop_length = 128;
    /* Constant */
    static final float PI = (float) Math.PI;
    /* Buffers */
    private List<short[]> bufferStack; // Store trunks of buffers
    private short[] fftBuffer; // buffer supporting the fft process
    private float[] re; // buffer holding real part during fft process
    private float[] im; // buffer holding imaginary part during fft process

    /* Plot Display */
    GraphView graph;
    /* Setup :  graph axis, scrollable  */
    public void AddDetail(GraphView graph){

        /** Axis Formats */
        /*graph.getGridLabelRenderer().setLabelFormatter(new DefaultLabelFormatter() {
            @Override
            public String formatLabel(double value, boolean isValueX) {
                if (isValueX) { // Plot for x
                    // show normal x values
                    return super.formatLabel(value/16000.0, isValueX) + "s";
                } else { // Dont plot for y
                    // show currency for y values
                    return super.formatLabel(value, isValueX);
                }
            }
        });*/

        /** Set Scrolling */
        // activate zooming
        graph.getViewport().setScalable(true);
        graph.getViewport().setScalableY(true);
        // activate scrolling
        graph.getViewport().setScrollable(true);
        graph.getViewport().setScrollableY(true);
    }


    /** ------ When App start, run these by default ------ */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mySong = MediaPlayer.create(MainActivity.this, R.raw.test);

        // JNI interface
        nativeLib = new SoundEngine();
        nativeLib.initFSin();

        /* Buttons */
        graph = (GraphView) findViewById(R.id.graph);
    }
    /** When pause the application, stop the song. */
    @Override
    protected void onPause() {
        super.onPause();
        mySong.release();
    }


    /** ------ Play/Pause Buttons. ------ */
    /* Play button. */
    public void playIT(View v){
        mySong.start();
    }
    /* Stop button. */
    public void stopIT(View v){
        mySong.release();
        mySong = MediaPlayer.create(MainActivity.this, R.raw.test);
    }


    /** ------ Helper for Reading Audio ------ */
    /* Converting bytes to int */
    public long getLE2(byte[] buffer) {
        long val = buffer[1] & 0xFF;
        val = (val << 8) + (buffer[0] & 0xFF);
        return val;
    }
    public long getLE4(byte[] buffer) {
        long val = buffer[3] & 0xFF;
        val = (val << 8) + (buffer[2] & 0xFF);
        val = (val << 8) + (buffer[1] & 0xFF);
        val = (val << 8) + (buffer[0] & 0xFF);
        return val;
    }
    /* Reading Audio */
    public int readAudio_numSamples(int rawID) {
        long numSamples = 0;
        try {
            InputStream inputStream = this.getResources().openRawResource(rawID);

            /** Header Details */
            /* Get other info */
            byte[] bytes_others0 = new byte[4];
            inputStream.read(bytes_others0, 0, bytes_others0.length);

            /** Get ChunkSize */
            byte[] bytes_ChunkSize = new byte[4];
            inputStream.read(bytes_ChunkSize, 0, bytes_ChunkSize.length);
            long ChunkSize = getLE4(bytes_ChunkSize);

            /* Get other info */
            byte[] bytes_others1 = new byte[4*4];
            inputStream.read(bytes_others1, 0, bytes_others1.length);

            /** Get SampleRate */
            byte[] bytes_SampleRate = new byte[4];
            inputStream.read(bytes_SampleRate, 0, bytes_SampleRate.length);
            long SampleRate = getLE4(bytes_SampleRate);

            /* Get other info */
            inputStream.read(bytes_others0, 0, bytes_others0.length);

            /** Get BlockAlign */
            byte[] bytes_BlockAlign = new byte[2];
            inputStream.read(bytes_BlockAlign, 0, bytes_BlockAlign.length);
            long BlockAlign = getLE2(bytes_BlockAlign);

            /** Impt Details */
            numSamples = ChunkSize / BlockAlign;
            float numSeconds = (float) numSamples / SampleRate;

            /** Close */
            inputStream.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return (int) numSamples;
    }
    public short[] readAudio(int rawID, int readAudio_numSamples) {
        final short[] recordBuffer = new short[readAudio_numSamples];
        try {
            InputStream inputStream = this.getResources().openRawResource(rawID);
            int read;

            /** Header Details */
            /* Ignore wave info */
            byte[] bytes_tmp = new byte[44];
            read = inputStream.read(bytes_tmp, 0, bytes_tmp.length);

            /** Reading wave samples */
            /* Reading WaveOut */
            byte[] bytes = new byte[2];
            long longtmp;
            int idx=0;
            while ( read != -1 ){
                read = inputStream.read(bytes, 0, bytes.length);
                longtmp = getLE2(bytes);
                recordBuffer[idx] = (short) longtmp;
                idx++;
            }

            /** Close */
            inputStream.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return recordBuffer;
    }
    /** Displaying Waveform */
    public void dispWave(View v){
        /** ------ Init ------ */
        /* -- Wave Params -- */
        int recordLength;
        final short[] recordBuffer;
        /* -- Plot Params -- */
        DataPoint[] GraphDPts;
        LineGraphSeries<DataPoint> series;

        /** ------ Read Audio ------ */
        recordLength = readAudio_numSamples(R.raw.test);
        recordBuffer = readAudio(R.raw.test, recordLength);

        /** ------ Plot Wave ------ */
        GraphDPts = new DataPoint[recordLength];
        for (int i = 0; i < recordLength; i++) {
            GraphDPts[i] = new DataPoint((float) i/samplingRate, recordBuffer[i]);
        }
        series = new LineGraphSeries<>(GraphDPts);
        graph.addSeries(series);
        AddDetail(graph);
    }


    /** ------ Helper for Beat Tracking ------ */
    private void ArrayList2File(ArrayList<Integer> arrayList, String TextFile) {
        try {
            FileOutputStream fileOutputStream = openFileOutput(TextFile, Context.MODE_PRIVATE);
            ObjectOutputStream out = new ObjectOutputStream(fileOutputStream);
            out.writeObject(arrayList);
            out.close();
            fileOutputStream.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private ArrayList<Integer> File2ArrayList(String TextFile) {
        ArrayList<Integer> savedArrayList = null;

        try {
            FileInputStream inputStream = openFileInput(TextFile);
            ObjectInputStream in = new ObjectInputStream(inputStream);
            savedArrayList = (ArrayList<Integer>) in.readObject();
            in.close();
            inputStream.close();

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }

        return savedArrayList;
    }
    /** Beat Tracking */
    public void beattrack(View v){
        /** ------ Init ------ */
        /* -- Beats Params -- */
        Integer BeatsSize = 0;
        ArrayList<Integer> Beats;
        /* -- Plot Params -- */
        DataPoint[] GraphDPts;
        LineGraphSeries<DataPoint> series;
        Integer SampleIdx;

        /** ------ Get the beats (fake) ------ */
        Beats = new ArrayList<>();
        for (int i=0; i<12; i++){
            Beats.add(i*30);
        }
        BeatsSize = Beats.size();
        /* -- Write in string form to local file  */
        ArrayList2File(Beats, "Beats.txt");

        /** ------ Get the beats ------ */
        // After integrate cpp algo, insert here.

        /** ------ Read the Beats ------ */
        Beats = File2ArrayList("Beats.txt");

        /** ------ Plot Vertical Lines ------ */
        GraphDPts = new DataPoint[2];
        for (int i = 0; i < BeatsSize; i++) {
            SampleIdx = Beats.get(i)*hop_length;
            GraphDPts[0] = new DataPoint((float) SampleIdx/samplingRate, -40000);
            GraphDPts[1] = new DataPoint((float) SampleIdx/samplingRate, 40000);
            series = new LineGraphSeries<>(GraphDPts);
            series.setColor(Color.RED);
            graph.addSeries(series);
        }
    }

}