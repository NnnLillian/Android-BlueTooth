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
import android.widget.ListView;
import java.util.ArrayList;
import java.util.List;

public class historyFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.history_main, container, false);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        ActivityController.FragmentChangeListener listener = new ActivityController.FragmentChangeListener() {
            @Override
            public void updateFragment(int i) {
                updateTable();
            }
        };
        ActivityController controller = ActivityController.get();
        controller.registerFragmentChangeListener(listener);

    }

    void updateTable() {
        DBHelper helper = new DBHelper(getActivity());
        SQLiteDatabase db = helper.getReadableDatabase();

        Cursor cursor = db.rawQuery("select * from history order by time DESC limit 10;", null);
        if (cursor.getCount() <= 0) {
            return;
        }
        List<History> list = new ArrayList<History>();
        while (cursor.moveToNext()) {
            String time, LeakSize;
            time = cursor.getString(0);
            LeakSize = cursor.getString(1);
            list.add(new History(time, LeakSize));
        }
        ListView tableListView = getView().findViewById(R.id.list);
        HistoryTableAdapter adapter = new HistoryTableAdapter(getActivity(), list);
        tableListView.setAdapter(adapter);
    }
}
