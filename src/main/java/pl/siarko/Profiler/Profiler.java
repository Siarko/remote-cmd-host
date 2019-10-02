package pl.siarko.Profiler;

import java.util.HashMap;
import java.util.Map;

public class Profiler {
    private long startTime;
    private static Map<String, ProfilerCategory> profilerCategoryMap = new HashMap<>();
    private static boolean showPartial = true;

    public static void setShowPartial(boolean state){
        showPartial = state;
    }

    public Profiler(){
        this.start();
    }
    public Profiler( boolean start){
        if(start){
            this.start();
        }
    }



    public void start(){
        this.startTime = System.currentTimeMillis();
    }

    public int end(String title){
        int t = this.end();
        if(!profilerCategoryMap.containsKey(title)){
            profilerCategoryMap.put(title, new ProfilerCategory());
        }
        profilerCategoryMap.get(title).update(t);
        if(showPartial){
            System.out.println("[PROFILER] "+title+" : "+t+"ms");
        }
        return t;
    }

    public int end(){
        return (int) (System.currentTimeMillis()-this.startTime);
    }

    private static String fillRight(String s, int len, String filler){
        int l = s.length();
        String spc = new String(new char[len-l]).replace("\0", filler);
        return s+spc;
    }

    private static String fillRight(String s, int len){
        return fillRight(s, len, " ");
    }

    public static void resetSummary() {
        profilerCategoryMap.clear();
    }

    public static void printSummary(){
        String header = " PROFILOWANIE: SREDNIE CZASY ";
        String firstCol = " KATEGORIA ";
        String secCol = " CZAS " ;
        String thrCol = " POWTORZENIA ";

        int longest = 0;
        for(String s: profilerCategoryMap.keySet()){
            if(s.length() > longest){longest = s.length();}
        }
        if(longest < firstCol.length()){ longest = firstCol.length()+1;}
        int targetW = longest + 2;
        int s = targetW/2-firstCol.length()/2;
        String sep1 = fillRight("=", (s<2?2:s), "=");
        int s2 = 7-secCol.length()/2;
        String sep2 = fillRight("=", s2, "=");
        String tabHeader = "|"+sep1+firstCol+sep1+"|"+sep2+secCol+sep2+"=|=="+thrCol+"==|";
        int s3 = tabHeader.length()/2-header.length()/2;
        String sep3 = fillRight("=", s3, "=");
        System.out.println(" ");
        System.out.println(sep3+header+sep3);
        System.out.println(tabHeader);
        for(Map.Entry<String, ProfilerCategory> entry: profilerCategoryMap.entrySet()){
            System.out.println(
                    "| "+fillRight(entry.getKey(), targetW)+
                            "| "+fillRight(entry.getValue().getAvg()+"", 10)+" ms | ["
                            +entry.getValue().getCount()+"]");
        }

    }
}
