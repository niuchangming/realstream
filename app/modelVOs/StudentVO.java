package modelVOs;

public class StudentVO {
	public long accountId;
	public String username;
	public String lessonTitle;
	public double progress;

	public StudentVO(long accountId, String username, String lessonTitle, double progress) {
		super();
		this.accountId = accountId;
		this.lessonTitle = lessonTitle;
		this.username = username;
		this.progress = progress;
	}
}
