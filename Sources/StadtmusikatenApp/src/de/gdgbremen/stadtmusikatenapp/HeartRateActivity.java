package de.gdgbremen.stadtmusikatenapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.dsi.ant.plugins.AntPluginMsgDefines;
import com.dsi.ant.plugins.AntPluginPcc.IDeviceStateChangeReceiver;
import com.dsi.ant.plugins.AntPluginPcc.IPluginAccessResultReceiver;
import com.dsi.ant.plugins.antplus.pcc.AntPlusHeartRatePcc;
import com.dsi.ant.plugins.antplus.pcc.AntPlusHeartRatePcc.IHeartRateDataReceiver;

public class HeartRateActivity extends Activity {

	AntPlusHeartRatePcc hrPcc = null;

	TextView tv_status;

	TextView tv_computedHeartRate;

	TextView tv_computedHeartRate_p2;

	RelativeLayout rootView;

	private static final long GET_DATA_INTERVAL = 3000;
	int images[] = { R.drawable.horror, R.drawable.schock, R.drawable.creepy2,
			R.drawable.creepy3, R.drawable.dis, R.drawable.disgusting,
			R.drawable.howto, R.drawable.images, R.drawable.imagesca,
			R.drawable.imagescalm, R.drawable.imagesdis, R.drawable.imagestwo };
	int index = 0;
	Handler hand = new Handler();
	
	boolean started = false;
	
	boolean killRunnable = false;
	
	Runnable run = null;

