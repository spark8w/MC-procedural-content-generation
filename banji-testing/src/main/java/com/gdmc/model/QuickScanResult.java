package com.gdmc.model;

import java.util.List;

public class QuickScanResult {

    public final List<PlotCandidate> allPlots;
    public final List<PlotCandidate> bestPlots;
    public final PlotCandidate       champion;

    public QuickScanResult(List<PlotCandidate> allPlots,
                           List<PlotCandidate> bestPlots,
                           PlotCandidate champion) {
        this.allPlots  = allPlots;
        this.bestPlots = bestPlots;
        this.champion  = champion;
    }
}