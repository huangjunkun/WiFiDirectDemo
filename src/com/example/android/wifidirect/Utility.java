package com.example.android.wifidirect;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.util.Enumeration;
import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Pair;

public class Utility {
	
	/**
	 * 根据文件后缀名获得对应的MIME类型。
	 * @param file
	 */
	static private String getMIMEType(File file)
	{
	    String type="*/*";
	    String fName=file.getName();
	    //获取后缀名前的分隔符"."在fName中的位置。
	    int dotIndex = fName.lastIndexOf(".");
	    if(dotIndex < 0){
	        return type;
	    }
	    /* 获取文件的后缀名 */
	    String end=fName.substring(dotIndex,fName.length()).toLowerCase();
	    if(end=="")return type;
	    //在MIME和文件类型的匹配表中找到对应的MIME类型。
	    for(int i=0;i<MIME_MapTable.length;i++){
	        if(end.equals(MIME_MapTable[i][0]))
	            type = MIME_MapTable[i][1];
	    }
	    return type;
	}
	/**
	 * 打开文件
	 * @param activity
	 * @param file
	 */
	static public void openFile(Activity activity, File file){
	    //Uri uri = Uri.parse("file://"+file.getAbsolutePath());
	    Intent intent = new Intent();
	    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	    //设置intent的Action属性
	    intent.setAction(Intent.ACTION_VIEW);
	    //获取文件file的MIME类型
	    String type = getMIMEType(file);
	    //设置intent的data和Type属性。
	    intent.setDataAndType(/*uri*/Uri.fromFile(file), type);
	    //跳转
	    activity.startActivity(intent);    
	}
	
	//建立一个MIME类型与文件后缀名的匹配表 	"文件类型——MIME类型"的匹配表:
	static private final String[][] MIME_MapTable={
	    //{后缀名，    MIME类型}
	    {".3gp",    "video/3gpp"},
	    {".apk",    "application/vnd.android.package-archive"},
	    {".asf",    "video/x-ms-asf"},
	    {".avi",    "video/x-msvideo"},
	    {".bin",    "application/octet-stream"},
	    {".bmp",      "image/bmp"},
	    {".c",        "text/plain"},
	    {".class",    "application/octet-stream"},
	    {".conf",    "text/plain"},
	    {".cpp",    "text/plain"},
	    {".doc",    "application/msword"},
	    {".exe",    "application/octet-stream"},
	    {".gif",    "image/gif"},
	    {".gtar",    "application/x-gtar"},
	    {".gz",        "application/x-gzip"},
	    {".h",        "text/plain"},
	    {".htm",    "text/html"},
	    {".html",    "text/html"},
	    {".jar",    "application/java-archive"},
	    {".java",    "text/plain"},
	    {".jpeg",    "image/jpeg"},
	    {".jpg",    "image/jpeg"},
	    {".js",        "application/x-javascript"},
	    {".log",    "text/plain"},
	    {".m3u",    "audio/x-mpegurl"},
	    {".m4a",    "audio/mp4a-latm"},
	    {".m4b",    "audio/mp4a-latm"},
	    {".m4p",    "audio/mp4a-latm"},
	    {".m4u",    "video/vnd.mpegurl"},
	    {".m4v",    "video/x-m4v"},    
	    {".mov",    "video/quicktime"},
	    {".mp2",    "audio/x-mpeg"},
	    {".mp3",    "audio/x-mpeg"},
	    {".mp4",    "video/mp4"},
	    {".mpc",    "application/vnd.mpohun.certificate"},        
	    {".mpe",    "video/mpeg"},    
	    {".mpeg",    "video/mpeg"},    
	    {".mpg",    "video/mpeg"},    
	    {".mpg4",    "video/mp4"},    
	    {".mpga",    "audio/mpeg"},
	    {".msg",    "application/vnd.ms-outlook"},
	    {".ogg",    "audio/ogg"},
	    {".pdf",    "application/pdf"},
	    {".png",    "image/png"},
	    {".pps",    "application/vnd.ms-powerpoint"},
	    {".ppt",    "application/vnd.ms-powerpoint"},
	    {".prop",    "text/plain"},
	    {".rar",    "application/x-rar-compressed"},
	    {".rc",        "text/plain"},
	    {".rmvb",    "audio/x-pn-realaudio"},
	    {".rtf",    "application/rtf"},
	    {".sh",        "text/plain"},
	    {".tar",    "application/x-tar"},    
	    {".tgz",    "application/x-compressed"}, 
	    {".txt",    "text/plain"},
	    {".wav",    "audio/x-wav"},
	    {".wma",    "audio/x-ms-wma"},
	    {".wmv",    "audio/x-ms-wmv"},
	    {".wps",    "application/vnd.ms-works"},
	    //{".xml",    "text/xml"},
	    {".xml",    "text/plain"},
	    {".z",        "application/x-compress"},
	    {".zip",    "application/zip"},
	    {"",        "*/*"}    
	};

