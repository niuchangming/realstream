package models;

public enum LessonKey {
	//最新， 金牌讲师， 限时抢购，开课预售, 热门推荐, 可试听，人气, 线上课堂
	NEW("New", 0), TOP_LECTURER("Top Lecturer", 1), FLASH_OFFER("Flash offer", 2), PRE_SALE("Pre-Sale", 3), RECOMMEND("Recommend", 4), 
	TRIAL("Trial", 5), POPULARITY("Popularity", 6), ONLINE_CLASSROOM("Real-Class", 7);
	
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

}
