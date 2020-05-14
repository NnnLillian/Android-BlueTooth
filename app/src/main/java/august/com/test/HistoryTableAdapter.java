package august.com.test;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

public class HistoryTableAdapter extends BaseAdapter {

    private List<History> list;
    private LayoutInflater inflater;

    public HistoryTableAdapter(Context context, List<History> list) {
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

        History history = (History) this.getItem(position);

        ViewHolder viewHolder;

        if (convertView == null) {

            viewHolder = new ViewHolder();

            convertView = inflater.inflate(R.layout.list_item, null);
            viewHolder.time = convertView.findViewById(R.id.time);
            viewHolder.LeakSize = convertView.findViewById(R.id.leakSize);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        viewHolder.time.setText(history.getTime());
        viewHolder.time.setTextSize(16);
        viewHolder.LeakSize.setText(history.getLeakSize());
        viewHolder.LeakSize.setTextSize(16);

        return convertView;
    }

    public static class ViewHolder {
        public TextView time;
        public TextView LeakSize;
    }

}
