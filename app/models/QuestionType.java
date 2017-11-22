package models;

public enum QuestionType {
	MULTIPLE_CHOICE(0, "Multiple Choice"), SHORT(1, "Short Question"), PICTURE_CHOICE(2, "Picture Question"), READ_COMPREHENSION(3, "Reading Comprehenson"), ESSAY(4, "Essay");
	
	private int index;
	private String name;

	private QuestionType(int index, String name) { 
		this.index = index;
		this.name = name;
	}

	
	public static QuestionType getIndex(int index) {
		for (QuestionType type : QuestionType.values()) {
			if (type.index == index) return type;
		}
		throw new IllegalArgumentException("Type not found. Amputated?");
	}
	
	public int getIndex() {
		return index;
	}


	public void setIndex(int index) {
		this.index = index;
	}


	public String getName() {
		return name;
	}


	public void setName(String name) {
		this.name = name;
	}


	public static String getName(int index) {
        for (QuestionType q : QuestionType.values()) {
            if (q.index == index) {
                return q.name;
            }
        }
        throw new IllegalArgumentException("Type not found. Amputated?");
    }
}
