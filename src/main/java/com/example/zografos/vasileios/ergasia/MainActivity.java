package com.example.zografos.vasileios.ergasia;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.CountDownTimer;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

public class MainActivity extends AppCompatActivity implements SensorEventListener{


    private final String TAG = "Main Activity";

    private TextView timeText;
    private Button abortBtn;
    private Button sosBtn;
    public static CountDownTimer timer;
    private final long START_TIME = 30000;
    public static long timeLeft = 30000;

    private SensorManager sensorManager;
    Sensor accelerometer;
    Sensor lightSensor;

    LocationManager locationManager;
    String[] permission = new String[]{ Manifest.permission.ACCESS_FINE_LOCATION };
    private final int REQUEST_LOCATION = 1;

    TextToSpeech speech;
    String helpText = "HELP !HELP !HELP !";
    int result;

    private final int SEND_SMS_PERMISSION = 1;

    DatabaseHelper dbHelper;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Lock screen orientation
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // setup text-view refs and button refs
        timeText = (TextView) findViewById(R.id.timeText);
        abortBtn = (Button) findViewById(R.id.abortBtn);
        sosBtn = (Button) findViewById(R.id.sosBtn);


        // set timer's text with the value of START_TIME
        updateTimeText();


        // abort button stops and resets timer
        abortBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

               // start other activity
               Intent intent = new Intent(MainActivity.this, SecondScreen.class);
               startActivity(intent);

            }
        });
        abortBtn.setEnabled(false); // disable abort button if the device is not falling


        // setup sos button listener
        sosBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopTimer();
                resetTimer();
                Help(); // send SOS signal
                sosBtn.setEnabled(false);
                abortBtn.setEnabled(false);
            }
        });
        sosBtn.setEnabled(false);


        // setup sensor-manager, accelerometer and light-sensor
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);

        // register sensors
        sensorManager.registerListener(MainActivity.this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(MainActivity.this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);


        // setup locationManager
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        // request GPS permission
        ActivityCompat.requestPermissions(this, permission, REQUEST_LOCATION);

        // setup Text To Speech object
        speech = new TextToSpeech(MainActivity.this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {

                if (i == TextToSpeech.SUCCESS){
                    result = speech.setLanguage(Locale.UK);
                }else{
                    ToastMsg("TTS is not supported");
                }

            }
        });

        // request permission to send sms
        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.SEND_SMS}, SEND_SMS_PERMISSION);

        // setup db helper
        dbHelper = new DatabaseHelper(this);

    }


    void AddEvent(String name, String info){

        boolean isAdded = dbHelper.addEvent(name, info);
        if (isAdded){
            ToastMsg("Insertion completed successfully.");
        }else{
            ToastMsg("Insertion failed.");
        }

    }

    void SendSMS(String phoneNo, String message){

        try{
            SmsManager sms = SmsManager.getDefault();
            sms.sendTextMessage(phoneNo, null, message, null, null);
            ToastMsg("SOS message was sent.");
        }catch (Exception e){
            ToastMsg("SOS message was not sent.");
        }

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
        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){

            // request ACCESS_FINE_LOCATION permission
            ActivityCompat.requestPermissions(MainActivity.this, permission, REQUEST_LOCATION);

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
                SendSMS(phoneNo, msg);

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
                SendSMS(phoneNo, msg);

                // store the event in db
                String name = "SOS-SMS";
                String info = "PhoneNo:"+phoneNo+",Message:"+msg;
                AddEvent(name, info);

            }

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
                abortBtn.setEnabled(false);
                sosBtn.setEnabled(false);
            }
        }.start();

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {/* nothing happens here... */}

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

        // check the type of the changed sensor
        int SensorType = sensorEvent.sensor.getType();

        if (SensorType == Sensor.TYPE_ACCELEROMETER){
            // detect if user has fall down
            DetectFall(sensorEvent.values[0], sensorEvent.values[1], sensorEvent.values[2]);
        }

        if (SensorType == Sensor.TYPE_LIGHT){
            // detect strong light
            float level = sensorEvent.values[0];
            DetectLightLevel(level);
        }

    }

    void DetectLightLevel(float level){

        if (level > 10000){
            // inform user
            ToastMsg("The light intensity is too high !");

            // store the event in db
            String name = "Intense-Light";
            String info = "Light-Level:"+String.valueOf(level);
            AddEvent(name, info);
        }

    }

    void DetectFall(float X, float Y, float Z){

        // calculate acceleration (A = acceleration)
        double A = Math.sqrt(Math.pow(X, 2) + Math.pow(Y, 2) + Math.pow(Z, 2));

        // check if the device is falling
        if (A < 2.0) {
            // The device fall down !
            ToastMsg("Device fall down !");
            abortBtn.setEnabled(true);
            sosBtn.setEnabled(true);
            startTimer();

            // store the event in db
            String name = "Device-Fall-Down";
            String info = "Acceleration:"+A;
            AddEvent(name, info);
        }

    }

    void stopTimer(){
        // stop timer
        timer.cancel();
    }

    void resetTimer(){
        // reset time-left var
       timeLeft = START_TIME;
       updateTimeText();
    }

    void updateTimeText(){
        // update count down text
        String secs = "  "+String.valueOf(timeLeft / 1000)+"s";
        timeText.setText(secs);
    }

    void ToastMsg(String message){
        Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
    }

}
