package modelVOs;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import models.Lesson;

public class LessonVO {
	public long lessonId;
	public String title;
	public String description;
	public double price;
	public long categoryId;
	public int maxCount;
	public boolean interactive;
	public Date startDatetime;
	public Date endDatetime;
	public int studentCount;
	public int favoriteCount;
	public int commentCount;
	public List<LessonImageVO> lessonImages;
	public TeacherVO teacher;
	
	public static List<LessonVO> getLessonVOs(List<Lesson> lessons){
		List<LessonVO> lessonVOs = new ArrayList<LessonVO>(lessons.size());
		for(Lesson lesson : lessons){
			LessonVO lessonVO = new LessonVO();
			lessonVO.lessonId = lesson.id;
			lessonVO.title = lesson.title;
			lessonVO.description = lesson.description;
			lessonVO.price = lesson.price;
			lessonVO.categoryId = lesson.category.id;
			lessonVO.maxCount = lesson.maxCount;
			lessonVO.interactive = lesson.interactive;
			lessonVO.startDatetime = lesson.startDatetime;
			lessonVO.endDatetime = lesson.endDatetime;
			lessonVO.studentCount = lesson.userLessons.size();
			lessonVO.favoriteCount = lesson.users.size();
			lessonVO.commentCount = lesson.comments.size();
			lessonVO.lessonImages = LessonImageVO.getLessonVOs(lesson.lessonImages);
			lessonVO.teacher = TeacherVO.getTeacherVO(lesson.teacher);
			lessonVOs.add(lessonVO);
		}
		return lessonVOs;
	}
}
