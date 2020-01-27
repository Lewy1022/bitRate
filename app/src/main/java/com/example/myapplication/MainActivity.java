package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener
{
final String GDAX_WS_URL ="wss://ws-feed.pro.coinbase.com";

final String TAG = "GDAX";
    WebSocketClient webSocketClientUSD ;
    String currency ="BTC-USD";
    String currencySymbol = "$";
    boolean webSocketRunningFlag = false;



    String GDAX_HISTORY_URL ="https://api.gdax.com/products/"+currency+"/candles/?granularity=900";

    ArrayList<Candle> candles = new ArrayList<>();

    double lowHighSum, trendTenElements, lastHistory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Spinner  spinner = findViewById(R.id.spinner1);
        ArrayAdapter <CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.currencies, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(this);

        Button refreshbtn = (Button) findViewById(R.id.refreshbtn);
        refreshbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                recreate();
            }
        });
        initWebSocketUSD();
        getHistory(GDAX_HISTORY_URL);
    }

    public void getHistory(String historyUrl){
        class HistoryRunner implements Runnable{

           String url;
            HistoryRunner(String url){
                this.url =url;
            }


            @Override
            public void run() {
             JSONArray arr = JsonHelper.getJSONOArrayFromURL(url);
             Log.d(TAG, arr.toString());
             JSONArray candelArr = null;
                for(int i=0; i<arr.length();i++ ){
                    try {
                        candelArr = (JSONArray) arr.get(i);

                    if(candelArr!=null) {
                        Candle candle = new Candle(candelArr.getString(0),
                                                    candelArr.getDouble(1),
                                                    candelArr.getDouble(2),
                                                    candelArr.getDouble(3),
                                                    candelArr.getDouble(4),
                                                    candelArr.getDouble(1)
                                                    );
                        candles.add(candle);
                        Log.d(TAG, ""+(candle.open + candle.close)/2);
                        lowHighSum += (candle.open + candle.close)/2;
                        if(i<10)
                            trendTenElements+=(candle.open + candle.close)/2;
                        if(i==0)
                            lastHistory = (candle.open + candle.close)/2;
                    }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }
                lowHighSum /= arr.length();
                final String trendText;
                if((trendTenElements-lastHistory)>0)
                    trendText = "UP";
                else
                    trendText="DOWN";

                Log.d(TAG, "Srednia cena BTC 24h: "+lowHighSum);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ((TextView)findViewById(R.id.averageTextView)).setText("AvgUSD24h: " + Math.round(lowHighSum) + "$");
                        ((TextView)findViewById(R.id.trendTextView)).setText("Trend: " + trendText);



                    }
                });
            }
        }
        new Thread(new HistoryRunner(historyUrl)).start();

    }

    public void initWebSocketUSD()
    {
        URI gdaxURI = null;
        try
        {
            gdaxURI = new URI(GDAX_WS_URL);
        }
        catch (URISyntaxException e)
        {
            e.printStackTrace();
        }
        webSocketClientUSD = new WebSocketClient(gdaxURI) {
            @Override
            public void onOpen(ServerHandshake handshakedata) {
                Log.d(TAG,"onOpen");
                subscribeUSD();
            }

            @Override
            public void onMessage(String message) {
                Log.d(TAG,"onMessage"+message);
                JSONObject jsonObjectUSD = null;

                try
                {
                    jsonObjectUSD = new JSONObject(message);
                }
                catch (JSONException e)
                {
                    e.printStackTrace();
                }


                if(jsonObjectUSD!= null)
                {
                    try {
                        final String bid = jsonObjectUSD.getString("best_bid");
                        final String size = jsonObjectUSD.getString("last_size");
                        final String ask = jsonObjectUSD.getString("best_ask");
                        final String priceUSD = jsonObjectUSD.getString("price");
                        final String USDmin24 = jsonObjectUSD.getString("low_24h").substring(0,7);
                        final String USDmax24 =  jsonObjectUSD.getString("high_24h").substring(0,7);
                        final String timeUSD = jsonObjectUSD.getString("time");
                        final String USDvolume = jsonObjectUSD.getString("volume_24h").substring(0,9);
                        final String USDvolume30 = jsonObjectUSD.getString("volume_30d").substring(0,10);
                        final String productId= jsonObjectUSD.getString("product_id");
                        webSocketRunningFlag = true;

                        if(currency.equals(productId)){
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    ((TextView)findViewById(R.id.price_text2)).setText(priceUSD + currencySymbol );
                                    ((TextView)findViewById(R.id.USDmin24)).setText("Low: " + USDmin24 + currencySymbol);
                                    ((TextView)findViewById(R.id.USDmax24)).setText("High: " + USDmax24 + currencySymbol);
                                    ((TextView)findViewById(R.id.Time)).setText(timeUSD);
                                    ((TextView)findViewById(R.id.USDvolume)).setText("Volume 24h: " + USDvolume);
                                    ((TextView)findViewById(R.id.USDbid)).setText("Bid: " + bid + currencySymbol);
                                    ((TextView)findViewById(R.id.USDask)).setText("Ask: " + ask + currencySymbol);
                                    ((TextView)findViewById(R.id.USDsize)).setText("Size: " + size + " BTC");
                                    ((TextView)findViewById(R.id.USDvolume30d)).setText("Volume 30d: " + USDvolume30);

                                }
                            });
                        }


                    }
                    catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                Log.d(TAG,"onClose");
            }

            @Override
            public void onError(Exception ex) {
                Log.d(TAG,"onError");
            }
        };

        SSLSocketFactory sslSocketFactory = (SSLSocketFactory)SSLSocketFactory.getDefault();
        try {
            webSocketClientUSD.setSocket(sslSocketFactory.createSocket());
        } catch (IOException e) {
            e.printStackTrace();
        }
        webSocketClientUSD.connect();
    }
    public void subscribeUSD()
    {
        webSocketClientUSD.send("{\n" +
                "    \"type\": \"subscribe\",\n" +
                "    \"channels\": [{ \"name\": \"ticker\", \"product_ids\": [\""+currency+"\"] }]\n" +
                "}");
    }
    public void unSubscribe()
    {
        webSocketClientUSD.send("{\n" +
                "    \"type\": \"unsubscribe\",\n" +
                "    \"channels\": [{ \"name\": \"ticker\" }]\n" +
                "}");
        Log.i(TAG, "Unsubscribed succesfully");
    }
    @Override
    public void recreate() {

        super.recreate();
        //subscribeUSD();
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        String text = parent.getItemAtPosition(position).toString();
        currency = text;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ((TextView)findViewById(R.id.USDlabel)).setText(currency);



            }
        });

        if(currency.equals("BTC-USD")){
            currencySymbol = "$";
        }
        else if(currency.equals("BTC-EUR")){
            currencySymbol = "€";
        }
        else if(currency.equals("BTC-GBP")){
            currencySymbol= "£";
        }
        else if(currency.equals("ETH-BTC")){
            currencySymbol= "BTC";
        }

        Toast.makeText(parent.getContext(),text, Toast.LENGTH_SHORT).show();
        if(webSocketRunningFlag) {
            getHistory(GDAX_HISTORY_URL);
            this.unSubscribe();
            this.subscribeUSD();
            //super.recreate();

        }




    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }
}
