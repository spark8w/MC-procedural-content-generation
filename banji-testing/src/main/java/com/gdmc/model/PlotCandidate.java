package com.gdmc.model;

public class PlotCandidate {

    public final int north;
    public final int south;
    public final int east;
    public final int west;
    public final int gradient;
    public final int seaCount;

    public PlotCandidate(int north, int south, int east, int west,
                         int gradient, int seaCount) {
        this.north    = north;
        this.south    = south;
        this.east     = east;
        this.west     = west;
        this.gradient = gradient;
        this.seaCount = seaCount;
    }

    @Override
    public String toString() {
        return String.format(
                "Plot[N=%d S=%d E=%d W=%d gradient=%d seaCount=%d]",
                north, south, east, west, gradient, seaCount
        );
    }
}