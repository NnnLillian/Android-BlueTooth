package august.com.test;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
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
    Button btnConnect, btnSend, btnQuit, btnOn, btnOff;
    EditText etReceived, etSend;
    TextView T, AP, Tc, AF, LS, PRunit, TEunit, ParamAshow, ParamBshow;
    Toolbar toolbar;
    Spinner spinner;
    List<String> data_list;
    List<String> address_list;
    ArrayAdapter<String> arr_adapter;
    AlertDialog.Builder builder;

    //device var
    private BluetoothAdapter mBluetoothAdapter = null;

    private BluetoothSocket btSocket = null;

    private OutputStream outStream = null;

    private InputStream inStream = null;

    //这条是蓝牙串口通用的UUID，不要更改
    private static final UUID MY_UUID =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

//    private static String address = "00:0C:BF:17:66:0F"; // <==要连接的目标蓝牙设备MAC地址

    String address = "";

    private ReceiveThread rThread = null;  //数据接收线程

    private ConnectTask task;

    //接收到的字符串
    String ReceiveData = "";
    String times = "";
    String pressure = "";
    String temperature = "";
    String flow = "";
    double flows;
    //计算得出的字符串
    String LeakResult = "";
    double LeakResults;
    //计算参数
    double paramA = 0.05;
    double paramB = 0.015;
    String ON = "ON";
    String OFF = "OFF";
    String pressureUnit = "KPA";
    String temperatureUnit = " ℃";
    final String[] units_item = {"British", "Metric"};
    String current_unit = "Metric";
    Boolean btn_click = false;
    Boolean connect_click = false;
    Boolean enable_click = false;
    Boolean is_disconnect = false;  //代表connect按钮是否变为disconnect
    MyHandler handler;


    // 关于底部栏的声明
    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_data:
                    View dialogView = View.inflate(MainActivity.this, R.layout.dialog_change_param, null);
                    final EditText etA = dialogView.findViewById(R.id.et_A);
                    final EditText etB = dialogView.findViewById(R.id.et_B);
                    etA.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                    etB.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                    ParamAshow = dialogView.findViewById(R.id.cur_paramA);
                    ParamBshow = dialogView.findViewById(R.id.cur_paramB);
                    ParamAshow.setText(paramA + "");
                    ParamBshow.setText(paramB + "");
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("Parameter Change\n")
                            .setView(dialogView)
                            .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            })
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    String strA = etA.getText().toString();
                                    String strB = etB.getText().toString();
                                    if (TextUtils.isEmpty(strA) || TextUtils.isEmpty(strB)) {
                                        Toast toast = Toast.makeText(MainActivity.this, "Arguments unchanged", Toast.LENGTH_LONG);
                                        toast.setGravity(Gravity.CENTER, 0, 0);
                                        toast.show();
                                    } else {
                                        paramA = Double.parseDouble(strA);
                                        paramB = Double.parseDouble(strB);
                                        System.out.println(paramA + "---------" + paramB);
                                    }
                                }
                            }).create().show();
                    return true;
                case R.id.navigation_state:
                    statusLabel.setText("dashboard");
                    return true;
                case R.id.navigation_setting:
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("Which one?")
                            .setItems(units_item, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if (is_disconnect) {
                                        Toast.makeText(MainActivity.this, "Choose: " + units_item[which], Toast.LENGTH_SHORT).show();
                                        current_unit = units_item[which];
                                    } else {
                                        Toast tast = Toast.makeText(MainActivity.this, "CONNECT BLUETOOTH FIRST", Toast.LENGTH_LONG);
                                        tast.setGravity(Gravity.CENTER, 0, 0);
                                        tast.show();
                                    }
                                }
                            }).create().show();
                    return true;
            }
            return false;
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //首先调用初始化函数
        Init();
        InitBluetooth();

        handler = new MyHandler();

        //判断蓝牙是否打开
        if (!mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.enable();
        }
        mBluetoothAdapter.startDiscovery();

        data_list = new ArrayList<String>();
        address_list = new ArrayList<String>();

        // 寻找已经配对设备
        Set<BluetoothDevice> devices = mBluetoothAdapter.getBondedDevices();
        if (devices.size() > 0) {
            for (Iterator<BluetoothDevice> it = devices.iterator(); it.hasNext(); ) {
                BluetoothDevice device = (BluetoothDevice) it.next();
                data_list.add(device.getName());
                address_list.add(device.getAddress());
                Log.e("设备：", "[" + device.getName() + "]" + ":" + device.getAddress());
            }
        } else {
            builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle("Remind");
            builder.setMessage("Sorry, No paired bluetooth devices now, Please pair the bluetooth device first");
            builder.setPositiveButton("OK", null);
            builder.show();
            Log.e("sorry", "no device bonded");
        }

        // 下拉菜单定义一个数组适配器，这个数组适配器就用到之前定义的data_list，装的都是list所添加的内容
        arr_adapter = new ArrayAdapter<String>(this, R.layout.spinner_item, data_list);
        // 为适配器设置下来菜单样式
        arr_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // 以上声明完毕后，加载适配器
        spinner.setAdapter(arr_adapter);
        // 为下拉列表设置各种点击事件，一响应菜单中的文本item被选中，用setOnItemSelectedListener
        spinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                // TODO Auto-generated method stub
                // 所执行事件
                address = address_list.get(arg2);
                Log.e("address: ", address);
                Toast.makeText(getApplicationContext(), "选择的是" + arr_adapter.getItem(arg2), Toast.LENGTH_LONG).show();
            }

            public void onNothingSelected(AdapterView<?> arg0) {
                Toast.makeText(getApplicationContext(), "nothing", Toast.LENGTH_LONG).show();
            }
        });

        /**
         * 原始，有两个按钮时候的connect和disconnect（quit）
         *
         btnConnect.setOnClickListener(new View.OnClickListener() {
        @Override public void onClick(View v) {
        if (!btn_click) {
        if (address != null) {
        //创建连接
        task = new ConnectTask();
        task.execute(address);
        statusLabel.setText("Connecting");
        btnConnect.setEnabled(btn_click);
        } else {
        statusLabel.setText("Nothing be selected");
        }
        } else {
        Toast.makeText(getApplicationContext(), "Don't click repeatedly", Toast.LENGTH_LONG).show();
        }

        }
        });


         btnQuit.setOnClickListener(new View.OnClickListener() {

        @Override public void onClick(View v) {
        // TODO Auto-generated method stub
        task.cancel(true);
        btnConnect.setActivated(false);
        btnConnect.setEnabled(true);
        btnOn.setActivated(false);
        btnOn.setEnabled(false);
        btnOff.setEnabled(false);
        btn_click = false;
        if (btSocket != null) {
        try {
        btSocket.close();
        btSocket = null;
        if (rThread != null) {
        rThread.join();
        }
        statusLabel.setText("No Connection");

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
         */

        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                System.out.println("~~~~~~~~~");
                if (is_disconnect) {
                    {
                        is_disconnect = false;
                        task.cancel(true);
                        btnConnect.setActivated(false);
                        btnConnect.setEnabled(true);
                        btnConnect.setText("CONNECT");
                        btnOn.setActivated(false);
                        btnOn.setEnabled(false);
                        btnOff.setEnabled(false);
                        btn_click = false;
                        if (btSocket != null) {
                            try {
                                btSocket.close();
                                btSocket = null;
                                if (rThread != null) {
                                    rThread.join();
                                }
                                statusLabel.setText("No Connection");

                                T.setText("0.00");
                                AP.setText("00.0");
                                Tc.setText("00.0");
                                AF.setText("0.00");
                                LS.setText("0.00");
                            } catch (IOException e) {
                                e.printStackTrace();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                } else {
                    if (!btn_click) {
                        if (address != null) {
                            //创建连接
                            task = new ConnectTask();
                            task.execute(address);
                            statusLabel.setText("Connecting");
                            btnConnect.setEnabled(btn_click);
                        } else {
                            statusLabel.setText("Nothing be selected");
                        }
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
                if (!connect_click) {
                    if (enable_click) {
                        connect_click = true;
                        new SendInfoTask().execute(ON);
                        btnOn.setActivated(connect_click);
                    } else {
                        statusLabel.setText("Illegal!\nPress Disconnect\nSelect again");
                    }

                }

            }
        });
        btnOff.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                if (connect_click) {
                    connect_click = false;
                    new SendInfoTask().execute(OFF);
                    btnOn.setActivated(connect_click);
                }

            }
        });

        // 监听底部栏目
        BottomNavigationView navigation = findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
    }

    public void Init() {
        statusLabel = (TextView) this.findViewById(R.id.textView1);
        T = (TextView) this.findViewById(R.id.stoppedTime);
        AP = (TextView) this.findViewById(R.id.press);
        Tc = (TextView) this.findViewById(R.id.degree);
        AF = (TextView) this.findViewById(R.id.flowData);
        LS = (TextView) this.findViewById(R.id.result);
        PRunit = this.findViewById(R.id.PRunit);
        TEunit = this.findViewById(R.id.TEunit);
        btnConnect = (Button) this.findViewById(R.id.button1);
//        btnSend=(Button)this.findViewById(R.id.button2);
        btnOn = (Button) this.findViewById(R.id.on);
        btnOff = (Button) this.findViewById(R.id.off);
//        btnQuit = (Button) this.findViewById(R.id.button3);
        toolbar = (Toolbar) this.findViewById(R.id.toolbar);
        spinner = (Spinner) this.findViewById(R.id.spinner);
//        etSend=(EditText)this.findViewById(R.id.editText1);
//        etReceived=(EditText)this.findViewById(R.id.editText2);

        btnOn.setEnabled(enable_click);
        btnOff.setEnabled(enable_click);
    }

    public void InitBluetooth() {
        //得到一个蓝牙适配器
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
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
    class ConnectTask extends AsyncTask<String, String, String> {
        @Override
        protected String doInBackground(String... params) {
            // TODO Auto-generated method stub
            BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(params[0]);
            try {
                btSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
                btSocket.connect();
                Log.e("message", "ON RESUME: BT connection established, data transfer link open.");
            } catch (IOException e) {
                e.printStackTrace();
                try {
                    btSocket.close();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            btn_click = true;
                            btnConnect.setEnabled(true);
                        }
                    });
                    return "Socket succeeded  Connect fail";
                } catch (IOException e2) {
                    Log.e("error", "ON RESUME: Unable to close socket during connection failure", e2);
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
            enable_click = true;
            // 更改button样式
            is_disconnect = true;
            Message msg = Message.obtain();
            msg.what = 3;
            handler.sendMessage(msg);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    btnConnect.setActivated(true);
                    btnOn.setEnabled(true);
                    btnOff.setEnabled(true);
                }
            });
            return "Connected";
        }

        @Override    //这个方法是在主线程中运行的，所以可以更新界面
        protected void onPostExecute(String result) {
            // TODO Auto-generated method stub

            //连接成功则启动监听
            rThread = new ReceiveThread();

            rThread.start();

            statusLabel.setText(result);

            super.onPostExecute(result);
        }


    }

    //发送数据到蓝牙设备的异步任务
    class SendInfoTask extends AsyncTask<String, String, String> {

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

            if (btSocket == null) {
                return "No Connection";
            }

            if (arg0[0].length() > 0)//不是空白串
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
    class ReceiveThread extends Thread {

        String buffer = "";

        @Override
        public void run() {

            while (btSocket != null) {
                //定义一个存储空间buff
                byte[] buff = new byte[1024];
                try {
                    inStream = btSocket.getInputStream();
                    System.out.println("waitting for instream");
                    inStream.read(buff); //读取数据存储在buff数组中
//                    System.out.println("buff receive :"+buff.length);


                    processBuffer(buff, 1024);

                    //System.out.println("receive content:"+ReceiveData);
                } catch (IOException e) {

                    e.printStackTrace();
                }
            }
        }

        private void processBuffer(byte[] buff, int size) {
            int length = 0;
            for (int i = 0; i < size; i++) {
                if (buff[i] > '\0') {
                    length++;
                } else {
                    break;
                }
            }

//			System.out.println("receive fragment size:"+length);

            byte[] newbuff = new byte[length];  //newbuff字节数组，用于存放真正接收到的数据

            if (length > 20) {
                for (int j = 0; j < length; j++) {
                    newbuff[j] = buff[j];
                }

//            ReceiveData=ReceiveData+new String(newbuff);
                System.out.println(newbuff);
                ReceiveData = new String(newbuff);
                String line[] = ReceiveData.split("\n");
                ReceiveData = line[0];
                System.out.println(ReceiveData);
                String pureData = ReceiveData.replaceAll("\\s", "");
                pureData = pureData.replaceAll("[A-z]{1,2}?[:]|[T]", ",");
                System.out.println(pureData);
                String item[] = pureData.split(",");
                for (int i = 0; i < item.length; i++) {
                    if (item[i].trim().isEmpty()) {
                        item[i] = "No Sensor";
                    }
                }
                times = item[1];
                pressure = item[2];
                temperature = item[3];
                flow = item[4];
                flows = Double.parseDouble(flow);
                LeakResults = flows * paramA + paramB;
                // 保留小数点后两位
                LeakResult = String.format("%.2f", LeakResults);
                Log.e("Data", ReceiveData);

                // 英式与公式换算
                switch (current_unit) {
                    case "British":
                        String[] result = UnitExchange.Metric2British(pressure, temperature);
                        pressureUnit = "PSI";
                        temperatureUnit = " °F";
                        pressure = result[0];
                        temperature = result[1];
                        break;
                    case "Metric":
                        pressureUnit = "KPA";
                        temperatureUnit = " ℃";
                        break;
                }


                Message msg = Message.obtain();
                msg.what = 1;
                handler.sendMessage(msg);  //发送消息:系统会自动调用handleMessage( )方法来处理消息
            } else {
                Message msg = Message.obtain();
                msg.what = 2;
                handler.sendMessage(msg);
            }
        }

    }


    //更新界面的Handler类
    class MyHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {
                case 1:
//                    etReceived.setText(ReceiveData);
                    T.setText(times);
                    AP.setText(pressure);
                    Tc.setText(temperature);
                    AF.setText(flow);
                    LS.setText(LeakResult);
                    PRunit.setText(pressureUnit);
                    TEunit.setText(temperatureUnit);
                    break;
                case 2:
                    break;
                case 3:
                    btnConnect.setText("DISCONNECT");
                    btnConnect.setActivated(is_disconnect);
                    btnConnect.setEnabled(true);
                    break;
            }
        }
    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();

        try {
            if (rThread != null) {

                btSocket.close();
                btSocket = null;

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
