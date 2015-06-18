package com.mxchip.easylink_plus;

import java.util.ArrayList;
import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Parcelable;
import android.util.Log;

import com.mxchip.helper.Crc8Code;
import com.mxchip.helper.ProbeReqData;
import com.mxchip.helper.RC4;

/**
 * Created by wangchao on 6/9/15.
 */
public class EasyLink_minus {
	private static final String TAG = "EasyLink_minus";
	// private static final String ARC4_KEY = "mxchip_easylink_minus";
	private Thread mCallbackThread; // call start ap after wifi enabled
	private Context mContext;
	boolean mStopTransmitting = false;
	// private ArrayList mSSid;
	private IntentFilter mIntentFilter = null;
	private boolean mScanning;
	private int mErrorId = 0;

	public boolean isScanning() {
		return mScanning;
	}

	private BroadcastReceiver mReceiver = new BroadcastReceiver() {
		public void onReceive(Context paramAnonymousContext, Intent intent) {
			EasyLink_minus.this.mScanning = false;
			mContext.unregisterReceiver(this);
			Log.d(TAG, "action:" + intent.getAction());
			if (intent.getAction().equals("android.net.wifi.SCAN_RESULTS")) {
				System.out.println("SCAN_RESULTS_AVAILABLE");
				EasyLink_minus.this.mScanning = false;
			}
			if (intent.getAction().equals("android.net.wifi.STATE_CHANGE")) {
				try {
					Parcelable localParcelable = intent
							.getParcelableExtra("networkInfo");
					if ((localParcelable != null)
							&& (!((NetworkInfo) localParcelable).isAvailable())) {
						EasyLink_minus.this.mErrorId = 102;
						EasyLink_minus.this.mScanning = false;
						EasyLink_minus.this.clearNetList();
						return;
					}
				} catch (Exception localException2) {
					localException2.printStackTrace();
					return;
				}
			}
			try {
				switch (intent.getIntExtra("wifi_state", 0)) {
				case 0:
					EasyLink_minus.this.clearNetList();
					return;
				}
			} catch (Exception localException1) {
				localException1.printStackTrace();
				return;
			}
			EasyLink_minus.this.mErrorId = 101;
			EasyLink_minus.this.mScanning = false;
			return;
		}
	};

	private List<Integer> mNetId = new ArrayList<Integer>();

	// private List<String> mNetSsid = new ArrayList<String>();
	// private int mChannel;

	public EasyLink_minus(Context ctx, Thread t) {
		this(ctx);
		mCallbackThread = t;
	}

	public EasyLink_minus(Context ctx) {
		mContext = ctx;
		mIntentFilter = new IntentFilter();
		mIntentFilter.addAction("android.net.wifi.SCAN_RESULTS");
		mIntentFilter.addAction("android.net.wifi.WIFI_STATE_CHANGED");
		mIntentFilter.addAction("android.net.wifi.STATE_CHANGE");
		mContext.registerReceiver(this.mReceiver, this.mIntentFilter);
	}

	// private int ieee80211_frequency_to_channel(int paramInt) {
	// if (paramInt == 2484)
	// return 14;
	// if (paramInt < 2484)
	// return (paramInt - 2407) / 5;
	// return -1000 + paramInt / 5;
	// }

	// private int getAndroidSDKVersion() {
	// try {
	// int i = Integer.valueOf(Build.VERSION.SDK_INT).intValue();
	// return i;
	// } catch (NumberFormatException localNumberFormatException) {
	// Log.e(localNumberFormatException.toString(),
	// localNumberFormatException.getMessage());
	// }
	// return 0;
	// }

	private void clearNetList() {
		WifiManager localWifiManager = (WifiManager) mContext
				.getSystemService(Context.WIFI_SERVICE);
		if (localWifiManager == null
				|| localWifiManager.getConfiguredNetworks() == null) {
			return;
		}
		for (WifiConfiguration localWifiConfiguration : localWifiManager
				.getConfiguredNetworks()) {
			String ssid = localWifiConfiguration.SSID.replaceAll("\"", "");
			if (!ssid.contains("#?1234"))
				// if (!ssid.matches("^#[0x00-0xff]*@[0x00-0xff]*"))
				// if (!ssid.matches("^#.*@.*"))
				continue;
			localWifiManager.removeNetwork(localWifiConfiguration.networkId);
			localWifiManager.saveConfiguration();
		}
	}

