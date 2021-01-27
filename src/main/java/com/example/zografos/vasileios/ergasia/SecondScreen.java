package com.example.zografos.vasileios.ergasia;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.speech.tts.TextToSpeech;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.util.Locale;

/**
 * Created by vasilis on 8/1/18.
 */

public class SecondScreen extends AppCompatActivity {

    TextView username;
    TextView password;
    Button cancelBtn;
    TextView timeText;

    CountDownTimer timer;
    long timeLeft;

    TextToSpeech speech;
    String helpText = "HELP !HELP !HELP !";
    int result;

    private final int SEND_SMS_PERMISSION = 1;
    private final int REQUEST_LOCATION = 1;

    LocationManager locationManager;

    DatabaseHelper dbHelper;



    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.second_screen_layout);

        // Lock screen orientation
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // stop main activity's timer
        MainActivity.timer.cancel();

        // setup timer-text in second screen activity
        timeLeft = MainActivity.timeLeft;

        // reset main activity's timer
        MainActivity.timeLeft = 30000;

        // setup text-view refs and button refs
        username = (TextView) findViewById(R.id.editText4);
        password = (TextView) findViewById(R.id.editText5);
        cancelBtn = (Button) findViewById(R.id.cancelBtn);
        timeText = (TextView) findViewById(R.id.timeText);

        // update timer-text in second screen activity
        updateTimeText();

        // start second screen's timer
        startTimer();

        // setup button listener for cancel button and back button
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // check username and password
                String uname = username.getText().toString();
                String pwd = password.getText().toString();

                if(!uname.isEmpty() && !pwd.isEmpty()){

                    // stop second-screen's timer
                    stopTimer();

                    // setup receiver and message
                    String msg = "Άκυρος ο συναγερμός. Όλα καλά";
                    String phoneNo = "0123";

                    // send message
                    SendSMS(phoneNo, msg, "Cancellation message was sent.", "Cancellation message was not sent.");

                    // store in db the cancellation message
                    String name = "Cancellation-SMS";
                    String info = "PhoneNo:"+phoneNo+",Message:"+msg;
                    AddEvent(name, info);

                    // go to main activity
                    Intent intent = new Intent(SecondScreen.this, MainActivity.class);
                    startActivity(intent);

                }else{
                    ToastMsg("Something is missing. Please try again.");
                }

            }
        });

        // setup Text To Speech object
        speech = new TextToSpeech(SecondScreen.this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {

                if (i == TextToSpeech.SUCCESS){
                    result = speech.setLanguage(Locale.UK);
                }else{
                    ToastMsg("TTS is not supported");
                }

            }
        });

        // setup locationManager
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        // request GPS permission
        ActivityCompat.requestPermissions(this, new String[]{ Manifest.permission.ACCESS_FINE_LOCATION }, REQUEST_LOCATION);

        // request permission to send sms
        ActivityCompat.requestPermissions(SecondScreen.this, new String[]{Manifest.permission.SEND_SMS}, SEND_SMS_PERMISSION);

        // setup db helper
        dbHelper = new DatabaseHelper(this);

    }



    // add new row in AppEvent table
    void AddEvent(String name, String info){

        boolean isAdded = dbHelper.addEvent(name, info);
        if (isAdded){
            ToastMsg("Insertion completed successfully.");
        }else{
            ToastMsg("Insertion failed.");
        }

    }

    void ToastMsg(String message){
        Toast.makeText(SecondScreen.this, message, Toast.LENGTH_SHORT).show();
    }


    // send's a sms message
    void SendSMS(String phoneNo, String message, String successMsg, String failMsg){

        try{
            SmsManager sms = SmsManager.getDefault();
            sms.sendTextMessage(phoneNo, null, message, null, null);
            ToastMsg(successMsg);
        }catch (Exception e){
            ToastMsg(failMsg);
        }

    }

    void startTimer(){
        // start timer
        timer = new CountDownTimer(timeLeft, 1000) {
            @Override
            public void onTick(long currentTime) {
                timeLeft = currentTime;
                updateTimeText();
            }

            @Override
            public void onFinish() {

                Help(); // send SOS signal if timer is finished

                // go to main activity
                Intent intent = new Intent(SecondScreen.this, MainActivity.class);
                startActivity(intent);

            }
        }.start();

    }

    void stopTimer(){
        // stop timer
        timer.cancel();
    }

    void updateTimeText(){
        // update count down text
        String secs = "  "+String.valueOf(timeLeft / 1000)+"s";
        timeText.setText(secs);
    }

    void SayHelp(){

        // check language support
        if(result == TextToSpeech.LANG_NOT_SUPPORTED || result == TextToSpeech.LANG_MISSING_DATA){
            return;
        }

        // speak
        speech.speak(helpText, TextToSpeech.QUEUE_FLUSH, null);

    }

    void Help(){

        boolean GPSProviderEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean NetworkProviderEnabled = locationManager.isProviderEnabled(LocationManager.PASSIVE_PROVIDER);
        boolean PassiveProviderEnabled = locationManager.isProviderEnabled(LocationManager.PASSIVE_PROVIDER);

        // check providers
        if (!PassiveProviderEnabled && !GPSProviderEnabled && !NetworkProviderEnabled){
            ToastMsg("GPS, Network and Passive providers are not enabled !");
        }else {
            SayHelp(); // says 'HELP !' 3 times
            SendLocation();
        }

    }

    void SendLocation(){

        // check location specific permissions
        if (ActivityCompat.checkSelfPermission(SecondScreen.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(SecondScreen.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){

            // request ACCESS_FINE_LOCATION permission
            ActivityCompat.requestPermissions(SecondScreen.this,  new String[]{ Manifest.permission.ACCESS_FINE_LOCATION }, REQUEST_LOCATION);

        }else{

            Location location = null;

            // get GPS location
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)){

                location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

            }else if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){

                location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

            }else if(locationManager.isProviderEnabled(LocationManager.PASSIVE_PROVIDER)){

                location = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);

            }

            // check location
            if(location != null) {

                // show location's latitude and longitude
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();

                String msg = "Βρίσκομαι στην τοποθεσία με γεωγραφικό μήκος " + longitude + " και γεωγραφικό πλάτος " + latitude + " και χρειάζομαι βοήθεια.";
                String phoneNo = "0123";

                // send sms
                SendSMS(phoneNo, msg, "SOS message was sent.", "SOS message was not sent.");

                // store the event in db
                String name = "SOS-SMS";
                String info = "PhoneNo:"+phoneNo+",Message:"+msg;
                AddEvent(name, info);

            }else{

                // Unable to find user's location
                // fake location
                location = new Location(LocationManager.GPS_PROVIDER);
                location.setLatitude(37.42d);
                location.setLongitude(-122.084d);
                location.setAltitude(0.0d);

                double latitude = location.getLatitude();
                double longitude = location.getLongitude();

                String msg = "Βρίσκομαι στην τοποθεσία με γεωγραφικό μήκος " + longitude + " και γεωγραφικό πλάτος " + latitude + " και χρειάζομαι βοήθεια.";
                String phoneNo = "0123";

                // send sms
                SendSMS(phoneNo, msg, "SOS message was sent.", "SOS message was not sent.");

                // store the event in db
                String name = "SOS-SMS";
                String info = "PhoneNo:"+phoneNo+",Message:"+msg;
                AddEvent(name, info);

            }

        }

    }

}
