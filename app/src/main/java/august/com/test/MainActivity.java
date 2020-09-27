package august.com.test;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Pair;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.util.Log;
import android.view.Menu;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    // 定义组件
    ArrayAdapter<String> arr_adapter;
    AlertDialog.Builder builder;

    private ViewPager tViewPager;
    private MenuItem menuItem;
    private BottomNavigationView tNavigator;
    private int remainTicks;
    private boolean isLeak;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final ActivityController controller = ActivityController.get();
        controller.bind(this);
        setContentView(R.layout.activity_main);

        final BluetoothConnector connector = BluetoothConnector.get();
        // 判断蓝牙是否打开
        // 开始搜索，获取可连接的蓝牙设备列表
        connector.init();
        if (!connector.isAvailable()) {
            Toast.makeText(this, "No supporting Bluetooth", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        tNavigator = findViewById(R.id.navigation_launch);
        tViewPager = findViewById(R.id.viewpager_launch);

        // 设置缓存页面
        tViewPager.setOffscreenPageLimit(3);

        List<Fragment> fragments = new ArrayList<>();
        fragments.add(new stateFragment());
        fragments.add(new historyFragment());
        fragments.add(new settingFragment());

        FragmentAdapter adapter = new FragmentAdapter(getSupportFragmentManager(), fragments);
        tViewPager.setAdapter(adapter);
        tNavigator.setOnNavigationItemSelectedListener(
                new BottomNavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.navigation_state:
                                tViewPager.setCurrentItem(0);
                                break;
                            case R.id.navigation_history:
                                tViewPager.setCurrentItem(1);
                                break;
                            case R.id.navigation_setting:
                                tViewPager.setCurrentItem(2);
                                break;
                        }

                        return false;
                    }
                });

        // ViewPager 滑动事件监听
        tViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i1) {
            }

            @Override
            public void onPageSelected(int i) {
                if (menuItem != null) {
                    menuItem.setChecked(false);
                } else {
                    tNavigator.getMenu().getItem(0).setChecked(false);
                }
                controller.onFragmentChange(i);
                menuItem = tNavigator.getMenu().getItem(i);
                menuItem.setChecked(true);
            }

            @Override
            public void onPageScrollStateChanged(int i) {

            }
        });

        List<String> names = new ArrayList<String>();
        final List<String> addresses = new ArrayList<String>();
        // 获取已配对的蓝牙设备列表
        ArrayList<Pair<String, String>> addressList = connector.getDevicesList();

        if (addressList.isEmpty()) {
            // 如果没有已配对的蓝牙设备，就弹窗提醒
            builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle("Remind");
            builder.setMessage("Sorry, No paired bluetooth devices now, Please pair the bluetooth device first");
            builder.setPositiveButton("OK", null);
            builder.show();
            Log.e("sorry", "no device bonded");
        }
        for (Pair<String, String> dev : addressList) {
            names.add(dev.first);
            addresses.add(dev.second);
        }
        // 下拉菜单定义一个数组适配器，这个数组适配器就用到之前定义的names，装的都是list所添加的内容
        arr_adapter = new ArrayAdapter<String>(this, R.layout.spinner_item, names);
        // 为适配器设置下拉菜单样式
        arr_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // 以上声明完毕后，加载适配器
        Spinner spinner = this.findViewById(R.id.spinner);
        spinner.setAdapter(arr_adapter);
        // 为下拉列表设置各种点击事件，一响应菜单中的文本item被选中，用setOnItemSelectedListener
        spinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                // 如果app已经连接其他蓝牙的时候，又重新选择了新的设备，那将用stateController断开连接（resetBluetooth())
                StateController stateController = StateController.get();
                stateController.resetBluetooth();
                // 主controller所执行事件
                controller.setAddress(addresses.get(arg2));
                Toast.makeText(getApplicationContext(), "Selected: " + arr_adapter.getItem(arg2), Toast.LENGTH_LONG).show();
            }

            public void onNothingSelected(AdapterView<?> arg0) {
                Toast.makeText(getApplicationContext(), "Nothing", Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    public void onAlertExchangeDialog() {
        final String[] units_item = {"British", "Metric"};
        new AlertDialog.Builder(MainActivity.this)
                .setItems(units_item, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        StateController controller = StateController.get();
                        if (controller.isWorking()) {
                            Toast.makeText(MainActivity.this, "Choose: " + units_item[which], Toast.LENGTH_SHORT).show();
                            controller.setUnit(units_item[which]);
                        } else {
                            Toast tast = Toast.makeText(MainActivity.this, "CONNECT BLUETOOTH FIRST", Toast.LENGTH_LONG);
                            tast.setGravity(Gravity.CENTER, 0, 0);
                            tast.show();
                        }
                    }
                }).create().show();
    }

    public void onAlertAddDialog() {
        View dialogView = View.inflate(MainActivity.this, R.layout.dialog_add_param, null);
        final EditText etA = dialogView.findViewById(R.id.et_A);
        final EditText etB = dialogView.findViewById(R.id.et_B);
        etA.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        etB.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        final StateController controller = StateController.get();
        AlertDialog dialog = new AlertDialog.Builder(MainActivity.this)
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
                            Toast toast = Toast.makeText(MainActivity.this, "ADD FAIL", Toast.LENGTH_LONG);
                            toast.setGravity(Gravity.CENTER, 0, 0);
                            toast.show();
                        } else {
                            controller.insertCalibration(strA, strB);
                            Toast toast = Toast.makeText(MainActivity.this, "SUCCESS", Toast.LENGTH_LONG);
                            toast.setGravity(Gravity.CENTER, 0, 0);
                            toast.show();
                        }
                    }
                }).create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.parseColor("#327475"));
        dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setTextColor(Color.parseColor("#303f9f"));
    }

    public void onAlertDeleteDialog() {
        View dialogView = View.inflate(MainActivity.this, R.layout.dialog_delete_param, null);
        final EditText etdA = dialogView.findViewById(R.id.etd_A);
        final EditText etdB = dialogView.findViewById(R.id.etd_B);
        etdA.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        etdB.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        final StateController controller = StateController.get();
        AlertDialog dialog = new AlertDialog.Builder(MainActivity.this)
                .setView(dialogView)
                .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String strA = etdA.getText().toString();
                        String strB = etdB.getText().toString();
                        if (TextUtils.isEmpty(strA) || TextUtils.isEmpty(strB)) {
                            Toast toast = Toast.makeText(MainActivity.this, "DELETE FAIL", Toast.LENGTH_LONG);
                            toast.setGravity(Gravity.CENTER, 0, 0);
                            toast.show();
                        } else {
                            int deleteCount = controller.deleteCalibration(strA, strB);
                            Toast toast = Toast.makeText(MainActivity.this, "", Toast.LENGTH_LONG);
                            if (deleteCount > 0) {
                                toast.setText("DELETE SUCCESS");
                            } else {
                                toast.setText("DELETE FAIL");
                            }
                            toast.setGravity(Gravity.CENTER, 0, 0);
                            toast.show();
                        }
                    }
                }).create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.parseColor("#d14246"));
        dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setTextColor(Color.parseColor("#303f9f"));
    }

    /**
     * 机器停止运行时的弹窗
     * 并发送off指令
     */
    public void onAlertStopMsg() {
        new AlertDialog.Builder(MainActivity.this)
                .setTitle("MACHINE STOPPED")
//                .setMessage("hi").create().show();
                .setIcon(R.drawable.ic_error_red_24dp)
                .setCancelable(true)
                .setPositiveButton("OFF", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        StateController controller = StateController.get();
                        controller.sendMsg("OFF");
                    }
                }).create().show();
    }

    /**
     * leak check dialog
     */
    public void onAlertLeakCheck() {
        final View dialogView = View.inflate(MainActivity.this, R.layout.dialog_timer, null);
        final StateController controller = StateController.get();
        remainTicks = (int) controller.remainTime();
        final AlertDialog ad = new AlertDialog.Builder(MainActivity.this)
                .setCancelable(false)
                .setView(dialogView)
                .setPositiveButton("OFF", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Toast tast = Toast.makeText(MainActivity.this, "Stop detect" + "\n" + "Stop machine", Toast.LENGTH_LONG);
                        tast.setGravity(Gravity.CENTER, 0, 0);
                        tast.show();
                        controller.sendMsg("OFF");
                    }
                }).create();
        final Timer tickTimer = new Timer();
        final ImageView h = dialogView.findViewById(R.id.hourglass_image);
        TimerTask tickTask = new TimerTask() {
            @Override
            public void run() {
                // isLeak |= controller.isLeak();
                isLeak = false;
                if (controller.isForceStopped() || remainTicks-- <= 0) {
                    // close alert dialog
                    ad.cancel();
                    cancel();
                    tickTimer.cancel();

                    // post result dialog
                    if (!controller.isForceStopped()) {
                        // UI线程都是Looper线程，每个Looper线程中维护一个消息队列
                        Looper.prepare();
                        if (!isLeak) {
                            new AlertDialog.Builder(MainActivity.this)
                                    .setTitle("Leakage Risk")
                                    .setIcon(R.drawable.ic_cancel_red_24dp)
                                    .setMessage("please detect again")
                                    .setCancelable(true)
                                    .create().show();
                        } else {
                            new AlertDialog.Builder(MainActivity.this)
                                    .setTitle("No Leak")
                                    .setIcon(R.drawable.ic_check_circle_green_24dp)
                                    .setCancelable(true)
                                    .create().show();
                        }
                        Looper.loop();
                    }
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String pressure = controller.getPressureKPa();
                        TextView t = dialogView.findViewById(R.id.timer);
                        TextView p = dialogView.findViewById(R.id.current_pressure);
                        if (t != null)
                            t.setText(String.valueOf(remainTicks));
                        if (p != null)
                            p.setText(pressure);
                    }
                });
            }
        };
        Window window = ad.getWindow();
        WindowManager.LayoutParams attributes = window.getAttributes();
        attributes.alpha = 0.6f;
        window.setAttributes(attributes);
        ad.show();
        // 开启hourglass旋转动画
        Animation operatingAnim = AnimationUtils.loadAnimation(MainActivity.this, R.anim.hourglass_image_rotate);
        LinearInterpolator lin = new LinearInterpolator();
        operatingAnim.setInterpolator(lin);
        if (operatingAnim != null) {
            h.startAnimation(operatingAnim);
        }

        tickTimer.schedule(tickTask, 0, 1000);
    }

    public void onAlertChangeBoundDialog(final String type) {
//        View dialogView = View.inflate(MainActivity.this, R.layout.dialog_add_param, null);
        final EditText newBound = new EditText(this);
        newBound.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        final StateController controller = StateController.get();
        AlertDialog dialog = new AlertDialog.Builder(MainActivity.this)
                .setTitle("UPDATE " + type + (type.equals("REMAIN_TIME") ? "" : "BOUND"))
                .setView(newBound)
                .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String input = newBound.getText().toString();
                        if (input.equals("")) {
                            Toast.makeText(getApplicationContext(), "No Change！" + input, Toast.LENGTH_LONG).show();
                        } else {
                            controller.updateDetection(Float.parseFloat(input), type);
                        }
                    }
                }).create();
        dialog.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        final BluetoothConnector connector = BluetoothConnector.get();
        try {
            connector.close();
            this.finish();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
