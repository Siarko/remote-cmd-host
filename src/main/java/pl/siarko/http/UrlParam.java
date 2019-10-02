package pl.siarko.http;

public class UrlParam {

    private String name, value;

    public UrlParam(String name, String value){
        this.name = name;
        this.value = value;
    }

    public String toString(){
        return String.format(name+"=%s",value);
    }
}
