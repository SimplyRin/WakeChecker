package net.simplyrin.wakechecker;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import lombok.Data;
import net.md_5.bungee.config.Configuration;
import net.simplyrin.config.Config;
import net.simplyrin.rinstream.RinStream;
import net.simplyrin.wakechecker.utils.Task;
import net.simplyrin.wakechecker.utils.ThreadPool;
import net.simplyrin.wakechecker.utils.Version;

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
public class Main {

	public static void main(String[] args) {
		new Main().run();
	}

	private HashMap<String, Date> map = new HashMap<>();

	public void run() {
		RinStream rinStream = new RinStream();
		rinStream.setPrefix("[yyyy/MM/dd HH:mm:ss]");

		Configuration config;

		File file = new File("config.yml");
		if (!file.exists()) {
			try {
				file.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}

			config = Config.getConfig(file);

			config.set("Monitoring.One.Name", "Task-1 PC");
			config.set("Monitoring.One.IP", "192.168.0.1");
			config.set("Monitoring.One.Port", 3389);
			config.set("Monitoring.One.Timeout", 500);
			config.set("Monitoring.One.Startup.Enabling", true);
			config.set("Monitoring.One.Startup.Request.URL", "https://api.yourserver.net/hooks.php");
			config.set("Monitoring.One.Startup.Request.Type", "GET");

			config.set("Monitoring.One.Shutdown.Enabling", true);
			config.set("Monitoring.One.Shutdown.Request.URL", Arrays.asList("https://api.yourserver.net/hooks1.php",  "https://api.yourserver.net/hooks2.php"));
			config.set("Monitoring.One.Shutdown.Request.Type", "POST");
			config.set("Monitoring.One.Shutdown.Request.Headers", Arrays.asList("User-Agent:Mozilla/5.0"));
			config.set("Monitoring.One.Shutdown.Request.Data", "message=Runtime: {time}");

			Config.saveConfig(config, file);
		}

		config = Config.getConfig(file);

		List<Task> tasks = new ArrayList<>();
		for (String key : config.getSection("Monitoring").getKeys()) {
			Task task = new Task();

			task.setName(config.getString("Monitoring." + key + ".Name"));
			task.setIp(config.getString("Monitoring." + key + ".IP"));
			task.setPort(config.getInt("Monitoring." + key + ".Port"));
			task.setTimeout(config.getInt("Monitoring." + key + ".Timeout"));

			task.getStartup().setEnabling(config.getBoolean("Monitoring." + key + ".Startup.Enabling"));

			String startupUrl = config.get("Monitoring." + key + ".Startup.Request.URL").toString();
			if (startupUrl.startsWith("[") && startupUrl.endsWith("]")) {
				task.getStartup().getRequest().setUrlList(config.getStringList("Monitoring." + key + ".Startup.Request.URL"));;
			} else {
				task.getStartup().getRequest().setUrl(startupUrl);
			}

			task.getStartup().getRequest().setType(config.getString("Monitoring." + key + ".Startup.Request.Type"));
			task.getStartup().getRequest().setHeaders(config.getStringList("Monitoring." + key + ".Startup.Request.Headers"));
			task.getStartup().getRequest().setData(config.getString("Monitoring." + key + ".Startup.Request.Data"));

			task.getShutdown().setEnabling(config.getBoolean("Monitoring." + key + ".Shutdown.Enabling"));

			task.getShutdown().getRequest().setUrl(config.getString("Monitoring." + key + ".Shutdown.Request.URL"));

			String shutdownUrl = config.get("Monitoring." + key + ".Shutdown.Request.URL").toString();
			if (shutdownUrl.startsWith("[") && shutdownUrl.endsWith("]")) {
				task.getStartup().getRequest().setUrlList(config.getStringList("Monitoring." + key + ".Shutdown.Request.URL"));;
			} else {
				task.getStartup().getRequest().setUrl(shutdownUrl);
			}

			task.getShutdown().getRequest().setType(config.getString("Monitoring." + key + ".Shutdown.Request.Type"));
			task.getShutdown().getRequest().setHeaders(config.getStringList("Monitoring." + key + ".Shutdown.Request.Headers"));
			task.getShutdown().getRequest().setData(config.getString("Monitoring." + key + ".Shutdown.Request.Data"));

			tasks.add(task);
		}

		System.out.println("WakeChecker が起動しました。");
		System.out.println("ビルド日: " + Version.BUILD_TIME);
		System.out.println("タスク監視を開始します。");

		for (Task task : tasks) {
			ThreadPool.run(() -> {
				boolean bool = false;

				while (true) {
					boolean portOpen = task.isPortOpen();
					if (bool != portOpen) {
						bool = portOpen;

						if (portOpen) {
							if (task.getStartup().isEnabling()) {
								task.getStartup().getRequest().postRequest();
							}

							this.map.put(task.getName(), new Date());
							System.out.println(task.getName() + "(" + task.getIp() + ") のポートが開放されました (起動しました)");
						} else {
							Date date = this.map.get(task.getName());
							Time time = this.getTimeFromDate(date);

							task.getShutdown().getRequest().setTime(time);

							if (task.getShutdown().isEnabling()) {
								task.getShutdown().getRequest().postRequest();
							}

							System.out.println(task.getName() + "(" + task.getIp() + ") のポートが閉鎖されました (起動時間: " + time.toString() + ")");
						}
					}

					try {
						TimeUnit.SECONDS.sleep(5);
					} catch (Exception e) {
					}
				}
			});
		}
	}

	public Time getTimeFromDate(Date createdTime) {
		int[] units = { Calendar.YEAR, Calendar.MONTH, Calendar.DAY_OF_MONTH, Calendar.HOUR_OF_DAY, Calendar.MINUTE, Calendar.SECOND };
		Date today = new Date();
		int[] result = new int[units.length];

		Calendar sCal = Calendar.getInstance();
		Calendar tCal = Calendar.getInstance();

		sCal.setTime(createdTime);
		tCal.setTime(today);

		for (int i = units.length - 1; i >= 0; i--) {
			result[i] = tCal.get(units[i]) - sCal.get(units[i]);
			if (result[i] < 0) {
				tCal.add(units[i - 1], -1);
				int add = tCal.getActualMaximum(units[i]);
				result[i] += (units[i] == Calendar.DAY_OF_MONTH) ? add : add + 1;
			}
		}

		Time time = new Time();

		time.year = result[0];
		time.month = result[1];
		time.day = result[2];
		time.hour = result[3];
		time.minute = result[4];
		time.second = result[5];

		return time;
	}

	@Data
	public static class Time {
		int year;
		int month;
		int day;
		int hour;
		int minute;
		int second;

		@Override
		public String toString() {
			String uptime = "";

			if (year > 0) {
				uptime += year + "年";
			}
			if (month > 0) {
				uptime += month + "ヶ月";
			}
			if (day > 0) {
				uptime += day + "日 ";
			}

			if (hour > 0) {
				uptime += hour + "時間";
			}
			if (minute > 0) {
				uptime += minute + "分";
			}
			if (second > 0) {
				uptime += second + "秒";
			}

			if (uptime.trim().length() == 0) {
				uptime += "0秒";
			}

			return uptime;
		}

		public static Time now() {
			Date date = new Date();
			Time time = new Time();

			time.setYear(date.getYear());
			time.setMonth(date.getMonth());
			time.setDay(date.getDay());
			time.setHour(date.getHours());
			time.setMinute(date.getMinutes());
			time.setSecond(date.getSeconds());

			return time;
		}
	}
}
