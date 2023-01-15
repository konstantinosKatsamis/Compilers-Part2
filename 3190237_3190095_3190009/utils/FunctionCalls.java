package utils;

import java.util.LinkedList;

public class FunctionCalls
{
    public LinkedList<Types> args;
    public String name;

    public FunctionCalls(String name)
    {
        this.name = name;
        this.args = new LinkedList<>();
    }

    // temp del functions
    public void printArguments(){
        for(Types t: args){
            System.out.println("\t" + t.toString());
            
        }
    }

    public int getCalls(){
        return args.size();
    }
}
