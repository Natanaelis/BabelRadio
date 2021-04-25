package com.babelsoft.babelradio;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.support.v7.app.AppCompatActivity;
import org.json.JSONArray;
import org.json.JSONException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class CountriesListActivity extends AppCompatActivity implements IHttpPostAsyncResponse, IImageAsyncResponse {
    private String inputUrl = "https://babelradio.000webhostapp.com/radios.php";
    private String[] flagsUrl = {"https://babelradio.000webhostapp.com/flags.png"};
    private ListView list;
    private SearchView searchView;
    private ProgressBar progressBar;
    public static String databaseResponse;
    private ArrayList<String> listInput = new ArrayList<>();
    private ArrayList<String> imagesOffset = new ArrayList<String>();
    private ArrayList<Bitmap> images = new ArrayList<Bitmap>();
    private JSONArray countriesArray = null;
    private String response;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.list_view);
        progressBar = (ProgressBar)findViewById(R.id.loading);
        list = (ListView) findViewById(R.id.list);
        searchView = (SearchView) findViewById(R.id.search);

        setupActionBar();

        listInput.clear();
        response = ContinentsListActivity.databaseResponse;
        try {
            countriesArray = new JSONArray(response);
            for (int i = 0; i < countriesArray.length(); i++) {
                listInput.add(countriesArray.getJSONObject(i).getString("country_name"));
                imagesOffset.add(countriesArray.getJSONObject(i).getString("flag_offset"));
            }
            downloadFlagsImage();
        } catch (JSONException e) {
            listInput.add(response);
            images.add(BitmapFactory.decodeResource(this.getResources(), R.drawable.error));
            showList();
        }
    }

    private void showList() {
        final ListsAdapter adapter = new ListsAdapter(this, listInput, images);

        progressBar.setVisibility(View.GONE);
        list.setAdapter(adapter);
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

        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                list.setEnabled(false);
                getInput(listInput.get(position));
            }
        });
    }

    private void downloadFlagsImage() {
        progressBar.setMax(imagesOffset.size());
        DownloadImageAsync di = new DownloadImageAsync();
        di.delegate = this;
        di.execute(flagsUrl);
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
        Intent myIntent = new Intent(CountriesListActivity.this, RadiosListActivity.class);
        databaseResponse = asyncResult;
        startActivity(myIntent);
        list.setEnabled(true);
    }

    @Override
    public void imageResult(Bitmap asyncImage) {
        progressBar.setMax(imagesOffset.size());
        for (int i = 0; i < imagesOffset.size(); i++) {
            Bitmap newImage = cropImage(asyncImage, Integer.parseInt(imagesOffset.get(i)) * 2);
            images.add(newImage);
            progressBar.setProgress(i);
        }
        showList();
    }

    private Bitmap cropImage(Bitmap inputImage, int offsetTop) {
        Bitmap newImage = Bitmap.createBitmap(inputImage, 0, offsetTop, 54, 36, null, true);
        return newImage;
    }

    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle("Countries");
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
/*        if(listInput != null) {
            listInput.clear();
            listInput = null;
        }
        if(imagesOffset != null) {
            imagesOffset.clear();
            imagesOffset = null;
        }
        if(images != null) {
            images.clear();
            images = null;
        }
        if(countriesArray != null) {
            countriesArray = null;
        }
        if(response != null) {
            response = null;
        }
        if (databaseResponse != null) {
            databaseResponse = null;
        }
*/
        finish();
        Runtime.getRuntime().gc();
    }
}
