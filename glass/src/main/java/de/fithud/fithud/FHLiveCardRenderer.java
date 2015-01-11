package de.fithud.fithud;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Message;
import android.os.SystemClock;
import android.os.UserHandle;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.glass.timeline.DirectRenderingCallback;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.concurrent.TimeUnit;

import de.fithud.fithudlib.FHSensorManager;
import de.fithud.fithudlib.MessengerClient;
import de.fithud.fithudlib.MessengerConnection;
import de.fithud.fithudlib.MessengerServiceActivity;

/**
 * Created by jandob on 11/17/14.
 */
public class FHLiveCardRenderer implements DirectRenderingCallback {
    //MessengerConnection conn = new MessengerConnection(this);
    private static final String TAG = FHLiveCardRenderer.class.getSimpleName();

    /** The refresh rate, in frames per second, of the compass. */
    private static final int REFRESH_RATE_FPS = 45;

    /** The duration, in milliseconds, of one frame. */
    private static final long FRAME_TIME_MILLIS = TimeUnit.SECONDS.toMillis(1) / REFRESH_RATE_FPS;

    private final TextView mTipsView;
    private SurfaceHolder mHolder;
    private boolean mRenderingPaused;

    private RenderThread mRenderThread;
    private int mSurfaceWidth;
    private int mSurfaceHeight;
    private final FrameLayout mLayout;
    //private final FithudView fithudView;
    private final RelativeLayout mTipsContainer;

    /*
    @Override
    public void handleMessage(Message msg) {
        switch(msg.what){
            case FHSensorManager.Messages.SENSOR_STATUS_MESSAGE:
                int[] sensor_status = msg.getData().getIntArray("value");
                Log.i(TAG,"Got Message: "+sensor_status[0]+" "+sensor_status[1]);
                break;
        }
    }
*/
    public FHLiveCardRenderer(Context context){
        LayoutInflater inflater = LayoutInflater.from(context);
        mLayout = (FrameLayout) inflater.inflate(R.layout.fithud, null);
        mLayout.setWillNotDraw(false);

        mTipsContainer = (RelativeLayout) mLayout.findViewById(R.id.text_container);
        mTipsView = (TextView) mLayout.findViewById(R.id.textView);
        mTipsView.setText("hans");
        Log.d(TAG, "Renderer init");
    }
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        mSurfaceWidth = width;
        mSurfaceHeight = height;
        doLayout();
        Log.d(TAG, "surface changed!");
    }
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // The creation of a new Surface implicitly resumes the rendering.
        //conn.connect(FHSensorManager.class);
        mRenderingPaused = false;
        mHolder = holder;
        updateRenderingState();
        Log.d(TAG, "surface created!");
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        //conn.disconnect();
        mHolder = null;
        updateRenderingState();
        Log.d(TAG, "surface destroyed!");
    }

    @Override
    public void renderingPaused(SurfaceHolder holder, boolean paused) {
        mRenderingPaused = paused;
        updateRenderingState();
        Log.d(TAG, "rendering paused, state: " + paused);
    }
    /**
     * Starts or stops rendering according to the livecard's state.
     */
    private void updateRenderingState() {
        boolean shouldRender = (mHolder != null) && !mRenderingPaused;
        boolean isRendering = (mRenderThread != null);

        if (shouldRender != isRendering) {
            if (shouldRender) {
                Log.d(TAG, "starting render Thread!");
                mRenderThread = new RenderThread();
                mRenderThread.start();
            } else {
                Log.d(TAG, "quiting render Thread!");
                mRenderThread.quit();
                mRenderThread = null;
            }
        }
    }
    private void doLayout() {
        // Measure and update the layout so that it will take up the entire surface space
        // when it is drawn.
        int measuredWidth = View.MeasureSpec.makeMeasureSpec(mSurfaceWidth,
                View.MeasureSpec.EXACTLY);
        int measuredHeight = View.MeasureSpec.makeMeasureSpec(mSurfaceHeight,
                View.MeasureSpec.EXACTLY);

        mLayout.measure(measuredWidth, measuredHeight);
        mLayout.layout(0, 0, mLayout.getMeasuredWidth(), mLayout.getMeasuredHeight());
    }
    private synchronized void repaint() {
        Canvas canvas = null;

        try {
            canvas = mHolder.lockCanvas();
        } catch (RuntimeException e) {
            Log.d(TAG, "lockCanvas failed", e);
        }

        if (canvas != null) {
            canvas.drawColor(Color.BLACK);
            mLayout.draw(canvas);

            try {
                mHolder.unlockCanvasAndPost(canvas);
            } catch (RuntimeException e) {
                Log.d(TAG, "unlockCanvasAndPost failed", e);
            }
        }
    }
    /**
     * Redraws the compass in the background.
     */
    private class RenderThread extends Thread {
        private boolean mShouldRun;

        /**
         * Initializes the background rendering thread.
         */
        public RenderThread() {
            mShouldRun = true;
        }

        /**
         * Returns true if the rendering thread should continue to run.
         *
         * @return true if the rendering thread should continue to run
         */
        private synchronized boolean shouldRun() {
            return mShouldRun;
        }

        /**
         * Requests that the rendering thread exit at the next opportunity.
         */
        public synchronized void quit() {
            mShouldRun = false;
        }
        Time mTime = new Time();
        @Override
        public void run() {
            while (shouldRun()) {
                long frameStart = SystemClock.elapsedRealtime();
                repaint();
                long frameLength = SystemClock.elapsedRealtime() - frameStart;
                mTime.setToNow();
                mTipsView.setText(mTime.toString());
                long sleepTime = FRAME_TIME_MILLIS - frameLength;
                if (sleepTime > 0) {
                    SystemClock.sleep(sleepTime);
                }
            }
        }
    }
}
