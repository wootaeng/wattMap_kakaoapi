package com.peng.plant.wattmap_kakaoapi.controller;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;


public class ZoomController extends FrameLayout {

    /**
     * Zooming view listener interface.
     *
     * @author karooolek
     */
    public interface ZoomViewListener {

        void onZoomStarted(float zoom, float zoomx, float zoomy);

        void onZooming(float zoom, float zoomx, float zoomy);

        void onZoomEnded(float zoom, float zoomx, float zoomy);
    }

    // zooming
    float zoom = 1.0f;
    float maxZoom = 2.0f;
    float smoothZoom = 1.0f;
    float zoomX, zoomY;
    float smoothZoomX, smoothZoomY;
    private boolean scrolling; // NOPMD by karooolek on 29.06.11 11:45

    // minimap variables
    private boolean showMinimap = false;
    private int miniMapColor = Color.BLACK;
    private int miniMapHeight = -1;
    private String miniMapCaption;
    private float miniMapCaptionSize = 10.0f;
    private int miniMapCaptionColor = Color.WHITE;

    // touching variables
    private long lastTapTime;
    private float touchStartX, touchStartY;
    private float touchLastX, touchLastY;
    private float startd;
    private boolean pinching;
    private float lastd;
    private float lastdx1, lastdy1;
    private float lastdx2, lastdy2;
    private float num1, num2;
    float dxk, dyk;
    // drawing
    private final Matrix m = new Matrix();
    private final Paint p = new Paint();

    // listener
    ZoomViewListener listener;

    private Bitmap ch;

    // Bitmap ch
    public ZoomController(final Context context) {
        super(context);
    }

    public float getZoom() {
        return zoom;
    }

    public float getMaxZoom() {
        return maxZoom;
    }

    public void setMaxZoom(final float maxZoom) {
        if (maxZoom < 1.0f) {
            return;
        }

        this.maxZoom = maxZoom;
    }

    public void setMiniMapEnabled(final boolean showMiniMap) {
        this.showMinimap = showMiniMap;
    }

    public boolean isMiniMapEnabled() {
        return showMinimap;
    }

    public void setMiniMapHeight(final int miniMapHeight) {
        if (miniMapHeight < 0) {
            return;
        }
        this.miniMapHeight = miniMapHeight;
    }

    public int getMiniMapHeight() {
        return miniMapHeight;
    }

    public void setMiniMapColor(final int color) {
        miniMapColor = color;
    }

    public int getMiniMapColor() {
        return miniMapColor;
    }

    public String getMiniMapCaption() {
        return miniMapCaption;
    }

    public void setMiniMapCaption(final String miniMapCaption) {
        this.miniMapCaption = miniMapCaption;
    }

    public float getMiniMapCaptionSize() {
        return miniMapCaptionSize;
    }

    public void setMiniMapCaptionSize(final float size) {
        miniMapCaptionSize = size;
    }

    public int getMiniMapCaptionColor() {
        return miniMapCaptionColor;
    }

    public void setMiniMapCaptionColor(final int color) {
        miniMapCaptionColor = color;
    }

    //이미지 줌 강도 메소드
    public void zoomTo(final float zoom, final float x, final float y) {
        this.zoom = Math.min(zoom, maxZoom);
        zoomX = x;
        zoomY = y;
        smoothZoomTo(this.zoom, x, y);
    }

    public void smoothZoomTo(final float zoom, final float x, final float y) {
        smoothZoom = clamp(1.0f, zoom, maxZoom);
        smoothZoomX = x;
        smoothZoomY = y;
        if (listener != null) {
            listener.onZoomStarted(smoothZoom, x, y);
        }
    }

    public ZoomViewListener getListener() {
        return listener;
    }

    public void setListner(final ZoomViewListener listener) {
        this.listener = listener;
    }

    public float getZoomFocusX() {
        return zoomX * zoom;
    }

    public float getZoomFocusY() {
        return zoomY * zoom;
    }

    @Override
    public boolean dispatchTouchEvent(final MotionEvent ev) {

        Log.d("TAGAGAGAG","dispatchTouchEvent: ev값 : " + ev);


        // single touch
        if (ev.getPointerCount() == 1) {
            processSingleTouchEvent(ev);
        }

        // // double touch
        if (ev.getPointerCount() == 2) {
            processDoubleTouchEvent(ev);
        }
        if (ev.getPointerCount() == 11) {
            processDoubleTouchEvent(ev);
        }

        // redraw , 다시그리기
        getRootView().invalidate();
        invalidate();

        return true;
    }

    private void processSingleTouchEvent(final MotionEvent ev) {

        num1 = ev.getX();
        num2 = ev.getY();
        final float x = ev.getX();
        final float y = ev.getY();

        final float w = miniMapHeight * (float) getWidth() / getHeight();
        final float h = miniMapHeight;
        final boolean touchingMiniMap = x >= 10.0f && x <= 10.0f + w && y >= 10.0f && y <= 10.0f + h;

        if (showMinimap && smoothZoom > 1.0f && touchingMiniMap) {
            processSingleTouchOnMinimap(ev);
        } else {
            processSingleTouchOutsideMinimap(ev);
        }
    }

