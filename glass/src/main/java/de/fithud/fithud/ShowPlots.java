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
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import de.fithud.fithudlib.FHSensorManager;
import de.fithud.fithudlib.MessengerClient;
import de.fithud.fithudlib.MessengerConnection;
import de.fithud.fithudlib.MessengerServiceActivity;

/**
 * An {@link android.app.Activity} showing a tuggable "Hello World!" card.
 * <p/>
 * The main content view is composed of a one-card {@link com.google.android.glass.widget.CardScrollView} that provides tugging
 * feedback to the user when swipe gestures are detected.
 * If your Glassware intends to intercept swipe gestures, you should set the content view directly
 * and use a {@link com.google.android.glass.touchpad.GestureDetector}.
 *
 * @see <a href="https://developers.google.com/glass/develop/gdk/touch">GDK Developer Guide</a>
 */
public class ShowPlots extends Activity implements MessengerClient {
    //CardBuilder mCard;
    MessengerConnection conn = new MessengerConnection(this);
    private final String TAG = "ShowPlots";
    private CardScrollView mCardScrollView;
    //private View mView;
    private List<CardBuilder> mCards;
    private ExampleCardScrollAdapter mAdapter;
    public static float last_speed = 0;
    public static int last_revolutions = 0;
    private static final double wheel_type = 4.4686;

    public boolean speedometer_connected = false;
    public boolean heartrate_conected = false;
    public boolean cadence_connected = false;

    View sensorview;
    View speedview;
    View heartview;
    RemoteViews heightview;
    View terrainview;

    @Override
    public void handleMessage(Message msg) {
        Log.i(TAG, "handling Msg");
        switch (msg.what) {
            case FHSensorManager.Messages.HEARTRATE_MESSAGE:
                int heartRate[] = msg.getData().getIntArray("value");
                addHeartData((int) heartRate[0]);
                heart_sensor = (int) heartRate[0];
                Log.i(TAG, "Heartrate " + heartRate[0]);
                break;
            case FHSensorManager.Messages.CADENCE_MESSAGE:
                int[] cadence = msg.getData().getIntArray("value");
                Log.i(TAG, "Cadence_rev: " + cadence[0] + " Cadence_time: " + cadence[1]);
                break;
            case FHSensorManager.Messages.SPEED_MESSAGE:
                int speed_dataset[] = msg.getData().getIntArray("value");
                float time_difference = 0;
                int revolutions_difference = speed_dataset[0] - last_revolutions;
                if (speed_dataset[1] < last_speed) {
                    time_difference = (float) speed_dataset[1] + 65536 - last_speed;
                } else {
                    time_difference = (float) speed_dataset[1] - last_speed;
                }
                last_speed = (float) speed_dataset[1];
                last_revolutions = speed_dataset[0];

                time_difference = time_difference / 1024;
                double speed = 0;
                if (time_difference > 0) {
                    speed = ((revolutions_difference * wheel_type) / time_difference) * 3.6;
                } else {
                    speed = 0;
                }

                Log.i(TAG, "Speed_rev: " + speed_dataset[0] + " speed_time: " + speed_dataset[1]);
                Log.i(TAG, "Speed: " + speed);

                speed_sensor = (int) speed;
                addSpeedData(speed_sensor);
                break;
            case FHSensorManager.Messages.SENSOR_STATUS_MESSAGE:
                int[] sensor_status = msg.getData().getIntArray("value");
                if (sensor_status[0] == 1) {
                    heartrate_conected = true;
                } else {
                    heartrate_conected = false;
                }
                if (sensor_status[1] == 1) {
                    speedometer_connected = true;
                } else {
                    heartrate_conected = false;
                }
                if (sensor_status[2] == 1) {
                    cadence_connected = true;
                } else {
                    heartrate_conected = false;
                }
                break;
        }
    }

    //private GestureDetector mGestureDetector;
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


    private TextView heightTopLabel;
    private TextView heightBottomLabel;

    private TextView speedTopLabel;
    private TextView speedBottomLabel;

    private TextView heartTopLabel;
    private TextView heartBottomLabel;

    private static final int HISTORY_SIZE = 50;
    private static double sin_counter = 0.0;
    private static boolean plot_speed = false;
    private static boolean plot_heart = false;
    private static boolean plot_height = false;
    private static boolean plot_terrain = false;

    private static boolean init_speed = false;
    private static boolean init_heart = false;
    private static boolean init_height = false;
    private static boolean init_terrain = false;
    private ImageView imageview = null;

