package com.mxchip.helper;

import android.util.Log;

/**
 * ͨ�����ʹ�������ssid��probe request��������ʵ��������Ϣ���ݸ�ģ�顣 ��Ŀ���ƣ�EasyLink_minus �����ˣ�Rocke
 * ����ʱ�䣺2015��6��18�� ����5:56:17
 * 
 * @version 1.0
 */
public class ProbeReqData {
	private static final String TAG = "ProbeReqData";// ��־
	private static final String ARC4_KEY = "mxchip_easylink_minus";// ����
	private static final int version = 0x01;// �汾��

	/**
	 * 0x01 flag <Data��> ��һ���ֽ�ʼ����0x01����ʾ��EasyLink Minus�������ݡ� Flag��һ���ֽڶ������£�
	 * Bit7=0 Bit6~4=version Bit3~0=checksum
	 * Version:������1��7��version������ν��ssid���ݣ����嶨��ο����棺
	 * Checksum���Ƕ�Data��CRC8��У��ͼ��㣬ȡ��4bits�� Data�����Ƕ�ԭʼ����ͨ��ARC4�㷨���ܺ�����ݡ�
	 * ԭʼ���ݣ���version����ԭʼ���ݵĲ�����ʽ��
	 * �������ݣ���ԭʼ����ͨ��ARC4�����㷨��ʹ�����롱mxchip_easylink_minus�����������
	 * 
	 * Version�汾���� 1. Version=1. ԭʼ������ssid_len, <ssid>, <key> 3������ɣ� Ssid_len
	 * Ssid Key ֻ��һ��probe request��ɣ�
	 * ����ssid�32�ֽڣ�ȥ��ͷ����0x01��flag�������Data�������ֻ����30���ֽ�
	 * ����ssid��key����ԭʼ����ת��ΪData֮����ܳ��Ȳ��ܴ���30�ֽ�.
	 * 
	 * 2. Version=2. ԭʼ������ssid_len, key_len, <ssid>, <key>, <extra
	 * data>������������Data���ȴ���30�ֽڣ�����Ҫ������Version=3�����ݷ��ͺ������ݡ� 3. Version=3.
	 * �Ƕ�version=2�����䡣 ����version��ʱ��ʹ�á�
	 * 
	 * @param ssid
	 * @param key
	 * @return
	 */
	public String[] bgProtocol(String ssid, String key) {
		String rstdata[] = new String[2];
		// �������ʵ�ʴ�Сȷ�����Ĵ�С�������ȷ����������ڵõ�data�Ĵ�С������
		byte[] byteSSID = new byte[2];
		// ��һ���ֽ�ʼ����0x01
		byteSSID[0] = (byte) 0x01; //
		// Bit6~4=version
		byteSSID[1] = (byte) (version << 4); //

		// java�Դ���RC4�㷨
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

		// ȷ��ԭʼ���ݵĴ�С��ԭʼ������ssid_len, <ssid>, <key> 3�������
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

		// Data�����Ƕ�ԭʼ����ͨ��ARC4�㷨���ܺ������
		byte[] data = new RC4(ARC4_KEY.getBytes()).encrypt(tmpSsidAndKey);
		byte[] tdata = transfer(data);
		if (tdata.length > 30) {
			Log.e(TAG, "version 1 not support long ssid and key");
			return null;
		}

		// Checksum���Ƕ�Data��CRC8��У��ͼ��㣬ȡ��4bits
		byte byteCrc8 = Crc8Code.calcCrc8(tdata);
		byteSSID[1] |= byteCrc8 & 0x0f;

		// ȷ������ʵ�ʳ���
		byte[] result = new byte[2 + tdata.length];
		result[0] = byteSSID[0];
		result[1] = byteSSID[1];
		for (int j = 0; j < tdata.length; j++) {
			result[j + 2] = tdata[j];
		}
		Log.e(TAG, "tmpSsidAndKey:" + byteSSID.toString());

		// ǿ��ת����String����
//		return result;
		rstdata[1] = new String(result);
		return rstdata;
	}

	/**
	 * Data: ���ڼ���������һ��Hex���ݣ���Ҫ�Լ����������������±仯������Data���ݡ� 1. 7���ֽ����η��飬���һ����ܲ���7���ֽڡ�
	 * 2. ÿ�����ݺ�������һ���ֽڣ����һ���ֽ��ɸ����������е�bit7����λ����ɣ�ǰ�����ݶ�ֻ������7λ�������Ϳ��Ա�֤����ʼ�ղ�����0x7F��
	 * 3. �����ַ���Ҫ��ת���� ת���б� 0x7E 0x7E 0x01 0x00 0x7E 0x02
	 * 
	 * �����������������������һ�����ݣ� {0x8E, 0x99, 0x80, 0x12, 0x13, 0x34, 0x45, 0x56, 0x78}
	 * ������Data���� {0x7E, 0x01, 0x19, 0x7E, 0x02, 0x12, 0x13, 0x34, 0x45, 0x03,
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
