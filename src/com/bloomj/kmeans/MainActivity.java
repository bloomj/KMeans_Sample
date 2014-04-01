package com.bloomj.kmeans;

import java.util.Random;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import android.os.Bundle;
import android.app.Activity;
import android.graphics.Color;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.Toast;

public class MainActivity extends Activity {
	private static final String TAG = "MainActivity";
	
	private static final int K_VALUE = 3;
	private static final int POINTS = 300;
	private int currentStep = 0;
	private final int[] colors = {Color.CYAN, Color.GREEN, Color.MAGENTA, Color.RED, Color.YELLOW};
	private final PointStyle[] styles = {PointStyle.CIRCLE, PointStyle.DIAMOND, PointStyle.SQUARE, PointStyle.TRIANGLE, PointStyle.X};
	private Random rand = new Random();
	private GraphicalView mChartView;
	private XYMultipleSeriesDataset mDataset;
	private XYMultipleSeriesRenderer mRenderer;
	private double[][] centers = new double[K_VALUE][2];
	private XYSeries[] clusters = new XYSeries[K_VALUE];
	private Button mInitBtn;
	private Button mStepBtn;
	private Button mRunBtn;
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		// save the current data, for instance when changing screen orientation
		outState.putSerializable("dataset", mDataset);
		outState.putSerializable("renderer", mRenderer);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedState) {
		super.onRestoreInstanceState(savedState);
		// restore the current data, for instance when changing the screen
		// orientation
		mDataset = (XYMultipleSeriesDataset) savedState.getSerializable("dataset");
		mRenderer = (XYMultipleSeriesRenderer) savedState.getSerializable("renderer");
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mInitBtn = (Button) findViewById(R.id.initBtn);
		mRunBtn = (Button) findViewById(R.id.runBtn);
		mStepBtn = (Button) findViewById(R.id.stepBtn);
		
		mInitBtn.setOnClickListener(new View.OnClickListener() {
	        public void onClick(View v) {
	        	clearDataset();
	        	getInitialDataset();
	        	getRenderer(true, currentStep);

	        	// repaint the chart such as the newly added point to be visible
	        	Log.i(TAG,"Refreshing chart");
	        	mChartView.repaint();
	        	
	        	mStepBtn.setEnabled(true);
	        	mRunBtn.setEnabled(true);
	        }
	      });
		
		mStepBtn.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				//Toast.makeText(v.getContext(), "Clicked Step Button", Toast.LENGTH_SHORT).show();
				
				boolean improved = doKMeansStep(); 
				
				getRenderer(false, currentStep);
				
	        	// repaint the chart such as the newly added point to be visible
	        	Log.i(TAG,"Refreshing chart");
	        	mChartView.repaint();
	        	
	        	if(!improved) {
	        		mStepBtn.setEnabled(false);
	        		mRunBtn.setEnabled(false);
	        	}
			}
		});
		
		mRunBtn.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Toast.makeText(v.getContext(), "Clicked Run Button", Toast.LENGTH_SHORT).show();
				
				boolean improved = true;
				while(improved) {
					improved = doKMeansStep(); 
				}
				
				getRenderer(false, currentStep);
				
	        	// repaint the chart such as the newly added point to be visible
	        	Log.i(TAG,"Refreshing chart");
	        	mChartView.repaint();
	        	
	        	mStepBtn.setEnabled(false);
	        	mRunBtn.setEnabled(false);
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	protected void onResume() {
		super.onResume();
		
		if (mChartView == null) {
			Log.i(TAG,"Initializing Chart View");
			
		    LinearLayout layout = (LinearLayout) findViewById(R.id.chart);
		    mDataset = new XYMultipleSeriesDataset();
		    getInitialDataset();
		    mRenderer = new XYMultipleSeriesRenderer();
		    getRenderer(true, currentStep);
		    mChartView = ChartFactory.getScatterChartView(this, mDataset, mRenderer);
		    layout.addView(mChartView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		} else {
			Log.i(TAG,"Repainting Chart View");
			
		    mChartView.repaint();
		}
	}
	
	private void getInitialDataset() {
		Log.i(TAG,"Initializing New Data set");

		XYSeries series = new XYSeries("Initial Data");
		clusters[0] = new XYSeries("Cluster 1");
		for (int k = 0; k < POINTS; k++) {
			int x = rand.nextInt(500);
			int y = rand.nextInt(500);
			series.add(x, y);
			clusters[0].add(x, y);
		}
		mDataset.addSeries(series);
		
		Log.d(TAG, "Initializing clusters and randomly selecting centers");
		// get random points for initial dataset
		for(int n=0; n<centers.length; n++) {
			if(n > 0) {
				clusters[n] = new XYSeries("Cluster " + String.valueOf(n+1));
			}
			
			Log.d(TAG, "Getting random point");
			int randPoint = rand.nextInt(mDataset.getSeriesAt(0).getItemCount());
			Log.d(TAG, "Got point: " + randPoint);
			centers[n][0] = mDataset.getSeriesAt(0).getX(randPoint);
			centers[n][1] = mDataset.getSeriesAt(0).getY(randPoint);
			
			clusters[n].add(centers[n][0], centers[n][1]);
			if(n > 0) {
				clusters[0].remove(randPoint);
			}
			
			Log.i(TAG, "Cluster " + n + " center | x: " + centers[n][0] + " | y: " + centers[n][1]);
		}
		
		currentStep = 0;
	}
	
	private void clearDataset() {
		Log.i(TAG, "Removing datasets | Count: " + mDataset.getSeriesCount());
		for(int n=mDataset.getSeriesCount()-1; n>-1; n--) {
    		mDataset.removeSeries(n);
    		Log.i(TAG, "Removing Cluster: " + n);
    	}
	}

	private void getRenderer(boolean init, int step) {
		mRenderer.setAxisTitleTextSize(16);
		mRenderer.setChartTitleTextSize(30);
		mRenderer.setLabelsTextSize(15);
		mRenderer.setLegendTextSize(24);
		mRenderer.setPointSize(5f);
		mRenderer.setMargins(new int[] {20, 30, 15, 0});
		
		if(init) {
			mRenderer.removeAllRenderers();
			XYSeriesRenderer r = new XYSeriesRenderer();
			r.setColor(Color.WHITE);
			r.setPointStyle(PointStyle.CIRCLE);
			r.setFillPoints(true);
			mRenderer.addSeriesRenderer(r);
		}
		else {
			if(mRenderer.getSeriesRendererCount() != K_VALUE) {
				mRenderer.removeAllRenderers();
				for(int n=0; n<K_VALUE; n++) {
					XYSeriesRenderer r = new XYSeriesRenderer();
					r.setColor(colors[rand.nextInt(colors.length)]);
					r.setPointStyle(styles[rand.nextInt(styles.length)]);
					r.setFillPoints(true);
					mRenderer.addSeriesRenderer(r);
				}
			}
		}
		
		mRenderer.setAxesColor(Color.DKGRAY);
		mRenderer.setLabelsColor(Color.LTGRAY);
		mRenderer.setChartTitle("KMeans Demo");
		if(step != 0) {
			mRenderer.setChartTitle("KMeans Demo - Step " + String.valueOf(step));
		}
		mRenderer.setXTitle("x values");
		mRenderer.setYTitle("y values");
		mRenderer.setXAxisMin(0);
		mRenderer.setXAxisMax(500);
		mRenderer.setYAxisMin(0);
		mRenderer.setYAxisMax(500);
	}
	
	private double FindDistance(double x1, double y1, double x2, double y2)
	{	 
		//find euclidean distance
		double distance = Math.sqrt(Math.pow(x2 - x1, 2.0) + Math.pow(y2 - y1, 2.0));
		return (distance);
	}
	
	private boolean doKMeansStep() {
		boolean improved = false;
		
		int clusterSize = K_VALUE;
		if(mDataset.getSeriesCount() == 1) {
			clusterSize = 1;
		}
		
		Log.d(TAG, "Cluster size: " + clusterSize);
		for(int k=0; k<clusterSize; k++) {
			Log.d(TAG, " On cluster: " + k);
			Log.d(TAG, " Points: " + mDataset.getSeriesAt(k).getItemCount());
			Log.d(TAG, " Cluster points: " + clusters[k].getItemCount());
			for(int n=mDataset.getSeriesAt(k).getItemCount()-1; n>-1; n--) {
				double bestDist = Double.MAX_VALUE;
	    		int inx = -1;
				for(int m=0; m<centers.length; m++) {
	    			double d = FindDistance(centers[m][0], centers[m][1], mDataset.getSeriesAt(k).getX(n), mDataset.getSeriesAt(k).getY(n));
	    			if (inx == -1 || d<bestDist)
	    			{
	    				inx = m;
	    				bestDist = d;
	    			}
				}
				
				Log.d(TAG, "  - Point " + (n+1) + " | distance from cluster " + (inx+1) + ": " + bestDist);
	    		if (k != inx) {
	    			improved = true;
	    			
	    			Log.d(TAG, "   - Adding point to Cluster: " + (inx+1));
					// add point to best cluster, based on distance from center
					clusters[inx].add(mDataset.getSeriesAt(k).getX(n), mDataset.getSeriesAt(k).getY(n));
					
					// remove point from current cluster
					if(clusters[k].getItemCount() > n) {
						Log.d(TAG, "   - Removing point from Cluster: " + (k+1));
						clusters[k].remove(n);
					}
	    		}
			}
		}
		
		Log.d(TAG,"Improved: " + improved);
		if(improved) {
			currentStep++;
			Log.d(TAG, "Current step: " + currentStep);

	    	//now improve by updating the center of the clusters
			for(int n=0; n<K_VALUE; n++) {
		    	double sumX = 0.0;
		    	double sumY = 0.0;
		    	
		    	Log.i(TAG, "Cluster " + n + " old center | x: " + centers[n][0] + " | y: " + centers[n][1]);
		    	for (int i = 0; i<clusters[n].getItemCount(); i++)
		    	{
		    		sumX += clusters[n].getX(i);
		    		sumY += clusters[n].getY(i);
		    	}
		    	
		    	Log.d(TAG, "  - sumX: " + sumX + " | sumY: " + sumY + " | count: " + clusters[n].getItemCount());
		    	centers[n][0] = sumX/clusters[n].getItemCount();
		    	centers[n][1] = sumY/clusters[n].getItemCount();
		    	Log.i(TAG, "Cluster " + (n+1) + " new center | x: " + centers[n][0] + " | y: " + centers[n][1]);
			}
			
			clearDataset();
			
			for(int n=0; n<clusters.length; n++) {
				mDataset.addSeries(clusters[n]);
				Log.d(TAG, "  - Current number of points in Cluster " + (n+1) + ": " + clusters[n].getItemCount());
			}
		}
		
		return improved;
	}

}
