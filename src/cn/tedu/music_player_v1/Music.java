package cn.tedu.music_player_v1;

public class Music {
	private String path;
	private String title;

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	@Override
	public String toString() {
		return "Music [path=" + path + ", title=" + title + "]";
	}
	
	
}