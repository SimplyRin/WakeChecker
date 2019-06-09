package net.simplyrin.wakechecker.utils;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

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
		private String type;
		private List<String> headers;
		private String data;

		private Time time;

		public String postRequest() {
			if (url == null) {
				throw new RuntimeException("You need set URL!");
			}

			HttpClient httpClient;
			if (this.time != null) {
				httpClient = new HttpClient(url.replace("{time}", this.time.toString()));
			} else {
				httpClient = new HttpClient(url);
			}

			if (headers != null) {
				for (String value : headers) {
					httpClient.addHeader(value.split(":")[0].trim(), value.split(":")[1].trim());
				}
			}

			if (data != null) {
				SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
				if (this.time != null) {
					httpClient.setData(data.replace("{now}", simpleDateFormat.format(new Date())).replace("{time}", this.time.toString()));
				} else {
					httpClient.setData(data.replace("{now}", simpleDateFormat.format(new Date())));
				}
			}

			return httpClient.getResult();
		}
	}

}
