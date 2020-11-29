package com.example.audio_v2;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.media.MediaPlayer;
import android.os.*;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;


import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.LineGraphSeries;

import net.galmiza.android.engine.sound.SoundEngine;

import com.jjoe64.graphview.series.DataPoint;

public class MainActivity extends AppCompatActivity {
    /** ------------ Variables ------------ */
    /* Template song */
    MediaPlayer mySong;
    /* Attributes */
    private SoundEngine nativeLib;
    private int samplingRate = 16000;
    private int hop_length = 128;
    /* -- Wave Params -- */
    ArrayList<Float> amplitudes;
    /* -- Beats Params -- */
    ArrayList<Integer> Beats;
    /* -- Canvas Params -- */
    private WaveformView mWaveformView;


    /** ------------ When App start, run these by default ------------ */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mySong = MediaPlayer.create(MainActivity.this, R.raw.test);

        /* JNI interface */
        nativeLib = new SoundEngine();
        nativeLib.initFSin();

        /* WaveformView Canvas */
        mWaveformView = (WaveformView) findViewById(R.id.waveformView);
    }
    /* When pause the application, stop the song. */
    @Override
    protected void onPause() {
        super.onPause();
        mySong.release();
    }


    /** ------------ Play/Pause Buttons. ------------ */
    /* Play button. */
    public void playIT(View v){
        mySong.start();
    }
    /* Stop button. */
    public void stopIT(View v){
        mySong.release();
        mySong = MediaPlayer.create(MainActivity.this, R.raw.test);
    }


    /** ------------ Displaying Waveform ------------ */
    /** Helper for Reading Audio */
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
    public ArrayList<Float> readAudio_ArrayList(int rawID) {
        ArrayList<Float> WaveOut = new ArrayList<>();
        try {
            InputStream inputStream = this.getResources().openRawResource(rawID);
            int read;

            /** Header Details */
            /* Get ChunkID */
            byte[] bytes_tmp = new byte[44];
            read = inputStream.read(bytes_tmp, 0, bytes_tmp.length);

            /** Reading Wav file */
            /* Reading WaveOut */
            byte[] bytes = new byte[2];
            long longtmp;
            while ( read != -1 ){
                read = inputStream.read(bytes, 0, bytes.length);
                longtmp = getLE2(bytes);
                WaveOut.add( (float) longtmp );
            }

            /** Close */
            inputStream.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return WaveOut;
    }
    /** dispWave */
    public void dispWave_Canvas(View v){
        /** ------ Read Audio ------ */
        amplitudes = readAudio_ArrayList(R.raw.test);

        /** ------ Plot ------ */
        mWaveformView.clearAmplitudes();
        mWaveformView.setAmplitudes(amplitudes);
        mWaveformView.drawAmplitudes();

        /** ------ Write Waveform Details ------ */
        TextView WaveDetails = v.getRootView().findViewById(R.id.WaveformTextView);
        String StringWaves = "Size=" + amplitudes.size() + "\nMax=" + Collections.max(amplitudes) + "\nMin=" + Collections.min(amplitudes);
        WaveDetails.setText(StringWaves);
    }


    /** ------------ Beat Tracking ------------ */
    /** Helper for Beat Tracking */
    /* To files */
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
    public void PlotBeatTracks(View v){
        Beats = new ArrayList<Integer>();

        /** ------ Get the beats (fake) ------ */
        for (int i=0; i<1200; i++){
            Beats.add(3*i);
        }
        /* -- Write in string form to local file  */
        ArrayList2File(Beats, "Beats.txt");

        /** ------ Get the beats ------ */
        // After integrate cpp algo, insert here.

        /** ------ Read the Beats ------ */
        Beats = File2ArrayList("Beats.txt");

        /** ------ Plot Vertical Lines (WaveformView) ------ */
        mWaveformView.clearBeats();
        mWaveformView.setBeats(Beats) ;
        mWaveformView.drawBeats();

        /** ------ Write Beat Details ------ */
        TextView BeatDetails = v.getRootView().findViewById(R.id.BeatTextView);
        String StringBeats = "" + Beats.size();
        BeatDetails.setText(StringBeats);
    }


    /** ------------ runSlider ------------ */
    public void runSlider(View v){
    }
}