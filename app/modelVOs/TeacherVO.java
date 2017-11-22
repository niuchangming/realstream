package modelVOs;

import models.User;

public class TeacherVO {
	public long teacherId;
	public String realName;
	public String brief;
	public String avatarUUID;
	
	public static TeacherVO getTeacherVO(User teacher){
		TeacherVO teacherVO = new TeacherVO();
		teacherVO.teacherId = teacher.accountId;
		teacherVO.realName = teacher.realName;
		teacherVO.brief = teacher.brief;
		if(teacher.avatars != null && teacher.avatars.size() > 0){
			teacherVO.avatarUUID = teacher.avatars.get(0).thumbnailUUID;
		}
		return teacherVO;
	}
}
