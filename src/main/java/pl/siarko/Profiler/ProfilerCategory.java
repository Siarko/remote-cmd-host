package pl.siarko.Profiler;

public class ProfilerCategory {

    private int times = 0;
    private int count = 0;

    public void update(int time){
        this.times += time;
        this.count++;
    }

    public int getCount(){
        return this.count;
    }

    public float getAvg(){
        return (float)(this.times/this.count);
    }
}
