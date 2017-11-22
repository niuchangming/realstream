package modelVOs;

import java.math.BigInteger;

import models.Role;
import models.User;
import play.db.jpa.JPA;

public class AchievementVO {
	public int lessonCount;
	public int fileCount;
	public int studentCount;
	
	public int favLessonCount;
	public int credit;
	
	
	public AchievementVO(User user){
		if(user.role.equals(Role.TEACHER)){
			lessonCount = user.teacherLessons.size();
			
			fileCount = ((BigInteger)JPA.em()
					.createNativeQuery("SELECT COUNT(*) FROM media_file mf LEFT JOIN lesson ls ON mf.lesson_id = ls.id where ls.teacher_id=:teacherId")
			.setParameter("teacherId", user.accountId)
			.getSingleResult()).intValue();
			
			studentCount = ((BigInteger)JPA.em()
					.createNativeQuery("SELECT COUNT(*) FROM user_lesson ul LEFT JOIN lesson ls ON ul.lesson_id=ls.id LEFT JOIN user u ON ls.teacher_id = u.account_id where u.account_id=:teacherId")
					.setParameter("teacherId", user.accountId)
					.getSingleResult()).intValue();
		}else if(user.role.equals(Role.STUDENT)){
			lessonCount = user.userLessons.size();
			favLessonCount = user.favoriteLessons.size();
			credit = user.credit;
		}
	}

}
