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
	 * ImageButton�����Ż���ͣ
	 */
	private ImageButton ibPlayOrPause;
	/**
	 * ImageButton����һ��
	 */
	private ImageButton ibPrevious;
	/**
	 * ImageButton����һ��
	 */
	private ImageButton ibNext;
	/**
	 * TextView����ǰ���ŵĸ����ı���
	 */
	private TextView tvMusicTitle;
	/**
	 * SeekBar�������Ľ�����
	 */
	private SeekBar sbMusicProgress;
	/**
	 * TextView����ǰ�Ĳ���ʱ��
	 */
	private TextView tvMusicCurrentPosition;
	/**
	 * TextView����ǰ��������ʱ��
	 */
	private TextView tvMusicDuration;
	/**
	 * ���Ź���
	 */
	private MediaPlayer player;
	/**
	 * ��ͣ��λ��
	 */
	private int pausePosition;
	/**
	 * �����б�������Դ
	 */
	private List<Music> musics;
	/**
	 * ListView�������б�
	 */
	private ListView lvMusics;
	/**
	 * �����б���Adapter
	 */
	private MusicAdapter adapter;
	/**
	 * ��ǰ���ŵĸ���������
	 */
	private int currentMusicIndex;
	/**
	 * ���½��ȵ�ѭ������
	 */
	private boolean isRunning;
	/**
	 * ���½��ȵ��߳�
	 */
	private UpdateProgressThread thread;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// ��ȡ���и���������
		MusicDao dao = new MusicDao();
		musics = dao.getData();

		// ��ʼ���ؼ�
		ibPlayOrPause = (ImageButton) findViewById(R.id.ib_play_or_pause);
		ibPrevious = (ImageButton) findViewById(R.id.ib_previous);
		ibNext = (ImageButton) findViewById(R.id.ib_next);
		tvMusicTitle = (TextView) findViewById(R.id.tv_music_title);
		tvMusicCurrentPosition = (TextView) findViewById(R.id.tv_music_current_position);
		tvMusicDuration = (TextView) findViewById(R.id.tv_music_duration);
		sbMusicProgress = (SeekBar) findViewById(R.id.sb_music_progress);
		lvMusics = (ListView) findViewById(R.id.lv_musics);

		// ��ʾListView
		adapter = new MusicAdapter(this, musics);
		lvMusics.setAdapter(adapter);
		
		// Ϊ�ؼ����ü�����
		ibPlayOrPause.setOnClickListener(this);
		ibPrevious.setOnClickListener(this);
		ibNext.setOnClickListener(this);
		lvMusics.setOnItemClickListener(this);
		sbMusicProgress.setOnSeekBarChangeListener(this);

		// ��ʼ�����Ź���
		player = new MediaPlayer();
		
		// ���ò�����ɼ�����
		player.setOnCompletionListener(this);
	}
	
	/**
	 * �������½��ȵ��߳�
	 */
	private void startUpdateProgressThread() {
		if(thread == null) {
			isRunning = true;
			thread = new UpdateProgressThread();
			thread.start();
		}
	}
	
	/**
	 * ֹͣ���½��ȵ��߳�
	 */
	private void stopUpdateProgressThread() {
		isRunning = false;
		thread = null;
	}
	
	/**
	 * ���½��ȵ��߳�
	 */
	private class UpdateProgressThread extends Thread {
		private class Runner implements Runnable {

			@Override
			public void run() {
				// --- ���½��� ---
				// 1. ��ȡ��ǰ���ŵ���λ��
				int currentPosition = player.getCurrentPosition();
				// 2. ��ȡ��������ʱ��
				int duration = player.getDuration();
				// 3. ���㵱ǰ���ŵİٷֱ�
				int percent = currentPosition * 100 / duration;
				// 4. ���½������������ڰٷֱȣ�
				if(!isTrackingTouchSeekBar) {
					sbMusicProgress.setProgress(percent);
				}
				// 5. ����������ʾ�������ڵ�ǰ���ŵ���λ�ã�
				tvMusicCurrentPosition.setText(CommonUtils.getFormattedDate(currentPosition));
			}
			
		}
		
		private Runner runner = new Runner();
		
		@Override
		public void run() {
			while(isRunning) {
				// 1. ���½���
				if(player.isPlaying()) {
					runOnUiThread(runner);
				}
				
				// 2. ����
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
			// �жϲ������Ĳ���״̬�����ڲ��Ż�����ͣ
			if (player.isPlaying()) {
				// ���ڲ��ţ�����ͣ
				pause();
			} else {
				// û���ڲ��ţ��򲥷�
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
	 * ��ͣ
	 */
	private void pause() {
		// ��ͣ
		player.pause();
		// ��ȡ��ǰ���ŵ�λ�ã�������
		pausePosition = player.getCurrentPosition();
		// ���ð�ť�ϵ�ͼƬ������
		ibPlayOrPause.setImageResource(android.R.drawable.ic_media_play);
		// ֹͣ���½��ȵ��߳�
		stopUpdateProgressThread();
	}

	/**
	 * ����
	 */
	private void play() {
		try {
			// ----- ���Ÿ��� -----
			// 1. ���ò��Ź���
			player.reset();
			// 2. ������Ҫ���ŵĸ���
			player.setDataSource(musics.get(currentMusicIndex).getPath());
			// 3. ���壺׼����������/���ظ�������
			player.prepare();
			// 4. �������ͣ��λ��
			player.seekTo(pausePosition);
			// 5. ����
			player.start();
			// 6. ���ð�ť�ϵ�ͼƬ����ͣ
			ibPlayOrPause.setImageResource(android.R.drawable.ic_media_pause);
			// 7. ������ʾ��������
			tvMusicTitle.setText("���ڲ��ţ�" + musics.get(currentMusicIndex).getTitle());
			// 8. ������ʾ��ǰ��������ʱ��
			int duration = player.getDuration();
			tvMusicDuration.setText(CommonUtils.getFormattedDate(duration));
			// 9. �������½��ȵ��߳�
			startUpdateProgressThread();
			// 10. ��ʶ�Ѿ���ʼ����
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
	 * ������һ��
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
	 * ������һ��
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
		// �����ӣ�
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
			// 1. ������ק���Ӧ��ʱ��
			int percent = seekBar.getProgress();
			pausePosition = player.getDuration() * percent / 100;
			// 2. ����
			play();
		}
		isTrackingTouchSeekBar = false;
	}

}