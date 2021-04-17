package com.babelsoft.babelradio;

import android.content.Intent;
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

public class CountriesActivity extends AppCompatActivity implements IHttpPostAsyncResponse {
    String inputUrl = "https://babelradio.000webhostapp.com/radios.php";
    ListView list;
    SearchView searchView;
//    static ArrayList<String> countries = new ArrayList<>();
    static ArrayList<String> listInput = new ArrayList<>();
    static String databaseResponse;

    /*

    String[] subtitle ={
            "Sub Title 1","Sub Title 2",
            "Sub Title 3","Sub Title 4",
            "Sub Title 5",
    };
*/
//    Integer[] imgid = {R.drawable.worldwide, R.drawable.worldwide, R.drawable.worldwide, R.drawable.worldwide,
//            R.drawable.worldwide};
    Integer imgid = R.drawable.worldwide;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        JSONArray countriesArray;
        String response = ContinentsActivity.databaseResponse;
        try {
            countriesArray = new JSONArray(response);
            for(int i = 0; i < countriesArray.length(); i++) {
                listInput.add(countriesArray.getJSONObject(i).getString("country_name"));
            }
            initiateView();
        } catch (JSONException e) {
            listInput.add(response);
            imgid = R.drawable.error;
            initiateView();
        }
    }

    private void initiateView() {
        setContentView(R.layout.list_view);

        final ListsAdapter adapter = new ListsAdapter(this, listInput, imgid);
        list = (ListView)findViewById(R.id.list);
        searchView = (SearchView)findViewById(R.id.search);

        list.setAdapter(adapter);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
//                adapter.getFilter().filter(newText);
                adapter.filter(newText);
                return false;
            }
        });

        getWindow().getDecorView().setBackgroundColor(Color.DKGRAY);

        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                getInput(listInput.get(position));
            }
        });
    }

    private void getInput(String input) {
        Map<String, String> postData = new HashMap<>();
        postData.put("country", input);
        HttpPostAsync httpPost = new HttpPostAsync(postData);
        httpPost.delegate = this;
        httpPost.execute(inputUrl);
    }

    @Override
    public void postResult(String asyncResult) {
        Intent myIntent = new Intent(CountriesActivity.this, RadiosActivity.class);
        databaseResponse = asyncResult;
        CountriesActivity.this.startActivity(myIntent);
    }
}
