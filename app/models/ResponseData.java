package models;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class ResponseData {
	@JsonIgnore
	private final String DEFAULT_MESSAGE = "success";
	@JsonIgnore
	private final int DEFAULT_CODE = 0;
	
	public int code;
	public String message;
	public Object data;
	
	public ResponseData(){
		this.message = DEFAULT_MESSAGE;
		this.code = DEFAULT_CODE;
	}
	
	public ResponseData(Object data){
		this();
		this.data = data;
	}
}


