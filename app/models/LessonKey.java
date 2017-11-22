package models;

public enum LessonKey {
	//最新， 限时抢购，开课预售, 热门推荐, 可试听, 线上课堂
	//从1开始是因为在过滤课程的时候0代表全选
	NEW("new", 1), OFFER("offer", 2), HOT("hot", 4), TRIAL("trial", 5);
	
	private String name;
    private int index;

    private LessonKey(String name, int index) {
        this.name = name;
        this.index = index;
    }
    
    public int getIndex() {
        return index;
    }
    
    public String getName() {
        return name;
    }
    
    public static String getName(int index) {
        for (LessonKey a : LessonKey.values()) {
            if (a.getIndex() == index) {
                return a.name;
            }
        }
        return null;
    }

}
