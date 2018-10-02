package iot.asus.com.lab1_3;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import com.google.android.things.pio.PeripheralManager;
import com.google.android.things.pio.Pwm;

import java.io.IOException;
import java.util.List;

/**
 * Skeleton of an Android Things activity.
 * <p>
 * Android Things peripheral APIs are accessible through the class
 * PeripheralManagerService. For example, the snippet below will open a GPIO pin and
 * set it to HIGH:
 * <p>
 * <pre>{@code
 * PeripheralManagerService service = new PeripheralManagerService();
 * mLedGpio = service.openGpio("BCM6");
 * mLedGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
 * mLedGpio.setValue(true);
 * }</pre>
 * <p>
 * For more complex peripherals, look for an existing user-space driver, or implement one if none
 * is available.
 *
 * @see <a href="https://github.com/androidthings/contrib-drivers#readme">https://github.com/androidthings/contrib-drivers#readme</a>
 */
public class MainActivity extends Activity {
    private static final String TAG = MainActivity.class.getSimpleName();

    private static final double MIN_ACTIVE = 0;
    private static final double MAX_ACTIVE = 20;
    private static final double PULSE_PERIOD_MS = 20;   // Frequency of 50Hz (1000/20)

    private static double Change_step = 0.2;
    private static final int INTERVAL_BETWEEN_STEPS_MS = 100;

    private static final String PWM_NAME = BoardDefault.getPwmPin();
    private Pwm mPwm;
    private Handler mHandler = new Handler();
    private double mActivePulseDuration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG,"Start changing PWM pulse");

        PeripheralManager manager = PeripheralManager.getInstance();
        List<String> portList = manager.getPwmList();
        if(portList.isEmpty()){
            Log.i(TAG,"No PWM port available on this device.");
        } else {
            Log.i(TAG,"List of available ports: " + portList);
        }

        try{
            mActivePulseDuration = MIN_ACTIVE;
            mPwm = manager.openPwm(PWM_NAME);
            mPwm.setPwmFrequencyHz(1000/PULSE_PERIOD_MS);
            mPwm.setPwmDutyCycle(mActivePulseDuration);
            mPwm.setEnabled(true);

            Log.d(TAG,"Stat changing PWM pulse");
            mHandler.post(mChangePWMRunnable);
        } catch (IOException e){
            Log.w(TAG,"Error on IOException: ",e);
        }
    }

    protected void onDestroy(){
        super.onDestroy();
        mHandler.removeCallbacks(mChangePWMRunnable);
        Log.i(TAG,"Closing port");

        if(mPwm != null){
            try{
                mPwm.close();
            } catch(IOException e){
                Log.w(TAG,"Unable to close PWM",e);
            } finally {
                mPwm = null;
            }
        }
    }

    private Runnable mChangePWMRunnable = new Runnable() {
        @Override
        public void run() {
            if(mPwm == null){
                Log.w(TAG,"Stop runnable since mPwm is null");
                return;
            }

            mActivePulseDuration += Change_step;
            if (mActivePulseDuration > MAX_ACTIVE || mActivePulseDuration < MIN_ACTIVE){
                mActivePulseDuration = mActivePulseDuration > MAX_ACTIVE ? MAX_ACTIVE : MIN_ACTIVE;
                Change_step = - Change_step;
            }

            Log.d(TAG,"Changing PWM active pulse duration to " + mActivePulseDuration + "ms");

            try {
                mPwm.setPwmDutyCycle(100*mActivePulseDuration/PULSE_PERIOD_MS);
                mHandler.postDelayed(this, INTERVAL_BETWEEN_STEPS_MS);
            } catch (IOException e){
                Log.e(TAG,"Error on PeripheralIO API", e);
            }
        }
    };
}

