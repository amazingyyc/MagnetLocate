package com.amazing.main;

import android.hardware.Sensor;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.amazing.magnetsensor.MagnetSensor;
import com.amazing.magnetsensor.MagnetSensorEvent;
import com.amazing.magnetsensor.MagnetSensorListener;


public class MainActivity extends ActionBarActivity
{
    MagnetSensor magnetSensor = null;

    Button button = null;
    TextView text = null;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        magnetSensor = new MagnetSensor(this);
        magnetSensor.register();

        magnetSensor.setMagnetSensorListener(new MagnetSensorListener()
        {
            @Override
            public void onMagnetSensorEvent(MagnetSensorEvent event)
            {
                switch(event.type)
                {
                    case SENSOR_MESSAGE_MOVE:
                        text.setText("移动事件" + event.type + "\n" + event.values[0]  + "\n"+ event.values[1]  + "\n"+ event.values[2]);
                        break;
                    case SENSOR_MESSAGE_CLICK:
                        text.setText("点击事件" + event.values[0] + event.values[1] + event.values[2]);
                        break;
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy)
            {

            }
        });

        button = (Button)findViewById(R.id.button);
        text   = (TextView)findViewById(R.id.textview);

        button.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                magnetSensor.initial();
            }
        });
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
}
