package august.com.test;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.graphics.Color;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

enum State {
    DISCONNECT,
    CONNECTING,
    CONNECTED,
    AIR,
    SMOKE
};

public class StateController {
    private static StateController controller;

    protected View view;

    //接收到的字符串
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
    String pressureUnit = "PSI";
    String temperatureUnit = "°F";
    String currentUnit = "British";
    State state = State.DISCONNECT;

    DBHelper helper;

    void insertHistory(String receiveData) {

        // 测试数据，后期删除
        receiveData = "12.23";

        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        String time = dateFormat.format(timestamp);

        SQLiteDatabase db = helper.getReadableDatabase();
        SQLiteStatement statement = db.compileStatement("INSERT INTO history(time, LeakSize) VALUES(?, ?)");
        statement.bindString(1, time);
        statement.bindString(2, receiveData);
        statement.execute();
        statement.close();
    }

    BluetoothConnector.BluetoothDataListener listener = new BluetoothConnector.BluetoothDataListener() {
        @Override
        public void onReceived(String data) {
            updateStatus(data);
        }
    };

    private StateController() {
        BluetoothConnector connector = BluetoothConnector.get();
        connector.registerListener(listener);
    }

    static StateController get() {
        if (controller == null) {
            controller = new StateController();
        }
        return controller;
    }

    void bind(View view) {
        this.view = view;
        ActivityController activityController = ActivityController.get();
        assert activityController.activity != null : "bad activity";
        if (helper == null) {
            helper = new DBHelper(activityController.activity);
        }
    }

    void onConnectClicked() {
        assert view != null : "view not bind";
        ActivityController activityController = ActivityController.get();
        String mAddress = activityController.getAddress();
        BluetoothConnector connector = BluetoothConnector.get();
        connector.postConnectTask(mAddress);
        final Button tOn, tOff, tConnect;
        tConnect = view.findViewById(R.id.button1);
        tOn = view.findViewById(R.id.on);
        tOff = view.findViewById(R.id.off);
        final TextView tStatus = view.findViewById(R.id.textView1);

        if (tConnect == null || tOn == null || tOff == null) {
            Log.e("error", "failed to find button");
            return;
        }
        state = State.CONNECTING;
        tStatus.setText("Connecting");
        tConnect.setEnabled(false);
    }

    void setConnectButton(String result) {
        boolean succeed = (result.compareTo("Connected") == 0);
        final Button tOn, tOff, tConnect;
        tConnect = view.findViewById(R.id.button1);
        tOn = view.findViewById(R.id.on);
        tOff = view.findViewById(R.id.off);
        final TextView tStatus = view.findViewById(R.id.textView1);
        if (tConnect == null || tOn == null || tOff == null) {
            Log.e("error", "failed to find button");
            return;
        }
        if (succeed) {
            tConnect.setEnabled(true);
            tConnect.setActivated(true);
            tOn.setEnabled(true);
            tOff.setEnabled(true);
            tConnect.setText("DISCONNECT");
            state = State.CONNECTED;
        } else {
            tConnect.setActivated(false);
            tConnect.setEnabled(true);
            tConnect.setText("CONNECT");
            tOn.setActivated(false);
            tOn.setEnabled(false);
            tOff.setEnabled(false);
            tStatus.setText("No Connection");
            state = State.DISCONNECT;
        }
        tStatus.setText(result);
    }

    void onStatusChanged(String result) {
        assert view != null : "view not bind";
        TextView tStatusLabels = view.findViewById(R.id.textView1);
        tStatusLabels.setText(result);
    }

    void setTextViewHelper(int id, String val, String def) {
        if (view == null)
            return;
        final TextView textView = view.findViewById(id);
        if (textView != null) {
            textView.setText((val == null) ? def : val);
        } else {
            Log.e("error", "failed to find button");
        }
    }

