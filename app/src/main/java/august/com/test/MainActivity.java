package august.com.test;

import android.app.AlertDialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Pair;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.util.Log;
import android.view.Menu;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    // 定义组件
    ArrayAdapter<String> arr_adapter;
    AlertDialog.Builder builder;

    private ViewPager tViewPager;
    private MenuItem menuItem;
    private BottomNavigationView tNavigator;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final ActivityController controller = ActivityController.get();
        controller.bind(this);
        setContentView(R.layout.activity_main);

        final BluetoothConnector connector = BluetoothConnector.get();
        // 判断蓝牙是否打开
        // 获取待连接蓝牙设备列表
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
        ArrayList<Pair<String, String>> addressList = connector.getDevicesList();

        // 寻找已经配对设备
        if (addressList.isEmpty()) {
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
        // 为适配器设置下来菜单样式
        arr_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // 以上声明完毕后，加载适配器
        Spinner spinner = this.findViewById(R.id.spinner);
        spinner.setAdapter(arr_adapter);
        // 为下拉列表设置各种点击事件，一响应菜单中的文本item被选中，用setOnItemSelectedListener
        spinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                StateController stateController = StateController.get();
                stateController.resetBluetooth();
                // 所执行事件
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
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
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
