package models;

public enum TuiWenType {
	ARTICLE("article", 0), INTEREST("interest", 1), RESOURCE("resource", 2), EXAM("exam", 3);
	
	private String name;
    private int index;

    private TuiWenType(String name, int index) {
        this.name = name;
        this.index = index;
    }
    
    public int getIndex() {
        return index;
    }
    
    public String getName() {
        return name;
    }
    
    public static TuiWenType getTuiWenType(int index) {
    	for (TuiWenType l : TuiWenType.values()) {
            if (l.index == index) return l;
        }
        throw new IllegalArgumentException("TuiWenType not found. Amputated?");
    }
    
    public static String getName(int index) {
        for (TuiWenType a : TuiWenType.values()) {
            if (a.getIndex() == index) {
                return a.name;
            }
        }
        return null;
    }
}
