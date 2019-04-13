package com.example.orientedauthentication;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;

import java.util.ArrayList;

public class UnlockedActivity extends AppCompatActivity implements SensorEventListener {

    /**
     * Sensor variables section
     */
    private SensorManager sensorManager;

    private final float[] rotationMatrix = new float[9];
    private final float[] orientationAngles = new float[3];

    private float[] accelerometerReading = new float[3];
    private float[] magnetometerReading = new float[3];

    private float azimuth, pitch, roll; // azimuth, pitch and roll

    // dummy contacts to be listed in this activity
    private ArrayList<Contact> contacts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // set up basic layouts
        setContentView(R.layout.activity_unlocked);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // set up sensor manager
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        // Initialize contacts
        contacts = Contact.createContactsList(20);

        // Lookup the recyclerview in activity layout
        RecyclerView rvContacts = (RecyclerView) findViewById(R.id.rvContacts);

        // Setup contact adapter to recycler view from the sample user data
        ContactsAdapter adapter = new ContactsAdapter(contacts);
        rvContacts.setAdapter(adapter);

        // Set layout manager to position the items
        rvContacts.setLayoutManager(new LinearLayoutManager(this));
    }

    /**
     * Lock the app
     * (Finishing this activity will get back us to the locked activity)
     */
    public void lock(){
        Log.i("status change:", "app locked");

        finish();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // does nothing on purpose
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Get updates from the accelerometer and magnetometer at a constant rate.
        // To make batch operations more efficient and reduce power consumption,
        // provide support for delaying updates to the application.
        Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer,
                    SensorManager.SENSOR_DELAY_NORMAL);
        }

        Sensor magneticField = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        if (magneticField != null) {
            sensorManager.registerListener(this, magneticField,
                    SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Don't receive any more updates from either sensor when paused.
        sensorManager.unregisterListener(this);
    }

    // Get readings from accelerometer and magnetometer. To simplify calculations,
    // consider storing these readings as unit vectors.
    @Override
    public void onSensorChanged(SensorEvent event) {
        // get magnetometer or accelerometer according if they change
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            accelerometerReading = event.values.clone();
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            magnetometerReading = event.values.clone();
        }

        // if both magnetometer and accelerometer readings change
        // update all three orientation angles
        if (magnetometerReading != null && accelerometerReading != null) {
            updateOrientationAngles();

            // check requirements and lock the app
            if (this.isScreenLeveled()){
                lock();
            }

            // reset sensor readings
            magnetometerReading = null;
            accelerometerReading = null;
        }
    }


    /**
     * Compute the three orientation angles based on the most recent readings from
     * the device's accelerometer and magnetometer.
     */
    public void updateOrientationAngles() {
        // Update rotation matrix, which is needed to update orientation angles.
        SensorManager.getRotationMatrix(rotationMatrix, null,
                accelerometerReading, magnetometerReading);

        // "mRotationMatrix" now has up-to-date information.

        // remap to application coordinate system
        float[] outGravity = new float[9];  // the new rotation matrix
        SensorManager.remapCoordinateSystem(rotationMatrix, SensorManager.AXIS_X, SensorManager.AXIS_Z, outGravity);

        SensorManager.getOrientation(outGravity, orientationAngles);
        // "mOrientationAngles" now has up-to-date information.

        azimuth = orientationAngles[0] * 57.2957795f;
        pitch = orientationAngles[1] * 57.2957795f;
        roll = orientationAngles[2] * 57.2957795f;

        // debugging statements
        Log.d("pitch: ", String.valueOf(pitch));
        Log.d("roll: ", String.valueOf(roll));
        Log.d("azimuth: ", String.valueOf(azimuth));
    }

    /**
     * Intentionally override to change back press behaviour
     */
    @Override
    public void onBackPressed()
    {
        // Intentionally does nothing
    }

    // checks if the screen is parallel to the ground
    // with some range of angle being considered as parallel

    /**
     * Checks if the screen is parallel to the ground
     * with some range of angle being considered as parallel
     *
     * @return a boolean indicating if the screen is leveled
     *         as in the required orientation and range
     */
    public boolean isScreenLeveled(){
        if (pitch > 80 && pitch < 90) {
            return true;
        }

        return false;
    }
}
