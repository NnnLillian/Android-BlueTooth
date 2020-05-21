package august.com.test;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.BaseAdapter;

import java.util.List;

public class CalibrationTableAdapter extends BaseAdapter {

    private List<Calibration> list;
    private LayoutInflater inflater;

    public CalibrationTableAdapter(Context context, List<Calibration> list) {
        this.list = list;
        inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        int ret = 0;
        if (list != null) {
            ret = list.size();
        }
        return ret;
    }

    @Override
    public Object getItem(int position) {
        return list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        Calibration calibration = (Calibration) this.getItem(position);

        CalibrationTableAdapter.ViewHolder viewHolder;

        if (convertView == null) {

            viewHolder = new CalibrationTableAdapter.ViewHolder();

            convertView = inflater.inflate(R.layout.calibration_item, null);
            viewHolder.pressValue = convertView.findViewById(R.id.pressureValue);
            viewHolder.leakSizeValue = convertView.findViewById(R.id.leakSizeValue);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (CalibrationTableAdapter.ViewHolder) convertView.getTag();
        }

        viewHolder.pressValue.setText(calibration.getPressureValue());
        viewHolder.pressValue.setTextSize(16);
        viewHolder.leakSizeValue.setText(calibration.getLeakSizeValue());
        viewHolder.leakSizeValue.setTextSize(16);

        return convertView;
    }

    public static class ViewHolder {
        public TextView pressValue;
        public TextView leakSizeValue;
    }

}
