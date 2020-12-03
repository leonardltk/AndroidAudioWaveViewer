package com.example.audio_v2;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.TextView;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;

import net.galmiza.android.engine.sound.SoundEngine;

public class MainActivity extends AppCompatActivity {
    /** ------------ Variables ------------ */
    // Template song
    public MediaPlayer mySong;
    // Attributes
    public SoundEngine nativeLib;
    public int samplingRate = 16000;
    public int hop_length = 128;
    // -- Scroll Params --
    public int StartDelay = 900;
    public int SkipSample = 30; // Resolution for display
    public ObjectAnimator ScrollAnimator; // Animation to start/stop scrolling
    // -- Canvas Params --
    public HorizontalScrollView mHScrollView;
    public WaveformView mWaveformView;
    public SliderView mSliderView;
    // -- Buttons Params --
    public TextView PauseResumeTextView;
    public int PauseResumeFlag = 0;
    public int PausedLength;
    // -- Wave Params --
    public ArrayList<Float> amplitudes;
    public int NumSamples;
    public int DurMilli;
    // -- Beats Params --
    public ArrayList<Integer> Beats;


    /** ------------ When App start, run these by default ------------ */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /** Template Song */
        mySong = MediaPlayer.create(MainActivity.this, R.raw.test);

        /** JNI interface */
        nativeLib = new SoundEngine();
        nativeLib.initFSin();

        /** Canvas */
        mHScrollView = (HorizontalScrollView) findViewById(R.id.hscroll);
        mWaveformView = (WaveformView) findViewById(R.id.waveformView);
        mSliderView = (SliderView) findViewById(R.id.sliderview);

        /** Pause/Resume Button */
        PauseResumeTextView = findViewById(R.id.pause_button);
    }
    /* When pause the application, stop the song. */
    @Override
    protected void onPause() {
        super.onPause();
        mySong.release();
    }


    /** ------------ Play Pause/Resume Stop Buttons. ------------ */
    /* Play button. */
    public void playIT(View v){
        /** Play Song */
        mySong.start();

        /** Slider */
        mSliderView.StartSliding(NumSamples);

        /** Scroll the Canvas */
        // https://stackoverflow.com/questions/7202193/scroll-up-a-scrollview-slowly // Implement scroll within CountDownTimer()
        // https://www.daniweb.com/programming/mobile-development/threads/500977/how-to-start-countdown-timer-with-delay // for nested CountDownTimer()
        // https://stackoverflow.com/questions/20944186/scrollto0-250-with-animation-android-scrollview // Use ObjectAnimator.ofInt()
        // https://stackoverflow.com/questions/8642677/reduce-speed-of-smooth-scroll-in-scroll-view // also use ObjectAnimator.ofInt()
        /* Restart from the front if have not done so. */
        mHScrollView.fullScroll(HorizontalScrollView.FOCUS_LEFT);
        /* Delay the scrolling by StartDelay to place the slider in the middle of the screen */
        ScrollAnimator = ObjectAnimator.ofInt(
                mHScrollView, "scrollX",
                (NumSamples/SkipSample) - (StartDelay/2) );
        new CountDownTimer(StartDelay, StartDelay) {
            public void onTick(long millisUntilFinished) {
            }
            public void onFinish() {
                /* At the end of the delay,
                start Scrolling together with the slider. */
                ScrollAnimator.setDuration(DurMilli - StartDelay).start();
            }
        }.start();
    }
    /* Stop button. */
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void pauseIT(View v){
        /** Pause */
        if (PauseResumeFlag == 0){
            /** Pause song */
            mySong.pause();
            PausedLength = mySong.getCurrentPosition();

            /** Pause Sliding : PauseResumeFlag==0 */
            mSliderView.PauseResumeSliding(PauseResumeFlag);

            /** Pause Scrolling */
            ScrollAnimator.pause();

            /** Set flag and Button Text to Resume */
            PauseResumeFlag = 1;
            PauseResumeTextView.setText("Resume");
        }
        /** Resume */
        else {
            /** Resume song */
            mySong.seekTo(PausedLength);
            mySong.start();

            /** Resume Sliding : PauseResumeFlag==1 */
            mSliderView.PauseResumeSliding(PauseResumeFlag);

            /** Resume Scrolling */
            ScrollAnimator.resume();

            /** Set flag and Button Text to Pause */
            PauseResumeFlag = 0;
            PauseResumeTextView.setText("Pause");
        }
    }
    /* Stop button. */
    public void stopIT(View v){
        /** Stop song */
        mySong.release();
        mySong = MediaPlayer.create(MainActivity.this, R.raw.test);

        /** Stop Sliding */
        mSliderView.StopSliding();

        /** Stop Scrolling */
        ScrollAnimator.cancel();
        mHScrollView.fullScroll(HorizontalScrollView.FOCUS_LEFT);
    }


    /** ------------ Load Audio ------------ */
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
    public void LoadAudio(View v){
        /** ------ Read Audio ------ */
        amplitudes = readAudio_ArrayList(R.raw.test);
        NumSamples = amplitudes.size();
        DurMilli = NumSamples*1000/samplingRate;

        /** ------ Write Waveform Details ------ */
        TextView WaveDetails = v.getRootView().findViewById(R.id.WaveformTextView);
        String StringWaves =
                "Size=" + amplitudes.size() +
                "\nDuration=" + amplitudes.size()/samplingRate +
                "\nMax=" + Collections.max(amplitudes) +
                "\nMin=" + Collections.min(amplitudes);
        WaveDetails.setText(StringWaves);
    }


    /** ------------ Displaying Waveform ------------ */
    public void dispWave_Canvas(View v){
        /** ------ Plot ------ */
//        mWaveformView.clearAmplitudes();
        mWaveformView.drawAmplitudes(amplitudes);
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
        for (int i=0; i<13; i++){
            Beats.add(30*i);
        }
        /* -- Write in string form to local file  */
        ArrayList2File(Beats, "Beats.txt");

        /** ------ Get the beats ------ */
        // After integrate cpp algo, insert here.

        /** ------ Read the Beats ------ */
        Beats = File2ArrayList("Beats.txt");

        /** ------ Plot Vertical Lines (WaveformView) ------ */
//        mWaveformView.clearBeats();
        mWaveformView.drawBeats(Beats) ;

        /** ------ Write Beat Details ------ */
        TextView BeatDetails = v.getRootView().findViewById(R.id.BeatTextView);
        String StringBeats = "Number of Beats = " + Beats.size() + "";
        BeatDetails.setText(StringBeats);
    }
}