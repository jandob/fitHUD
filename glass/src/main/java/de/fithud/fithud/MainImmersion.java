package de.fithud.fithud;

import com.androidplot.Plot;
import com.androidplot.ui.AnchorPosition;
import com.androidplot.ui.LayoutManager;
import com.androidplot.ui.SizeLayoutType;
import com.androidplot.ui.SizeMetrics;
import com.androidplot.ui.XLayoutStyle;
import com.androidplot.ui.YLayoutStyle;
import com.androidplot.ui.widget.Widget;
import com.androidplot.util.PlotStatistics;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYStepMode;
import com.google.android.glass.view.WindowUtils;
import com.google.android.glass.widget.CardBuilder;
import com.google.android.glass.widget.CardScrollAdapter;
import com.google.android.glass.widget.CardScrollView;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainImmersion extends Activity {

    private CardScrollView mCardScrollView;
    private List<CardBuilder> mCards;
    private ExampleCardScrollAdapter mAdapter;
    private TextView speedText;
    Timer timer;
    TimerTask timerTask;
    private SimpleXYSeries speedSeries = null;

    final Handler handler = new Handler();
    private XYPlot mySimpleXYPlot = null;
    private static final int HISTORY_SIZE = 50;
    private static double sin_counter = 0.0;
    private static boolean plot_speed = false;
    private static boolean plot_heart = false;
    private static boolean plot_height = false;

    @Override
    public boolean onCreatePanelMenu(int featureId, Menu menu){
        if (featureId == WindowUtils.FEATURE_VOICE_COMMANDS || featureId ==  Window.FEATURE_OPTIONS_PANEL) {
            getMenuInflater().inflate(R.menu.main, menu);
            return true;
        }
        return super.onCreatePanelMenu(featureId, menu);
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        if (featureId == WindowUtils.FEATURE_VOICE_COMMANDS || featureId ==  Window.FEATURE_OPTIONS_PANEL) {
            switch (item.getItemId()) {
                case R.id.currentSpeed:
                    mCardScrollView.setSelection(0);
                    break;
                case R.id.heartRate:
                    mCardScrollView.setSelection(1);
                    break;
                case R.id.showHeight:
                    mCardScrollView.setSelection(2);
                    break;
                case R.id.showWhatever:
                    //setContentView(R.layout.heartrate);
                    //startService(new Intent(this, FHLiveCardService.class));
                    break;
            }
            return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }

    @Override
    protected void onCreate(Bundle bundle) {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().requestFeature(WindowUtils.FEATURE_VOICE_COMMANDS);

        super.onCreate(bundle);

        createCards();

        mCardScrollView = new CardScrollView(this);

        // Handle the TAP event.
        mCardScrollView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                openOptionsMenu();
            }
        });

        mAdapter = new ExampleCardScrollAdapter();
        mCardScrollView.setAdapter(mAdapter);
        mCardScrollView.activate();
        setContentView(mCardScrollView);

        mySimpleXYPlot = (XYPlot) findViewById(R.id.dynamicXYPlot);
        speedText = (TextView) findViewById(R.id.textView2);

        Widget gw = mySimpleXYPlot.getGraphWidget();

        // FILL mode with values of 0 means fill 100% of container:
        SizeMetrics sm = new SizeMetrics(0, SizeLayoutType.FILL, 0,SizeLayoutType.FILL);
        gw.setSize(sm);

        gw.position(0, XLayoutStyle.ABSOLUTE_FROM_LEFT, 0, YLayoutStyle.ABSOLUTE_FROM_TOP);

        mySimpleXYPlot.setBorderStyle(Plot.BorderStyle.NONE, null, null);
        mySimpleXYPlot.setPlotMargins(0, 0, 0, 0);
        mySimpleXYPlot.setPlotPadding(0, 0, 0, 0);
        mySimpleXYPlot.setGridPadding(0, 0, 0, 0);

        mySimpleXYPlot.setBackgroundColor(Color.TRANSPARENT);
        mySimpleXYPlot.getGraphWidget().getBackgroundPaint().setColor(Color.TRANSPARENT);
        mySimpleXYPlot.getGraphWidget().getGridBackgroundPaint().setColor(Color.TRANSPARENT);

        mySimpleXYPlot.getGraphWidget().getDomainLabelPaint().setColor(Color.TRANSPARENT);
        mySimpleXYPlot.getGraphWidget().getRangeLabelPaint().setColor(Color.TRANSPARENT);

        // ACHSEN
        mySimpleXYPlot.getGraphWidget().getDomainOriginLabelPaint().setColor(Color.TRANSPARENT);
        mySimpleXYPlot.getGraphWidget().getDomainOriginLinePaint().setColor(Color.TRANSPARENT);
        mySimpleXYPlot.getGraphWidget().getRangeOriginLinePaint().setColor(Color.TRANSPARENT);
        mySimpleXYPlot.getGraphWidget().getRangeSubGridLinePaint().setColor(Color.TRANSPARENT);
        mySimpleXYPlot.getGraphWidget().getDomainSubGridLinePaint().setColor(Color.TRANSPARENT);

        // GRIDLINE
        mySimpleXYPlot.getGraphWidget().getDomainGridLinePaint().setColor(Color.TRANSPARENT);
        mySimpleXYPlot.getGraphWidget().getRangeGridLinePaint().setColor(Color.TRANSPARENT);

        // Remove legend
        mySimpleXYPlot.getLayoutManager().remove(mySimpleXYPlot.getLegendWidget());
        mySimpleXYPlot.getLayoutManager().remove(mySimpleXYPlot.getDomainLabelWidget());
        mySimpleXYPlot.getLayoutManager().remove(mySimpleXYPlot.getRangeLabelWidget());
        mySimpleXYPlot.getLayoutManager().remove(mySimpleXYPlot.getTitleWidget());

        mySimpleXYPlot.setMarkupEnabled(false);

        speedSeries = new SimpleXYSeries("Speed");
        speedSeries.useImplicitXVals();

        mySimpleXYPlot.setRangeBoundaries(10,30, BoundaryMode.FIXED);
        mySimpleXYPlot.setDomainBoundaries(0, HISTORY_SIZE, BoundaryMode.FIXED);

        mySimpleXYPlot.addSeries(speedSeries,
                new LineAndPointFormatter(
                        Color.rgb(100, 100, 200), Color.BLUE, Color.BLUE, null));

        mySimpleXYPlot.setDomainStepMode(XYStepMode.INCREMENT_BY_VAL);
        mySimpleXYPlot.setDomainStepValue(1);
        mySimpleXYPlot.setTicksPerRangeLabel(1);

        mySimpleXYPlot.setDomainLabel("Time [s]");
        mySimpleXYPlot.getDomainLabelWidget().pack();
        mySimpleXYPlot.setRangeLabel("Speed [m/s]");
        mySimpleXYPlot.getRangeLabelWidget().pack();

        mySimpleXYPlot.setRangeValueFormat(new DecimalFormat("#"));
        mySimpleXYPlot.setDomainValueFormat(new DecimalFormat("#"));

        final PlotStatistics histStats = new PlotStatistics(1000, false);

        mySimpleXYPlot.addListener(histStats);

    }

    public void plotSpeedData(int speed){
        if (speedSeries.size() > HISTORY_SIZE) {
            speedSeries.removeFirst();
        }
        speedSeries.addLast(null, speed);
        mySimpleXYPlot.redraw();
        speedText.setText("Your current speed: "+speed+" km/h");
        mAdapter.notifyDataSetChanged();
    }

    public void stoptimertask() {
        //stop the timer, if it's not already null
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    public void startTimer() {
        //set a new Timer
        timer = new Timer();

        //initialize the TimerTask's job
        initializeTimerTask();

        //schedule the timer, after the first 5000ms the TimerTask will run every 10000ms
        timer.schedule(timerTask, 5000, 500); //
    }

    public void initializeTimerTask() {

        timerTask = new TimerTask() {
            public void run() {

                //use a handler to run a toast that shows the current timestamp
                handler.post(new Runnable() {
                    public void run() {
                        sin_counter = sin_counter + Math.PI/10;
                        double speed = Math.sin(sin_counter)*10+20.0;

                        switch (mCardScrollView.getSelectedItemPosition()) {
                            case 0: plot_speed = true;
                                    plot_heart = false;
                                    plot_height = false;
                                break;
                            case 1: plot_speed = false;
                                plot_heart = true;
                                plot_height = false;
                                while(speedSeries.size()>0){
                                    speedSeries.removeLast();
                                }

                            case 2: plot_speed = false;
                                plot_heart = false;
                                plot_height = true;
                                while(speedSeries.size()>0){
                                    speedSeries.removeLast();
                                }
                        }
                        if(plot_speed) {
                            plotSpeedData((int) speed);
                        }
                    }
                });
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        mCardScrollView.activate();
        startTimer();
    }

    @Override
    protected void onPause() {
        mCardScrollView.deactivate();
        super.onPause();
        stoptimertask();
    }

    private void createCards() {
        mCards = new ArrayList<CardBuilder>();

        mCards.add(new CardBuilder(this, CardBuilder.Layout.EMBED_INSIDE)
                .setEmbeddedLayout(R.layout.currentspeed)
                .setFootnote("Slower than a turtle")
                .setTimestamp("right now"));

        mCards.add(new CardBuilder(this, CardBuilder.Layout.CAPTION)
                .setText("Current heart rate: 0bpm")
                .setFootnote("You are dead"));

        mCards.add(new CardBuilder(this, CardBuilder.Layout.TEXT)
                .setText("Hate: 100m pure"));
    }

    private class ExampleCardScrollAdapter extends CardScrollAdapter {

        @Override
        public int getPosition(Object item) {
            return mCards.indexOf(item);
        }

        @Override
        public int getCount() {
            return mCards.size();
        }

        @Override
        public Object getItem(int position) {
            return mCards.get(position);
        }

        @Override
        public int getViewTypeCount() {
            return CardBuilder.getViewTypeCount();
        }

        @Override
        public int getItemViewType(int position){
            return mCards.get(position).getItemViewType();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return mCards.get(position).getView(convertView, parent);
        }
    }
}
