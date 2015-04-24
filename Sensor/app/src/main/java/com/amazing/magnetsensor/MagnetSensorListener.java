package com.amazing.magnetsensor;

import android.hardware.Sensor;

/**
 * Created by WSH on 2014/12/24.
 */
public interface MagnetSensorListener
{
    //当有消息来的时候的回调函数
    public void onMagnetSensorEvent(MagnetSensorEvent event);

    //精度改变时的函数
    public void onAccuracyChanged(Sensor sensor, int accuracy);
}
