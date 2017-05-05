package skylin.util;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Pattern;

import skylin.server.Server;


public class Util 
{

	public static void setSessionObject(String name, Object object) {
		// TODO
		
	}



	public static Object getCurrentRequest() 
	{
		// TODO
		return new Object();
	}
	
	public static String dateToString(Timestamp date, String format)
	{
		return dateToString(date.getTime(),format);
	}
	
	public static String dateToString(long date, String format)
	{
		SimpleDateFormat formatter = new SimpleDateFormat(format);


		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(date);
		return formatter.format(calendar.getTime()); 
	}
	
	public static Timestamp stringToDate(String s)
	{
		s = s.trim();
		s = s.replaceAll("-", " ").replaceAll("_", " ").replaceAll("/", " ").replaceAll(Pattern.quote("\\"), " ").replaceAll(",", " ").replaceAll(";", " ").replaceAll(":", " ");
		while (s.contains("  "))
		{
			s = s.replaceAll("  ", " ");
		}
		
		String[] parts = s.split(" ");
		if (parts.length != 3)
		{
			return null;
		}
		
		if (parts[0].length() == 1)
		{
			parts[0] = "0"+parts[0];
		}
		if (parts[0].length() != 2 || parts[0].equals("00"))
		{
			return null;
		}
		if (parts[2].length() == 2)
		{
			parts[2] = "20"+parts[2];
		}
		if (parts[2].length() != 4)
		{
			return null;
		}
		
		if (parts[1].length() == 1)
		{
			parts[1] = "0"+parts[1];
		}
		else if (parts[1].length() != 2)
		{
			String m = parts[1].toUpperCase();
			if (m.equals("JANUARY") || m.equals("JAN")){parts[1] = "01";}
			else if (m.equals("FEBRUARY") || m.equals("FEB")){parts[1] = "02";}
			else if (m.equals("MARCH") || m.equals("MAR")){parts[1] = "03";}
			else if (m.equals("APRIL") || m.equals("APR")){parts[1] = "04";}
			else if (m.equals("MAY") || m.equals("MAY")){parts[1] = "05";}
			else if (m.equals("JUNE") || m.equals("JUN")){parts[1] = "06";}
			else if (m.equals("JULY") || m.equals("JUL")){parts[1] = "07";}
			else if (m.equals("AUGUST") || m.equals("AUG")){parts[1] = "08";}
			else if (m.equals("SEPTEMBER") || m.equals("SEP")){parts[1] = "09";}
			else if (m.equals("OCTOBER") || m.equals("OCT")){parts[1] = "10";}
			else if (m.equals("NOVEMBER") || m.equals("NOV")){parts[1] = "11";}
			else if (m.equals("DECEMBER") || m.equals("DEC")){parts[1] = "12";}
		}
		if (parts[1].length() != 2 || parts[1].equals("00"))
		{
			return null;
		}

		s = parts[0] + " " + parts[1] + " " + parts[2];
		SimpleDateFormat format = new SimpleDateFormat("dd MM yyyy", Locale.ENGLISH);
		Date date = null;
		try {
			date = format.parse(s);
		} catch (ParseException e) {
			return null;
		}
		
		return new Timestamp(date.getTime());
	}
	
    private static SecureRandom random = new SecureRandom();
    final private static char[] hexArray = "0123456789ABCDEF".toCharArray();
	public static String getSecureUnsignedNumberAsHexString(int length)
	{

	      //byte bytes[] = new byte[length+1];
	      //random.nextBytes(bytes);
	      //bytes[0] = 0;
	      //return new BigInteger(bytes).toString(16);
	      
    	  byte bytes[] = new byte[length];
	      random.nextBytes(bytes);
    	
	      char[] hexChars = new char[bytes.length * 2];
	      for ( int j = 0; j < bytes.length; j++ ) {
	          int v = bytes[j] & 0xFF;
	          hexChars[j * 2] = hexArray[v >>> 4];
	          hexChars[j * 2 + 1] = hexArray[v & 0x0F];
	      }
	      return new String(hexChars);
	}
	
	public static String getFrameworkParam(String param)
	{
		return Server.frameworkParams.get(param);
	}
	
	public static String getCustomParam(String param)
	{
		return Server.customParams.get(param);
	}

}
