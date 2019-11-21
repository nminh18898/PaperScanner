package com.hcmus.thesis.nhatminhanhkiet.documentscanner.crop;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import com.hcmus.thesis.nhatminhanhkiet.documentscanner.SourceManager;
import com.hcmus.thesis.nhatminhanhkiet.documentscanner.processor.Corners;

import org.opencv.core.Point;
import org.opencv.core.Size;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class PaperRectangle extends View {
    private Paint rectPaint;
    private Paint circlePaint;
    private double ratioX = 1.0;
    private double ratioY = 1.0;
    private Point tl;
    private Point tr;
    private Point br;
    private Point bl;
    private Path path;
    private Point point2Move;
    private boolean cropMode;
    private float latestDownX = 0.0F;
    private float latestDownY = 0.0F;

    public void onCornersDetected(Corners corners) {

        this.ratioX = corners.size.width / (double) this.getMeasuredWidth();
        this.ratioY = corners.size.height / (double) this.getMeasuredHeight();
        Point temp = corners.corners.get(0);
        if (temp == null) {
            temp = new Point();
        }
        this.tl = temp;

        temp = corners.corners.get(1);
        if (temp == null) {
            temp = new Point();
        }
        this.tr = temp;

        temp = corners.corners.get(2);
        if (temp == null) {
            temp = new Point();
        }
        this.br = temp;

        temp = corners.corners.get(3);
        if (temp == null) {
            temp = new Point();
        }
        this.bl = temp;

        this.resize();
        this.path.reset();
        this.path.moveTo((float) this.tl.x, (float) this.tl.y);
        this.path.lineTo((float) this.tr.x, (float) this.tr.y);
        this.path.lineTo((float) this.br.x, (float) this.br.y);
        this.path.lineTo((float) this.bl.x, (float) this.bl.y);
        this.path.close();
        this.invalidate();
    }

    public void onCornersNotDetected() {
        this.path.reset();
        this.invalidate();
    }

    public void onCorners2Crop(Corners corners, Size size) {
        if (null == corners) {
            return;
        }

        this.cropMode = true;
        DisplayMetrics displayMetrics = new DisplayMetrics();
        WindowManager windowManager = ((Activity) this.getContext()).getWindowManager();
        windowManager.getDefaultDisplay().getMetrics(displayMetrics);


        if (corners.corners.get(0) == null || corners.corners.get(1) == null || corners.corners.get(2) == null || corners.corners.get(3) == null) {
            tl = SourceManager.Companion.getDefaultTl();
            tr = SourceManager.Companion.getDefaultTr();
            br = SourceManager.Companion.getDefaultBr();
            bl = SourceManager.Companion.getDefaultBl();
        }
        else {
            tl = corners.corners.get(0);
            tr = corners.corners.get(1);
            br = corners.corners.get(2);
            bl = corners.corners.get(3);
        }

        int statusBarHeight = this.getStatusBarHeight(this.getContext());

        double pictureRatio = size.height / size.width;
        //this.ratioX = size != null ? size.width / (double) displayMetrics.widthPixels : 1.0D;
        //this.ratioY = size != null ? size.height / (double) (displayMetrics.heightPixels - statusBarHeight) : 1.0D;
        ratioX = size.width / displayMetrics.widthPixels;

        ratioY = size.height / (displayMetrics.widthPixels * pictureRatio);

        this.resize();
        this.movePoints();
    }

    public List<Point> getCorners2Crop() {
        this.reverseSize();
        return Arrays.asList(this.tl, this.tr, this.br, this.bl);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (null == canvas) {
            return;
        }

        canvas.drawPath(this.path, this.rectPaint);

        if (this.cropMode) {
            canvas.drawCircle((float) this.tl.x, (float) this.tl.y, 20.0F, this.circlePaint);
            canvas.drawCircle((float) this.tr.x, (float) this.tr.y, 20.0F, this.circlePaint);
            canvas.drawCircle((float) this.bl.x, (float) this.bl.y, 20.0F, this.circlePaint);
            canvas.drawCircle((float) this.br.x, (float) this.br.y, 20.0F, this.circlePaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!this.cropMode) {
            return false;
        }

        if (null != event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                this.latestDownX = event.getX();
                this.latestDownY = event.getY();
                this.calculatePoint2Move(event.getX(), event.getY());
                return true;
            }

            if (event.getAction() == MotionEvent.ACTION_MOVE) {
                this.point2Move.x += (double) (event.getX() - this.latestDownX);
                this.point2Move.y += (double) (event.getY() - this.latestDownY);
                this.movePoints();
                this.latestDownY = event.getY();
                this.latestDownX = event.getX();
            }
        }

        return true;

    }

    private void calculatePoint2Move(float downX, float downY) {
        List<Point> points = Arrays.asList(this.tl, this.tr, this.br, this.bl);

        Iterator<Point> iterator = points.iterator();
        Point point = null;

        double minValue = Double.MAX_VALUE;
        while (iterator.hasNext()) {
            Point it = iterator.next();
            if (point == null) {
                point = it;
            }

            double min = Math.abs((it.x - (double) downX) * (it.y - (double) downY));
            if (minValue > min) {
                point = it;
                minValue = min;
            }
        }

        if (point == null) {
            point = this.tl;
        }

        this.point2Move = point;
    }

    private void movePoints() {
        this.path.reset();
        this.path.moveTo((float) this.tl.x, (float) this.tl.y);
        this.path.lineTo((float) this.tr.x, (float) this.tr.y);
        this.path.lineTo((float) this.br.x, (float) this.br.y);
        this.path.lineTo((float) this.bl.x, (float) this.bl.y);
        this.path.close();
        this.invalidate();
    }

    private void resize() {
        this.tl.x /= this.ratioX;
        this.tl.y /= this.ratioY;
        this.tr.x /= this.ratioX;
        this.tr.y /= this.ratioY;
        this.br.x /= this.ratioX;
        this.br.y /= this.ratioY;
        this.bl.x /= this.ratioX;
        this.bl.y /= this.ratioY;
    }

    private void reverseSize() {
        this.tl.x *= this.ratioX;
        this.tl.y *= this.ratioY;
        this.tr.x *= this.ratioX;
        this.tr.y *= this.ratioY;
        this.br.x *= this.ratioX;
        this.br.y *= this.ratioY;
        this.bl.x *= this.ratioX;
        this.bl.y *= this.ratioY;
    }

    private int getNavigationBarHeight(Context pContext) {
        Resources resources = pContext.getResources();
        int resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android");
        return resourceId > 0 ? resources.getDimensionPixelSize(resourceId) : 0;
    }

    private int getStatusBarHeight(Context pContext) {
        Resources resources = pContext.getResources();
        int resourceId = resources.getIdentifier("status_bar_height", "dimen", "android");
        return resourceId > 0 ? resources.getDimensionPixelSize(resourceId) : 0;
    }

    public PaperRectangle(Context context) {
        super(context);
        init();
    }

    public PaperRectangle(Context context, AttributeSet attributes) {
        super(context, attributes);
        init();
    }

    public PaperRectangle(Context context, AttributeSet attributes, int defTheme) {
        super(context, attributes, defTheme);
        init();
    }

    private void init() {
        this.rectPaint = new Paint();
        this.circlePaint = new Paint();
        this.ratioX = 1.0D;
        this.ratioY = 1.0D;
        this.tl = new Point();
        this.tr = new Point();
        this.br = new Point();
        this.bl = new Point();
        this.path = new Path();
        this.point2Move = new Point();

        this.rectPaint.setColor(-16776961);
        this.rectPaint.setAntiAlias(true);
        this.rectPaint.setDither(true);
        this.rectPaint.setStrokeWidth(6.0F);
        this.rectPaint.setStyle(Paint.Style.STROKE);
        this.rectPaint.setStrokeJoin(Paint.Join.ROUND);
        this.rectPaint.setStrokeCap(Paint.Cap.ROUND);
        this.rectPaint.setPathEffect((new CornerPathEffect(10.0F)));

        this.circlePaint.setColor(-3355444);
        this.circlePaint.setDither(true);
        this.circlePaint.setAntiAlias(true);
        this.circlePaint.setStrokeWidth(4.0F);
        this.circlePaint.setStyle(Paint.Style.STROKE);
    }

}