    void setPanelData(String T, String AP, String Tc, String AF, String LS, String PrUnit, String TeUnit, String Status) {
        assert view != null : "view not bind";
        setTextViewHelper(R.id.stoppedTime, T, "No Sensor");
        setTextViewHelper(R.id.press, AP, "No Sensor");
        setTextViewHelper(R.id.degree, Tc, "No Sensor");
        setTextViewHelper(R.id.flowData, AF, "No Sensor");
        setTextViewHelper(R.id.result, LS, "No Sensor");
        setTextViewHelper(R.id.PRunit, PrUnit, "PSI");
        setTextViewHelper(R.id.TEunit, TeUnit, "°F");
        setTextViewHelper(R.id.textView1, Status, "Bad Status");
    }

    void setPanelData() {
        setPanelData("0.00", "00.0", "00.0", "0.00", "0.00", "PSI", "°F", "No Connection");
    }

    void updateStatus(String ReceiveData) {
        String line[] = ReceiveData.split("\n");
        ReceiveData = line[0];
        System.out.println(ReceiveData);
        String pureData = ReceiveData.replaceAll("\\s", "");
        pureData = pureData.replaceAll("[A-z]{1,2}?[:]|[T]", ",");
        System.out.println(pureData);
        String item[] = pureData.split(",");
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
        switch (currentUnit) {
            case "British":
                pressureUnit = "PSI";
                temperatureUnit = " °F";
                break;
            case "Metric":
                String[] result = UnitExchange.British2Metric(pressure, temperature);
                pressureUnit = "KPA";
                temperatureUnit = " ℃";
                pressure = result[0];
                temperature = result[1];
                break;
        }
        setPanelData(times, pressure, temperature, flow, LeakResult, pressureUnit, temperatureUnit, "Connected");
    }

    void resetBluetooth() {
        assert view != null : "view not bind";
        BluetoothConnector connector = BluetoothConnector.get();
        if (state == State.CONNECTED || state == State.CONNECTING) {
            Log.i("msg", "disconnect bluetooth");
            state = State.DISCONNECT;
            setConnectButton("");
            connector.reset();
        }
        setPanelData();
        setConnectButton("No Connection");
        setAir8SmokeLayout(state);
    }

    // 设置发送ON和OFF
    public void sendMsg(String Msg) {
        BluetoothConnector connector = BluetoothConnector.get();
        switch (Msg) {
            case "ON":
                switch (state) {
                    case CONNECTED:
                        state = State.AIR;
                        break;
                    case AIR:
                        state = State.SMOKE;
                        break;
                    default:
                        // alert
                        break;
                }
                break;
            case "OFF":
                switch (state) {
                    case SMOKE:
                        insertHistory(connector.getReceiveData());
                        state = State.CONNECTED;
                        break;
                    default:
                        state = connector.isConnected() ? State.CONNECTED : State.DISCONNECT;
                        break;
                }
                break;
        }
        setAir8SmokeLayout(state);
        connector.postMessageTask(Msg);
    }

    public void setAir8SmokeLayout(State state) {
        LinearLayout tAIR, tSMOKE;
        // 赋值air和smoke两个linear layout模块
        tAIR = view.findViewById(R.id.air);
        tSMOKE = view.findViewById(R.id.smoke);
        Button tOn = view.findViewById(R.id.on);
        switch (state) {
            case AIR:
                tAIR.setBackgroundColor(Color.parseColor("#327475"));
                break;
            case SMOKE:
                tAIR.setBackgroundColor(Color.parseColor("#327475"));
                tSMOKE.setBackgroundColor(Color.parseColor("#327475"));
                tOn.setActivated(true);
                break;
            default:
                tOn.setActivated(false);
                tAIR.setBackgroundColor(Color.parseColor("#d14246"));
                tSMOKE.setBackgroundColor(Color.parseColor("#d14246"));
                break;
        }
    }

    public void setUnit(String unit) {
        currentUnit = unit;
        for (SettingFragmentChangeListener sfl : settingFragmentChangeListenerArrayList) {
            sfl.update();
        }
    }


    interface SettingFragmentChangeListener {
        void update();
    }

    ArrayList<SettingFragmentChangeListener> settingFragmentChangeListenerArrayList = new ArrayList<SettingFragmentChangeListener>();

    void registerSettingFragmentChangeListener(SettingFragmentChangeListener listener) {
        if (settingFragmentChangeListenerArrayList.indexOf(listener) < 0) {
            settingFragmentChangeListenerArrayList.add(listener);
        }
    }
}