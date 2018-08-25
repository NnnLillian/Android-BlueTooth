package august.com.test;

import android.bluetooth.BluetoothAdapter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements OnClickListener{

    // 定义组件
    TextView T,AP,Tc,AF;
    private Button autopairbtn=null;
    private BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        autopairbtn=(Button) findViewById(R.id.button1);
        autopairbtn.setOnClickListener(this);

    }

    //设置按钮的监听方法
    public void onClick(View arg0) {

        if (!bluetoothAdapter.isEnabled())
        {
            bluetoothAdapter.enable();//异步的，不会等待结果，直接返回。
        }else{
            bluetoothAdapter.startDiscovery();
        }

    }
}
