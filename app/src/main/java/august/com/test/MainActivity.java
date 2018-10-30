package august.com.test;

import android.bluetooth.BluetoothAdapter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    //定义组件
    TextView statusLabel;
    Button btnConnect,btnSend,btnQuit,btnOn,btnOff;
    EditText etReceived,etSend;
    TextView T,AP,Tc,AF,LS;

    //device var
    private BluetoothAdapter mBluetoothAdapter = null;

    private BluetoothSocket btSocket = null;

    private OutputStream outStream = null;

    private InputStream inStream = null;

    //这条是蓝牙串口通用的UUID，不要更改
    private static final UUID MY_UUID =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private static String address = "00:0C:BF:17:66:0F"; // <==要连接的目标蓝牙设备MAC地址


    private ReceiveThread rThread=null;  //数据接收线程

    //接收到的字符串
    String ReceiveData="";
    String times="";
    String pressure="";
    String tempretures="";
    String flow="";
    double flows;
    String LeakResult="";
    double LeakResults;
    String ON="ON";
    String OFF="OFF";

    MyHandler handler;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //首先调用初始化函数
        Init();
        InitBluetooth();

        handler=new MyHandler();

        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //判断蓝牙是否打开
                if(!mBluetoothAdapter.isEnabled())
                {
                    mBluetoothAdapter.enable();
                }
                mBluetoothAdapter.startDiscovery();

                //创建连接
                new ConnectTask().execute(address);

            }
        });


        btnQuit.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub

                if(btSocket!=null)
                {
                    try {
                        btSocket.close();
                        btSocket=null;
                        if(rThread!=null)
                        {
                            rThread.join();
                        }
                        statusLabel.setText("Disconnect Bluetooth connection");

                        T.setText("0.00");
                        AP.setText("00.0");
                        Tc.setText("00.0");
                        AF.setText("0.00");
                        LS.setText("0.00");
//						etReceived.setText("");
                    } catch (IOException e) {

                        e.printStackTrace();
                    } catch (InterruptedException e) {

                        e.printStackTrace();
                    }
                }



            }
        });

