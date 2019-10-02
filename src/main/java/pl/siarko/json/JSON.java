package pl.siarko.json;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Set;

public class JSON {

    JSONObject json;

    //exists a.b.c.1234.s
    //get a.b.c.1
    //put a.b.c 234

    //a.b.c.a
    //a.1.2.3
    //a.1.b.1.c
    //a.1.2.3.2.1.a
    //a.1.2.3.2


    public JSON(){
        this.json = new JSONObject();
    }

    public JSON(String string){
        this.json = new JSONObject(string);
    }

    private boolean isInt(String s){
        return s.matches("-?(0|[1-9]\\d*)");
    }

    public boolean exists(String path){
        try {
            this.get(path);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public boolean getBoolean(String path){
        try {
            Object object = this.get(path);
            if (!object.equals(Boolean.FALSE) && (!(object instanceof String) || !((String)object).equalsIgnoreCase("false"))) {
                if (!object.equals(Boolean.TRUE) && (!(object instanceof String) || !((String)object).equalsIgnoreCase("true"))) {
                    throw new JSONException("Object in "+path+" is not a Boolean.");
                } else {
                    return true;
                }
            } else {
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public String getString(String path){
        try {
            return (String)this.get(path);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public float getFloat(String path){
        return this.getNumber(path).floatValue();
    }

    public Number getNumber(String path){
        try {
            Object object = this.get(path);
            return (Number)object;
        } catch (Exception var4) {
            var4.printStackTrace();
        }
        return null;
    }

    public Object get(String path) throws Exception {
        String[] elements = path.split("\\.");
        JSONObject currentObj = json;
        JSONArray currentArr = null;

        for(int i = 0; i < elements.length; i++){
            String part = elements[i];
            if(isInt(part)){
                //jest intem
                if(i == 0){ throw new MalformedPathException("First element in path cannot be numeric!");}
                int index = Integer.parseInt(part);
                if(currentArr != null && currentArr.length() > index){
                    if(i == elements.length-1){
                        return currentArr.get(index);
                    }
                    if(currentArr.get(index) instanceof JSONObject) {
                        currentObj = (JSONObject) currentArr.get(index);
                        currentArr = null;
                    }else if(currentArr.get(index) instanceof JSONArray){
                        currentArr = (JSONArray) currentArr.get(index);
                    }else{
                        throw new MalformedPathException("Malformed path: "+path+" index: "+i);
                    }
                }else{
                    throw new MalformedPathException("Malformed path or index out of bounds ["+index+"]");
                }
            }else{
                //nie jest intem
                if(currentObj != null && currentObj.has(part)){
                    if(i == elements.length-1){
                        return currentObj.get(part);
                    }
                    if(currentObj.get(part) instanceof JSONObject) {
                        currentObj = (JSONObject) currentObj.get(part);
                    }else if(currentObj.get(part) instanceof JSONArray){
                        currentArr = (JSONArray) currentObj.get(part);
                        currentObj = null;
                    }else{
                        throw new MalformedPathException("Malformed path: "+path+" index: "+i);
                    }
                }else{
                    throw new MalformedPathException("Malformed path: "+path+" ["+part+"]");
                }
            }
        }
        throw new Exception("This error should have never happened...");
    }

    //a     - ok
    //a.b
    //a.b.c
    //a.0
    //a.1
    //a.0.a
    //a.0.0

    public void put(String path, Object o, boolean throwErrors){
        try{
            this.put(path, o);
        }catch (MalformedPathException e) {
            e.printStackTrace();
        }
    }

    public void put(String path, Object o) throws MalformedPathException {
        int p = path.lastIndexOf('.');
        if(p == -1){
            this.json.put(path, o);
        }else{
            //System.out.println(path);
            String[] pathParts = path.split("\\.");
            String name = path.substring(p+1);
            Object target = json;

            for(int i = 0; i < pathParts.length-1; i++){
                String part = pathParts[i];
                if(isInt(part) || part.equals("#")){
                    if(part.equals("#")){ //put new at the end
                        if(target instanceof JSONArray){
                            Object next = (
                                    isInt(pathParts[i+1]) || pathParts[i+1].equals("#") ?
                                            new JSONArray() : new JSONObject()
                            );
                            ((JSONArray) target).put(next);
                            target = next;
                        }else{
                            throw new MalformedPathException("Trying to add new index to "+target.getClass());
                        }
                    }else{
                        int index = Integer.parseInt(part);
                        if(target instanceof JSONArray){
                            if(((JSONArray) target).length() > index){
                                target = ((JSONArray)target).get(index);
                            }else{
                                throw new MalformedPathException("Trying to access non existing index: "+index);
                            }
                        }
                    }
                }else{
                    if(target instanceof JSONObject){
                        if(!((JSONObject) target).has(part)){
                            Object cont = new JSONObject();
                            if(isInt(pathParts[i+1]) || pathParts[i+1].equals("#")){ cont = new JSONArray();}
                            ((JSONObject) target).put(part, cont);
                        }
                        target = ((JSONObject) target).get(part);
                    }else if(target instanceof JSONArray){
                        throw new MalformedPathException("Trying to access JSONArray as object: "+part);
                    }else{
                        throw new MalformedPathException(
                                "Trying to access "+target.getClass()+" as " + "JSONObject: "+part
                        );
                    }
                }
                //System.out.println(part+" -> "+target);

            }
            if(name.equals("#")){
                ((JSONArray)target).put(o);
            }else if(isInt(name)){
                ((JSONArray)target).put(Integer.parseInt(name), o);
            }else{
                ((JSONObject)target).put(name, o);
            }



            //System.out.println(this.json);
            //System.out.println("==============");


        }
    }

    public String prettyPrint(){
        return "JSONObject "+this.prettyPrint(this.json, 0);
    }

    private String prettyPrint(Object o, int indent){
        StringBuilder result;
        String baseIndent = "";
        if(indent > 0){
            baseIndent = new String(new char[indent]).replace('\0',' ');
        }
        String subIndent = new String(new char[indent+2]).replace('\0',' ');
        if(o instanceof JSONObject){
            result = new StringBuilder("{\n");
            int i = 0;
            Set<String> keys =((JSONObject) o).keySet();
            for(String key: keys){
                Object o1 = ((JSONObject) o).get(key);
                result.append(subIndent).append(key).append(": ")
                        .append(this.prettyPrint(o1, indent+2));
                if(i < keys.size()-1){
                    result.append(",");
                }
                result.append("\n");
            }
            result.append(baseIndent).append("}");
        }else if(o instanceof JSONArray){
            result = new StringBuilder("[\n");
            for(int i = 0; i < ((JSONArray) o).length(); i++){
                Object o1 = ((JSONArray) o).get(i);
                result.append(subIndent).append(this.prettyPrint(o1, indent+2));
                if(i < ((JSONArray) o).length()-1){
                    result.append(",");
                }
                result.append("\n");
            }
            result.append(baseIndent).append("]");
        }else{
            if(o instanceof Boolean || o instanceof Number){
                return o.toString();
            }
            return "\""+o.toString()+"\"";
        }

        return result.toString();
    }


    public String toString(){
        return this.json.toString();
    }

    public String toString(int indent){
        return this.json.toString(indent);
    }

    public JSONObject rawObject() {
        return this.json;
    }
}
