package modelVOs;

import java.util.ArrayList;
import java.util.List;
import models.LessonImage;

public class LessonImageVO {
	
	public long imageId;
	public String name;
	public String title;
	public String caption;
	public String uuid;
	public String thumbnailUUID;
	public boolean isCover;
	
	public static List<LessonImageVO> getLessonVOs(List<LessonImage> lessonImages){
		List<LessonImageVO> lessonImageVOs = new ArrayList<LessonImageVO>(lessonImages.size());
		for(LessonImage lessonImage : lessonImages){
			LessonImageVO lessonImageVO = new LessonImageVO();
			lessonImageVO.imageId = lessonImage.id;
			lessonImageVO.name = lessonImage.name;
			lessonImageVO.title = lessonImage.title;
			lessonImageVO.caption = lessonImage.caption;
			lessonImageVO.uuid = lessonImage.uuid;
			lessonImageVO.isCover = lessonImage.isCover;
			lessonImageVO.thumbnailUUID = lessonImage.thumbnailUUID;
			lessonImageVOs.add(lessonImageVO);
		}
		return lessonImageVOs;
	}

}
