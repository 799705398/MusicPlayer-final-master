package cn.tedu.music_player_v1;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.os.Environment;
import android.util.Log;

public class MusicDao {
	
	public List<Music> getData()
	{
		List<Music> musics = new ArrayList<Music>();
		
		if(Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()))
		{
			//判断sdcard是否存在
			File musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
			
			//判断文件夹是否存在
			if(musicDir.exists())
			{
				//获取目录下所有文件
				File[] files = musicDir.listFiles();
				//遍历
				for(int i=0;i<files.length;i++)
				{
					//判断是否文件
					if(files[i].isFile())
					{
						//判断扩展名
						if(files[i].getName().toLowerCase(Locale.CHINA).endsWith(".mp3"))
						{
							Log.d("tedu",""+files[i]);
							Music music = new Music();
							music.setPath(files[i].getAbsolutePath());
							music.setTitle(files[i].getName().substring(0, files[i].getName().length()-4));                                          
							musics.add(music);
						}
					}
				}
			}
		}
		return musics;
	}
}
