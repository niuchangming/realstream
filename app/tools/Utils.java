package tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.auth0.jwt.JWTSigner;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.monitorjbl.json.JsonView;
import com.monitorjbl.json.JsonViewSerializer;
import com.monitorjbl.json.Match;

public class Utils {
	public static String md5(final String input) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			md.update(input.getBytes());
			byte[] output = md.digest();
			return bytesToHex(output);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return input;
	}
	
	public static String genernateAcessToken(Date datetime, String key){
		return AES.encrypt(formatDateTime(datetime) , key).trim();
	}

	public static String bytesToHex(byte[] b) {
		char hexDigit[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };
		StringBuffer buf = new StringBuffer();
		for (int j = 0; j < b.length; j++) {
			buf.append(hexDigit[(b[j] >> 4) & 0x0f]);
			buf.append(hexDigit[b[j] & 0x0f]);
		}
		
		return buf.toString();
	}

	public static String toJson(Class<?> cls, Object obj, String...excludes) throws JsonProcessingException{
		ObjectMapper mapper = new ObjectMapper();
		SimpleModule module = new SimpleModule();
		module.addSerializer(JsonView.class, new JsonViewSerializer());
		mapper.registerModule(module);
		
		return mapper.writeValueAsString(JsonView.with(obj).onClass(cls, Match.match().exclude(excludes)));
	}
	
	public static boolean isBlank(final String str) {
		if (null == str)
			return true;
		if (str.isEmpty())
			return true;

		return str.trim().isEmpty();
	}

	public static String uuid() {
		return UUID.randomUUID().toString();
	}
	
	public static String getJWTString(int mins){
		final long iat = System.currentTimeMillis() / 1000L - 180;
		final long exp = iat + mins * 60L;
		
		JWTSigner signer = new JWTSigner(Constants.TOKBOX_SECRET);
		final HashMap<String, Object> claims = new HashMap<String, Object>();
		claims.put("iss", Constants.TOKBOX_API_KEY + "");
		claims.put("exp", exp);
		claims.put("iat", iat);
		claims.put("ist", "project");
		claims.put("jti", uuid());
		
		return signer.sign(claims);
	}
	
	public static String sendSMS(String mobile, String msg){
		String response = "";
		try{
			String url = "http://gateway.onewaysms.sg:10002/api.aspx?apiusername=APIKWEEZCI355&apipassword=APIKWEEZCI355KWEEZ&mobileno=" 
					+ URLEncoder.encode(mobile, "UTF-8") + "&senderid=AppName" + "&message=" + URLEncoder.encode(msg, "UTF-8") + "&languagetype=1";
			URL obj = new URL(url);
			HttpURLConnection con = (HttpURLConnection) obj.openConnection();
			con.setRequestMethod("GET");
			
			BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
			String inputLine;
			StringBuffer sb = new StringBuffer();
	 
			while ((inputLine = in.readLine()) != null) {
				sb.append(inputLine);
			}
			in.close();
			
			response = sb.toString();
		} catch(IOException e){
			e.printStackTrace();
		}
		return response; 
	}
	
	public static int genernateActiveCode(){
		Random r = new Random();
		List<Integer> codes = new ArrayList<Integer>();
		for (int i = 0; i < 10; i++){
		    int x = r.nextInt(9999);
		    while (codes.contains(x))
		        x = r.nextInt(9999);
		    codes.add(x);
		}
		return Integer.parseInt(String.format("%04d", codes.get(0)));
	}
	
	public static String convertByteToMB(long bytes){
		DecimalFormat df = new DecimalFormat("#.##"); 
		double d = (double)bytes / 1024 / 1024;
		return df.format(d); 
	}
	
	public static long differentDateTimeByMins(Date d1, Date d2){
		long diff = d2.getTime() - d1.getTime();
		return TimeUnit.MILLISECONDS.toMinutes(diff); 
	}
	
	public static String formatDateTime(Date date) {
		return formatDateTime(null, date);
	}

	public static String formatDateTime(String format, Date date) {
		if (format == null) {
			format = "yyyy-MM-dd HH:mm:ss";
		}
		String time = new java.text.SimpleDateFormat(format).format(date);
		return time;
	}
	
	public static String resolveTime(final String time) {
		String[] array = time.split(":");
		StringBuilder sb = new StringBuilder();
		for (String a : array) {
			if (sb.length() > 0)
				sb.append(":");

			if (a.length() == 1)
				a = new StringBuilder("0").append(a).toString();

			sb.append(a);
		}

		return sb.toString() + ":00";
	}

	public static boolean isValidTime(String str) {
		return str.matches("^\\d{2}:\\d{2}:\\d{2}$");
	}

	public static boolean isValidDate(String str) {
		return str != null ? str
				.matches("^\\d{4}(\\-|\\/|\\.)\\d{1,2}\\1\\d{1,2}$") : false;
	}

	public static String percent(long a, long b) {
		double k = (double) a / b * 100;
		java.math.BigDecimal big = new java.math.BigDecimal(k);
		return big.setScale(2, java.math.BigDecimal.ROUND_HALF_UP)
				.doubleValue() + "%";
	}

	public static long[] changeSecondsToTime(long seconds) {
		long hour = seconds / 3600;
		long minute = (seconds - hour * 3600) / 60;
		long second = (seconds - hour * 3600 - minute * 60);

		return new long[] { hour, minute, second };
	}

	public static int getDayOfYear(Date date) {
		Calendar c = Calendar.getInstance();
		c.setTime(date);
		return c.get(Calendar.DAY_OF_YEAR);
	}

	public static int getLastDayOfYear(Date date) {
		Calendar c = Calendar.getInstance();
		c.setTime(date);

		return c.getActualMaximum(Calendar.DAY_OF_YEAR);
	}

	public static int getDayOfMonth(Date date) {
		Calendar c = Calendar.getInstance();
		c.setTime(date);

		return c.get(Calendar.DAY_OF_MONTH);
	}

	public static int getLastDayOfMonth(Date date) {
		Calendar c = Calendar.getInstance();
		c.setTime(date);

		return c.getActualMaximum(Calendar.DAY_OF_MONTH);
	}

	// 判断日期为星期几,0为星期六,依此类推
	public static int getDayOfWeek(Date date) {
		// 首先定义一个calendar，必须使用getInstance()进行实例化
		Calendar aCalendar = Calendar.getInstance();
		// 里面野可以直接插入date类型
		aCalendar.setTime(date);
		// 计算此日期是一周中的哪一天
		int x = aCalendar.get(Calendar.DAY_OF_WEEK);
		return x;
	}

	public static int getLastDayOfWeek(Date date) {
		Calendar c = Calendar.getInstance();
		c.setTime(date);
		return c.getActualMaximum(Calendar.DAY_OF_WEEK);
	}

	public static long difference(Date date1, Date date2) {
		Calendar cal1 = Calendar.getInstance();
		cal1.setTime(date1);

		Calendar cal2 = Calendar.getInstance();
		cal2.setTime(date2);

		if (cal2.after(cal1)) {
			return cal2.getTimeInMillis() - cal1.getTimeInMillis();
		}

		return cal1.getTimeInMillis() - cal2.getTimeInMillis();
	}

	public static Date addSecond(Date source, int s) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(source);
		cal.add(Calendar.SECOND, s);

		return cal.getTime();
	}

	public static Date addMinute(Date source, int min) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(source);
		cal.add(Calendar.MINUTE, min);

		return cal.getTime();
	}

	public static Date addHour(Date source, int hour) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(source);
		cal.add(Calendar.HOUR_OF_DAY, hour);

		return cal.getTime();
	}

	public static Date addDate(Date source, int day) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(source);
		cal.add(Calendar.DAY_OF_MONTH, day);

		return cal.getTime();
	}

	public static Date addMonth(Date source, int month) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(source);
		cal.add(Calendar.MONTH, month);

		return cal.getTime();
	}

	public static Date addYear(Date source, int year) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(source);
		cal.add(Calendar.YEAR, year);

		return cal.getTime();
	}

	public static Date parse(String format, String source) throws ParseException {
		SimpleDateFormat sdf = new java.text.SimpleDateFormat(format);
		return sdf.parse(source);
	}

	public static Date parse(String source) throws ParseException {
		return parse("yyyy-MM-dd HH:mm", source);
	}
	
	public static Date parse(long milliseconds){
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(milliseconds);
		return calendar.getTime();
	}
	
	public static String upperFirst(String s) {
		return s.replaceFirst(s.substring(0, 1), s.substring(0, 1)
				.toUpperCase());
	}

	public static boolean isValidEmail(String mail) {
		String regex = "\\w+([-+.]\\w+)*@\\w+([-.]\\w+)*\\.\\w+([-.]\\w+)*";
		Pattern p = Pattern.compile(regex);
		Matcher m = p.matcher(mail);
		return m.find();
	}
	
	public static String byteToStr(byte[] byteArray) {
        String strDigest = "";
        for (int i = 0; i < byteArray.length; i++) {
            strDigest += byteToHexStr(byteArray[i]);
        }
        return strDigest;
    }
	
	public static String byteToHexStr(byte mByte) {
        char[] Digit = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A','B', 'C', 'D', 'E', 'F' };
        char[] tempArr = new char[2];
        tempArr[0] = Digit[(mByte >>> 4) & 0X0F];
        tempArr[1] = Digit[mByte & 0X0F];
        String s = new String(tempArr);
        return s;
    }
}
