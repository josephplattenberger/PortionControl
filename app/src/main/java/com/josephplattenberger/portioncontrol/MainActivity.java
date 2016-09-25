package com.josephplattenberger.portioncontrol;

import android.app.Activity;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.TextView;

import java.util.Locale;
import java.util.concurrent.TimeUnit;



public class MainActivity extends AppCompatActivity {

    private int unitsPerServ = 0, calsPerServ = 0, totalCals = 0;
    private long totalHours = 0, totalMinutes = 1, duration = 60000, timeBetweenUnits;
    private NumberPicker hourPicker, minutePicker;
    private Button startButton;
    private TextView timeBetweenUnitsText;
    Activity thisActivity = this;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        timeBetweenUnitsText = (TextView) findViewById(R.id.timeBetweenUnits);

        getUnitsPerServ();
        getCalsPerServ();
        getTotalCals();
        handleHourPicker();
        handleMinutePicker();
        startTimer();
    }

    private void getUnitsPerServ(){
        final EditText unitsPerServInput = (EditText)findViewById(R.id.unitsPerServInput);

        unitsPerServInput.setOnKeyListener(new View.OnKeyListener() {

            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                String temp = unitsPerServInput.getText().toString().trim();
                if (temp.equals("")){
                    unitsPerServ = 0;
                }else{
                    unitsPerServ = Integer.parseInt(temp);
                }
                handleStart();
                return false;
            }
        });

    }

    private void getCalsPerServ(){
        final EditText calsPerServInput = (EditText)findViewById(R.id.calsPerServInput);


        calsPerServInput.setOnKeyListener(new View.OnKeyListener() {

            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                String temp = calsPerServInput.getText().toString().trim();
                if (temp.equals("")){
                    calsPerServ = 0;
                }else {
                    calsPerServ = Integer.parseInt(temp);
                }
                handleStart();
                return false;
            }
        });
    }

    private void getTotalCals(){
        final EditText totalCalsInput = (EditText)findViewById(R.id.totalCalsInput);


        totalCalsInput.setOnKeyListener(new View.OnKeyListener() {

            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                String temp = totalCalsInput.getText().toString().trim();
                if (temp.equals("")){
                    totalCals = 0;
                }else {
                    totalCals = Integer.parseInt(temp);
                }
                handleStart();
                return false;
            }
        });
    }

    private void handleHourPicker(){
        hourPicker = (NumberPicker)findViewById(R.id.hourPicker);
        hourPicker.setMinValue(0); hourPicker.setMaxValue(23);

        hourPicker.setOnScrollListener(new NumberPicker.OnScrollListener(){
            @Override
            public void onScrollStateChange(NumberPicker picker, int n){
                hideSoftKeyboard(thisActivity);
            }

        });

        hourPicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                totalHours = newVal;
                checkZero();
                handleStart();
            }
        });
    }

    private void handleMinutePicker(){
        minutePicker = (NumberPicker)findViewById(R.id.minutePicker);
        minutePicker.setMinValue(0); minutePicker.setMaxValue(59);
        checkZero();

        minutePicker.setOnScrollListener(new NumberPicker.OnScrollListener(){
            @Override
            public void onScrollStateChange(NumberPicker picker, int n){
                hideSoftKeyboard(thisActivity);
            }

        });

        minutePicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                totalMinutes = newVal;
                checkZero();
                handleStart();
            }
        });
    }

    private boolean checkZero(){
        //makes sure that atleast 1 minute is selected
        if (hourPicker.getValue() == 0 && minutePicker.getValue() == 0){
            minutePicker.setValue(1);
            totalMinutes = 1;
            return true;
        }
        return false;
    }

    private boolean unitsPerServFlag(){
        return unitsPerServ > 0;
    }

    private boolean calsPerServFlag(){
        return calsPerServ > 0;
    }

    private boolean totalCalsFlag(){
        return totalCals > 0;
    }

    private boolean readyToStart(){
        //Check none of the input values are zero or less
        return unitsPerServFlag() && calsPerServFlag() && totalCalsFlag();
    }

    private void handleStart(){
        if (readyToStart()) {
            duration = TimeUnit.HOURS.toMillis(totalHours) + TimeUnit.MINUTES.toMillis(totalMinutes);
            timeBetweenUnits = evalTimeBetweenUnits();
            if (timeBetweenUnits == -1){
                timeBetweenUnitsText.setText("");
                startButton.setVisibility(View.INVISIBLE);
            }else {
                String text = "Consume one unit per " + formatTime(timeBetweenUnits);
                timeBetweenUnitsText.setText(text);
                startButton.setVisibility(View.VISIBLE);
            }
        }else{
            timeBetweenUnitsText.setText("");
            startButton.setVisibility(View.INVISIBLE);
        }
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
        if (microseconds.length() > 1) {
            microseconds.delete(2, microseconds.length());
        }
        if (hours == 0 && minutes == 0){
            result = String.format(Locale.getDefault(), "%d.%s seconds", seconds, microseconds);
        }else if (hours == 0){
            result = String.format(Locale.getDefault(), "%d minutes and %d.%s seconds", minutes, seconds, microseconds);
        }else{
            result = String.format(Locale.getDefault(), "%d hours, %d minutes, and %d.%s seconds", hours, minutes, seconds, microseconds);
        }
        return result;
    }

    private static void hideSoftKeyboard(Activity activity) {
        InputMethodManager inputMethodManager =
                (InputMethodManager) activity.getSystemService(INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(
                activity.getCurrentFocus().getWindowToken(), 0);
    }

    private void startTimer(){
        startButton = (Button) findViewById(R.id.startButton);
        startButton.setVisibility(View.INVISIBLE);

        startButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Bundle b = new Bundle();
                Intent in = new Intent(MainActivity.this, TimerActivity.class);
                b.putInt("unitsPerServ", unitsPerServ);
                b.putInt("calsPerServ", calsPerServ);
                b.putInt("totalCals", totalCals);
                b.putLong("duration", duration);
                b.putLong("timeBetweenUnits", timeBetweenUnits);
                in.putExtras(b);
                startActivity(in);
                finish();
            }
        });
    }

    private long evalTimeBetweenUnits(){
        double calsPerUnit = 1;
        if(unitsPerServ > 0) {
            calsPerUnit = (double) calsPerServ / (double) unitsPerServ;
        }
        double totalUnits = 1;
        if (calsPerUnit > 0) {
            totalUnits = (double) totalCals / calsPerUnit;
        }
        //multiply by 100 when converting to long to preserve some precision
        long temp = (long)(totalUnits * 100);
        if (temp > 0) {
            return (duration * 100) / temp;
        }
        return -1;
    }
}
