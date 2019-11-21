package com.hcmus.thesis.nhatminhanhkiet.documentscanner.processor;

import org.opencv.core.Point;
import org.opencv.core.Size;

import java.util.List;

public class Corners {

    public List<Point> corners;
    public Size size;

    public Corners(List<Point> corners, Size size) {
        this.corners = corners;
        this.size = size;
    }

    public void setCorners(List<Point> corners){
        this.corners = corners;
    }
}
