package models;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Entity
@Table(name = "question")
public class Question {
	@Id
	@GeneratedValue
	@JsonIgnore
	public long id;
	
	@Lob
	public String content;
	
	@Column(name="question_type")
	public QuestionType questionType;
	
	@OneToMany(mappedBy = "question", targetEntity=QuestionImage.class)
	public List<QuestionImage> questionImages; //以OneToMany来模仿OneToOne，实现懒加载
	
	@OneToMany(mappedBy = "question", targetEntity=ChoiceImage.class)
	public List<ChoiceImage> choiceImages;
	
	@Column(name="video_url")
	public String videoUrl;
	
	@OneToMany(mappedBy = "question")
	public List<Answer> answers; //以OneToMany来模仿OneToOne，实现懒加载
	
	@OneToMany(mappedBy="parent")
	@JsonDeserialize(contentAs=Category.class)
	@JsonIgnore
	public List<Question> children;
	
	@ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="parent_id")
	@JsonIgnore
	public Question parent;
	
	@ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id") 
    public Lesson lesson;
	
	@ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_session_id") 
    public LessonSession lessonSession;
	
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "create_datetime")
	public Date createDatetime; 
	
	public Question (){}
	
	public Question (Lesson lesson, LessonSession lessonSession){
		this.lesson = lesson;
		this.lessonSession = lessonSession;
		this.createDatetime = new Date();
	}
	
	public String initChoices(Map<String, String> data) throws ParseException, JsonProcessingException{
		Iterator<String> iterator = data.keySet().iterator();
		Map<Integer, String> choiceResult = new HashMap<>();
	    Map<Integer, String> choiceContent = new HashMap<>();
	    Map<Integer, String> choiceKey = new HashMap<>();
	    
	    String key;
	    while(iterator.hasNext()){
	    	key = iterator.next();
	    	if(key.contains("choiceResult")){
	    		int startIdx = key.indexOf("[") + 1;
	    		int endIdx = key.indexOf("]");
	    		int pos = Integer.parseInt(key.substring(startIdx, endIdx));
	    		choiceResult.put(pos, data.get(key));
	    	}
	    	
	    	if(key.contains("choiceContent")){
	    		int startIdx = key.indexOf("[") + 1;
	    		int endIdx = key.indexOf("]");
	    		int pos = Integer.parseInt(key.substring(startIdx, endIdx));
	    		choiceContent.put(pos, data.get(key));
	    	}
	    	
	    	if(key.contains("choiceKey")){
	    		int startIdx = key.indexOf("[") + 1;
	    		int endIdx = key.indexOf("]");
	    		int pos = Integer.parseInt(key.substring(startIdx, endIdx));
	    		choiceKey.put(pos, data.get(key));
	    	}
	    }
	    
	    List<ObjectNode> nodes = new ArrayList<>();
	    for(int i = 0; i < choiceKey.size(); i++){
	    	ObjectNode node = JsonNodeFactory.instance.objectNode();
	    	node.put("content", choiceContent.get(i));
	    	node.put("key", choiceKey.get(i));
	    	node.put("result", choiceResult.get(i));
	    	nodes.add(node);
	    }
	    
	    ObjectMapper mapper = new ObjectMapper();
	    return mapper.writeValueAsString(nodes);
	}
}







































