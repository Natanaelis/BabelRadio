package com.babelsoft.babelradio;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SearchView;
import android.support.v7.app.AppCompatActivity;
import org.json.JSONArray;
import org.json.JSONException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class RadiosActivity extends AppCompatActivity implements IHttpPostAsyncResponse {
//    String radiosUrl = "https://babelradio.000webhostapp.com/countries.php";
    ListView listView;
    SearchView searchView;
//    ArrayList<String> radios = new ArrayList<>();
    static ArrayList<String> listInput = new ArrayList<>();
    ArrayList<String> tags = new ArrayList<>();


//    Integer[] imgid = {R.drawable.worldwide, R.drawable.worldwide, R.drawable.worldwide, R.drawable.worldwide,
//            R.drawable.worldwide};
    Integer imgid = R.drawable.worldwide;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        JSONArray radiosArray;
        String response = CountriesActivity.databaseResponse;
        try {
            radiosArray = new JSONArray(response);
            for(int i = 0; i < radiosArray.length(); i++) {
                listInput.add(radiosArray.getJSONObject(i).getString("radio_name"));
                tags.add(radiosArray.getJSONObject(i).getString("radio_tag"));
            }
            initiateView();
        } catch (JSONException e) {
            listInput.add(response);
            tags.add(" ");
            imgid = R.drawable.error;
            initiateView();
        }
    }

    private void initiateView() {
        setContentView(R.layout.list_view);

        final ListsAdapter adapter = new ListsAdapter(this, listInput, tags, imgid);
        listView = (ListView)findViewById(R.id.list);
        searchView = (SearchView)findViewById(R.id.search);

        listView.setAdapter(adapter);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                adapter.filter(newText);
                return false;
            }
        });

        getWindow().getDecorView().setBackgroundColor(Color.DKGRAY);


        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//                getRadios(listInput.get(position));
            }
        });
    }

    private void getRadios(String radio) {
        Map<String, String> postData = new HashMap<>();
        postData.put("radio", radio);
        HttpPostAsync httpPost = new HttpPostAsync(postData);
        httpPost.delegate = this;
//        httpPost.execute(radiosUrl);
    }


    @Override
    public void postResult(String asyncResult) {

    }
}
