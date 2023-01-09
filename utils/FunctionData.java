package utils;

import java.util.Hashtable;

import minipython.node.PExpression;

public class FunctionData {
    public String name;
    public Hashtable<String, Types> arguments;
    public Types type;
    public PExpression returnExpression = null;

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
}
