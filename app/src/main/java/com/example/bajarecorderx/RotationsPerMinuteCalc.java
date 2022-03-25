package com.example.bajarecorderx;

public class RotationsPerMinuteCalc {
    public static double calcWRPM(double speed) {
       double WheelRPM= (speed/.5588) * 2*Math.PI*60;
        return WheelRPM;

    }
}
