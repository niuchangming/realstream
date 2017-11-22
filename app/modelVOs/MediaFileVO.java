package modelVOs;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import models.MediaFile;

public class MediaFileVO {
	public long fileId;
	public String name;
	public long size;
	public String fileType;
	public String uuid;
	public Date uploadedDatetime;
    public long lessonId;
    public long lessonSessionId;
	
	public static List<MediaFileVO> getMediaFileVOs(List<MediaFile> mediaFiles){
		List<MediaFileVO> mediaFileVOs = new ArrayList<MediaFileVO>();
		for(MediaFile mediaFile : mediaFiles){
			MediaFileVO mediaFileVO = new MediaFileVO();
			mediaFileVO.fileId = mediaFile.id;
			mediaFileVO.name = mediaFile.name;
			mediaFileVO.size = mediaFile.size;
			mediaFileVO.uuid = mediaFile.uuid;
			mediaFileVO.fileType = mediaFile.fileType;
			mediaFileVO.uploadedDatetime = mediaFile.uploadedDatetime;
			mediaFileVO.lessonId = mediaFile.lesson.id;
			if(mediaFile.lessonSession != null){
				mediaFileVO.lessonSessionId = mediaFile.lessonSession.id;
			}
			mediaFileVOs.add(mediaFileVO);
		}
		return mediaFileVOs;
	}
}