	static public String getLocalIpAddress() {
		try {
			for (Enumeration<NetworkInterface> en = NetworkInterface
					.getNetworkInterfaces(); en.hasMoreElements();) {
				NetworkInterface intf = en.nextElement();
				for (Enumeration<InetAddress> enumIpAddr = intf
						.getInetAddresses(); enumIpAddr.hasMoreElements();) {
					InetAddress inetAddress = enumIpAddr.nextElement();
					if (inetAddress instanceof Inet4Address)
						if (!inetAddress.isLoopbackAddress()) {
							return inetAddress.getHostAddress();
						}
				}
			}
		} catch (SocketException ex) {
			Log.e("AndroidNetworkAddressFactory", "getLocalIPAddress()", ex);
		} catch (NullPointerException ex) {
			Log.e("AndroidNetworkAddressFactory", "getLocalIPAddress()", ex);
		}
		return null;
	}
	
	static public boolean sendPeerInfo(String host, int port) {
		Socket socket = new Socket();
		String strIP = getLocalIpAddress();
		boolean result = true;
		Log.d(WiFiDirectActivity.TAG, "peer:" + strIP);
		try {
			Log.d(WiFiDirectActivity.TAG, "Opening client socket - ");
			socket.bind(null);
			socket.connect((new InetSocketAddress(host, port)), ConfigInfo.SOCKET_TIMEOUT);// host

			Log.d(WiFiDirectActivity.TAG,
					"Client socket - " + socket.isConnected());
			OutputStream stream = socket.getOutputStream();
			stream.write(ConfigInfo.COMMAND_ID_SEND_PEER_INFO);// id
			String strSend = "peer:" + strIP + "port:" + port;
			stream.write(strSend.getBytes(), 0, strSend.length());
			Log.d(WiFiDirectActivity.TAG, "Client: Data written strSend:"
					+ strSend);

		} catch (IOException e) {
			Log.e(WiFiDirectActivity.TAG, e.getMessage());
			result = false;
		} finally {
			if (socket != null) {
				if (socket.isConnected()) {
					try {
						socket.close();
						Log.d(WiFiDirectActivity.TAG, "socket.close();");
					} catch (IOException e) {
						// Give up
						e.printStackTrace();
					}
				}
			}
		}
		return result;
	}

	static public boolean sendFileInfo(String name, int size, String host, int port) {
		Socket socket = new Socket();
		try {
			Log.d(WiFiDirectActivity.TAG, "Opening client socket - ");
			socket.bind(null);
			socket.connect((new InetSocketAddress(host, port)), ConfigInfo.SOCKET_TIMEOUT);

			Log.d(WiFiDirectActivity.TAG,
					"Client socket - " + socket.isConnected());
			OutputStream stream = socket.getOutputStream();
			String strSend = "size:" + size + "name:" + name;
			stream.write(ConfigInfo.COMMAND_ID_REQUEST_SEND_FILE);// id
			stream.write(strSend.length());
			stream.write(strSend.getBytes(), 0, strSend.length());
			Log.d(WiFiDirectActivity.TAG, "Client: Data written strSend:"
					+ strSend);
			return true;

		} catch (IOException e) {
			Log.e(WiFiDirectActivity.TAG, e.getMessage());
			return false;
		} finally {
			if (socket != null) {
				if (socket.isConnected()) {
					try {
						socket.close();
						Log.d(WiFiDirectActivity.TAG, "socket.close();");
					} catch (IOException e) {
						// Give up
						e.printStackTrace();
					}
				}
			}			
		}
	}
	
