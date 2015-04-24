package com.amazing.magnetsensor;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import java.util.Vector;

import static com.amazing.magnetsensor.MagnetSensorManager.*;

/**
 * Created by WSH on 2014/12/23.
 */

/*
主类型 负责传感器的监听 和 消息的发送
 */
public class MagnetSensor implements SensorEventListener
{
    private static final int ORIGIN_NUM = 200;  //当取原始磁场和加速度的值时 取ORIGIN_NUM个平均值

    private Vector<Vec3> magnetVec = new Vector<Vec3>();    //存储最新磁场强度的值 ORIGIN_NUM个
    private Vector<Vec3> acceleVec = new Vector<Vec3>();    //存储最新加速计 ORIGIN_NUM个

    //------------------------------------------------------------------------------------------------------
    //磁感应数据处理需要的属性

    //k = log(c)/log(m_A) + m_B
    //m_Q = 10^k 噪声激励误差
    private static final float m_A = 1.625f;
    private static final float m_B = -4.265f;

    //测量噪声协方差
    private static final float m_RX = 0.079f;
    private static final float m_RY = 0.064f;
    private static final float m_RZ = 0.058f;

    //当前的磁感应强度 3个方向
    private float m_MAGNET_X = 0;
    private float m_MAGNET_Y = 0;
    private float m_MAGNET_Z = 0;

    //上个方向对应的 后验估计误差协方差矩阵 会自动收敛
    private float m_PX = 1;
    private float m_PY = 1;
    private float m_PZ = 1;

    //------------------------------------------------------------------
    private Vec3 originMagnet  = new Vec3();   //原始磁场
    private Vec3 currentMagnet = new Vec3();    //当前磁场强度

    private Vec3 prePos     = new Vec3();   //上一个的磁铁位置
    private Vec3 currentPos = new Vec3();   //这一次的位置 用于判断是否移动

    //------------------------------------------------------------------------------------
    //加速计需要的属性

    //k = log(c)/log(m_A) + m_B
    //m_Q = 10^k 噪声激励误差
    private static final float a_A = 2.137f;
    private static final float a_B = -1.913f;

    //测量噪声协方差
    private static final float a_RX = 0.00037f;
    private static final float a_RY = 0.00043f;
    private static final float a_RZ = 0.00079f;

    //当前的加速计 3个方向
    private float a_ACCELE_X = 0;
    private float a_ACCELE_Y = 0;
    private float a_ACCELE_Z = 0;

    //上个方向对应的 后验估计误差协方差矩阵 会自动收敛
    private float a_PX = 1;
    private float a_PY = 1;
    private float a_PZ = 1;

    //------------------------------------------------------
    private Vec3 originAccele   = new Vec3();   //原始加速计

    //记录相邻的三个值 用于判断 是否有点击操作
    private Vec3 preAccele  = new Vec3();
    private Vec3 hitAccele  = new Vec3();
    private Vec3 nextAccele = new Vec3();


    //-----------------------------------------------------------------------------------------------------
    //默认开始为 未初始化状态
    private SENSOR_STATUS sensorStatus = SENSOR_STATUS.SENSOR_UNREGISTER;

    //加速计点击的精度 默认为 高精度 和 阈值
    private SENSOR_CLICK_ACCURACY clickAccuracy = SENSOR_CLICK_ACCURACY.SENSOR_CLICK_MEDIUM;
    private float clickThreshold = CLICK_MEDIUM_THRESHOLD;

    //磁铁的三维坐标的数值扩大比例 默认为1
    private float scale = 1.0f;

    //----------------------------------------------------------------------
    //回调函数
    private MagnetSensorListener magnetSensorListener = null;

    //handle回调函数
    private Handler.Callback sensorHandleCallback = new Handler.Callback()
    {
        public boolean handleMessage(Message msg)
        {
            //是本人发的消息
            if(HANGLE_MAGNET_MESSAGE == msg.what)
            {
                //读取传入的事件 包含事件的类型和坐标等信息
                MagnetSensorEvent event = (MagnetSensorEvent)msg.obj;

                if(null != magnetSensorListener)
                    magnetSensorListener.onMagnetSensorEvent(event);
            }

            return true;
        }
    };

    //getMainLooper()主线程looper sensorHandleCallback回调函数
    private Handler sensorHandle = new Handler(Looper.getMainLooper(), sensorHandleCallback);

    //------------------------------------------------
    Activity activity = null;

    public MagnetSensor(Activity activity)
    {
        this.activity = activity;
    }

