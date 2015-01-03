package de.fithud.fithud;

import com.androidplot.Plot;
import com.androidplot.pie.PieChart;
import com.androidplot.pie.PieRenderer;
import com.androidplot.pie.Segment;
import com.androidplot.pie.SegmentFormatter;
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
import android.widget.ImageView;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainImmersion extends Activity {

    private CardScrollView mCardScrollView;
    private List<CardBuilder> mCards;
    private ExampleCardScrollAdapter mAdapter;

    // Timer variables

    Timer timer;
    TimerTask timerTask;
    final Handler handler = new Handler();

    // Plotting Variables
    private SimpleXYSeries speedSeries = null;
    private SimpleXYSeries heartSeries = null;
    private SimpleXYSeries heightSeries = null;
    private TextView speedText;
    private TextView heartText;
    private TextView heightText;
    private TextView terrainRoadText;
    private TextView terrainOffroadText;
    private TextView terrainAsphaltText;
    private XYPlot speedPlot = null;
    private XYPlot heartPlot = null;
    private XYPlot heightPlot = null;
    private static final int HISTORY_SIZE = 50;
    private static double sin_counter = 0.0;
    private static boolean plot_speed = false;
    private static boolean plot_heart = false;
    private static boolean plot_height = false;
    private static boolean plot_terrain = false;
    private ImageView imageview = null;

    private PieChart terrainPie;

    private Segment s1;
    private Segment s2;
    private Segment s3;

    @Override
    public boolean onCreatePanelMenu(int featureId, Menu menu) {
        if (featureId == WindowUtils.FEATURE_VOICE_COMMANDS || featureId == Window.FEATURE_OPTIONS_PANEL) {
            getMenuInflater().inflate(R.menu.main, menu);
            return true;
        }
        return super.onCreatePanelMenu(featureId, menu);
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        if (featureId == WindowUtils.FEATURE_VOICE_COMMANDS || featureId == Window.FEATURE_OPTIONS_PANEL) {
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

    public void initializePlot(XYPlot plot, String choice) {
        Widget gw = plot.getGraphWidget();

        // FILL mode with values of 0 means fill 100% of container:
        SizeMetrics sm = new SizeMetrics(0, SizeLayoutType.FILL, 0, SizeLayoutType.FILL);
        gw.setSize(sm);

        gw.position(0, XLayoutStyle.ABSOLUTE_FROM_LEFT, 0, YLayoutStyle.ABSOLUTE_FROM_TOP);

        plot.setBorderStyle(Plot.BorderStyle.NONE, null, null);
        plot.setPlotMargins(0, 0, 0, 0);
        plot.setPlotPadding(0, 0, 0, 0);
        plot.setGridPadding(0, 0, 0, 0);

        plot.setBackgroundColor(Color.TRANSPARENT);
        plot.getGraphWidget().getBackgroundPaint().setColor(Color.TRANSPARENT);
        plot.getGraphWidget().getGridBackgroundPaint().setColor(Color.TRANSPARENT);

        plot.getGraphWidget().getDomainLabelPaint().setColor(Color.TRANSPARENT);
        plot.getGraphWidget().getRangeLabelPaint().setColor(Color.TRANSPARENT);

        // ACHSEN

        plot.getGraphWidget().getDomainOriginLabelPaint().setColor(Color.TRANSPARENT);
        plot.getGraphWidget().getDomainOriginLinePaint().setColor(Color.TRANSPARENT);
        plot.getGraphWidget().getRangeOriginLinePaint().setColor(Color.TRANSPARENT);
        plot.getGraphWidget().getRangeSubGridLinePaint().setColor(Color.TRANSPARENT);
        plot.getGraphWidget().getDomainSubGridLinePaint().setColor(Color.TRANSPARENT);

        // GRIDLINE
        plot.getGraphWidget().getDomainGridLinePaint().setColor(Color.TRANSPARENT);
        plot.getGraphWidget().getRangeGridLinePaint().setColor(Color.TRANSPARENT);

        // Remove legend
        plot.getLayoutManager().remove(plot.getLegendWidget());
        plot.getLayoutManager().remove(plot.getDomainLabelWidget());
        plot.getLayoutManager().remove(plot.getRangeLabelWidget());
        plot.getLayoutManager().remove(plot.getTitleWidget());

        plot.setMarkupEnabled(false);

        if (choice.equalsIgnoreCase("speed")) {
            speedSeries = new SimpleXYSeries("Speed");
            speedSeries.useImplicitXVals();
            plot.addSeries(speedSeries,
                    new LineAndPointFormatter(
                            Color.rgb(100, 100, 200), Color.BLUE, Color.BLUE, null));
        }

        if (choice.equalsIgnoreCase("heart")) {
            heartSeries = new SimpleXYSeries("heart");
            heartSeries.useImplicitXVals();
            plot.addSeries(heartSeries,
                    new LineAndPointFormatter(
                            Color.rgb(100, 100, 200), Color.BLUE, Color.BLUE, null));
        }

        if (choice.equalsIgnoreCase("height")) {
            heightSeries = new SimpleXYSeries("height");
            heightSeries.useImplicitXVals();
            plot.addSeries(heightSeries,
                    new LineAndPointFormatter(
                            Color.rgb(100, 100, 200), Color.BLUE, Color.BLUE, null));
        }

        plot.setRangeBoundaries(10, 30, BoundaryMode.FIXED);
        plot.setDomainBoundaries(0, HISTORY_SIZE, BoundaryMode.FIXED);


        plot.setDomainStepMode(XYStepMode.INCREMENT_BY_VAL);
        plot.setDomainStepValue(1);
        plot.setTicksPerRangeLabel(1);

        if (choice.equalsIgnoreCase("speed")) {
            plot.setDomainLabel("Time [s]");
            plot.setRangeLabel("Speed [m/s]");
            final PlotStatistics histStatsSpeed = new PlotStatistics(1000, false);
            plot.addListener(histStatsSpeed);
        } else if (choice.equalsIgnoreCase("heart")) {
            plot.setDomainLabel("Time [s]");
            plot.setRangeLabel("Heartrate [bpm]");
            final PlotStatistics histStatsHeart = new PlotStatistics(1000, false);
            plot.addListener(histStatsHeart);
        } else if (choice.equalsIgnoreCase("height")) {
            plot.setDomainLabel("Time [s]");
            plot.setRangeLabel("Height [m]");
            final PlotStatistics histStatsHeight = new PlotStatistics(1000, false);
            plot.addListener(histStatsHeight);
        }

        plot.getDomainLabelWidget().pack();
        plot.getRangeLabelWidget().pack();
        plot.setRangeValueFormat(new DecimalFormat("#"));
        plot.setDomainValueFormat(new DecimalFormat("#"));
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


        speedPlot = (XYPlot) findViewById(R.id.dynamicXYPlot);
        speedText = (TextView) findViewById(R.id.textView2);

        heartPlot = (XYPlot) findViewById(R.id.heartRatePlot);
        heartText = (TextView) findViewById(R.id.heartRateText);

        heightPlot = (XYPlot) findViewById(R.id.heightPlot);
        heightText = (TextView) findViewById(R.id.heightText);

        initializePlot(speedPlot, "speed");
        initializePlot(heartPlot, "heart");
        initializePlot(heightPlot, "height");
    }

    public void plotSpeedData(int speed) {
        Log.d("FitHUD", "plot speed now");
        if (speedSeries.size() > HISTORY_SIZE) {
            speedSeries.removeFirst();
        }
        speedSeries.addLast(null, speed);
        speedPlot.redraw();
        speedText.setText("Your current speed: " + speed + " km/h");

    }

    public void plotHeartData(int heartrate) {
        Log.d("FitHUD", "plot heartrate now");
        if (heartSeries.size() > HISTORY_SIZE) {
            heartSeries.removeFirst();
        }
        heartSeries.addLast(null, heartrate);
        heartPlot.redraw();
        heartText.setText("Your current heartrate: " + heartrate + " bpm");
    }

    public void plotHeightData(int height) {
        Log.d("FitHUD", "plot height now");
        if (heightSeries.size() > HISTORY_SIZE) {
            heightSeries.removeFirst();
        }
        heightSeries.addLast(null, height);
        heightPlot.redraw();
        heightText.setText("Your current height: " + height + " m");
    }

    public void plotTerrainPie(int s1_val, int s2_val, int s3_val, PieChart work_pie)
    {
        Log.d("FitHUD", "test2 "+work_pie);
        s1 = new Segment("offroad", s1_val);
        s2 = new Segment("road", s2_val);
        s3 = new Segment("asphalt", s3_val);

        terrainOffroadText.setText("Offroad: "+s1_val+"%");
        terrainRoadText.setText("Road: "+s2_val+"%");
        terrainAsphaltText.setText("Asphalt: "+s3_val+"%");

        work_pie.clear();

        SegmentFormatter sf1 =  new SegmentFormatter(Color.rgb(106, 168, 79), Color.BLACK,Color.BLACK, Color.BLACK);
        SegmentFormatter sf2 =  new SegmentFormatter(Color.rgb(255, 0, 0), Color.BLACK,Color.BLACK, Color.BLACK);
        SegmentFormatter sf3 =  new SegmentFormatter(Color.rgb(255, 153, 0), Color.BLACK,Color.BLACK, Color.BLACK);

        work_pie.addSeries(s1, sf1);
        work_pie.addSeries(s2, sf2);
        work_pie.addSeries(s3, sf3);

        work_pie.getRenderer(PieRenderer.class).setDonutSize(0/100f,
                PieRenderer.DonutMode.PERCENT);
        work_pie.redraw();
    }


    public void initializeTerrainPie(PieChart init_pie){
        init_pie.clear();

        Widget gw = init_pie.getPieWidget();

        // FILL mode with values of 0 means fill 100% of container:
        SizeMetrics sm = new SizeMetrics(0, SizeLayoutType.FILL, 0, SizeLayoutType.FILL);
        gw.setSize(sm);

        gw.position(0, XLayoutStyle.ABSOLUTE_FROM_LEFT, 0, YLayoutStyle.ABSOLUTE_FROM_TOP);

        init_pie.setBorderStyle(Plot.BorderStyle.NONE, null, null);
        init_pie.setPlotMargins(0, 0, 0, 0);
        init_pie.setPlotPadding(0, 0, 0, 0);
        init_pie.setPadding(0,0,0,0);

        init_pie.getBorderPaint().setColor(Color.TRANSPARENT);
        init_pie.getBackgroundPaint().setColor(Color.TRANSPARENT);


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

        //schedule the timer, after the first 5000ms the TimerTask will run every 500ms
        timer.schedule(timerTask, 5000, 500); //
    }

    public void initializeTimerTask() {

        timerTask = new TimerTask() {
            public void run() {

                //use a handler to run the plots
                handler.post(new Runnable() {
                    public void run() {
                        sin_counter = sin_counter + Math.PI / 10;
                        double speed = Math.sin(sin_counter) * 10 + 20.0;
                        Log.d("FitHUD", "test "+mCardScrollView.getSelectedItemPosition());

                        switch (mCardScrollView.getSelectedItemPosition()) {

                            case 0:
                                plot_speed = true;
                                plot_heart = false;
                                plot_height = false;
                                plot_terrain = false;
                                while (heartSeries.size() > 0) {
                                    heartSeries.removeLast();
                                }
                                while (heightSeries.size() > 0) {
                                    heightSeries.removeLast();
                                }
                                break;
                            case 1:
                                plot_speed = false;
                                plot_heart = true;
                                plot_height = false;
                                plot_terrain = false;
                                while (speedSeries.size() > 0) {
                                    speedSeries.removeLast();
                                }
                                while (heightSeries.size() > 0) {
                                    heightSeries.removeLast();
                                }
                                break;

                            case 2:
                                plot_speed = false;
                                plot_heart = false;
                                plot_height = true;
                                plot_terrain = false;
                                while (speedSeries.size() > 0) {
                                    speedSeries.removeLast();
                                }
                                while (heartSeries.size() > 0) {
                                    heartSeries.removeLast();
                                }
                                break;

                            case 3:
                                plot_speed = false;
                                plot_heart = false;
                                plot_height = false;
                                plot_terrain = true;
                                while (speedSeries.size() > 0) {
                                    speedSeries.removeLast();
                                }
                                while (heartSeries.size() > 0) {
                                    heartSeries.removeLast();
                                }
                                while (heightSeries.size() > 0) {
                                    heightSeries.removeLast();
                                }

                                terrainPie = (PieChart) mCardScrollView.getSelectedView().findViewById(R.id.terrainPie);
                                terrainRoadText = (TextView) mCardScrollView.getSelectedView().findViewById(R.id.terrainRoadText);
                                terrainAsphaltText = (TextView) mCardScrollView.getSelectedView().findViewById(R.id.terrainAsphaltText);
                                terrainOffroadText = (TextView) mCardScrollView.getSelectedView().findViewById(R.id.terrainOffroadText);
                                break;
                        }

                        if (plot_speed) {
                            plotSpeedData((int) speed);
                        }

                        if (plot_heart) {
                            plotHeartData((int) speed);
                        }

                        if (plot_height){
                            plotHeightData((int) speed);
                        }

                        if (plot_terrain){
                            initializeTerrainPie(terrainPie);
                            plotTerrainPie(33,33,33,terrainPie);
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

        mCards.add(new CardBuilder(this, CardBuilder.Layout.EMBED_INSIDE)
                .setEmbeddedLayout(R.layout.heartrate)
                .setFootnote("You are dead"));

        mCards.add(new CardBuilder(this, CardBuilder.Layout.EMBED_INSIDE)
                .setEmbeddedLayout(R.layout.height)
                .setFootnote("Very low"));

        mCards.add(new CardBuilder(this, CardBuilder.Layout.EMBED_INSIDE)
                .setEmbeddedLayout(R.layout.terrain)
                .setFootnote("Equally weighted"));

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
        public int getItemViewType(int position) {
            return mCards.get(position).getItemViewType();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return mCards.get(position).getView(convertView, parent);
        }
    }
}
