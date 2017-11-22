package modelVOs;

import models.Chapter;

public class ChapterVO {

	public long id;
	public int chapterIndex;
	public String title;
	public String brief;
	
	public static ChapterVO getChapterVO(Chapter chapter){
		if(chapter != null){
			ChapterVO chapterVO = new ChapterVO();
			chapterVO.id = chapter.id;
			chapterVO.chapterIndex = chapter.chapterIndex;
			chapterVO.title = chapter.title;
			chapterVO.brief= chapter.brief;
			return chapterVO;
		}
		return null;
	}
}
