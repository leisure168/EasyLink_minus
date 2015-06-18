/**
 * 
 */
package com.mxchip.easylink_plus;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.content.Context;

/**
 * @author Perry
 * 
 * @date 2014-10-21
 */
public class EasyLink_plus {
	private static EasyLink_minus minus;
	private static EasyLink_plus me;
	boolean sending = true;
	ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor();

	public EasyLink_plus(Context ctx) {
		try {
			minus = new EasyLink_minus(ctx);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static EasyLink_plus getInstence(Context ctx) {
		if (me == null) {
			me = new EasyLink_plus(ctx);
		}
		return me;
	}

	public void transmitSettings(final String ssid, final String key) {
		singleThreadExecutor = Executors.newSingleThreadExecutor();
		sending = true;
		singleThreadExecutor.execute(new Runnable() {
			@Override
			public void run() {
				while (sending) {
					try {
						minus.transmitSettings(ssid, key);
						// if(true) break;
						// Log.e("minus--->", "sending");
						try {
							Thread.sleep(500L);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		});
	}

	public void stopTransmitting() {
		sending = false;
		singleThreadExecutor.shutdown();
		minus.stopTransmitting();
	}
}
