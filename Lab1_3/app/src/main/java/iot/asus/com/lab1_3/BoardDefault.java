package iot.asus.com.lab1_3;

import android.os.Build;

public class BoardDefault {
    private static final String DEVICE_RPI3 = "rpi3";

    public static String getPwmPin(){
        switch(Build.DEVICE){
            case DEVICE_RPI3:
                return "PWM0";
            default:
                throw new IllegalStateException("Unknown Build.DEVICE " + Build.DEVICE);
        }
    }
}