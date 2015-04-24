package com.amazing.magnetsensor;

/**
 * Created by WSH on 2014/12/23.
 */
public class Vec3
{
    public float x;
    public float y;
    public float z;

    Vec3()
    {
        this(0, 0, 0);
    }

    Vec3(float x, float y, float z)
    {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public float length()
    {
        return (float) Math.sqrt(x*x + y*y + z*z);
    }

    public void sub(Vec3 vec)
    {
        this.x -= vec.x;
        this.y -= vec.y;
        this.z -= vec.z;
    }

    public void add(Vec3 vec)
    {
        this.x += vec.x;
        this.y += vec.y;
        this.z += vec.z;
    }

    public Vec3 clone()
    {
        return new Vec3(x, y, z);
    }
}