	static public Pair<String, Integer> getFileNameAndSize(Activity activaty, Uri uri)
			throws IOException {
//		try {
		String[] proj = { MediaStore.Images.Media.DATA, MediaStore.Video.Media.DATA, 
				MediaStore.Audio.Media.DATA, MediaStore.Files.FileColumns.DATA };
		Cursor actualimagecursor = activaty.managedQuery(uri,
				proj, null, null, null);
		int actual_image_column_index = actualimagecursor
				.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
		actualimagecursor.moveToFirst();
		String img_path = actualimagecursor
				.getString(actual_image_column_index);
		File file = new File(img_path);
		FileInputStream fis = new FileInputStream(file);
		int fileLen = fis.available();
		fis.close();
		return new Pair<String, Integer>(file.getName(), fileLen);
//		} catch (IOException e) {
//			Log.e(WiFiDirectActivity.TAG, e.getMessage());
//			return null;
//		}
	}
	
	public void sendFileInfo2(Activity activaty, Uri uri) {
		Socket socket = new Socket();
		int port = ConfigInfo.LISTEN_PORT;
		try {
			Log.d(WiFiDirectActivity.TAG, "Opening client socket - ");
			socket.bind(null);
			socket.connect((new InetSocketAddress("192.168.49.1", port)), ConfigInfo.SOCKET_TIMEOUT);// host

			Log.d(WiFiDirectActivity.TAG,
					"Client socket - " + socket.isConnected());
			OutputStream stream = socket.getOutputStream();

			String[] proj = { MediaStore.Images.Media.DATA };
			Cursor actualimagecursor = activaty.managedQuery(uri,
					proj, null, null, null);
			int actual_image_column_index = actualimagecursor
					.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
			actualimagecursor.moveToFirst();
			String img_path = actualimagecursor
					.getString(actual_image_column_index);
			File file = new File(img_path);
			FileInputStream fis = new FileInputStream(file);
			int fileLen = fis.available();
			fis.close();
			String strSend = "size:" + fileLen + "name:" + file.getName();
			stream.write(ConfigInfo.COMMAND_ID_REQUEST_SEND_FILE);// id
			stream.write(strSend.length());
			stream.write(strSend.getBytes(), 0, strSend.length());
			Log.d(WiFiDirectActivity.TAG, "Client: Data written strSend:"
					+ strSend);

		} catch (IOException e) {
			Log.e(WiFiDirectActivity.TAG, e.getMessage());
		} finally {
			if (socket != null) {
				if (socket.isConnected()) {
					try {
						socket.close();
						Log.d(WiFiDirectActivity.TAG, "socket.close();");
					} catch (IOException e) {
						// Give up
						e.printStackTrace();
					}
				}
			}
		}
	}
	
	static public boolean sendStream (String host, int port, InputStream data) {
		Socket socket = new Socket();
		boolean result = true;

		try {
			Log.d(WiFiDirectActivity.TAG, "Opening client socket - ");
			socket.bind(null);
			socket.connect((new InetSocketAddress(host, port)), ConfigInfo.SOCKET_TIMEOUT);// host

			Log.d(WiFiDirectActivity.TAG,
					"Client socket - " + socket.isConnected());
			OutputStream stream = socket.getOutputStream();
			copyStream(data, stream);
			Log.d(WiFiDirectActivity.TAG, "Client: Data written data's length:" + data.available());

		} catch (IOException e) {
			Log.e(WiFiDirectActivity.TAG, e.getMessage());
			result = false;
		} finally {
			if (socket != null) {
				if (socket.isConnected()) {
					try {
						socket.close();
						Log.d(WiFiDirectActivity.TAG, "socket.close();");
					} catch (IOException e) {
						// Give up
						e.printStackTrace();
					}
				}
			}
		}
		return result;
	}
	
	static public long copyStream(InputStream ins, OutputStream outs) {
		long copyLen = 0;
		byte buf[] = new byte[1024];
		int len;
		try {
			while ((len = ins.read(buf)) != -1) {
				outs.write(buf, 0, len);
				copyLen = copyLen + len;
			}
		} catch (IOException e) {
			Log.d(WiFiDirectActivity.TAG, e.toString());
			return 0;
		}
		return copyLen;
	}

}
