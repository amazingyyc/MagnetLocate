package com.amazing.magnetsensor;

/**
 * Created by WSH on 2014/12/24.
 */

import com.amazing.magnetsensor.MagnetSensorManager.SENSOR_MESSAGE_TYPE;

public class MagnetSensorEvent
{
    public int accuracy;    //精度
    public long timestamp;  //时间戳

    public SENSOR_MESSAGE_TYPE type;   //消息类型
    public float[] values;   //位置信息

    MagnetSensorEvent(int size)
    {
        values = new float[size];
    }
}
