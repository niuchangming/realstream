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

//4000 logical error
//4001 server throws error
//4002 opentok get broadcast info failed due to delay time, so have to wait 15-20 seconds
//5000 warning


