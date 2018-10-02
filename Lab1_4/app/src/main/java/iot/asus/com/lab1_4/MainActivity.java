package iot.asus.com.lab1_4;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.GpioCallback;
import com.google.android.things.pio.PeripheralManager;
import com.google.android.things.pio.Pwm;

import java.io.IOException;

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
    private static final double PULSE_PERIOD_MS = 20;

    private static final int INTERVAL_BETWEEN_STEPS_MS = 3000;

    private static final String buttonPin = BoardDefault.getGPIOForButton();

    private Gpio mButtonGpio, mLedGpioR, mLedGpioG, mLedGpioB;
    private boolean mLedStateR = true;
    private boolean mLedStateG = true;
    private boolean mLedStateB = true;
    private Pwm mPwm;
    private int i = 0;

    private final int STATE_LED = 0;
    private final int STATE_RED = 2;
    private final int STATE_GREEN = 3;
    private final int STATE_BLUE = 1;
    private int state = STATE_LED;

    private static final int LED_RED = 1;
    private static final int LED_GREEN = 2;
    private static final int LED_BLUE = 3;

    private int mLedState = LED_RED;

    private Handler mHandler = new Handler();
    private double mActivePulseDuration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "Starting ButtonActivity");
        try {
            PeripheralManager manager = PeripheralManager.getInstance();

            mLedGpioR = manager.openGpio(BoardDefault.getGPIOForLedR());
            mLedGpioR.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
            mLedGpioR.setValue(true);

            mLedGpioG = manager.openGpio(BoardDefault.getGPIOForLedG());
            mLedGpioG.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
            mLedGpioG.setValue(true);

            mLedGpioB = manager.openGpio(BoardDefault.getGPIOForLedB());
            mLedGpioB.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
            mLedGpioB.setValue(true);

            mPwm = manager.openPwm(BoardDefault.getGPIOForPwm());
            mPwm.setPwmFrequencyHz(1000 / PULSE_PERIOD_MS);
            mPwm.setPwmDutyCycle(0);
            mPwm.setEnabled(true);

            mButtonGpio = manager.openGpio(buttonPin);
            mButtonGpio.setDirection(Gpio.DIRECTION_IN);
            mButtonGpio.setEdgeTriggerType(Gpio.EDGE_FALLING);

            Log.i(TAG, "Gonna go register for button");
            mButtonGpio.registerGpioCallback(new GpioCallback() {
                @Override
                public boolean onGpioEdge(Gpio gpio) {
                    switch (state) {
                        case STATE_LED:
                            state = STATE_RED;
                            break;
                        case STATE_RED:
                            state = STATE_GREEN;
                            break;
                        case STATE_GREEN:
                            state = STATE_BLUE;
                            break;
                        case STATE_BLUE:
                            state = STATE_LED;
                            break;
                        default:
                            Log.e(TAG, "Error");
                            break;
                    }
                    Log.i(TAG, "Button pressed" + state);
                    return true;
                }
            });

            mHandler.post(mChangePWMRunnable);
        } catch (IOException e) {
            Log.e(TAG, "Error press button", e);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacks(mChangePWMRunnable);
        if (mButtonGpio != null || mPwm != null || mLedGpioR != null || mLedGpioB != null || mLedGpioG != null) {
            Log.i(TAG, "Closing Button GPIO pin");
            try {
                mPwm.close();
                mLedGpioR.close();
                mLedGpioG.close();
                mLedGpioB.close();
                mButtonGpio.close();
            } catch (IOException e) {
                Log.e(TAG, "Error on PeripheralIO API27", e);
            } finally {
                mButtonGpio = null;
                mLedGpioR = null;
                mLedGpioB = null;
                mLedGpioG = null;
                mPwm = null;
            }
        }
    }

    private Runnable mChangePWMRunnable = new Runnable() {
        @Override
        public void run() {
            if (mPwm == null || mLedGpioG == null || mLedGpioR == null || mLedGpioB == null) {
                Log.w(TAG, "Stop runnable since mPwm is null");
                return;
            }

            try {
                switch (mLedState) {
                    case LED_RED:
                        mLedState = LED_GREEN;
                        mLedStateR = false;
                        mLedStateG = true;
                        mLedStateB = true;
                        break;
                    case LED_GREEN:
                        mLedState = LED_BLUE;
                        mLedStateG = false;
                        mLedStateB = true;
                        mLedStateR = true;
                        break;
                    case LED_BLUE:
                        mLedState = LED_RED;
                        mLedStateB = false;
                        mLedStateG = true;
                        mLedStateR = true;
                        break;
                    default:
                        throw new IllegalStateException("error");
                }

                mLedGpioG.setValue(mLedStateG);
                mLedGpioR.setValue(mLedStateR);
                mLedGpioB.setValue(mLedStateB);

                if (state == mLedState) {
                    mPwm.setPwmDutyCycle(90);
                } else {
                    mPwm.setPwmDutyCycle(0);
                }
                mHandler.postDelayed(this, INTERVAL_BETWEEN_STEPS_MS);
            } catch (IOException e) {
                Log.e(TAG, "Error on PeripheralIO API", e);
            }
        }
    };
}
