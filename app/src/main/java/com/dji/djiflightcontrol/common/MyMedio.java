package com.dji.djiflightcontrol.common;

/**
 * Created by 杰 on 2016/12/13.
 */

public class MyMedio {
    private double h;
    private double la;
    private double lo;
    private String name;

    public MyMedio(double h, double la, double lo, String name) {
        this.h = h;
        this.la = la;
        this.lo = lo;
        this.name = name;
    }

    public double getH() {
        return h;
    }

    public void setH(float h) {
        this.h = h;
    }

    public double getLa() {
        return la;
    }

    public void setLa(float la) {
        this.la = la;
    }

    public double getLo() {
        return lo;
    }

    public void setLo(float lo) {
        this.lo = lo;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "名称" + name + "\n高度" + h + "\n经度" + lo + "\n纬度" + la + "\n";
    }
}
