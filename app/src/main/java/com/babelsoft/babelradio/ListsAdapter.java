package com.babelsoft.babelradio;

import android.app.Activity;
import android.graphics.Bitmap;
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
//    private final Integer[] imgid;
//    private final Integer imgid;
    private final ArrayList<String> listInputBeforeFilter = new ArrayList<>();
    private final ArrayList<String> listInputAfterFilter;
    private final ArrayList<String> tagsBeforeFilter = new ArrayList<>();
    private final ArrayList<String> tagsAfterFilter;
    private final ArrayList<String> streamsBeforeFilter = new ArrayList<>();
    private final ArrayList<String> streamsAfterFilter;
    private final ArrayList<Bitmap> imagesBeforeFilter = new ArrayList<>();
    private final ArrayList<Bitmap> imagesAfterFilter;

//    public ListsAdapter(Activity context, ArrayList<String> listInput, Integer imgid) {
    public ListsAdapter(Activity context, ArrayList<String> listInput, ArrayList<Bitmap> images) {
//        this(context, listInput, null, null, imgid);
        this(context, listInput, null, null, images);
    }

//    public ListsAdapter(Activity context, ArrayList<String> listInput, ArrayList<String> tags, ArrayList<String> streams, Integer imgid) {
    public ListsAdapter(Activity context, ArrayList<String> listInput, ArrayList<String> tags, ArrayList<String> streams, ArrayList<Bitmap> images) {
        super(context, R.layout.list, listInput);
        this.context = context;
//        this.imgid = imgid;

        listInputBeforeFilter.addAll(listInput);
        listInputAfterFilter = listInput;
        imagesBeforeFilter.addAll(images);
        imagesAfterFilter = images;
        if (tags != null) {
            tagsBeforeFilter.addAll(tags);
            streamsBeforeFilter.addAll(streams);
        }
        tagsAfterFilter = tags;
        streamsAfterFilter = streams;
    }

    public View getView(int position, View view, ViewGroup parent) {
        LayoutInflater inflater=context.getLayoutInflater();
        View rowView=inflater.inflate(R.layout.list, null,true);

        TextView titleText = (TextView) rowView.findViewById(R.id.title);
        ImageView imageView = (ImageView) rowView.findViewById(R.id.icon);
        TextView subtitleText = (TextView) rowView.findViewById(R.id.subtitle);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        if(tagsAfterFilter == null) {
            params.setMargins(10, 40, 10, 10);
            subtitleText.setVisibility(TextView.INVISIBLE);
        }
        else {
            params.setMargins(10, 10, 10, 10);
            subtitleText.setVisibility(TextView.VISIBLE);
            subtitleText.setText(tagsAfterFilter.get(position));
        }
        titleText.setLayoutParams(params);
        titleText.setText(listInputAfterFilter.get(position));
//        imageView.setImageResource(imgid);
        imageView.setImageBitmap(imagesAfterFilter.get(position));

        return rowView;
    }

    public void filter(String charText) {
        charText = charText.toLowerCase(Locale.getDefault());
        listInputAfterFilter.clear();
        imagesAfterFilter.clear();
        if (context.getLocalClassName().equals(RadiosListActivity.class.getSimpleName())) {
            tagsAfterFilter.clear();
            streamsAfterFilter.clear();
        }
        if (charText.length() == 0) {
            listInputAfterFilter.addAll(listInputBeforeFilter);
            imagesAfterFilter.addAll(imagesBeforeFilter);
            if (context.getLocalClassName().equals(RadiosListActivity.class.getSimpleName())) {
                tagsAfterFilter.addAll(tagsBeforeFilter);
                streamsAfterFilter.addAll(streamsBeforeFilter);
            }
        }
        else {
            for (int i = 0; i < listInputBeforeFilter.size(); i++) {
                if (listInputBeforeFilter.get(i).toLowerCase(Locale.getDefault()).contains(charText)) {
                    listInputAfterFilter.add(listInputBeforeFilter.get(i));
                    imagesAfterFilter.add(imagesBeforeFilter.get(i));
                    if (context.getLocalClassName().equals(RadiosListActivity.class.getSimpleName())) {
                        tagsAfterFilter.add(tagsBeforeFilter.get(i));
                        streamsAfterFilter.add(streamsBeforeFilter.get(i));
                    }
                }
            }
        }
        notifyDataSetChanged();
    }
}