    @Override
    public void onSensorChanged(SensorEvent event)
    {
        switch(event.sensor.getType())
        {
            case Sensor.TYPE_MAGNETIC_FIELD:    //磁感应
                onMagnetChanged(event);
                sendMagnetMessage(event);
                break;
            case Sensor.TYPE_ACCELEROMETER:     //加速感应
                onAcceleChanged(event);
                sendAcceleMessage(event);
                break;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy)
    {
        //执行回调
        if(null != magnetSensorListener)
            magnetSensorListener.onAccuracyChanged(sensor, accuracy);
    }

    //根据当前的状态 判断是否发送 发送什么信息
    private void sendMagnetMessage(SensorEvent event)
    {
        //没有初始化完成 不发送任何事件消息
        if(SENSOR_STATUS.SENSOR_INITIAL != sensorStatus)
            return;

        //位置为移动不发送任何消息
        if(prePos.x == currentPos.x && prePos.y == currentPos.y && prePos.z == currentPos.z)
            return;

        //磁铁移动发送 移动消息
        MagnetSensorEvent magnetSensorEvent = new MagnetSensorEvent(3);

        magnetSensorEvent.accuracy  = event.accuracy;   //精度
        magnetSensorEvent.timestamp = event.timestamp;  //时间戳

        magnetSensorEvent.type = SENSOR_MESSAGE_TYPE.SENSOR_MESSAGE_MOVE;   //类型
        magnetSensorEvent.values[0] = currentPos.x;
        magnetSensorEvent.values[1] = currentPos.y;
        magnetSensorEvent.values[2] = currentPos.z;

        //创建消息
        Message message = new Message();
        message.what = HANGLE_MAGNET_MESSAGE;
        message.obj  = magnetSensorEvent;

        //发送消息
        sensorHandle.sendMessage(message);
    }

    private void sendAcceleMessage(SensorEvent event)
    {
        //没有初始化完成 不发送任何事件消息
        if(SENSOR_STATUS.SENSOR_INITIAL != sensorStatus)
            return;

        //利用z轴的值进行判断
        float preC  = hitAccele.z - preAccele.z;
        float nextC = hitAccele.z - nextAccele.z;
        float hitC  = Math.abs(hitAccele.z - originAccele.z);

        //不存在点击 直接返回
        if( !( (0 < preC*nextC) && (hitC > clickThreshold) ) )
            return;

        //发送点击事件
        MagnetSensorEvent magnetSensorEvent = new MagnetSensorEvent(3);

        magnetSensorEvent.accuracy  = event.accuracy;   //精度
        magnetSensorEvent.timestamp = event.timestamp;  //时间戳

        magnetSensorEvent.type = SENSOR_MESSAGE_TYPE.SENSOR_MESSAGE_CLICK;   //类型
        magnetSensorEvent.values[0] = currentPos.x;
        magnetSensorEvent.values[1] = currentPos.y;
        magnetSensorEvent.values[2] = currentPos.z;

        //创建消息
        Message message = new Message();
        message.what = HANGLE_MAGNET_MESSAGE;
        message.obj  = magnetSensorEvent;

        //发送消息
        sensorHandle.sendMessage(message);
    }

    //当磁感应发生变化时 调用
    private void onMagnetChanged(SensorEvent event)
    {
        //如果处于未注册状态 直接返回
        if(sensorStatus == SENSOR_STATUS.SENSOR_UNREGISTER)
            return;

        float[] values = event.values;

        //卡尔曼 对三个值进行滤波
        magnetKalman(values[0], m_MAGNET_X, m_PX, COORD_DIMENSION.X_DIMENSION);
        magnetKalman(values[1], m_MAGNET_Y, m_PY, COORD_DIMENSION.Y_DIMENSION);
        magnetKalman(values[2], m_MAGNET_Z, m_PZ, COORD_DIMENSION.Z_DIMENSION);

        //记录当前的磁场强度
        currentMagnet.x = m_MAGNET_X;
        currentMagnet.y = m_MAGNET_Y;
        currentMagnet.z = m_MAGNET_Z;

        //已经注册传感器 但是还没有初始化原始磁场
        if(sensorStatus == SENSOR_STATUS.SENSOR_REGISTER)
        {
            Vec3 vec = new Vec3(m_MAGNET_X, m_MAGNET_Y, m_MAGNET_Z);

            if(magnetVec.size() >= ORIGIN_NUM)
                magnetVec.remove(0);

            magnetVec.add(vec);

            return;
        }

        //更新位置
        updatePos();
    }

    //对磁感应强度进行 滤波
    //calMagnet 当前测量值 preMagnet上一次的最优值 preP上一次的后验估计误差协方差矩阵
    //dimension 维度
    private void magnetKalman(float calMagnet, float preMagnet, float preP, COORD_DIMENSION dimension)
    {
        float Q = getMagnetQ(calMagnet - preMagnet);
        float R = getMagnetR(dimension);

        float estMagnet = preMagnet;    //预测值
        float estP      = preP + Q;     //预测矩阵
        float KG        = estP / (estP + R);    //卡尔曼增益

        float besMagnet = estMagnet + KG * (calMagnet - estMagnet);
        float nowP      = (1 - KG) * estP;

        //设置新的值
        switch(dimension)
        {
            case X_DIMENSION:
                m_MAGNET_X = besMagnet;
                m_PX       = nowP;
                return;
            case Y_DIMENSION:
                m_MAGNET_Y = besMagnet;
                m_PY       = nowP;
                return;
            case Z_DIMENSION:
                m_MAGNET_Z = besMagnet;
                m_PZ       = nowP;
                return;
        }
    }

    //根据差值 得到磁感应滤波对应的Q
    private float getMagnetQ(float dif)
    {
        if (0 == dif)   return 0;

        double k = Math.log(Math.abs(dif)) / Math.log(m_A) + m_B;

        return (float)Math.pow(10.0, k);
    }

    //根据维度 的得到对应的R
    private float getMagnetR(COORD_DIMENSION dimension)
    {
        switch(dimension)
        {
            case X_DIMENSION:
                return m_RX;
            case Y_DIMENSION:
                return m_RY;
            case Z_DIMENSION:
            default:
                return m_RZ;
        }
    }

    private void updatePos()
    {
        prePos.x = currentPos.x;
        prePos.y = currentPos.y;
        prePos.z = currentPos.z;

        double bX = currentMagnet.x - originMagnet.x;
        double bY = currentMagnet.y - originMagnet.y;
        double bZ = currentMagnet.z - originMagnet.z;

        if(0 == bX && 0 == bY)  return;

        double m = (bZ + Math.sqrt(9.0*bZ*bZ + 8.0*bX*bX + 8.0*bY*bY)) / (2.0*bZ*bZ + 2.0*bX*bX + 2.0*bY*bY);
        double t = Math.pow(1000000.0*m, 1.0 / 3.0);
        double u = Math.sqrt(m * bZ + 1.0);

        currentPos.x = (float) (scale * m * bX * t / u);
        currentPos.y = (float) (scale * m * bY * t / u);
        currentPos.z = (float) (scale * t * u);
    }

    //------------------------------------------------------------------------
    private void onAcceleChanged(SensorEvent event)
    {
        //如果处于未注册状态 直接返回
        if(sensorStatus == SENSOR_STATUS.SENSOR_UNREGISTER)
            return;

        float[] values = event.values;

        //卡尔曼 对三个值进行滤波
        acceleKalman(values[0], a_ACCELE_X, a_PX, COORD_DIMENSION.X_DIMENSION);
        acceleKalman(values[1], a_ACCELE_Y, a_PY, COORD_DIMENSION.Y_DIMENSION);
        acceleKalman(values[2], a_ACCELE_Z, a_PZ, COORD_DIMENSION.Z_DIMENSION);

        //已经注册传感器 但是还没有初始化原始磁场
        if(sensorStatus == SENSOR_STATUS.SENSOR_REGISTER)
        {
            Vec3 vec = new Vec3(a_ACCELE_X, a_ACCELE_Y, a_ACCELE_Z);

            if(acceleVec.size() >= ORIGIN_NUM)
                acceleVec.remove(0);

            acceleVec.add(vec);

            return;
        }

        //更新加速计参数
        updateAccele();
    }

    private void acceleKalman(float calAccele, float preAccele, float preP, COORD_DIMENSION dimension)
    {
        float Q = getAcceleQ(calAccele - preAccele);
        float R = getAcceleR(dimension);

        float estAccele = preAccele;    //预测值
        float estP      = preP + Q;     //预测矩阵
        float KG        = estP / (estP + R);    //卡尔曼增益

        float besAccele = estAccele + KG * (calAccele - estAccele);
        float nowP      = (1 - KG) * estP;

        //设置新的值
        switch(dimension)
        {
            case X_DIMENSION:
                a_ACCELE_X = besAccele;
                a_PX       = nowP;
                return;
            case Y_DIMENSION:
                a_ACCELE_Y = besAccele;
                a_PY       = nowP;
                return;
            case Z_DIMENSION:
                a_ACCELE_Z = besAccele;
                a_PZ       = nowP;
                return;
        }
    }

    //根据差值 得到磁感应滤波对应的Q
    private float getAcceleQ(float dif)
    {
        if (0 == dif)   return 0;

        double k = Math.log(Math.abs(dif)) / Math.log(a_A) + a_B;

        return (float)Math.pow(10.0, k);
    }

    //根据维度 的得到对应的R
    private float getAcceleR(COORD_DIMENSION dimension)
    {
        switch(dimension)
        {
            case X_DIMENSION:
                return a_RX;

            case Y_DIMENSION:
                return a_RY;

            case Z_DIMENSION:
            default:
                return a_RZ;
        }
    }

    private void updateAccele()
    {
        preAccele.x = hitAccele.x;
        preAccele.y = hitAccele.y;
        preAccele.z = hitAccele.z;

        hitAccele.x = nextAccele.x;
        hitAccele.y = nextAccele.y;
        hitAccele.z = nextAccele.z;

        nextAccele.x = a_ACCELE_X;
        nextAccele.y = a_ACCELE_Y;
        nextAccele.z = a_ACCELE_Z;
    }

    /////////////////////////////////////////////////////////////////////////////////////////////
    public void register()
    {
        SensorManager sensorManager = (SensorManager)activity.getSystemService(Activity.SENSOR_SERVICE);

        //磁感应传感器
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                SensorManager.SENSOR_DELAY_UI);

        //加速传感器
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_UI);

