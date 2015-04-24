package com.amazing.magnetsensor;

/**
 * Created by WSH on 2014/12/23.
 */

/*
包含MagnetSensor的状态参数
还有 点击操作的精确度
 */
public class MagnetSensorManager
{
    //----------------------------------------------------------------------------------------------------------
    //传感器类的状态参数 包括是否设置初始的 磁感应强度和加速计 正常状态 精确度不准状态
    public enum SENSOR_STATUS
    {
        SENSOR_UNREGISTER,      //未注册状态 这是传感器还没有进行注册
        SENSOR_REGISTER,        //已经注册过传感器但还没有进行 初始化原始磁场和加速计
        SENSOR_INITIAL,         //初始化状态已经注册过原始 磁场和加速计
    }

    //----------------------------------------------------------------------------------------------------------
    //点击精确度 分为三种 高 中 低
    public enum SENSOR_CLICK_ACCURACY
    {
        SENSOR_CLICK_HIGH,       //敏感度高
        SENSOR_CLICK_MEDIUM,     //敏感度中
        SENSOR_CLICK_LOW,        //敏感度低
    }

    public static final float CLICK_HIGH_THRESHOLD      = 0.2f; //高敏感度 对应的 z轴加速的阈值
    public static final float CLICK_MEDIUM_THRESHOLD    = 0.5f; //中敏感度 对应的 z轴加速的阈值
    public static final float CLICK_LOW_THRESHOLD       = 0.9f; //低敏感度 对应的 z轴加速的阈值

    //--------------------------------------------------------------------------------------------
    //坐标的三个维度 x y z
    enum COORD_DIMENSION
    {
        X_DIMENSION,
        Y_DIMENSION,
        Z_DIMENSION,
    }

    //----------------------------------------------------------------------------------------------------------
    //消息类型 包括 磁铁的移动和点击消息
    public enum SENSOR_MESSAGE_TYPE
    {
        SENSOR_MESSAGE_MOVE,    //移动类型的消息
        SENSOR_MESSAGE_CLICK,   //点击消息
        SENSOR_MESSAGE_UNKNOW,  //不知道的类型
    }

    //-------------------------------------------------
    //handlemessage的类型  所有的消息必须是这个类型
    public static final int HANGLE_MAGNET_MESSAGE = 123;
}






















