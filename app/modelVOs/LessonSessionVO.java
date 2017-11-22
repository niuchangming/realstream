package modelVOs;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import models.LessonSession;

public class LessonSessionVO {
	public long lessonSessionId;
	public String title;
	public String brief;
	public Date startDatetime;
	public Date endDatetime;
	public int duration;
	public boolean isTrial;
	public ChapterVO chapter;
	public int fileCount;
	public int questionCount;
	
	public static List<LessonSessionVO> getLessonSessionVOs(List<LessonSession> lessonSessions){
		List<LessonSessionVO> lessonSessionVOs = new ArrayList<LessonSessionVO>(lessonSessions.size());
		for(LessonSession lessonSession : lessonSessions){
			LessonSessionVO lessonSessionVO = new LessonSessionVO();
			lessonSessionVO.lessonSessionId = lessonSession.id;
			lessonSessionVO.title = lessonSession.title;
			lessonSessionVO.brief = lessonSession.brief;
			lessonSessionVO.startDatetime = lessonSession.startDatetime;
			lessonSessionVO.endDatetime = lessonSession.endDatetime;
			lessonSessionVO.duration = lessonSession.duration;
			lessonSessionVO.isTrial = lessonSession.isTrial;
			lessonSessionVO.chapter = ChapterVO.getChapterVO(lessonSession.chapter);
			lessonSessionVO.fileCount = lessonSession.mediaFiles.size();
			lessonSessionVO.questionCount = lessonSession.questions.size();
			lessonSessionVOs.add(lessonSessionVO);
		}
		return lessonSessionVOs;
	}

}
