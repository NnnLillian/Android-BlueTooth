package august.com.test;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class settingFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.settings_mian, container, false);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        StateController stateController = StateController.get();

        // 页面切换监听器
        ActivityController.FragmentChangeListener listener = new ActivityController.FragmentChangeListener() {
            @Override
            public void updateFragment(int i) {
                updateUnit();
                getTable();
                updateBound();
            }
        };
        final ActivityController controller = ActivityController.get();
        controller.registerFragmentChangeListener(listener);

        final MainActivity mainActivity = (MainActivity) getActivity();
        final ImageButton btnExchangeUnit = getView().findViewById(R.id.exchangeUnit);
        btnExchangeUnit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mainActivity.onAlertExchangeDialog();
            }
        });

        final ImageButton btnAdd = getView().findViewById(R.id.add_calibration);
        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mainActivity.onAlertAddDialog();
            }
        });
        final ImageButton btnDelete = getView().findViewById(R.id.delete_calibration);
        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mainActivity.onAlertDeleteDialog();
            }
        });

        // 数据变化监听器
        StateEventListener stateEventListener = new StateEventListener() {
            @Override
            void onUpdate() {
                updateUnit();
                getTable();
                updateBound();
            }
        };
        stateController.registerStateEventListener(stateEventListener);

        final ImageButton btnUpBound = getView().findViewById(R.id.upper_bound_btn);
        btnUpBound.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mainActivity.onAlertChangeBoundDialog("UPPER");
            }
        });
        final ImageButton btnLowBound = getView().findViewById(R.id.lower_bound_btn);
        btnLowBound.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mainActivity.onAlertChangeBoundDialog("LOWER");
            }
        });
        final ImageButton btnRemainTime = getView().findViewById(R.id.remain_time_btn);
        btnRemainTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mainActivity.onAlertChangeBoundDialog("REMAIN_TIME");
            }
        });
    }

    void updateUnit() {
        TextView tvUnit = getView().findViewById(R.id.currentUnit);
        StateController stateController = StateController.get();
        tvUnit.setText(stateController.currentUnit);
    }

    void getTable() {
        StateController stateController = StateController.get();
        SQLiteDatabase db = stateController.helper.getReadableDatabase();

        Cursor cursor = db.rawQuery("select * from calibration;", null);
        if (cursor.getCount() <= 0) {
            return;
        }
        List<Calibration> list = new ArrayList<Calibration>();
        while (cursor.moveToNext()) {
            String pressureValue, leakSizeValue;
            pressureValue = cursor.getString(0);
            leakSizeValue = cursor.getString(1);
            list.add(new Calibration(pressureValue, leakSizeValue));
        }
        ListView tableListView = getView().findViewById(R.id.calibration_list);
        CalibrationTableAdapter adapter = new CalibrationTableAdapter(getActivity(), list);
        tableListView.setAdapter(adapter);
    }

    void updateBound() {
        StateController stateController = StateController.get();
//        stateController.restartDetection();
        TextView upperBound = getView().findViewById(R.id.pressure_upper_bound);
        TextView lowerBound = getView().findViewById(R.id.pressure_lower_bound);
        TextView remainTime = getView().findViewById(R.id.remain_time);

        upperBound.setText(String.format("%.2f kPa", stateController.pressureUpperBound()));
        lowerBound.setText(String.format("%.2f kPa", stateController.pressureLowerBound()));
        remainTime.setText(String.format("%d(s)", (int)stateController.remainTime()));
    }
}
