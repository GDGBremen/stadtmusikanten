package de.gdgbremen.stadtmusikatenapp;

import java.math.BigDecimal;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.dsi.ant.plugins.AntPluginMsgDefines;
import com.dsi.ant.plugins.AntPluginPcc.IDeviceStateChangeReceiver;
import com.dsi.ant.plugins.AntPluginPcc.IPluginAccessResultReceiver;
import com.dsi.ant.plugins.antplus.legacycommon.AntPlusLegacyCommonPcc.ICumulativeOperatingTimeReceiver;
import com.dsi.ant.plugins.antplus.legacycommon.AntPlusLegacyCommonPcc.IManufacturerAndSerialReceiver;
import com.dsi.ant.plugins.antplus.legacycommon.AntPlusLegacyCommonPcc.IVersionAndModelReceiver;
import com.dsi.ant.plugins.antplus.pcc.AntPlusHeartRatePcc;
import com.dsi.ant.plugins.antplus.pcc.AntPlusHeartRatePcc.IHeartRateDataReceiver;
import com.dsi.ant.plugins.antplus.pcc.AntPlusHeartRatePcc.IHeartRateDataTimestampReceiver;
import com.dsi.ant.plugins.antplus.pcc.AntPlusHeartRatePcc.IPage4AddtDataReceiver;

public class HeartRateActivity extends Activity{

	AntPlusHeartRatePcc hrPcc = null;
    
    TextView tv_status;
    
    TextView tv_msgsRcvdCount;
    
    TextView tv_computedHeartRate;
    TextView tv_heartBeatCounter;
    TextView tv_timestampOfLastEvent;
    
    TextView tv_manufacturerSpecificByte;
    TextView tv_previousToLastHeartBeatEventTimeStamp;
    
    TextView tv_cumulativeOperatingTime;
    
    TextView tv_manufacturerID;
    TextView tv_serialNumber;
    