	private boolean startTransmit(String Ssid, String Key, String Userinfo) {
		Log.d(TAG, new String(Ssid));
		// clearNetList();
		// if(Ssid == "") {return false;}
		mIsWorking = true;
		mStopTransmitting = false;
		String param = "#" + Userinfo + Ssid + "@" + Key;

		// String param = new ProbeReqData().bgProtocol(Userinfo, Ssid, Key);

		WifiManager localWifiManager = (WifiManager) mContext
				.getSystemService(Context.WIFI_SERVICE);
		if (localWifiManager == null)
			return false;
		if (!localWifiManager.isWifiEnabled()) {
			if (!localWifiManager.setWifiEnabled(true)) {
				return false;
			}
			// while (!localWifiManager.isWifiEnabled()) {
			// try {
			// Thread.sleep(10L);
			// } catch (InterruptedException e) {
			// e.printStackTrace();
			// }
			// }
		}
		WifiInfo localWifiInfo = localWifiManager.getConnectionInfo();
		if (localWifiInfo == null)
			return false;
		// List<ScanResult> results = localWifiManager.getScanResults();
		// for (ScanResult result : results) {
		// if (String.format("\"%s\"", new Object[] { Ssid }).equals(
		// result.SSID)) {
		// continue;
		// }
		// if (mChannel > 14)
		// continue;
		// mChannel = ieee80211_frequency_to_channel(result.frequency);
		// break;
		// }
		WifiConfiguration config = new WifiConfiguration();
		config.SSID = String.format("\"%s\"", new Object[] { param });
		config.BSSID = null;
		config.preSharedKey = null;
		config.wepKeys = new String[4];
		config.wepTxKeyIndex = 0;
		config.priority = 50;
		config.hiddenSSID = true;
		config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
		config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
		config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
		config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
		config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
		config.allowedPairwiseCiphers
				.set(WifiConfiguration.PairwiseCipher.TKIP);
		config.allowedPairwiseCiphers
				.set(WifiConfiguration.PairwiseCipher.CCMP);
		config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
		config.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
		localWifiManager.addNetwork(config);
		localWifiManager.saveConfiguration();

		for (WifiConfiguration cfg : localWifiManager.getConfiguredNetworks()) {
			if (cfg.SSID.equals(config.SSID)) {
				mNetId.add(cfg.networkId);
			}
		}
		// Log.e("---netIds---", "SSID = " + config.SSID);
		sendProbeRequest(localWifiManager, mNetId);
		return true;
	}

	public void transmitSettings(final String Ssid, final String Key) {
		final String Userinfo = "?1234"; // faked ip
		new Thread() {
			@Override
			public void run() {
				startTransmit(Ssid, Key, Userinfo);
			}
		}.start();
	}

	private void sendProbeRequest(WifiManager localWifiManager,
			List<Integer> netIds) {
		try {
			for (int netId : netIds) {
				// Log.e("---netIds---", "netIds = " + netIds);
				localWifiManager.disableNetwork(netId);
				// Thread.sleep(10L);
				localWifiManager.enableNetwork(netId, true);
				// Thread.sleep(10L);
				// Log.e("---minus--->---", "netId = " + netId);
				// mScanning = true;
				// if (mCallbackThread != null) {
				// mCallbackThread.start();
				// }
				// localWifiManager.setWifiEnabled(true);
				localWifiManager.startScan();
				// Log.e("---minus--->---", "--->---startScan");
				// localWifiManager.startScan();
				// do {
				// Log.d(TAG, "Probe Waiting SCAN END");
				// Thread.sleep(10L);
				// if (!mScanning && mErrorId <= 0) break;
				// if (mStopTransmitting) break;
				// if (!mIsWorking) break;
				// }
				// while (mErrorId <= 0);
			}
		} catch (Exception e) {
			// Log.d(TAG, e.getMessage());
		} finally {
			mNetId = new ArrayList<Integer>();
			// clearNetList();
		}
	}

	boolean mIsWorking = false;

	public void stopTransmitting() {
		mIsWorking = false;
		mStopTransmitting = true;
		clearNetList();
	}
}
