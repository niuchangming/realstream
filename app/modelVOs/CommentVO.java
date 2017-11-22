package modelVOs;

import java.util.Date;

public class CommentVO {
	public String comment;
	public int point;
	public String author;
	public String authorAvatar;
	public Date createDatetime; 
	
	public CommentVO(String comment, int point, Date createDatetime, String authorAvatar, String author) {
		super();
		this.comment = comment;
		this.point = point;
		this.author = author;
		this.authorAvatar = authorAvatar;
		this.createDatetime = createDatetime;
	}

}