    private PieChart terrainPie;

    private Segment s1;
    private Segment s2;
    private Segment s3;

    private int speed_sensor = 0;
    private int heart_sensor = 0;


    @Override
    public boolean onCreatePanelMenu(int featureId, Menu menu) {
        if (featureId == WindowUtils.FEATURE_VOICE_COMMANDS || featureId == Window.FEATURE_OPTIONS_PANEL) {
            getMenuInflater().inflate(R.menu.showplotsmenu, menu);
            return true;
        }
        return super.onCreatePanelMenu(featureId, menu);
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        if (featureId == WindowUtils.FEATURE_VOICE_COMMANDS || featureId == Window.FEATURE_OPTIONS_PANEL) {
            switch (item.getItemId()) {
                case R.id.currentSpeedPlotMenu:
                    mCardScrollView.setSelection(0);
                    break;
                case R.id.heartRatePlotMenu:
                    mCardScrollView.setSelection(1);
                    break;
                case R.id.showHeightPlotMenu:
                    mCardScrollView.setSelection(2);
                    break;
                case R.id.showTerrainPlotMenu:
                    mCardScrollView.setSelection(3);
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

            plot.addSeries(speedSeries,
                    new LineAndPointFormatter(
                            Color.rgb(100, 100, 200), Color.BLUE, Color.BLUE, null));
            plot.setRangeBoundaries(10, 30, BoundaryMode.FIXED);
            speedTopLabel.setText("30");
            speedBottomLabel.setText("10");
        }

        if (choice.equalsIgnoreCase("heart")) {

            plot.addSeries(heartSeries,
                    new LineAndPointFormatter(
                            Color.rgb(100, 100, 200), Color.BLUE, Color.BLUE, null));
            plot.setRangeBoundaries(50, 130, BoundaryMode.FIXED);
            heartTopLabel.setText("130");
            heartBottomLabel.setText("50");
        }

        if (choice.equalsIgnoreCase("height")) {

            plot.addSeries(heightSeries,
                    new LineAndPointFormatter(
                            Color.rgb(100, 100, 200), Color.BLUE, Color.BLUE, null));
            plot.setRangeBoundaries(10, 30, BoundaryMode.FIXED);
            heightTopLabel.setText("30");
            heightBottomLabel.setText("10");
        }


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

    public void initSeries() {
        speedSeries = new SimpleXYSeries("Speed");
        speedSeries.useImplicitXVals();

        heartSeries = new SimpleXYSeries("heart");
        heartSeries.useImplicitXVals();

        heightSeries = new SimpleXYSeries("height");
        heightSeries.useImplicitXVals();
    }

    @Override
    protected void onCreate(Bundle bundle) {
        conn.connect(FHSensorManager.class);

        init_heart = false;
        init_height = false;
        init_speed = false;
        init_terrain = false;

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().requestFeature(WindowUtils.FEATURE_VOICE_COMMANDS);

        super.onCreate(bundle);
        createCards();

        mCardScrollView = new CardScrollView(this);

        // Handle the TAP event.
        mCardScrollView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //openOptionsMenu();
                switch (mCardScrollView.getSelectedItemPosition()) {

                }
            }
        });

