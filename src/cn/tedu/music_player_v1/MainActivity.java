package cn.tedu.music_player_v1;

import java.io.IOException;
import java.util.List;

import android.app.Activity;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;

public class MainActivity extends Activity implements OnSeekBarChangeListener, OnCompletionListener, View.OnClickListener, OnItemClickListener {
	/**
	 * ImageButton：播放或暂停
	 */
	private ImageButton ibPlayOrPause;
	/**
	 * ImageButton：上一首
	 */
	private ImageButton ibPrevious;
	/**
	 * ImageButton：下一首
	 */
	private ImageButton ibNext;
	/**
	 * TextView：当前播放的歌曲的标题
	 */
	private TextView tvMusicTitle;
	/**
	 * SeekBar：歌曲的进度条
	 */
	private SeekBar sbMusicProgress;
	/**
	 * TextView：当前的播放时间
	 */
	private TextView tvMusicCurrentPosition;
	/**
	 * TextView：当前歌曲的总时长
	 */
	private TextView tvMusicDuration;
	/**
	 * 播放工具
	 */
	private MediaPlayer player;
	/**
	 * 暂停的位置
	 */
	private int pausePosition;
	/**
	 * 歌曲列表的数据源
	 */
	private List<Music> musics;
	/**
	 * ListView：歌曲列表
	 */
	private ListView lvMusics;
	/**
	 * 歌曲列表的Adapter
	 */
	private MusicAdapter adapter;
	/**
	 * 当前播放的歌曲的索引
	 */
	private int currentMusicIndex;
	/**
	 * 更新进度的循环条件
	 */
	private boolean isRunning;
	/**
	 * 更新进度的线程
	 */
	private UpdateProgressThread thread;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// 获取所有歌曲的数据
		MusicDao dao = new MusicDao();
		musics = dao.getData();

		// 初始化控件
		ibPlayOrPause = (ImageButton) findViewById(R.id.ib_play_or_pause);
		ibPrevious = (ImageButton) findViewById(R.id.ib_previous);
		ibNext = (ImageButton) findViewById(R.id.ib_next);
		tvMusicTitle = (TextView) findViewById(R.id.tv_music_title);
		tvMusicCurrentPosition = (TextView) findViewById(R.id.tv_music_current_position);
		tvMusicDuration = (TextView) findViewById(R.id.tv_music_duration);
		sbMusicProgress = (SeekBar) findViewById(R.id.sb_music_progress);
		lvMusics = (ListView) findViewById(R.id.lv_musics);

		// 显示ListView
		adapter = new MusicAdapter(this, musics);
		lvMusics.setAdapter(adapter);
		
		// 为控件配置监听器
		ibPlayOrPause.setOnClickListener(this);
		ibPrevious.setOnClickListener(this);
		ibNext.setOnClickListener(this);
		lvMusics.setOnItemClickListener(this);
		sbMusicProgress.setOnSeekBarChangeListener(this);

		// 初始化播放工具
		player = new MediaPlayer();
		
		// 配置播放完成监听器
		player.setOnCompletionListener(this);
	}
	
	/**
	 * 开启更新进度的线程
	 */
	private void startUpdateProgressThread() {
		if(thread == null) {
			isRunning = true;
			thread = new UpdateProgressThread();
			thread.start();
		}
	}
	
	/**
	 * 停止更新进度的线程
	 */
	private void stopUpdateProgressThread() {
		isRunning = false;
		thread = null;
	}
	
	/**
	 * 更新进度的线程
	 */
	private class UpdateProgressThread extends Thread {
		private class Runner implements Runnable {

			@Override
			public void run() {
				// --- 更新进度 ---
				// 1. 获取当前播放到的位置
				int currentPosition = player.getCurrentPosition();
				// 2. 获取歌曲的总时长
				int duration = player.getDuration();
				// 3. 计算当前播放的百分比
				int percent = currentPosition * 100 / duration;
				// 4. 更新进度条（依赖于百分比）
				if(!isTrackingTouchSeekBar) {
					sbMusicProgress.setProgress(percent);
				}
				// 5. 更新文字显示（依赖于当前播放到的位置）
				tvMusicCurrentPosition.setText(CommonUtils.getFormattedDate(currentPosition));
			}
			
		}
		
		private Runner runner = new Runner();
		
		@Override
		public void run() {
			while(isRunning) {
				// 1. 更新进度
				if(player.isPlaying()) {
					runOnUiThread(runner);
				}
				
				// 2. 休眠
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.ib_play_or_pause:
			// 判断播放器的播放状态：正在播放或者暂停
			if (player.isPlaying()) {
				// 正在播放，则暂停
				pause();
			} else {
				// 没有在播放，则播放
				play();
			}
			break;
			
		case R.id.ib_previous:
			previous();
			break;
			
		case R.id.ib_next:
			next();
			break;
		}
	}
	
	
	@Override
	protected void onDestroy() {
		player.release();
		stopUpdateProgressThread();
		super.onDestroy();
	}

	/**
	 * 暂停
	 */
	private void pause() {
		// 暂停
		player.pause();
		// 获取当前播放的位置：毫秒数
		pausePosition = player.getCurrentPosition();
		// 设置按钮上的图片：播放
		ibPlayOrPause.setImageResource(android.R.drawable.ic_media_play);
		// 停止更新进度的线程
		stopUpdateProgressThread();
	}

	/**
	 * 播放
	 */
	private void play() {
		try {
			// ----- 播放歌曲 -----
			// 1. 重置播放工具
			player.reset();
			// 2. 设置需要播放的歌曲
			player.setDataSource(musics.get(currentMusicIndex).getPath());
			// 3. 缓冲：准备歌曲数据/加载歌曲数据
			player.prepare();
			// 4. 快进到暂停的位置
			player.seekTo(pausePosition);
			// 5. 播放
			player.start();
			// 6. 设置按钮上的图片：暂停
			ibPlayOrPause.setImageResource(android.R.drawable.ic_media_pause);
			// 7. 设置显示歌曲标题
			tvMusicTitle.setText("正在播放：" + musics.get(currentMusicIndex).getTitle());
			// 8. 设置显示当前歌曲的总时长
			int duration = player.getDuration();
			tvMusicDuration.setText(CommonUtils.getFormattedDate(duration));
			// 9. 开启更新进度的线程
			startUpdateProgressThread();
			// 10. 标识已经开始播放
			isStarted = true;
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 播放上一首
	 */
	private void previous() {
		currentMusicIndex--;
		if(currentMusicIndex < 0) {
			currentMusicIndex = musics.size() - 1;
		}
		pausePosition = 0;
		play();
	}
	
	/**
	 * 播放下一首
	 */
	private void next() {
		currentMusicIndex++;
		if(currentMusicIndex >= musics.size()) {
			currentMusicIndex = 0;
		}
		pausePosition = 0;
		play();
	}

	@Override
	public void onItemClick(
			AdapterView<?> parent, 
			View view, 
			int position,
			long id) {
		currentMusicIndex = position;
		pausePosition = 0;
		play();
	}

	@Override
	public void onCompletion(MediaPlayer mp) {
		next();
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress,
			boolean fromUser) {
		// （无视）
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
		isTrackingTouchSeekBar = true;
	}
	
	private boolean isStarted;
	private boolean isTrackingTouchSeekBar;

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		if(isStarted) {
			// 1. 计算拖拽后对应的时间
			int percent = seekBar.getProgress();
			pausePosition = player.getDuration() * percent / 100;
			// 2. 播放
			play();
		}
		isTrackingTouchSeekBar = false;
	}

}
