package net.simplyrin.wakechecker.utils;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.IOUtils;

import lombok.Data;
import net.simplyrin.httpclient.HttpClient;
import net.simplyrin.wakechecker.Main.Time;

/**
 * Created by SimplyRin on 2019/06/08.
 *
 * Copyright (c) 2019 SimplyRin
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
@Data
public class Task {

	private String name;
	private String ip;
	private int port;
	private int timeout;
	private Startup startup = new Startup();
	private Shutdown shutdown = new Shutdown();

	private Date startTime;

	public boolean isPortOpen() {
		try {
			Socket socket = new Socket();
			socket.connect(new InetSocketAddress(ip, port), timeout);
			socket.close();
			return true;
		} catch (Exception e) {
		}
		return false;
	}

	@Data
	public static class Startup {
		private boolean enabling;
		private Request request = new Request();
	}

	@Data
	public static class Shutdown {
		private boolean enabling;
		private Request request = new Request();
	}

	@Data
	public static class Request {
		private String url;
		private List<String> urlList = new ArrayList<>();

		private String type;
		private List<String> headers;
		private String data;

		private Time time;

		public String postRequest() throws Exception {
			if (this.url == null) {
				throw new RuntimeException("You need set URL!");
			}

			HttpClient httpClient;
			if (this.time != null) {
				httpClient = new HttpClient(this.url.replace("{time}", this.time.toString()));
			} else {
				httpClient = new HttpClient(this.url);
			}

			if (this.headers != null) {
				for (String value : this.headers) {
					httpClient.addHeader(value.split(":")[0].trim(), value.split(":")[1].trim());
				}
			}

			if (this.data != null) {
				SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
				if (this.time != null) {
					httpClient.setData(this.data.replace("{now}", simpleDateFormat.format(new Date())).replace("{time}", this.time.toString()));
				} else {
					httpClient.setData(this.data.replace("{now}", simpleDateFormat.format(new Date())));
				}
			}

			if (this.urlList.isEmpty() && this.urlList.size() >= 2) {
				String lastResult = this.getResult(httpClient);

				for (String url : this.urlList) {
					httpClient.setUrl(new URL(url));
					lastResult = this.getResult(httpClient);
				}
				return lastResult;
			}

			return this.getResult(httpClient);
		}

		private String getResult(HttpClient httpClient) throws Exception {
			HttpURLConnection httpURLConnection = httpClient.getHttpURLConnection();
			InputStream inputStream = httpURLConnection.getInputStream();
			String lastResult = IOUtils.toString(inputStream, Charset.forName("UTF-8"));
			return lastResult;
		}
	}

}