        mAdapter = new ExampleCardScrollAdapter();
        mCardScrollView.setAdapter(mAdapter);
        mCardScrollView.activate();
        setContentView(mCardScrollView);
        initSeries();
        //mCardScrollView.setSelection(1);
    }

    public void addSpeedData(int speed) {
        //if (init_speed) {
        Log.d("FitHUD", "add speed now");
        if (speedSeries.size() > HISTORY_SIZE) {
            speedSeries.removeFirst();
        }
        speedSeries.addLast(null, speed);
        //}
    }

    public void plotSpeedData(int speed) {
        if (init_speed) {
            Log.d("FitHUD", "plot speed now");
            if (plot_speed) {
                speedPlot.redraw();
                speedText.setText("Your current speed: " + speed + " km/h");
            }
        }
    }

    public void addHeartData(int heartrate) {
        if (init_heart) {
            Log.d("FitHUD", "add heartrate now");
            if (heartSeries.size() > HISTORY_SIZE) {
                heartSeries.removeFirst();
            }
            heartSeries.addLast(null, heartrate);
        }
    }

    public void plotHeartData(int heartrate) {
        if (init_heart) {
            if (plot_heart) {
                Log.d("FitHUD", "plot heartrate now");
                heartPlot.redraw();
                heartText.setText("Your current heartrate: " + heartrate + " bpm");
            }
        }
    }

    public void addHeightData(int height) {
        if (init_height) {
            Log.d("FitHUD", "add height now");
            if (heightSeries.size() > HISTORY_SIZE) {
                heightSeries.removeFirst();
            }
            heightSeries.addLast(null, height);
        }
    }

    public void plotHeightData(int height) {
        if (init_height) {
            if (plot_height) {
                Log.d("FitHUD", "Plot height now");
                heightPlot.redraw();
                heightText.setText("Your current height: " + height + " m");
            }
        }
    }

    public void plotTerrainPie(int s1_val, int s2_val, int s3_val, PieChart work_pie) {
        Log.d("FitHUD", "test2 " + work_pie);
        s1 = new Segment("offroad", s1_val);
        s2 = new Segment("road", s2_val);
        s3 = new Segment("asphalt", s3_val);

        terrainOffroadText.setText("Offroad: " + s1_val + "%");
        terrainRoadText.setText("Road: " + s2_val + "%");
        terrainAsphaltText.setText("Asphalt: " + s3_val + "%");

        work_pie.clear();

        SegmentFormatter sf1 = new SegmentFormatter(Color.rgb(106, 168, 79), Color.BLACK, Color.BLACK, Color.BLACK);
        SegmentFormatter sf2 = new SegmentFormatter(Color.rgb(255, 0, 0), Color.BLACK, Color.BLACK, Color.BLACK);
        SegmentFormatter sf3 = new SegmentFormatter(Color.rgb(255, 153, 0), Color.BLACK, Color.BLACK, Color.BLACK);

        work_pie.addSeries(s1, sf1);
        work_pie.addSeries(s2, sf2);
        work_pie.addSeries(s3, sf3);

        work_pie.getRenderer(PieRenderer.class).setDonutSize(0 / 100f,
                PieRenderer.DonutMode.PERCENT);
        work_pie.redraw();
    }


    public void initializeTerrainPie(PieChart init_pie) {
        init_pie.clear();

        Widget gw = init_pie.getPieWidget();

        // FILL mode with values of 0 means fill 100% of container:
        SizeMetrics sm = new SizeMetrics(0, SizeLayoutType.FILL, 0, SizeLayoutType.FILL);
        gw.setSize(sm);

        gw.position(0, XLayoutStyle.ABSOLUTE_FROM_LEFT, 0, YLayoutStyle.ABSOLUTE_FROM_TOP);

        init_pie.setBorderStyle(Plot.BorderStyle.NONE, null, null);
        init_pie.setPlotMargins(0, 0, 0, 0);
        init_pie.setPlotPadding(0, 0, 0, 0);
        init_pie.setPadding(0, 0, 0, 0);

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
        timer.schedule(timerTask, 2000, 1000); //
    }

    public void initializeTimerTask() {

        timerTask = new TimerTask() {
            public void run() {

                //use a handler to run the plots
                handler.post(new Runnable() {
                    public void run() {

                        sin_counter = sin_counter + Math.PI / 10;
                        double speed = Math.sin(sin_counter) * 10 + 20.0;
                        switch (mCardScrollView.getSelectedItemPosition()) {

                            case 0:
                                speedPlot = (XYPlot) (mCardScrollView.getSelectedView().findViewById(R.id.dynamicXYPlot));
                                speedText = (TextView) mCardScrollView.getSelectedView().findViewById(R.id.textView2);
                                speedTopLabel = (TextView) mCardScrollView.findViewById(R.id.labelTopSpeed);
                                speedBottomLabel = (TextView) mCardScrollView.findViewById(R.id.labelBottomSpeed);

                                //Log.d("FitHUD", "speedPlot " + speedText);

                                if (!init_speed) {
                                    initializePlot(speedPlot, "speed");
                                    init_speed = true;
                                    speedPlot.setVisibility(View.VISIBLE);
                                } else {
                                    speedPlot.setVisibility(View.VISIBLE);
                                }
                                plot_speed = true;
                                plot_heart = false;
                                plot_height = false;
                                plot_terrain = false;
                                /*
                                if (init_heart) {
                                    while (heartSeries.size() > 0) {
                                        heartSeries.removeLast();
                                    }
                                }
                                if (init_height) {
                                    while (heightSeries.size() > 0) {
                                        heightSeries.removeLast();
                                    }
                                }
                                */
                                break;
                            case 1:
                                heartPlot = (XYPlot) (mCardScrollView.getSelectedView().findViewById(R.id.heartRatePlot));
                                heartText = (TextView) mCardScrollView.getSelectedView().findViewById(R.id.heartRateText);
                                heartTopLabel = (TextView) mCardScrollView.findViewById(R.id.labelTopHeart);
                                heartBottomLabel = (TextView) mCardScrollView.findViewById(R.id.labelBottomHeart);
                                if (!init_heart) {
                                    initializePlot(heartPlot, "heart");
                                    init_heart = true;
                                    heartPlot.setVisibility(View.VISIBLE);
                                } else {
                                    heartPlot.setVisibility(View.VISIBLE);
                                }
                                plot_speed = false;
                                plot_heart = true;
                                plot_height = false;
                                plot_terrain = false;

                                break;

                            case 2:
                                heightPlot = (XYPlot) (mCardScrollView.findViewById(R.id.heightPlot));
                                heightText = (TextView) mCardScrollView.findViewById(R.id.heightText);
                                heightTopLabel = (TextView) mCardScrollView.findViewById(R.id.labelTopHeight);
                                heightBottomLabel = (TextView) mCardScrollView.findViewById(R.id.labelBottomHeight);
                                if (!init_height) {
                                    initializePlot(heightPlot, "height");
                                    init_height = true;
                                    heightPlot.setVisibility(View.VISIBLE);
                                } else {
                                    heightPlot.setVisibility(View.VISIBLE);
                                }
                                plot_speed = false;
                                plot_heart = false;
                                plot_height = true;
                                plot_terrain = false;

                                break;

                            case 3:
                                plot_speed = false;
                                plot_heart = false;
                                plot_height = false;
                                plot_terrain = true;

                                terrainPie = (PieChart) mCardScrollView.getSelectedView().findViewById(R.id.terrainPie);
                                terrainRoadText = (TextView) mCardScrollView.getSelectedView().findViewById(R.id.terrainRoadText);
                                terrainAsphaltText = (TextView) mCardScrollView.getSelectedView().findViewById(R.id.terrainAsphaltText);
                                terrainOffroadText = (TextView) mCardScrollView.getSelectedView().findViewById(R.id.terrainOffroadText);
                                Log.i(TAG, "ID pie: " + terrainPie);
                                Log.i(TAG, "ID roadtex: " + terrainRoadText);
                                Log.i(TAG, "ID asphalt: " + terrainAsphaltText);
                                Log.i(TAG, "ID offroadtext: " + terrainOffroadText);
                                break;
                        }

                        if (plot_speed) {
                            plotSpeedData((int) speed_sensor);
                        }

                        if (plot_heart) {

                            plotHeartData((int) heart_sensor );
                        }

                        if (plot_height) {
                            addHeightData((int) speed);
                            plotHeightData((int) speed);
                        }

                        if (plot_terrain) {
                            if (!init_terrain) {
                                initializeTerrainPie(terrainPie);
                                terrainPie.setVisibility(View.VISIBLE);
                            }
                            plotTerrainPie(33, 33, 33, terrainPie);
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

        CardBuilder speedcard = new CardBuilder(this, CardBuilder.Layout.EMBED_INSIDE)
                .setEmbeddedLayout(R.layout.currentspeed)
                .setFootnote("Slower than a turtle")
                .setTimestamp("right now");
//        speedview = speedcard.getView();
        mCards.add(speedcard);

        CardBuilder heartcard = new CardBuilder(this, CardBuilder.Layout.EMBED_INSIDE)
                .setEmbeddedLayout(R.layout.heartrate)
                .setFootnote("You are dead");
        //heartview = heartcard.getView();
        mCards.add(heartcard);

        CardBuilder heightcard = new CardBuilder(this, CardBuilder.Layout.EMBED_INSIDE)
                .setEmbeddedLayout(R.layout.height)
                .setFootnote("Very low");
        mCards.add(heightcard);


        CardBuilder terraincard = new CardBuilder(this, CardBuilder.Layout.EMBED_INSIDE)
                .setEmbeddedLayout(R.layout.terrain)
                .setFootnote("Equally weighted");
        //terrainview = terraincard.getView();
        mCards.add(terraincard);


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        conn.disconnect();
        Log.i(TAG,"Showplots destroyed");
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
