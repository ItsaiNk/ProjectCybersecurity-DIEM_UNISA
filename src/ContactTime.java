import java.io.Serializable;

public class ContactTime implements Serializable {

    private int startContactTime;
    private int endContactTime;

    public ContactTime(int startContactTime,  int endContactTime) {
        this.startContactTime = startContactTime;
        this.endContactTime = endContactTime;
    }

    public ContactTime(int startContactTime) {
        this.startContactTime = startContactTime;
        this.endContactTime = startContactTime+1;
    }

    public int getStartContactTime() {
        return startContactTime;
    }

    public void setStartContactTime(int startContactTime) {
        this.startContactTime = startContactTime;
    }

    public int getEndContactTime() {
        return endContactTime;
    }

    public void setEndContactTime(int endContactTime) {
        this.endContactTime = endContactTime;
    }

    public int getContactTime(){
        return this.endContactTime-this.startContactTime;
    }
}
