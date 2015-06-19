package com.mxchip.helper;

import android.util.Log;

/**
 * 通过发送带有特殊ssid的probe request包，把真实的配网信息传递给模块。 项目名称：EasyLink_minus 创建人：Rocke
 * 创建时间：2015年6月18日 下午5:56:17
 * 
 * @version 1.0
 */
public class ProbeReqData {
	private static final String TAG = "ProbeReqData";// 日志
	private static final String ARC4_KEY = "mxchip_easylink_minus";// 密码
	private static final int version = 0x01;// 版本号

	/**
	 * 0x01 flag <Data…> 第一个字节始终是0x01，表示是EasyLink Minus配网数据。 Flag：一个字节定义如下：
	 * Bit7=0 Bit6~4=version Bit3~0=checksum
	 * Version:可以是1到7，version决定如何解包ssid数据，具体定义参考后面：
	 * Checksum：是对Data做CRC8的校验和计算，取低4bits。 Data数据是对原始数据通过ARC4算法加密后的数据。
	 * 原始数据：由version决定原始数据的产生方式。
	 * 加密数据：把原始数据通过ARC4加密算法，使用密码”mxchip_easylink_minus”计算产生。
	 * 
	 * Version版本定义 1. Version=1. 原始数据由ssid_len, <ssid>, <key> 3部分组成： Ssid_len
	 * Ssid Key 只由一个probe request组成，
	 * 由于ssid最长32字节，去掉头部的0x01和flag，这里的Data数据最多只能有30个字节
	 * ，即ssid和key经过原始数据转换为Data之后的总长度不能大于30字节.
	 * 
	 * 2. Version=2. 原始数据由ssid_len, key_len, <ssid>, <key>, <extra
	 * data>组成如果产生的Data长度大于30字节，则需要在增加Version=3的数据发送后续数据。 3. Version=3.
	 * 是对version=2的扩充。 其他version暂时不使用。
	 * 
	 * @param ssid
	 * @param key
	 * @return
	 */
	public String[] bgProtocol(String ssid, String key) {
		String rstdata[] = new String[2];
		// 最后会根据实际大小确定包的大小，这里先放两个，后期得到data的大小后扩大
		byte[] byteSSID = new byte[2];
		// 第一个字节始终是0x01
		byteSSID[0] = (byte) 0x01; //
		// Bit6~4=version
		byteSSID[1] = (byte) (version << 4); //

		// java自带的RC4算法
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

		// 确定原始数据的大小，原始数据由ssid_len, <ssid>, <key> 3部分组成
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
		rstdata[0] = new String(tmpSsidAndKey);

		// Data数据是对原始数据通过ARC4算法加密后的数据
		byte[] data = new RC4(ARC4_KEY.getBytes()).encrypt(tmpSsidAndKey);
		byte[] tdata = transfer(data);
		if (tdata.length > 30) {
			Log.e(TAG, "version 1 not support long ssid and key");
			return null;
		}

		// Checksum：是对Data做CRC8的校验和计算，取低4bits
		byte byteCrc8 = Crc8Code.calcCrc8(tdata);
		byteSSID[1] |= byteCrc8 & 0x0f;

		// 确定包的实际长度
		byte[] result = new byte[2 + tdata.length];
		result[0] = byteSSID[0];
		result[1] = byteSSID[1];
		for (int j = 0; j < tdata.length; j++) {
			result[j + 2] = tdata[j];
		}
		Log.e(TAG, "tmpSsidAndKey:" + byteSSID.toString());

		// 强制转换成String返回
//		return result;
		rstdata[1] = new String(result);
		return rstdata;
	}

	/**
	 * Data: 由于加密数据是一组Hex数据，需要对加密数据依次做如下变化，产生Data数据。 1. 7个字节依次分组，最后一组可能不足7个字节。
	 * 2. 每组数据后面增加一个字节，最后一个字节由改组数据所有的bit7按照位置组成，前面数据都只保留低7位。这样就可以保证数据始终不大于0x7F。
	 * 3. 特殊字符需要做转换： 转换列表： 0x7E 0x7E 0x01 0x00 0x7E 0x02
	 * 
	 * 举例：如果加密数据是如下一组数据： {0x8E, 0x99, 0x80, 0x12, 0x13, 0x34, 0x45, 0x56, 0x78}
	 * 产生的Data就是 {0x7E, 0x01, 0x19, 0x7E, 0x02, 0x12, 0x13, 0x34, 0x45, 0x03,
	 * 0x56, 0x78 ,0x7E, 0x02}
	 * 
	 * @param data_in
	 * @return
	 */
	byte[] transfer(byte[] data_in) {
		int len_in = data_in.length;
		byte[] data_out = new byte[len_in * 2];
		int i, j = 0, k, left;
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
			if (((i % 7) == 6)) {
				tmp = 0;
				left = i - 6;
				for (k = 0; k < 7; k++) {
					tmp += ((data_in[left + k] & 0x80) >> (7 - k));
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
			} else if (i == len_in - 1) {
				tmp = 0;
				left = (len_in % 7);
				for (k = 0; k < left; k++) {
					tmp += ((data_in[len_in - left + k] & 0x80) >> (7 - k));
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