    private void processSingleTouchOnMinimap(final MotionEvent ev) {
        final float x = ev.getX();
        final float y = ev.getY();

        final float w = miniMapHeight * (float) getWidth() / getHeight();
        final float h = miniMapHeight;
        final float zx = (x - 10.0f) / w * getWidth();
        final float zy = (y - 10.0f) / h * getHeight();
        smoothZoomTo(smoothZoom, zx, zy);
    }

    private void processSingleTouchOutsideMinimap(final MotionEvent ev) {
        final float x = ev.getX();
        final float y = ev.getY();
        float lx = x - touchStartX;
        float ly = y - touchStartY;
        final float l = (float) Math.hypot(lx, ly);
        float dx = x - touchLastX;
        float dy = y - touchLastY;
        touchLastX = x;
        touchLastY = y;
        Log.d("Tatata", "touchLastX : " + touchLastX);//화면 클릭 좌표


        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touchStartX = x;
                touchStartY = y;
                touchLastX = x;
                touchLastY = y;
                dx = 0;
                dy = 0;
                lx = 0;
                ly = 0;
                scrolling = false;
                break;

            case MotionEvent.ACTION_MOVE:
                //스크롤시?드래그시 이동값
                if (scrolling || (smoothZoom > 1.0f && l > 30.0f)) {
                    if (!scrolling) {
                        scrolling = true;
                        ev.setAction(MotionEvent.ACTION_CANCEL);
                        super.dispatchTouchEvent(ev);
                    }
                    smoothZoomX -= dx / zoom;
                    smoothZoomY -= dy / zoom;
                    Log.d("actionM", "processSingleTouchOutsideMinimap: " + smoothZoomX);

                    return;
                }
                break;

            case MotionEvent.ACTION_BUTTON_PRESS:
                if (l < 30.0f) {
                }
            case MotionEvent.ACTION_OUTSIDE:
            case MotionEvent.ACTION_UP:

                // tap
                if (l < 30.0f) {
                    // check double tap
                    if (System.currentTimeMillis() - lastTapTime < 500) {
                        if (smoothZoom == 1.0f) {
                            smoothZoomTo(maxZoom, x, y);
                        } else {
                            smoothZoomTo(1.0f, getWidth() / 2.0f, getHeight() / 2.0f);
                        }
                        lastTapTime = 0;
                        ev.setAction(MotionEvent.ACTION_CANCEL);
                        super.dispatchTouchEvent(ev);
                        return;
                    }

                    lastTapTime = System.currentTimeMillis();

                    performClick();
                }
                break;

            default:
                break;
        }

        ev.setLocation(zoomX + (x - 0.5f * getWidth()) / zoom, zoomY + (y - 0.5f * getHeight()) / zoom);

        ev.getX();
        ev.getY();

