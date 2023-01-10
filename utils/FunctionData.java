package utils;

import java.util.Hashtable;

import minipython.node.PExpression;

public class FunctionData {
    private String name;
    public Hashtable<String, Types> arguments;
    private Types type;
    private PExpression returnExpression = null;

    public FunctionData(String name){
        this.name = name;
        this.arguments = new Hashtable<>();
        this.type = Types.VOID;
    }

    public int getArguments(){
        int n = 0;
        for(Types t: arguments.values()){
            if(t == Types.NAN){
                n++;
            }
        }
        return n;
    }

    public int getArgumentsSize(){
        return arguments.size();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Types getType() {
        return type;
    }

    public void setType(Types type) {
        this.type = type;
    }

    public PExpression getReturnExpression() {
        return returnExpression;
    }

    public void setReturnExpression(PExpression returnExpression) {
        this.returnExpression = returnExpression;
    }

}
