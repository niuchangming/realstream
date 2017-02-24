package models;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.type.TypeFactory;

@Entity
@Table(name="category")
public class Category {
	
	@Id
	@GeneratedValue	
	@JsonIgnore
	public long id;
	
	public String name;
	
	@OneToMany(mappedBy="parent", fetch = FetchType.LAZY)
	@JsonDeserialize(contentAs=Category.class)
	public List<Category> children;
	
	@ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="parent_id")
	public Category parent;
	
	@OneToMany(mappedBy = "category")
	public List<Lesson> lessons;
	
	public Category(){}
	
	public Category(String name) {
		this.name = name;
	}
	
	public static List<Category> initCategoryByJson(InputStream in){
		List<Category> categories = null;
		try {
			ObjectMapper mapper = new ObjectMapper();
			categories = mapper.readValue(in,
					TypeFactory.defaultInstance().constructCollectionType(List.class, Category.class));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return categories;
	}

}