        //设置状态 已经注册
        sensorStatus = SENSOR_STATUS.SENSOR_REGISTER;
    }

    public void unregister()
    {
        SensorManager sensorManager = (SensorManager)activity.getSystemService(Activity.SENSOR_SERVICE);

        sensorManager.unregisterListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD));
        sensorManager.unregisterListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));

        //设置状态 为 未注册注册
        sensorStatus = SENSOR_STATUS.SENSOR_UNREGISTER;
    }

    //初始化原始磁场和加速计
    public void initial()
    {
        int size = magnetVec.size();

        float sumX = 0;
        float sumY = 0;
        float sumZ = 0;

        for(int i = 0; i < size; ++i)
        {
            Vec3 vec = magnetVec.get(i);

            sumX += vec.x;
            sumY += vec.y;
            sumZ += vec.z;
        }

        //计算平均值作为原始磁场
        originMagnet.x = sumX / size;
        originMagnet.y = sumY / size;
        originMagnet.z = sumZ / size;

        size = acceleVec.size();

        sumX = 0;
        sumY = 0;
        sumZ = 0;

        for(int i = 0; i < size; ++i)
        {
            Vec3 vec = acceleVec.get(i);

            sumX += vec.x;
            sumY += vec.y;
            sumZ += vec.z;
        }

        //计算原始加速计
        originAccele.x = sumX / size;
        originAccele.y = sumY / size;
        originAccele.z = sumZ / size;

        //设置状态
        sensorStatus = SENSOR_STATUS.SENSOR_INITIAL;
    }

    //----------------------------------------------------------------------------
    //设置点击精度
    public void setClickAccuracy(SENSOR_CLICK_ACCURACY clickAccuracy)
    {
        this.clickAccuracy = clickAccuracy;

        switch(clickAccuracy)
        {
            case SENSOR_CLICK_HIGH:
                //高敏感度
                this.clickThreshold = CLICK_HIGH_THRESHOLD;
                break;
            case SENSOR_CLICK_MEDIUM:
                //中敏感度
                this.clickThreshold = CLICK_MEDIUM_THRESHOLD;
                break;
            case SENSOR_CLICK_LOW:
                //低敏感度
                this.clickThreshold = CLICK_LOW_THRESHOLD;
                break;
        }
    }

    public SENSOR_CLICK_ACCURACY getClickAccuracy()
    {
        return clickAccuracy;
    }

    public void setScale(float scale)
    {
        this.scale = scale;
    }

    public float getScale()
    {
        return scale;
    }


    //---------------------------------------------------------------------------------
    //设置事件回调
    public void setMagnetSensorListener(MagnetSensorListener sensorListener)
    {
        this.magnetSensorListener = sensorListener;
    }
}







































