package modelVOs;

import models.User;

public class ProfileVO {
	public String avatarUUID;
	public String username;
	public String realName;
	public String email;
	public String wechat;
	public String qq;
	public int credit;
	public int coin;
	public String ic;
	public String brief;
	public String bestInstitution;
	public int favoriteLessonCount;
	public int myLessonCount;
	
	public ProfileVO(User user){
		if(user.avatars != null &&  user.avatars.size() > 0){
			this.avatarUUID = user.avatars.get(0).thumbnailUUID;
		}
		this.username = user.username;
		this.realName = user.realName;
		this.email = user.email;
		this.wechat = user.wechat;
		this.qq = user.qq;
		this.credit = user.credit;
		this.coin = user.coin;
		this.ic = user.ic;
		this.brief = user.brief;
		this.bestInstitution = user.bestInstitution;
		this.favoriteLessonCount = user.favoriteLessons.size();
		this.myLessonCount = user.userLessons.size();
	}
}