//        btnSend.setOnClickListener(new View.OnClickListener() {
//
//            @Override
//            public void onClick(View v) {
//                // TODO Auto-generated method stub
//                new SendInfoTask().execute(etSend.getText().toString());
//
//            }
//        });
        btnOn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                new SendInfoTask().execute(ON);

            }
        });
        btnOff.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                new SendInfoTask().execute(OFF);

            }
        });
    }

    public void Init()
    {
        statusLabel=(TextView)this.findViewById(R.id.textView1);
        T=(TextView) this.findViewById(R.id.stoppedTime);
        AP=(TextView)this.findViewById(R.id.press);
        Tc=(TextView)this.findViewById(R.id.degree);
        AF=(TextView)this.findViewById(R.id.flowData);
        LS=(TextView)this.findViewById(R.id.result);
        btnConnect=(Button)this.findViewById(R.id.button1);
//        btnSend=(Button)this.findViewById(R.id.button2);
        btnOn=(Button)this.findViewById(R.id.on);
        btnOff=(Button)this.findViewById(R.id.off);
        btnQuit=(Button)this.findViewById(R.id.button3);
//        etSend=(EditText)this.findViewById(R.id.editText1);
//        etReceived=(EditText)this.findViewById(R.id.editText2);
    }

    public void InitBluetooth()
    {
        //得到一个蓝牙适配器
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(mBluetoothAdapter == null)
        {
            Toast.makeText(this, "No supporting Bluetooth", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    //连接蓝牙设备的异步任务
    class ConnectTask extends AsyncTask<String,String,String>
    {


        @Override
        protected String doInBackground(String... params) {
            // TODO Auto-generated method stub
            BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(params[0]);

            try {

                btSocket = device.createRfcommSocketToServiceRecord(MY_UUID);


                btSocket.connect();

                Log.e("error", "ON RESUME: BT connection established, data transfer link open.");

            } catch (IOException e) {

                try {
                    btSocket.close();
                    return "Socket succeeded";

                } catch (IOException e2) {

                    Log .e("error","ON RESUME: Unable to close socket during connection failure", e2);
                    return "Socket failed";
                }

            }
            //取消搜索
            mBluetoothAdapter.cancelDiscovery();

            try {
                outStream = btSocket.getOutputStream();

            } catch (IOException e) {
                Log.e("error", "ON RESUME: Output stream creation failed.", e);
                return "Socket stream failed";
            }


            return "Connected";
        }

        @Override    //这个方法是在主线程中运行的，所以可以更新界面
        protected void onPostExecute(String result) {
            // TODO Auto-generated method stub

            //连接成功则启动监听
            rThread=new ReceiveThread();

            rThread.start();

            statusLabel.setText(result);

            super.onPostExecute(result);
        }



    }

    //发送数据到蓝牙设备的异步任务
    class SendInfoTask extends AsyncTask<String,String,String>
    {

        @Override
        protected void onPostExecute(String result) {
            // TODO Auto-generated method stub
            super.onPostExecute(result);

            statusLabel.setText(result);

            //将发送框清空
//            etSend.setText("");
        }

        @Override
        protected String doInBackground(String... arg0) {
            // TODO Auto-generated method stub

            if(btSocket==null)
            {
                return "No connection";
            }

            if(arg0[0].length()>0)//不是空白串
            {
                //String target=arg0[0];

                byte[] msgBuffer = arg0[0].getBytes();

                try {
                    //  将msgBuffer中的数据写到outStream对象中
                    outStream.write(msgBuffer);

                } catch (IOException e) {
                    Log.e("error", "ON RESUME: Exception during write.", e);
                    return "Fail";
                }

            }

            return "Success";
        }

    }


    //从蓝牙接收信息的线程
    class ReceiveThread extends Thread
    {

        String buffer="";

        @Override
        public void run() {

            while(btSocket!=null )
            {
                //定义一个存储空间buff
                byte[] buff=new byte[1024];
                try {
                    inStream = btSocket.getInputStream();
                    System.out.println("waitting for instream");
                    inStream.read(buff); //读取数据存储在buff数组中
//                    System.out.println("buff receive :"+buff.length);


                    processBuffer(buff,1024);

                    //System.out.println("receive content:"+ReceiveData);
                } catch (IOException e) {

                    e.printStackTrace();
                }
            }
        }

        private void processBuffer(byte[] buff,int size)
        {
            int length=0;
            for(int i=0;i<size;i++)
            {
                if(buff[i]>'\0')
                {
                    length++;
                }
                else
                {
                    break;
                }
            }

//			System.out.println("receive fragment size:"+length);

            byte[] newbuff=new byte[length];  //newbuff字节数组，用于存放真正接收到的数据

            if (length > 20) {
                for (int j = 0; j < length; j++) {
                    newbuff[j] = buff[j];
                }

//            ReceiveData=ReceiveData+new String(newbuff);
                ReceiveData = new String(newbuff);
                String line[] = ReceiveData.split("\n");
                ReceiveData = line[0];
//                String item[] = ReceiveData.split(" ");
//                Log.e("item", item[1]);
//            Log.e("item3",item[3]);
                times = ReceiveData.substring(2,6);
//            ReceiveData = item[1];
                pressure = ReceiveData.substring(10,14);
                tempretures = ReceiveData.substring(19,23);
                flow = ReceiveData.substring(27,31);
                flows = Double.parseDouble(flow);
                LeakResults = flows * 0.05 + 0.015;
                // 保留小数点后两位
                LeakResult = String.format("%.2f", LeakResults).toString();
                Log.e("Data", ReceiveData);
//			System.out.println("result :"+ReceiveData);
                Message msg = Message.obtain();
                msg.what = 1;
                handler.sendMessage(msg);  //发送消息:系统会自动调用handleMessage( )方法来处理消息
            }else {
                Message msg = Message.obtain();
                msg.what = 2;
                handler.sendMessage(msg);
            }
        }

    }



    //更新界面的Handler类
    class MyHandler extends Handler{

        @Override
        public void handleMessage(Message msg) {

            switch(msg.what){
                case 1:
//                    etReceived.setText(ReceiveData);
                    T.setText(times);
                    AP.setText(pressure);
                    Tc.setText(tempretures);
                    AF.setText(flow);
                    LS.setText(LeakResult);
                    break;
                case 2:
                    break;
            }
        }
    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();

        try {
            if(rThread!=null)
            {

                btSocket.close();
                btSocket=null;

                rThread.join();
            }

            this.finish();

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

}
