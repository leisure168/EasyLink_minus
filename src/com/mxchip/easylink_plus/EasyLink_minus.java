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
import com.mxchip.helper.RC4;

/**
 * Created by wangchao on 6/9/15.
 */
public class EasyLink_minus {
	private static final String TAG = "EasyLink_minus";
	private static final String ARC4_KEY = "mxchip_easylink_minus";
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
		// String param = "#" + Userinfo + Ssid + "@" + Key;

		String param = ProbeReqData(Userinfo, Ssid, Key);

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
				localWifiManager.enableNetwork(netId, false);
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

	// 处理数据格式
	private String ProbeReqData(String userinfo, String ssid, String key) {
		byte[] byteSSID = new byte[2];

		byteSSID[0] = (byte) 0x01; //

		int version = 0x01;
		byteSSID[1] = (byte) (version << 4); //

		// byte [] origin = {0x8E, 0x99, 0x80, 0x12, 0x13, 0x34, 0x45, 0x56,
		// 0x78};

		// Cipher cip;
		// try {
		// cip = Cipher.getInstance("RC4");
		// SecretKeySpec k = new SecretKeySpec(ARC4_KEY.getBytes(), "RC4");
		// cip.init(Cipher.ENCRYPT_MODE, k);
		// byte [] r = cip.doFinal("abc".getBytes());
		// byte[] r = new RC4(ARC4_KEY.getBytes()).encrypt("012".getBytes());
		// byte[] t = new RC4(ARC4_KEY.getBytes()).decrypt(r);
		// String txt = new String(t);
		// String h = bytesToHex(r);
		// Log.d(TAG, h);
		// } catch (Exception e) {
		// }

		byte[] tmpSsidAndKey = new byte[1 + ssid.getBytes().length
				+ key.getBytes().length];
		tmpSsidAndKey[0] = (byte) ssid.length();
		int i = 1;
		for (byte b : ssid.getBytes()) {
			tmpSsidAndKey[i++] = b;
		}
		for (byte b : key.getBytes()) {
			tmpSsidAndKey[i++] = b;
		}

		byte[] data = new RC4(ARC4_KEY.getBytes()).encrypt(tmpSsidAndKey);
		byte[] tdata = transfer(data);
		if (tdata.length > 30) {
			Log.e(TAG, "version 1 not support long ssid and key");
			return null;
		}

		byte byteCrc8 = Crc8Code.calcCrc8(tdata);
		byteSSID[1] |= byteCrc8 & 0x0f;

		byte[] result = new byte[2 + tdata.length];
		result[0] = byteSSID[0];
		result[1] = byteSSID[1];
		for (int j = 0; j < tdata.length; j++) {
			result[j + 2] = tdata[j];
		}
		Log.e(TAG, "tmpSsidAndKey:" + byteSSID.toString());

		return new String(result);
	}

	byte[] transfer(byte[] data_in) {
		int len_in = data_in.length;
		byte[] data_out = new byte[len_in * 2];
		int i, j = 0, k;
		byte tmp;

		for (i = 0; i < len_in; i++) {
			tmp = (byte) (data_in[i] & 0x7F);
			if (tmp == 0x7E) {
				data_out[j++] = 0x7E;
				data_out[j++] = 0x01;
			} else if (tmp == 0) {
				data_out[j++] = 0x7E;
				data_out[j++] = 0x02;
			} else {
				data_out[j++] = tmp;
			}
			if (((i % 7) == 6) || (i == len_in - 1)) {
				tmp = 0;
				for (k = 0; k < 7; k++) {
					tmp += ((data_in[i - (6 - k)] & 0x80) >> (7 - k));
				}
				if (tmp == 0x7E) {
					data_out[j++] = 0x7E;
					data_out[j++] = 0x01;
				} else if (tmp == 0) {
					data_out[j++] = 0x7E;
					data_out[j++] = 0x02;
				} else {
					data_out[j++] = tmp;
				}
			}
		}
		byte[] result = new byte[j];
		for (i = 0; i < j; i++) {
			result[i] = data_out[i];
		}
		return result;
	}

	public static byte[] hexStringToBytes(String hexString) {
		if (hexString == null || hexString.equals("")) {
			return null;
		}
		// hexString = hexString.toUpperCase();
		int length = hexString.length() / 2;
		char[] hexChars = hexString.toCharArray();
		byte[] d = new byte[length];
		for (int i = 0; i < length; i++) {
			int pos = i * 2;
			d[i] = (byte) (charToByte(hexChars[pos]) << 4 | charToByte(hexChars[pos + 1]));
		}
		return d;
	}

	/**
	 * Convert char to byte
	 * 
	 * @param c
	 *            char
	 * @return byte
	 */
	public static byte charToByte(char c) {
		return (byte) "0123456789ABCDEF".indexOf(c);
	}

	final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

	public static String bytesToHex(byte[] bytes) {
		char[] hexChars = new char[bytes.length * 2];
		for (int j = 0; j < bytes.length; j++) {
			int v = bytes[j] & 0xFF;
			hexChars[j * 2] = hexArray[v >>> 4];
			hexChars[j * 2 + 1] = hexArray[v & 0x0F];
		}
		return new String(hexChars);
	}
}
