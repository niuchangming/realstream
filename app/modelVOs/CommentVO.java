package modelVOs;

public class CommentVO extends Object{
	public String comment;
	public int point;
	public String author;
	public String authorAvatar;
	
	public CommentVO(String comment, int point, String authorAvatar, String author) {
		super();
		this.comment = comment;
		this.point = point;
		this.author = author;
		this.authorAvatar = authorAvatar;
	}

}
