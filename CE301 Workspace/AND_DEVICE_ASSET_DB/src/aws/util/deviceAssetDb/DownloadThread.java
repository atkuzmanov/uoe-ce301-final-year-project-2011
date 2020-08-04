package aws.util.deviceAssetDb;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class DownloadThread extends Thread {
	Handler mHandler;
	Context c;
	UsefulBits uB;
	public final static int STATE_DONE = 0;
	public final static int STATE_RUNNING = 1;

	public final static int RESULT_OK = 0;
	public final static int RESULT_ERROR = -1;

	final String TAG = this.getClass().getName();

	int mState;
	String mUrl = "";
	String mLocalFilePath = "";
	String mLocalFileName = "";
	String errorMsg = "";

	public DownloadThread(Handler h, String Url, String LocalFilePath,
			String LocalFileName, Context ctx) {
		mHandler = h;
		mUrl = Url;
		mLocalFilePath = LocalFilePath;
		mLocalFileName = LocalFileName;
		c = ctx;
		uB = new UsefulBits(c);
	}

	public void run() {
		mState = STATE_RUNNING;

		boolean interrupted = false;
		Bundle b = new Bundle();
		Message msg = new Message();
		boolean res = false;

		Log.d(TAG, "^ DownloadThread: Thread Started");
		errorMsg = "";

		while (mState == STATE_RUNNING) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				Log.e(TAG, "^ DownloadThread: Thread Interrupted");
				interrupted = true;
			}

			if (!interrupted) {

				msg = mHandler.obtainMessage();

				//res = downloadFile(mUrl, mLocalFilePath + mLocalFileName);
				res = downloadFile(mUrl,mLocalFilePath, mLocalFileName);
			}

			if (res) {
				b.putInt("status", RESULT_OK);
			} else {
				b.putInt("status", RESULT_ERROR);
				b.putString("msg", errorMsg);
			}
			msg.setData(b);
			mHandler.sendMessage(msg);
			mState = STATE_DONE;
		}
		Log.d(TAG, "^ DownloadThread: Thread Exited");
	}

	public boolean downloadFile(String fileUri, String fileOut) {
		boolean res = true;

		try {
			URL google = new URL(fileUri);
			ReadableByteChannel rbc = Channels.newChannel(google.openStream());
			FileOutputStream fos = new FileOutputStream(fileOut);
			fos.getChannel().transferFrom(rbc, 0, 1 << 24);
		} catch (Exception e) {
			errorMsg = e.getMessage();
			Log.e(TAG, "^ DownloadThread@downloadFile: " + errorMsg);
			res = false;
		}

		return res;
	}

	public boolean downloadFile(String fileUri, String destinationDir, String localFileName) {
		Boolean res = false;
		OutputStream os = null;
		URLConnection URLConn = null;
		final int size = 1024;

		// URLConnection class represents a communication link between the
		// application and a URL.
		Log.d(TAG, "^ downloadFile - Downloading file: '" + fileUri + "' as '" + destinationDir + localFileName +"'");
		InputStream is = null;
		try {
			URL fileUrl;
			byte[] buf;
			int ByteRead, ByteWritten = 0;
			fileUrl = new URL(fileUri);
			os = new BufferedOutputStream(new FileOutputStream(destinationDir + "\\" + localFileName));
			// The URLConnection object is created by invoking the
			// openConnection method on a URL.

			URLConn = fileUrl.openConnection();
			is = URLConn.getInputStream();
			buf = new byte[size];
			while ((ByteRead = is.read(buf)) != -1) {
				os.write(buf, 0, ByteRead);
				ByteWritten += ByteRead;
			}
			Log.d(TAG, "^ downloadFile - Downloaded Successfully");
			Log.d(TAG, "^ downloadFile - File name:'" + localFileName
					+ "' No of	bytes :" + ByteWritten);
			res = true;
		} catch (Exception e) {
			errorMsg = e.getMessage();
			Log.e(TAG, "^ downloadFile - Error downloading:" + errorMsg);
			res = false;
		} finally {
			try {
				is.close();
				os.close();
			} catch (IOException e) {
				errorMsg = e.getMessage();
				Log.e(TAG, "^ downloadFile - Error cleaning up:" + errorMsg);
			}
		}
		return res;
	}

	/*
	 * sets the current state for the thread, used to stop the thread
	 */
	public void setState(int state) {
		mState = state;
	}
}
