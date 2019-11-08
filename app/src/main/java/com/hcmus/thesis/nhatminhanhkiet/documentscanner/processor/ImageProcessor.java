package com.hcmus.thesis.nhatminhanhkiet.documentscanner.processor;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.Image;
import android.util.Log;

import com.hcmus.thesis.nhatminhanhkiet.documentscanner.SourceManager;
import com.hcmus.thesis.nhatminhanhkiet.documentscanner.crop.CropActivity;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ImageProcessor {

    Context context;
    public ImageProcessor(Context context) {
        this.context = context;

        if (!OpenCVLoader.initDebug()) {
            Log.e("InitOpenCV", "Load OpenCV error");

        }
        else {
            Log.e("InitOpenCV", "Load OpenCV successfully");
        }
    }

    public Corners processPicture(Mat previewFrame){
        ArrayList<MatOfPoint> contours = findContours(previewFrame);
        return getCorners(contours, previewFrame.size());
    }

    public Mat cropPicture(Mat picture, List<Point> pts){
        Point tl = pts.get(0);
        Point tr = pts.get(1);
        Point br = pts.get(2);
        Point bl = pts.get(3);

        double widthA = Math.sqrt(Math.pow(br.x - bl.x, 2.0) + Math.pow(br.y - bl.y, 2.0));
        double widthB = Math.sqrt(Math.pow(tr.x - tl.x, 2.0) + Math.pow(tr.y - tl.y, 2.0));
        double dw = Math.max(widthA, widthB);
        int maxWidth = (int) Math.round(dw);

        double heightA = Math.sqrt(Math.pow(tr.x - br.x, 2.0) + Math.pow(tr.y - br.y, 2.0));
        double heightB = Math.sqrt(Math.pow(tl.x - bl.x, 2.0) + Math.pow(tl.y - bl.y, 2.0));
        double dh = Math.max(heightA, heightB);
        int maxHeight = (int) Math.round(dh);

        Mat croppedPic = new Mat(maxHeight, maxWidth, CvType.CV_8UC4);

        Mat src_mat = new Mat(4, 1, CvType.CV_32FC2);
        Mat dst_mat = new Mat(4, 1, CvType.CV_32FC2);

        src_mat.put(0, 0, tl.x, tl.y, tr.x, tr.y, br.x, br.y, bl.x, bl.y);
        dst_mat.put(0, 0, 0.0, 0.0, dw, 0.0, dw, dh, 0.0, dh);

        Mat m = Imgproc.getPerspectiveTransform(src_mat, dst_mat);
        Imgproc.warpPerspective(picture, croppedPic, m, croppedPic.size());

        m.release();
        src_mat.release();
        dst_mat.release();

        return croppedPic;
    }

    public Corners getCorners(ArrayList<MatOfPoint> contours, Size size) {
        int indexTo;

        if(contours.size() >=0 && contours.size() <= 5){
            indexTo = contours.size() - 1;
        }
        else {
            indexTo = 4;
        }

        for(int i =0;i<contours.size();i++){
            if(i<= indexTo) {
                MatOfPoint2f c2f = new MatOfPoint2f(contours.get(i).toArray());
                Double peri = Imgproc.arcLength(c2f, true);
                MatOfPoint2f approx = new MatOfPoint2f();
                Imgproc.approxPolyDP(c2f, approx, 0.02*peri, true);
                List<Point> points = approx.toList();

                if(points.size() == 4){
                    List<Point> foundPoints = sortPoint(points);
                    return new Corners(foundPoints, size);
                }

            }
            else {
                return null;
            }
        }
        return null;
    }

    public void processImage(Image image){

        /*val pictureSize = p1?.parameters?.pictureSize
        Log.i(TAG, "picture size: " + pictureSize.toString())
        val mat = Mat(Size(pictureSize?.width?.toDouble() ?: 1920.toDouble(),
                pictureSize?.height?.toDouble() ?: 1080.toDouble()), CvType.CV_8U)
        mat.put(0, 0, p0)
        val pic = Imgcodecs.imdecode(mat, Imgcodecs.CV_LOAD_IMAGE_UNCHANGED)
        Core.rotate(pic, pic, Core.ROTATE_90_CLOCKWISE)
        mat.release()
        SourceManager.corners = processPicture(pic)
        Imgproc.cvtColor(pic, pic, Imgproc.COLOR_RGB2BGRA)
        SourceManager.pic = pic
        context.startActivity(Intent(context, CropActivity::class.java))
        busy = false*/
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);

        //Mat mat = new Mat(new Size(image.getWidth(), image.getHeight()), CvType.CV_8U);
        Mat mat = new Mat(new Size(image.getHeight(), image.getWidth()), CvType.CV_8U);
        mat.put(0, 0, bytes);

        Mat pic = Imgcodecs.imdecode(new MatOfByte(bytes), Imgcodecs.IMREAD_UNCHANGED);
        //Mat pic = Imgcodecs.imdecode(mat, Imgcodecs.IMREAD_UNCHANGED);
        //Core.rotate(pic, pic, Core.ROTATE_90_CLOCKWISE);
        mat.release();
        Corners corners = processPicture(pic);
        Imgproc.cvtColor(pic, pic, Imgproc.COLOR_RGB2BGRA);
        /*SourceManager sourceManager = new SourceManager();
        sourceManager.Companion.setCorners(corners);
        sourceManager.Companion.setPic(pic);*/
    }

    public void processImage(byte[] bytes, int width, int height){

        /*val pictureSize = p1?.parameters?.pictureSize
        Log.i(TAG, "picture size: " + pictureSize.toString())
        val mat = Mat(Size(pictureSize?.width?.toDouble() ?: 1920.toDouble(),
                pictureSize?.height?.toDouble() ?: 1080.toDouble()), CvType.CV_8U)
        mat.put(0, 0, p0)
        val pic = Imgcodecs.imdecode(mat, Imgcodecs.CV_LOAD_IMAGE_UNCHANGED)
        Core.rotate(pic, pic, Core.ROTATE_90_CLOCKWISE)
        mat.release()
        SourceManager.corners = processPicture(pic)
        Imgproc.cvtColor(pic, pic, Imgproc.COLOR_RGB2BGRA)
        SourceManager.pic = pic
        context.startActivity(Intent(context, CropActivity::class.java))
        busy = false*/

        //Mat mat = new Mat(new Size(image.getWidth(), image.getHeight()), CvType.CV_8U);
        Mat mat = new Mat(new Size(height, width), CvType.CV_8U);
        mat.put(0, 0, bytes);

        Mat pic = Imgcodecs.imdecode(new MatOfByte(bytes), Imgcodecs.IMREAD_UNCHANGED);
        //Core.rotate(pic, pic, Core.ROTATE_90_CLOCKWISE);
        mat.release();
        Corners corners = processPicture(pic);

        boolean isDuplicate = isCornersListDuplicates(corners);

        Imgproc.cvtColor(pic, pic, Imgproc.COLOR_RGB2BGRA);
        SourceManager sourceManager = new SourceManager();
        sourceManager.Companion.setCorners(corners);
        sourceManager.Companion.setPic(pic);

        Intent intent = new Intent(context, CropActivity.class);
        context.startActivity(intent);
    }


    public boolean isCornersListDuplicates(Corners corners)
    {
        final Set<Point> set = new HashSet<>();

        for(int i=0;i<corners.corners.size();i++){
            if(set.add(corners.corners.get(i)) == false){
                return true;
            }
        }
        return false;
    }

    public Bitmap enhancePicture(Bitmap src){
        Mat src_mat = new Mat();
        Utils.bitmapToMat(src, src_mat);
        Imgproc.cvtColor(src_mat, src_mat, Imgproc.COLOR_RGB2GRAY);
        Imgproc.adaptiveThreshold(src_mat, src_mat, 255.0,
                Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 15, 15.0);

        Bitmap result = Bitmap.createBitmap(src.getWidth(), src.getHeight(), Bitmap.Config.RGB_565);
        Utils.matToBitmap(src_mat, result, true);
        src_mat.release();
        return result;
    }

    public ArrayList<MatOfPoint> findContours(Mat src){
        Mat grayImage;
        Mat cannedImage;
        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(9.0, 9.0));
        Mat dilate;

        Size size = new Size(src.size().width, src.size().height);
        grayImage = new Mat(size, CvType.CV_8UC4);
        cannedImage = new Mat(size, CvType.CV_8UC1);
        dilate = new Mat(size, CvType.CV_8UC1);

        Imgproc.cvtColor(src, grayImage, Imgproc.COLOR_RGB2GRAY);
        Imgproc.GaussianBlur(grayImage, grayImage, new Size(5.0, 5.0), 0.0);
        Imgproc.threshold(grayImage, grayImage, 20.0, 255.0, Imgproc.THRESH_TRIANGLE);
        Imgproc.Canny(grayImage, cannedImage, 75.0, 200.0);
        Imgproc.dilate(cannedImage, dilate, kernel);

        ArrayList<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();

        Imgproc.findContours(dilate, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
        sortContoursByArea(contours);

        hierarchy.release();
        grayImage.release();
        cannedImage.release();
        kernel.release();
        dilate.release();
        return contours;
    }


    public static void sortContoursByArea(ArrayList<MatOfPoint> contours) {
        if (contours == null)
            return;

        if (contours.size() == 0 || contours.size() == 1)
            return;


        int largestIndex;
        MatOfPoint largest;

        for (int curIndex = 0; curIndex < contours.size(); curIndex++) {
            largest = contours.get(curIndex);
            largestIndex = curIndex;

            for (int i = curIndex + 1; i < contours.size(); i++) {
                if (Imgproc.contourArea(largest) < Imgproc.contourArea(contours.get(i))) {

                    largest = contours.get(i);
                    largestIndex = i;
                }
            }


            if (largestIndex == curIndex)
                ;

            else {
                MatOfPoint temp = contours.get(curIndex);
                contours.set(curIndex, contours.get(largestIndex));
                contours.set(largestIndex, temp);
            }

        }
    }

    List<Point> sortPoint(List<Point> points){
        ArrayList<Double> sumCoordinates = new ArrayList<>();
        ArrayList<Double> minusCoordinates = new ArrayList<>();
        for(int i =0;i<points.size();i++){
            sumCoordinates.add(points.get(i).x + points.get(i).y);
            minusCoordinates.add(points.get(i).x - points.get(i).y);
        }

        Point p0 = points.get(sumCoordinates.indexOf(Collections.min(sumCoordinates)));
        Point p1 = points.get(minusCoordinates.indexOf(Collections.max(minusCoordinates)));
        Point p2 = points.get(sumCoordinates.indexOf(Collections.max(sumCoordinates)));
        Point p3 = points.get(minusCoordinates.indexOf(Collections.min(minusCoordinates)));

        return Arrays.asList(p0, p1, p2, p3);
    }

    public Bitmap convertToGrayscale(Bitmap srcBitmap){
        Mat srcMat= new Mat();
        Utils.bitmapToMat(srcBitmap, srcMat);

        Mat destMat = new Mat();
        Imgproc.cvtColor(srcMat, destMat, Imgproc.COLOR_RGB2GRAY);

        Bitmap destBitmap = Bitmap.createBitmap(destMat.width(), destMat.height(), Bitmap.Config.ARGB_8888);;
        Utils.matToBitmap(destMat, destBitmap);
        Log.e("Test", "test");
        return destBitmap;
    }

}