    TextView tv_hardwareVersion;
    TextView tv_softwareVersion;
    TextView tv_modelNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_heart_rate);
        
        tv_status = (TextView)findViewById(R.id.textView_Status);
        
        tv_msgsRcvdCount = (TextView)findViewById(R.id.textView_MsgsRcvdCount);
        
        tv_computedHeartRate = (TextView)findViewById(R.id.textView_ComputedHeartRate);
        tv_heartBeatCounter = (TextView)findViewById(R.id.textView_HeartBeatCounter);
        tv_timestampOfLastEvent = (TextView)findViewById(R.id.textView_TimestampOfLastEvent);
        
        tv_manufacturerSpecificByte = (TextView)findViewById(R.id.textView_ManufacturerSpecificByte);
        tv_previousToLastHeartBeatEventTimeStamp = (TextView)findViewById(R.id.textView_PreviousToLastEventTime);
        
        tv_cumulativeOperatingTime = (TextView)findViewById(R.id.textView_CumulativeOperatingTime);
        
        tv_manufacturerID = (TextView)findViewById(R.id.textView_ManufacturerID);
        tv_serialNumber = (TextView)findViewById(R.id.textView_SerialNumber);
        
        tv_hardwareVersion = (TextView)findViewById(R.id.textView_HardwareVersion);
        tv_softwareVersion = (TextView)findViewById(R.id.textView_SoftwareVersion);
        tv_modelNumber = (TextView)findViewById(R.id.textView_ModelNumber);
        
        resetPcc();
    }

    /**
     * Resets the PCC connection to request access again and clears any existing display data.
     */    
    private void resetPcc()
    {
        //Release the old access if it exists
        if(hrPcc != null)
        {
            hrPcc.releaseAccess();
            hrPcc = null;
        }
        
        
        //Reset the text display
        tv_status.setText("Connecting...");
        
        tv_msgsRcvdCount.setText("---");
        
        tv_computedHeartRate.setText("---");
        tv_heartBeatCounter.setText("---");
        tv_timestampOfLastEvent.setText("---");
        
        tv_manufacturerSpecificByte.setText("---");
        tv_previousToLastHeartBeatEventTimeStamp.setText("---");
        
        tv_cumulativeOperatingTime.setText("---");
        
        tv_manufacturerID.setText("---");
        tv_serialNumber.setText("---");
        
        tv_hardwareVersion.setText("---");
        tv_softwareVersion.setText("---");
        tv_modelNumber.setText("---");
        
        
        //Make the access request
        //AntPlusHeartRatePcc.requestAccess(this, 0,
        AntPlusHeartRatePcc.requestAccess(this, this, false, 
                new IPluginAccessResultReceiver<AntPlusHeartRatePcc>()
                {
                    //Handle the result, connecting to events on success or reporting failure to user.
                    @Override
                    public void onResultReceived(AntPlusHeartRatePcc result, int resultCode,
                            int initialDeviceStateCode)
                    {
                        switch(resultCode)
                        {
                            case AntPluginMsgDefines.MSG_REQACC_RESULT_whatSUCCESS:
                                hrPcc = result;
                                tv_status.setText(result.getDeviceName() + ": " + AntPlusHeartRatePcc.statusCodeToPrintableString(initialDeviceStateCode));
                                subscribeToEvents();
                                break;
                            case AntPluginMsgDefines.MSG_REQACC_RESULT_whatCHANNELNOTAVAILABLE:
                                Toast.makeText(HeartRateActivity.this, "Channel Not Available", Toast.LENGTH_SHORT).show();
                                tv_status.setText("Error. Do Menu->Reset.");
                                break;
                            case AntPluginMsgDefines.MSG_REQACC_RESULT_whatOTHERFAILURE:
                                Toast.makeText(HeartRateActivity.this, "RequestAccess failed. See logcat for details.", Toast.LENGTH_SHORT).show();
                                tv_status.setText("Error. Do Menu->Reset.");
                                break;
                            case AntPluginMsgDefines.MSG_REQACC_RESULT_whatDEPENDENCYNOTINSTALLED:
                                tv_status.setText("Error. Do Menu->Reset.");
                                AlertDialog.Builder adlgBldr = new AlertDialog.Builder(HeartRateActivity.this);
                                adlgBldr.setTitle("Missing Dependency");
                                adlgBldr.setMessage("The required application\n\"" + AntPlusHeartRatePcc.getMissingDependencyName() + "\"\n is not installed. Do you want to launch the Play Store to search for it?");
                                adlgBldr.setCancelable(true);
                                adlgBldr.setPositiveButton("Go to Store", new OnClickListener()
                                        {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which)
                                            {
                                                Intent startStore = null;
                                                startStore = new Intent(Intent.ACTION_VIEW,Uri.parse("market://details?id=" + AntPlusHeartRatePcc.getMissingDependencyPackageName()));
                                                startStore.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                                
                                                HeartRateActivity.this.startActivity(startStore);
                                            }
                                        });
                                adlgBldr.setNegativeButton("Cancel", new OnClickListener()
                                        {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which)
                                            {
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
                                Toast.makeText(HeartRateActivity.this, "Unrecognized result: " + resultCode, Toast.LENGTH_SHORT).show();
                                tv_status.setText("Error. Do Menu->Reset.");
                                break;
                        } 
                    }
                    
                    /**
                     * Subscribe to all the heart rate events, connecting them to display their data.
                     */
                    private void subscribeToEvents()
                    {
                        hrPcc.subscribeHeartRateDataEvent(new IHeartRateDataReceiver()
                                {
                                    @Override
                                    public void onNewHeartRateData(final int currentMessageCount,
                                            final int computedHeartRate, final long heartBeatCounter)
                                    {
                                        runOnUiThread(new Runnable()
                                                {                                            
                                                    @Override
                                                    public void run()
                                                    {
                                                        tv_msgsRcvdCount.setText(String.valueOf(currentMessageCount));
                                                        
                                                        tv_computedHeartRate.setText(String.valueOf(computedHeartRate));
                                                        tv_heartBeatCounter.setText(String.valueOf(heartBeatCounter));
                                                    }
                                                });
                                    }
                                });
                        
                        hrPcc.subscribeHeartRateDataTimestampEvent(new IHeartRateDataTimestampReceiver()
                                {
                                    @Override
                                    public void onNewHeartRateDataTimestamp(final int currentMessageCount, final BigDecimal timestampOfLastEvent)
                                    {
                                        runOnUiThread(new Runnable()
                                                {                                            
                                                    @Override
                                                    public void run()
                                                    {
                                                        tv_msgsRcvdCount.setText(String.valueOf(currentMessageCount));
                                                        
                                                        tv_timestampOfLastEvent.setText(String.valueOf(timestampOfLastEvent));
                                                    }
                                                });
                                    }
                                });
                        
                        hrPcc.subscribePage4AddtDataEvent(new IPage4AddtDataReceiver()
                                {
                                    
                                    @Override
                                    public void onNewPage4AddtData(final int currentMessageCount,
                                            final int manufacturerSpecificByte,
                                            final BigDecimal timestampOfPreviousToLastHeartBeatEvent)
                                    {
                                        runOnUiThread(new Runnable()
                                                {                                            
                                                    @Override
                                                    public void run()
                                                    {
                                                        tv_msgsRcvdCount.setText(String.valueOf(currentMessageCount));
                                                        
                                                        tv_manufacturerSpecificByte.setText(String.format("0x%02X", manufacturerSpecificByte));
                                                        tv_previousToLastHeartBeatEventTimeStamp.setText(String.valueOf(timestampOfPreviousToLastHeartBeatEvent));
                                                    }
                                                });
                                    }
                                });
                        
                        hrPcc.subscribeCumulativeOperatingTimeEvent(new ICumulativeOperatingTimeReceiver()
                                {
                                    @Override
                                    public void onNewCumulativeOperatingTime(final int currentMessageCount, final long cumulativeOperatingTime)
                                    {
                                        runOnUiThread(new Runnable()
                                                {                                            
                                                    @Override
                                                    public void run()
                                                    {
                                                        tv_msgsRcvdCount.setText(String.valueOf(currentMessageCount));
                                                        
                                                        tv_cumulativeOperatingTime.setText(String.valueOf(cumulativeOperatingTime));
                                                    }
                                                });
                                    }
                                });
                        
                        hrPcc.subscribeManufacturerAndSerialEvent(new IManufacturerAndSerialReceiver()
                                {
                                    @Override
                                    public void onNewManufacturerAndSerial(final int currentMessageCount, final int manufacturerID,
                                            final int serialNumber)
                                    {
                                        runOnUiThread(new Runnable()
                                                {                                            
                                                    @Override
                                                    public void run()
                                                    {
                                                        tv_msgsRcvdCount.setText(String.valueOf(currentMessageCount));
                                                        
                                                        tv_manufacturerID.setText(String.valueOf(manufacturerID));
                                                        tv_serialNumber.setText(String.valueOf(serialNumber));
                                                    }
                                                });
                                    }
                                });
                        
                        hrPcc.subscribeVersionAndModelEvent(new IVersionAndModelReceiver()
                                {
                                    @Override
                                    public void onNewVersionAndModel(final int currentMessageCount, final int hardwareVersion,
                                            final int softwareVersion, final int modelNumber)
                                    {
                                        runOnUiThread(new Runnable()
                                                {                                            
                                                    @Override
                                                    public void run()
                                                    {
                                                        tv_msgsRcvdCount.setText(String.valueOf(currentMessageCount));
                                                        
                                                        tv_hardwareVersion.setText(String.valueOf(hardwareVersion));
                                                        tv_softwareVersion.setText(String.valueOf(softwareVersion));
                                                        tv_modelNumber.setText(String.valueOf(modelNumber));
                                                    }
                                                });
                                    }
                                });
                    }
                }, 
                //Receives state changes and shows it on the status display line
                new IDeviceStateChangeReceiver()
                        {                    
                            @Override
                            public void onDeviceStateChange(final int newDeviceState)
                            {
                                runOnUiThread(new Runnable()
                                        {                                            
                                            @Override
                                            public void run()
                                            {
                                                tv_status.setText(hrPcc.getDeviceName() + ": " + AntPlusHeartRatePcc.statusCodeToPrintableString(newDeviceState));
                                                if(newDeviceState == AntPluginMsgDefines.DeviceStateCodes.DEAD)
                                                    hrPcc = null;
                                            }
                                        });
                                
                                
                            }
                        } );
    }

    @Override
    protected void onDestroy()
    {
        if(hrPcc != null)
        {
            hrPcc.releaseAccess();
            hrPcc = null;
        }
        super.onDestroy();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_heart_rate, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch(item.getItemId())
        {
            case R.id.menu_reset:
                resetPcc();
                tv_status.setText("Resetting...");
                return true;
            default:
                return super.onOptionsItemSelected(item);                
        }
    }
}
