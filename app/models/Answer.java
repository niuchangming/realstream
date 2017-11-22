package models;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@Entity
@Table(name = "answer")
public class Answer {
	@Id
	@GeneratedValue
	public long id;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "question_id")
	public Question question;
	
	@Column(name="choice_json")
	public String choiceJson;
	
	public String explanation;// 如何得到的答案
	
	public String content;//答案
	
	@OneToMany(mappedBy = "answer")
	public List<AnswerImage> answerImages;
	
	public Answer(){}
	
	public Answer(Question question){
		this.question = question;
	}
}
