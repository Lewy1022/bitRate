package com.example.myapplication;

public class Candle {

    public Candle(String time, double low, double high, double open, double close,double volume){
        this.time = time;
        this.open = open;
        this.close = close;
        this.high = high;
        this.low = low;
        this.volume = volume;

    }

    String time;
    double open;
    double close;
    double high;
    double low;
    double volume;

}