        super.dispatchTouchEvent(ev);
    }
    //더블탭? 더블터치
    private void processDoubleTouchEvent(final MotionEvent ev) {
        final float x1 = ev.getX(0);
        final float dx1 = x1 - lastdx1;
        lastdx1 = x1;
        final float y1 = ev.getY(0);
        final float dy1 = y1 - lastdy1;
        lastdy1 = y1;
        final float x2 = ev.getX(1);
        final float dx2 = x2 - lastdx2;
        lastdx2 = x2;
        final float y2 = ev.getY(1);
        final float dy2 = y2 - lastdy2;
        lastdy2 = y2;

        // pointers distance
        final float d = (float) Math.hypot(x2 - x1, y2 - y1);
        final float dd = d - lastd;
        lastd = d;
        final float ld = Math.abs(d - startd);

        Math.atan2(y2 - y1, x2 - x1);
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                startd = d;
                pinching = false;
                break;

            case MotionEvent.ACTION_MOVE:
                if (pinching || ld > 30.0f) {
                    pinching = true;
                    dxk = 0.5f * (dx1 + dx2);
                    dyk = 0.5f * (dy1 + dy2);
                    smoothZoomTo(Math.max(1.0f, zoom * d / (d - dd)), zoomX - dxk / zoom, zoomY - dyk / zoom);
                }

                break;

            case MotionEvent.ACTION_UP:
            default:
                pinching = false;
                break;
        }

        ev.setAction(MotionEvent.ACTION_CANCEL);
        super.dispatchTouchEvent(ev);
    }

    private float clamp(final float min, final float value, final float max) {
        return Math.max(min, Math.min(value, max));
    }

    private float lerp(final float a, final float b, final float k) {
        return a + (b - a) * k;
    }

    private float bias(final float a, final float b, final float k) {
        return Math.abs(b - a) >= k ? a + k * Math.signum(b - a) : b;
    }

    @Override
    protected void dispatchDraw(final Canvas canvas) {
        // do zoom

        zoom = lerp(bias(zoom, smoothZoom, 0.05f), smoothZoom, 0.2f);
        smoothZoomX = clamp(0.5f * getWidth() / smoothZoom, smoothZoomX, getWidth() - 0.5f * getWidth() / smoothZoom);
        smoothZoomY = clamp(0.5f * getHeight() / smoothZoom, smoothZoomY, getHeight() - 0.5f * getHeight() / smoothZoom);


        zoomX = lerp(bias(zoomX, smoothZoomX, 0.1f), smoothZoomX, 0.35f);
        zoomY = lerp(bias(zoomY, smoothZoomY, 0.1f), smoothZoomY, 0.35f);
        if (zoom != smoothZoom && listener != null) {
            listener.onZooming(zoom, zoomX, zoomY);
        }

        final boolean animating = Math.abs(zoom - smoothZoom) > 0.0000001f
                || Math.abs(zoomX - smoothZoomX) > 0.0000001f || Math.abs(zoomY - smoothZoomY) > 0.0000001f;

        // nothing to draw
        if (getChildCount() == 0) {
            return;
        }


        // prepare matrix
        m.setTranslate(0.5f * getWidth(), 0.5f * getHeight());
        m.preScale(zoom, zoom);
        m.preTranslate(-clamp(0.5f * getWidth() / zoom, zoomX, getWidth() - 0.5f * getWidth() / zoom),
                -clamp(0.5f * getHeight() / zoom, zoomY, getHeight() - 0.5f * getHeight() / zoom));

        // get view
        final View v = getChildAt(0);
        m.preTranslate(v.getLeft(), v.getTop());

        // get drawing cache if available
        if (animating && ch == null && isAnimationCacheEnabled()) {
            v.setDrawingCacheEnabled(true);
            ch = v.getDrawingCache();
        }

        // draw using cache while animating
        if (animating && isAnimationCacheEnabled() && ch != null) {
            p.setColor(0xffffffff);
            canvas.drawBitmap(ch, m, p);
        } else { // zoomed or cache unavailable
//            ch = null;
            canvas.save();
            canvas.concat(m);
            v.draw(canvas);
            canvas.restore();
        }

        // draw minimap
        if (showMinimap) {
            if (miniMapHeight < 0) {
                miniMapHeight = getHeight() / 4;
            }

            //미니맵 가로
            final int w = (int) (miniMapHeight * (float) getWidth() / getHeight() /2 );
            //미니맵 세로
            final int h = (int) (miniMapHeight /2);
            //미니맵 화면 위치
            canvas.translate(40, 40);

            //미니맵 이미지 resize
            Bitmap resizing = Bitmap.createScaledBitmap(v.getDrawingCache(), w, h, true);
            //이미지 그리기
            canvas.drawBitmap(resizing, 0, 0, p);

            if (miniMapCaption != null && miniMapCaption.length() > 0) {
                p.setTextSize(miniMapCaptionSize);
                p.setColor(miniMapCaptionColor);
                p.setAntiAlias(true);
                canvas.drawText(miniMapCaption, 10.0f, 10.0f + miniMapCaptionSize, p);
                p.setAntiAlias(false);
            }

            //배경 테두리 색
            p.setColor(Color.BLACK);
//            p.setAlpha(100);
            p.setStyle(Paint.Style.STROKE);
            //테두리 두께
            p.setStrokeWidth(3);
            final float dx = w * zoomX / getWidth();
            final float dy = h * zoomY / getHeight();

            final float fix_dx = (float) (w * 420.0 / getWidth());
            final float fix_dy = (float) (h * 240.0 / getHeight());

            canvas.drawRect(fix_dx - 0.5f * w, fix_dy - 0.5f * h, fix_dx + 0.5f * w, fix_dy + 0.5f * h, p);

//            canvas.translate(40, 40); //중복으로 그려짐
            p.setColor(Color.RED);
            //줌 들어가는 테두리
            canvas.drawRect(dx - 0.5f * w / zoom, dy - 0.5f * h / zoom, dx + 0.5f * w / zoom, dy + 0.5f * h / zoom, p);

//            canvas.translate(-10.0f, -10.0f);
        }

        getRootView().invalidate();
        invalidate();
    }

    //줌레벨 메소드
    public void zoomlevel(float num) {
        //줌 강도
        zoom = num;
        //줌 강도별 확대
        smoothZoomTo(num, zoomX - dxk / zoom, zoomY - dyk / zoom);

    }
    //줌레벨 별 좌표이동
    public void Move_Sensor(float dx, float dy) {
        smoothZoomX -= dx / zoom;
        smoothZoomY -= dy / zoom;
    }


}
