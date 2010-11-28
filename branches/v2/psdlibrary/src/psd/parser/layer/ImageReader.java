package psd.parser.layer;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.IOException;
import java.util.List;

import psd.metadata.ChannelInfo;
import psd.parser.PsdInputStream;

public class ImageReader {

//	public void readImage(PsdInputStream input) throws IOException {
//		readImage(input, true, null);
//	}

	public BufferedImage readImage(PsdInputStream input, List<ChannelInfo> channelInfos, int w, int h, int opacity, boolean needReadPlaneInfo, short[] lineLengths) throws IOException {
		byte[] r = null, g = null, b = null, a = null;
		int planeNum = 0;
		for (ChannelInfo info : channelInfos) {
			switch (info.getId()) {
			case 0:
				r = readPlane(input, w, h, lineLengths, needReadPlaneInfo, planeNum);
				break;
			case 1:
				g = readPlane(input, w, h, lineLengths, needReadPlaneInfo, planeNum);
				break;
			case 2:
				b = readPlane(input, w, h, lineLengths, needReadPlaneInfo, planeNum);
				break;
			case -1:
				a = readPlane(input, w, h, lineLengths, needReadPlaneInfo, planeNum);
				if (opacity != -1) {
					double o = (opacity & 0xff) / 256.0;
					for (int i = 0; i < a.length; i++) {
						a[i] = (byte) ((a[i] & 0xff) * o);
					}
				}
				break;
			default:
				input.skipBytes(info.getDataLength());
				// layer mask
			}
			planeNum++;
		}
		int n = w * h;
		if (r == null)
			r = fillBytes(n, 0);
		if (g == null)
			g = fillBytes(n, 0);
		if (b == null)
			b = fillBytes(n, 0);
		if (a == null)
			a = fillBytes(n, 255);

		return makeImage(w, h, r, g, b, a);
	}

	private byte[] readPlane(PsdInputStream input, int w, int h, short[] lineLengths, boolean needReadPlaneInfo,
			int planeNum) throws IOException {
		// read a single color plane
		// get RLE compression info for channel

		boolean rleEncoded;

		if (needReadPlaneInfo) {
			short encoding = input.readShort();
			if (encoding != 0 && encoding != 1) {
				throw new IOException("invalid encoding: " + encoding);
			}
			rleEncoded = encoding == 1;
			if (rleEncoded) {
				if (lineLengths == null) {
					lineLengths = new short[h];
					for (int i = 0; i < h; i++) {
						lineLengths[i] = input.readShort();
					}
				}
			}
			planeNum = 0;
		} else {
			rleEncoded = lineLengths != null;
		}

		if (rleEncoded) {
			return parsePlaneCompressed(input, w, h, lineLengths, planeNum);
		} else {
			int size = w * h;
			byte[] b = new byte[size];
			input.readBytes(b, size);
			return b;
		}
	}

	private byte[] parsePlaneCompressed(PsdInputStream input, int w, int h, short[] lineLengths, int planeNum)
			throws IOException {

		byte[] b = new byte[w * h];
		byte[] s = new byte[w * 2];
		int pos = 0;
		int lineIndex = planeNum * h;
		for (int i = 0; i < h; i++) {
			int len = lineLengths[lineIndex++];
			input.readBytes(s, len);
			decodeRLE(s, 0, len, b, pos);
			pos += w;
		}
		return b;
	}

	private void decodeRLE(byte[] src, int sindex, int slen, byte[] dst, int dindex) throws IOException {
		try {
			int max = sindex + slen;
			while (sindex < max) {
				byte b = src[sindex++];
				int n = (int) b;
				if (n < 0) {
					n = 1 - n;
					b = src[sindex++];
					for (int i = 0; i < n; i++) {
						dst[dindex++] = b;
					}
				} else {
					n = n + 1;
					System.arraycopy(src, sindex, dst, dindex, n);
					dindex += n;
					sindex += n;
				}
			}
		} catch (Exception e) {
			throw new IOException("format error " + e);
		}
	}

	private byte[] fillBytes(int size, int value) {
		byte[] b = new byte[size];
		if (value != 0) {
			byte v = (byte) value;
			for (int i = 0; i < size; i++) {
				b[i] = v;
			}
		}
		return b;
	}

	private BufferedImage makeImage(int w, int h, byte[] r, byte[] g, byte[] b, byte[] a) {
		if (w == 0 || h == 0) {
			return null;
		}
		BufferedImage im = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		int[] data = ((DataBufferInt) im.getRaster().getDataBuffer()).getData();
		int n = w * h;
		int j = 0;
		while (j < n) {
			int ac = a[j] & 0xff;
			int rc = r[j] & 0xff;
			int gc = g[j] & 0xff;
			int bc = b[j] & 0xff;
			data[j] = (((((ac << 8) | rc) << 8) | gc) << 8) | bc;
			j++;
		}
		return im;
	}

}
