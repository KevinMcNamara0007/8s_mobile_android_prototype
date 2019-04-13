package com.example.orientedauthentication;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    /**
     * Sensor variables section
     */
    private SensorManager sensorManager;

    private final float[] rotationMatrix = new float[9];
    private final float[] orientationAngles = new float[3];

    private float[] accelerometerReading = new float[3];
    private float[] magnetometerReading = new float[3];

    private float azimuth, pitch, roll; // azimuth, pitch and roll


    /**
     * Screen size variables section
     */
    private int width, height;
    private float density;
    private float dpRange = 75f;    // range of a touch in dp
    private float pixRange;     // range of a touch in pixels, will be calculated later
    private boolean inRange = false;
    private float xRangeStart;  // xRangeEnd is assumed to be witdth end of the screen
    private float yRangeStart;
    private float yRangeEnd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // set up basic layouts
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // set up sensor manager
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        // get height and width of the display(does not consider navbar in height)
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        width = displayMetrics.widthPixels;
        height = displayMetrics.heightPixels;
        density = displayMetrics.density;
        pixRange = dpRange * displayMetrics.density;    // calculate the range of a touch span in pixels

        // valid x touch position is at the end of the screen
        xRangeStart = width - pixRange;

        // valid y touch position is at half of the screen
        float halfHeight = 0.5f * height;
        yRangeStart = halfHeight - pixRange;
        yRangeEnd = halfHeight + pixRange;

        // debugging some variables
        Log.d("display", String.format("pixRange: %f", pixRange));
        Log.d("display", String.format("width: %d, height: %d", width, height));
        Log.d("display", String.format("wdp: %f, hdp: %f", width / displayMetrics.density, height / displayMetrics.density));

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
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
                    SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
        }

        Sensor magneticField = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        if (magneticField != null) {
            sensorManager.registerListener(this, magneticField,
                    SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Don't receive any more updates from either sensor when paused.
        sensorManager.unregisterListener(this);
    }

    /**
     * Unlock the app
     * (Opening the unlocked activity is enough to unlock the app)
     */
    public void unlock() {
        Log.i("status change:", "app unlocked");

        Intent unlockIntent = new Intent(this, UnlockedActivity.class);
        startActivity(unlockIntent);
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

            // check requirements and unlock the app
            if (this.inRange) {
                if (pitch > 40 && pitch < 50) {
                    // since pitch angles will be repeated when phone top edge is
                    // towards the sky and the ground, we will use roll to block pitch angles
                    // created when phone top edge is towards the ground
                    if (roll > -50 && roll < 100) {
                        unlock();
                    }
                }
            }

            // reset sensor readings
            magnetometerReading = null;
            accelerometerReading = null;
        }
    }

    /**
     * Checks if a touch is in the range of the screen that is supposed to unlock the phone
     *
     * @param x current x position of touch on screen
     * @param y current y position of touch on screen
     * @return a boolean indicating if the touch is in the unlocking range or not
     */
    public boolean isTouchinRange(float x, float y) {
        if (x > xRangeStart) {
            if (y > yRangeStart && y < yRangeEnd) {
                return true;
            }
        }

        return false;
    }

    // Handle touch action if the touch should be considered in unlocking range or not
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = MotionEventCompat.getActionMasked(event);

        switch (action) {
            case (MotionEvent.ACTION_DOWN):
                if (isTouchinRange(event.getX(), event.getY())) {
                    this.inRange = true;
                } else {
                    this.inRange = false;
                }
                Log.d("mouse", "Action was DOWN");
                return true;
            case (MotionEvent.ACTION_MOVE):
                if (isTouchinRange(event.getX(), event.getY())) {
                    this.inRange = true;
                } else {
                    this.inRange = false;
                }
                Log.d("mouse", "Action was MOVE");
                return true;
            case (MotionEvent.ACTION_UP):
                this.inRange = false;
                Log.d("mouse", "Action was UP");
                return true;
            case (MotionEvent.ACTION_CANCEL):
                this.inRange = false;
                Log.d("mouse", "Action was CANCEL");
                return true;
            case (MotionEvent.ACTION_OUTSIDE):
                this.inRange = false;
                Log.d("mouse", "Movement occurred outside bounds " +
                        "of current screen element");
                return true;
            default:
                return super.onTouchEvent(event);
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
}
