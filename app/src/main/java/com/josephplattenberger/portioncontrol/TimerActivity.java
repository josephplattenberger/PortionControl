package com.josephplattenberger.portioncontrol;

import android.app.Dialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Vibrator;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.NotificationCompat;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

import java.util.Locale;
import java.util.Timer;
import java.util.concurrent.TimeUnit;

public class TimerActivity extends AppCompatActivity {

    private int unitsPerServ, totalCals;
    private double calsPerUnit;
    private long totalDuration, timeBetweenUnits, nextUnitTime, millisRemaining;
    private final String pause = "Pause", resume = "Resume", finish = "Finish", cancel = "Cancel";
    private TextView timerText;
    private CountDownTimer timer;
    private Button cancelButton, pauseButton;
    private TextView unitsEaten; private int unitsEatenDigit = 0;
    private TextView servingsEaten; private double servingsEatenDigit = 0;
    private TextView caloriesEaten; private double caloriesEatenDigit = 0;
    private Vibrator v;
    private NotificationCompat.Builder notifyBuilder;
    private NotificationManager mNotificationManager;
    private AdView mAdView;
    private int sdkVersion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timer);

        //for updating button ui
        sdkVersion = Build.VERSION.SDK_INT;

        setupAd();

        //get Intent extras
        Intent in = getIntent();
        Bundle b = in.getExtras();
        unitsPerServ = b.getInt("unitsPerServ");
        int calsPerServ = b.getInt("calsPerServ");
        totalCals = b.getInt("totalCals");
        totalDuration = b.getLong("duration");
        timeBetweenUnits = b.getLong("timeBetweenUnits");

        //needed to update stats
        calsPerUnit = (double)calsPerServ / (double)unitsPerServ;
        nextUnitTime = totalDuration - timeBetweenUnits;

        setupStats();

        //setup Vibrator
        v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        setupNotifications();

        handleTimer(totalDuration);
        handlePause();
        handleCancel();
    }

    private void setupAd(){
        MobileAds.initialize(getApplicationContext(), AdInfo.getAdID());
        mAdView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
    }

    private void setupStats(){
        final String zero = "0";
        unitsEaten = (TextView)findViewById(R.id.unitsEatenDigit); unitsEaten.setText(zero);
        servingsEaten = (TextView)findViewById(R.id.servingsEatenDigit); servingsEaten.setText(zero);
        caloriesEaten = (TextView)findViewById(R.id.caloriesEatenDigit); caloriesEaten.setText(zero);
    }

    private void setupNotifications(){
        Intent notifyIntent = new Intent(this, TimerActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(
                this, 0, notifyIntent, 0);
        notifyIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        notifyBuilder = new NotificationCompat.Builder(this);
        notifyBuilder.setSmallIcon(R.drawable.timer_running);
        notifyBuilder.setContentTitle("Eat One Unit!");
        notifyBuilder.setContentIntent(contentIntent);
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

    }

    private void handleTimer(long duration){
        timerText = (TextView)findViewById(R.id.timerText);
        timer = new CountDownTimer(duration, 100) {
            @Override
            public void onTick(long millisUntilFinished) {
                //save place in timer to handle pause
                millisRemaining = millisUntilFinished;
                //update UI
                timerText.setText(formatTime(millisUntilFinished));
                //if its time to eat notify and update UI
                if (millisUntilFinished <= nextUnitTime){
                    updateStats();
                    v.vibrate(500);
                    notifyBuilder.setContentText(String.format(Locale.getDefault(),
                            "Units: %d, Servings: %.1f, Calories: %.1f",
                            unitsEatenDigit, servingsEatenDigit, caloriesEatenDigit));
                    mNotificationManager.notify(1, notifyBuilder.build());
                    nextUnitTime = nextUnitTime - timeBetweenUnits;
                }
            }
            @Override
            public void onFinish() {
                millisRemaining = 0;
                String finishText = "00.0";
                timerText.setText(finishText);
                /*due to imperfect floating point math, must check if timer missed updating
                the stats on the last tick*/
                if (((double)totalCals - caloriesEatenDigit) >= (calsPerUnit - (calsPerUnit*.01))){
                    updateStats();
                }
                if ((double)totalCals != caloriesEatenDigit){
                    caloriesEatenDigit = totalCals;
                }
                //notify user snack has finished
                v.vibrate(2000);
                notifyBuilder.setContentTitle("Snack Finished!");
                notifyBuilder.setSmallIcon(R.drawable.timer_finish);
                notifyBuilder.setContentText(String.format(Locale.getDefault(),
                        "Units: %d, Servings: %.1f, Calories: %.1f",
                        unitsEatenDigit, servingsEatenDigit, caloriesEatenDigit));
                mNotificationManager.notify(2, notifyBuilder.build());
                mNotificationManager.cancel(1);
                finishTimer();
            }
        };

        timer.start();
    }

    private void finishTimer(){
        final Dialog finishDialog = new Dialog(this);
        finishDialog.setContentView(R.layout.finish_dialog);
        finishDialog.setCancelable(false);
        //Set dialog UI text
        String results = "Units Eaten: " + unitsEatenDigit;
        TextView finishUnitsEaten = (TextView)finishDialog.findViewById(R.id.finishUnitsEatenText);
        finishUnitsEaten.setText(results);
        results = String.format(Locale.getDefault(), "Servings Eaten: %.2f", servingsEatenDigit);
        TextView finishServEaten = (TextView)finishDialog.findViewById(R.id.finishServEatenText);
        finishServEaten.setText(results);
        results = String.format(Locale.getDefault(), "Calories Eaten: %.2f", caloriesEatenDigit);
        TextView finishCalsEaten = (TextView)finishDialog.findViewById(R.id.finishCalsEatenText);
        finishCalsEaten.setText(results);
        results = "Duration of Snack: " + formatTime(totalDuration - millisRemaining);
        TextView finishDuration = (TextView)finishDialog.findViewById(R.id.finishDurationText);
        finishDuration.setText(results);

        Button finishButton = (Button)finishDialog.findViewById(R.id.finishButton);
        //finish activity on click
        finishButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                mNotificationManager.cancelAll();
                Intent in = new Intent(TimerActivity.this, MainActivity.class);
                finishDialog.cancel();
                startActivity(in);
                finish();
            }
        });

        finishDialog.show();

    }

    private void handlePause(){
        pauseButton = (Button)findViewById(R.id.pauseButton);
        pauseButton.setText(pause);
        if(sdkVersion < 21){
            pauseButton.setTextColor(Color.WHITE);
        }
        pauseButton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                updateUIButtons();
            }
        });
    }

    private void updateUIButtons(){
        if (pauseButton.getText().equals("Pause")) {
            timer.cancel();
            pauseButton.setText(resume);
            if (sdkVersion >= 21) {
                pauseButton.getBackground().setColorFilter(ContextCompat.getColor(
                        this, R.color.colorPrimary), PorterDuff.Mode.MULTIPLY);
                pauseButton.setTextColor(Color.WHITE);
            }
            cancelButton.setText(finish);
            if(sdkVersion >= 21) {
                cancelButton.getBackground().setColorFilter(ContextCompat.getColor(
                        this, R.color.colorPrimary), PorterDuff.Mode.MULTIPLY);
                cancelButton.setTextColor(Color.WHITE);
            }
        }else{
            pauseButton.setText(pause);
            if(sdkVersion >= 21) {
                pauseButton.getBackground().setColorFilter(Color.WHITE, PorterDuff.Mode.MULTIPLY);
                pauseButton.setTextColor(ContextCompat.getColor(TimerActivity.this,
                        R.color.colorPrimaryDark));
            }
            cancelButton.setText(cancel);
            if(sdkVersion >= 21) {
                cancelButton.getBackground().setColorFilter(Color.WHITE, PorterDuff.Mode.MULTIPLY);
                cancelButton.setTextColor(ContextCompat.getColor(TimerActivity.this,
                        R.color.colorPrimaryDark));
            }
            handleTimer(millisRemaining);
        }
    }

    private void handleCancel(){
        cancelButton = (Button)findViewById(R.id.cancelButton);
        if (sdkVersion < 21){
            cancelButton.setTextColor(Color.WHITE);
        }
        cancelButton.setOnClickListener(new View.OnClickListener(){
           public void onClick(View v){
               if (cancelButton.getText().equals("Cancel")) {
                   cancelTimer();
               }else{
                   finishTimer();
               }
           }
        });
    }

    private void updateStats(){
        unitsEatenDigit++;
        unitsEaten.setText(String.format(Locale.getDefault(), "%d", unitsEatenDigit));
        servingsEatenDigit = (double)unitsEatenDigit / (double)unitsPerServ;
        servingsEaten.setText(String.format (Locale.getDefault(),"%.2f", servingsEatenDigit));
        caloriesEatenDigit = unitsEatenDigit * calsPerUnit;
        caloriesEaten.setText(String.format(Locale.getDefault(), "%.2f", caloriesEatenDigit));
    }

    private void cancelTimer(){
        final Dialog cancelDialog = new Dialog(this);
        cancelDialog.setContentView(R.layout.cancel_dialog);
        cancelDialog.setCanceledOnTouchOutside(true);

        Button yesButton = (Button)cancelDialog.findViewById(R.id.yesButton);
        Button noButton = (Button)cancelDialog.findViewById(R.id.noButton);

        yesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                timer.cancel();
                mNotificationManager.cancelAll();
                Intent in = new Intent(TimerActivity.this, MainActivity.class);
                cancelDialog.cancel();
                startActivity(in);
                finish();
            }
        });

        noButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cancelDialog.dismiss();
            }
        });

        cancelDialog.show();
    }

    private String formatTime(long duration){
        String result;
        long hours = TimeUnit.MILLISECONDS.toHours(duration);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(duration)
                - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(duration));
        long seconds = TimeUnit.MILLISECONDS.toSeconds(duration)
                - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(duration));
        StringBuilder microseconds = new StringBuilder(
                String.valueOf(TimeUnit.MILLISECONDS.toMicros(duration)
                        - TimeUnit.SECONDS.toMicros(TimeUnit.MILLISECONDS.toSeconds(duration))));
        microseconds.delete(1, microseconds.length());
        if (hours == 0 && minutes == 0){
            result = String.format(Locale.getDefault(), "%d.%s", seconds, microseconds);
        }else if (hours == 0){
            result = String.format(Locale.getDefault(), "%d:%02d.%s", minutes, seconds, microseconds);
        }else{
            result = String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds);
        }
        return result;
    }

    @Override
    public void onBackPressed(){
        cancelTimer();
    }

    @Override
    public void onPause() {
        if (mAdView != null) {
            mAdView.pause();
        }
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mAdView != null) {
            mAdView.resume();
        }
    }

    @Override
    public void onDestroy() {
        if (mAdView != null) {
            mAdView.destroy();
        }
        super.onDestroy();
    }
}
