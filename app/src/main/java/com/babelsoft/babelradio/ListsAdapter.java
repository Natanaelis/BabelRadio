package com.babelsoft.babelradio;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Locale;

public class ListsAdapter extends ArrayAdapter<String> {

    private final Activity context;
//    private final ArrayList<String> listInput;
    private final ArrayList<String> subtitle;
//    private final Integer[] imgid;
    private final Integer imgid;
    private final ArrayList<String> listBeforeFilter = new ArrayList<>();
    private final ArrayList<String> listAfterFilter;

    public ListsAdapter(Activity context, ArrayList<String> listInput, Integer imgid) {
        this(context, listInput, null, imgid);
    }

    public ListsAdapter(Activity context, ArrayList<String> listInput, ArrayList<String> subtitle, Integer imgid) {
        super(context, R.layout.list, listInput);
        this.context = context;
//        this.listInput = listInput;
        this.subtitle = subtitle;
        this.imgid = imgid;

        listBeforeFilter.addAll(listInput);
        listAfterFilter = listInput;
    }

    public View getView(int position, View view, ViewGroup parent) {
        LayoutInflater inflater=context.getLayoutInflater();
        View rowView=inflater.inflate(R.layout.list, null,true);

        TextView titleText = (TextView) rowView.findViewById(R.id.title);
        ImageView imageView = (ImageView) rowView.findViewById(R.id.icon);
        TextView subtitleText = (TextView) rowView.findViewById(R.id.subtitle);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        if(subtitle == null) {
            params.setMargins(10, 40, 10, 10);
            subtitleText.setVisibility(TextView.INVISIBLE);
        }
        else {
            params.setMargins(10, 10, 10, 10);
            subtitleText.setVisibility(TextView.VISIBLE);
            subtitleText.setText(subtitle.get(position));
        }
        titleText.setLayoutParams(params);
        titleText.setText(listAfterFilter.get(position));
        imageView.setImageResource(imgid);

        return rowView;
    }

    public void filter(String charText) {
        charText = charText.toLowerCase(Locale.getDefault());
        listAfterFilter.clear();
        if (charText.length() == 0) {
            listAfterFilter.addAll(listBeforeFilter);
        } else {
            for (String item : listBeforeFilter) {
                if (item.toLowerCase(Locale.getDefault()).contains(charText)) {
                    listAfterFilter.add(item);
                }
            }
        }
        notifyDataSetChanged();
    }
}
