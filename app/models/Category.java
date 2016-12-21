package models;

import java.io.File;
import java.io.IOException;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.type.TypeFactory;

import play.db.jpa.JPA;

@Entity
@Table(name="category")
public class Category {
	@Transient
	private static final String categoryJsonPath = "public/raws/category.json";
	
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
	
	public static List<Category> initCategoryByJson(){
		List<Category> categories = null;
		try {
			ObjectMapper mapper = new ObjectMapper();
			categories = mapper.readValue(new File(categoryJsonPath),
					TypeFactory.defaultInstance().constructCollectionType(List.class, Category.class));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return categories;
	}

}
