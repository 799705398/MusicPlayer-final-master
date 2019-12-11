package cn.tedu.music_player_v1;

import java.text.SimpleDateFormat;
import java.util.Date;

public class CommonUtils {
	static SimpleDateFormat sdf = new SimpleDateFormat("mm:ss");
	
	public static String getFormattedDate(int time)
	{
		String t = sdf.format(time);
		return t;
	}
}
