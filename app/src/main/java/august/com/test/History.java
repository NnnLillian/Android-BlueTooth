package august.com.test;


public class History {
    private String time;
    private String LeakSize;

    public History() {
    }

    public History(String time, String LeakSize) {
        this.time = time;
        this.LeakSize = LeakSize;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getLeakSize() {
        return LeakSize;
    }

    public void setLeakSize(String LeakSize) {
        this.LeakSize = LeakSize;
    }
}