	private RelativeLayout big_red_heart1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_heart_rate);

		tv_status = (TextView) findViewById(R.id.textView_Status);

		tv_computedHeartRate = (TextView) findViewById(R.id.textView_ComputedHeartRate);

		tv_computedHeartRate_p2 = (TextView) findViewById(R.id.textView_ComputedHeartRate2);
		
		big_red_heart1 = (RelativeLayout) findViewById(R.id.big_red_heart1);

		rootView = (RelativeLayout) findViewById(R.id.heart_rate_root);
		rootView.setBackgroundColor(Color.GREEN);

		rootView.setOnClickListener(new android.view.View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if(started){
					killRunnable = true;
					started =false;
				}else{
					killRunnable = false; 
					run = new Runnable() {
						
						@Override
						public void run() {
							if(!killRunnable){
								rootView.setBackgroundResource(images[index++]);
								if (index == images.length)
									index = 0;
								hand.postDelayed(run, GET_DATA_INTERVAL);
							}
						}
					};
					hand.postDelayed(run, GET_DATA_INTERVAL);
					started = true;
				}
			}
		});

		resetPcc();
	}

	

	/**
	 * Resets the PCC connection to request access again and clears any existing
	 * display data.
	 */
	private void resetPcc() {
		// Release the old access if it exists
		if (hrPcc != null) {
			hrPcc.releaseAccess();
			hrPcc = null;
		}

		// Reset the text display
		tv_status.setText("Connecting...");

		tv_computedHeartRate.setText("P1");

		// Make the access request
		// AntPlusHeartRatePcc.requestAccess(this, 0,
		AntPlusHeartRatePcc.requestAccess(this, this, false,
				new IPluginAccessResultReceiver<AntPlusHeartRatePcc>() {
					// Handle the result, connecting to events on success or
					// reporting failure to user.
					@Override
					public void onResultReceived(AntPlusHeartRatePcc result,
							int resultCode, int initialDeviceStateCode) {
						switch (resultCode) {
						case AntPluginMsgDefines.MSG_REQACC_RESULT_whatSUCCESS:
							hrPcc = result;
							tv_status.setText(result.getDeviceName()
									+ ": "
									+ AntPlusHeartRatePcc
											.statusCodeToPrintableString(initialDeviceStateCode));
							subscribeToEvents();
							break;
						case AntPluginMsgDefines.MSG_REQACC_RESULT_whatCHANNELNOTAVAILABLE:
							Toast.makeText(HeartRateActivity.this,
									"Channel Not Available", Toast.LENGTH_SHORT)
									.show();
							tv_status.setText("Error. Do Menu->Reset.");
							break;
						case AntPluginMsgDefines.MSG_REQACC_RESULT_whatOTHERFAILURE:
							Toast.makeText(
									HeartRateActivity.this,
									"RequestAccess failed. See logcat for details.",
									Toast.LENGTH_SHORT).show();
							tv_status.setText("Error. Do Menu->Reset.");
							break;
						case AntPluginMsgDefines.MSG_REQACC_RESULT_whatDEPENDENCYNOTINSTALLED:
							tv_status.setText("Error. Do Menu->Reset.");
							AlertDialog.Builder adlgBldr = new AlertDialog.Builder(
									HeartRateActivity.this);
							adlgBldr.setTitle("Missing Dependency");
							adlgBldr.setMessage("The required application\n\""
									+ AntPlusHeartRatePcc
											.getMissingDependencyName()
									+ "\"\n is not installed. Do you want to launch the Play Store to search for it?");
							adlgBldr.setCancelable(true);
							adlgBldr.setPositiveButton("Go to Store",
									new OnClickListener() {
										@Override
										public void onClick(
												DialogInterface dialog,
												int which) {
											Intent startStore = null;
											startStore = new Intent(
													Intent.ACTION_VIEW,
													Uri.parse("market://details?id="
															+ AntPlusHeartRatePcc
																	.getMissingDependencyPackageName()));
											startStore
													.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

											HeartRateActivity.this
													.startActivity(startStore);
										}
									});
							adlgBldr.setNegativeButton("Cancel",
									new OnClickListener() {
										@Override
										public void onClick(
												DialogInterface dialog,
												int which) {
											dialog.dismiss();
										}
									});

							final AlertDialog waitDialog = adlgBldr.create();
							waitDialog.show();
							break;
						case AntPluginMsgDefines.MSG_REQACC_RESULT_whatUSERCANCELLED:
							tv_status.setText("Cancelled. Do Menu->Reset.");
							break;
						default:
							Toast.makeText(HeartRateActivity.this,
									"Unrecognized result: " + resultCode,
									Toast.LENGTH_SHORT).show();
							tv_status.setText("Error. Do Menu->Reset.");
							break;
						}
					}

					/**
					 * Subscribe to all the heart rate events, connecting them
					 * to display their data.
					 */
					private void subscribeToEvents() {
						hrPcc.subscribeHeartRateDataEvent(new IHeartRateDataReceiver() {
							@Override
							public void onNewHeartRateData(
									final int currentMessageCount,
									final int computedHeartRate,
									final long heartBeatCounter) {
								runOnUiThread(new Runnable() {
									@Override
									public void run() {
										// tv_msgsRcvdCount.setText(String.valueOf(currentMessageCount));

										tv_computedHeartRate.setText(String
												.valueOf(computedHeartRate));
										big_red_heart1.setScaleX(0.01f);
										big_red_heart1.setScaleY(0.01f);
										big_red_heart1.setAlpha(0);
										big_red_heart1.animate()
											.scaleX(1)
											.scaleY(1)
											.alpha(1)
											.setDuration(100)
											.start();
										// tv_heartBeatCounter.setText(String.valueOf(heartBeatCounter));
									}
								});
							}
						});

					}
				},
				// Receives state changes and shows it on the status display
				// line
				new IDeviceStateChangeReceiver() {
					@Override
					public void onDeviceStateChange(final int newDeviceState) {
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								tv_status.setText(hrPcc.getDeviceName()
										+ ": "
										+ AntPlusHeartRatePcc
												.statusCodeToPrintableString(newDeviceState));
								if (newDeviceState == AntPluginMsgDefines.DeviceStateCodes.DEAD)
									hrPcc = null;
							}
						});

					}
				});
	}

	@Override
	protected void onDestroy() {
		if (hrPcc != null) {
			hrPcc.releaseAccess();
			hrPcc = null;
		}
		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_heart_rate, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_reset:
			resetPcc();
			tv_status.setText("Resetting...");
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
}