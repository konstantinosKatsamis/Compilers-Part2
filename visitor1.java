

import java.util.*;
import java.lang.String;
import static java.lang.System.out;

import minipython.analysis.DepthFirstAdapter;
import minipython.node.AFunction;
import utils.FunctionData;
import utils.Types;
import minipython.node.*;

public class Visitor1 extends DepthFirstAdapter{
    private Hashtable<String, LinkedList<FunctionData>> functions;
    private Hashtable<String, Types> variables;
    private FunctionData currentFunc;

    public Visitor1(Hashtable<String, LinkedList<FunctionData>> functions, Hashtable<String, Types> variables){
        this.functions = functions;
        this.variables = variables;
    }
    
    // we got in a new function, so we create a new FunctionData object
    @Override
    public void inAFunction(AFunction node){
        // out.println(" ====== in A Function =======");
        Hashtable<String, Types> temp_args = new Hashtable<>();
        LinkedList<AIdentifierValue> idefs = node.getIdentifierValue();
        int def_args = 0, non_def_args = 0;
        for(AIdentifierValue i: idefs){
            if(getValueType(i.getValue()) == Types.NULL){
                non_def_args++;
            } else{
                def_args++;
            }
            temp_args.put(i.getIdentifier().toString(), getValueType(i.getValue()));
        }
        // out.println("Default args: " + def_args + "\nNON Def args: " + non_def_args);
        /*for (Map.Entry<String, Types> entry : temp_args.entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue());
        }*/
        
        currentFunc = new FunctionData(node.getIdentifier().toString()); //new FunctionData(node.getId().toString());
        currentFunc.arguments = temp_args; 
        currentFunc.setDefaultArguments(def_args);               
        currentFunc.setNonDefaultArguments(non_def_args);
    }

    // exit the function declaretion. collect all required info and check for fuplicates
    @Override
    public void outAFunction(AFunction node){
        // if other function exist with the same name
        if(functions.containsKey(currentFunc.getName())){
            // get all functions with same name
            for(FunctionData f: functions.get(currentFunc.getName())){
                // and check if there is a function with same arguments
                if((f.getArgumentsSize() == currentFunc.getArgumentsSize())){ // katastrofikos elegxos: f.getArguments() == currentFunc.getArguments())
                    // out.println(" =-==-=-==-=-=-=-=-=-=-==-=-=-=-=-=-=-==-=-=- alreadyDefinedFunction from check 1");
                    // out.println(" ------------ ----------- ---------- f.getArguments(): " + f.getArguments());
                    // out.println(" ------------ ----------- ---------- currentFunc.getArguments(): " + currentFunc.getArguments());
                    // out.println(" ------------ ----------- ---------- f.getArgumentsSize(): " + f.getArgumentsSize());
                    // out.println(" ------------ ----------- ---------- currentFunc.getArgumentsSize(): " + currentFunc.getArgumentsSize());
                    alreadyDefinedFunction(((TIdentifier) node.getIdentifier()).getLine(), currentFunc.getName());
                    return;
                }
                if(f.getNonDefaultArguments() == currentFunc.getNonDefaultArguments()){
                    // out.println(" =-==-=-==-=-=-=-=-=-=-==-=-=-=-=-=-=-==-=-=- alreadyDefinedFunction from check 2");
                    // out.println(" ------------ ----------- ---------- f.getNonDefaultArguments(): " + f.getNonDefaultArguments());
                    // out.println(" ------------ ----------- ---------- currentFunc.getNonDefaultArguments(): " + currentFunc.getNonDefaultArguments());
                    
                    // out.println(" ------------ ----------- ---------- f.getArguments(): " + f.getArguments());
                    // out.println(" ------------ ----------- ---------- currentFunc.getArguments(): " + currentFunc.getArguments());
                    // out.println(" ------------ ----------- ---------- f.getArgumentsSize(): " + f.getArgumentsSize());
                    // out.println(" ------------ ----------- ---------- currentFunc.getArgumentsSize(): " + currentFunc.getArgumentsSize());
                    alreadyDefinedFunction(((TIdentifier) node.getIdentifier()).getLine(), currentFunc.getName());
                    return;
                }
                if(f.getArgumentsSize() == currentFunc.getNonDefaultArguments()){
                    alreadyDefinedFunction(((TIdentifier) node.getIdentifier()).getLine(), currentFunc.getName());
                    return;
                }
            }
            functions.get(currentFunc.getName()).add(currentFunc);
        }
        LinkedList<FunctionData> temp = new LinkedList<>();
        temp.add(currentFunc);
        functions.put(currentFunc.getName(), temp);
    }

    private void alreadyDefinedFunction(int line, String name)
	{
		name = name.substring(0, name.lastIndexOf(' '));
		System.err.println("Line " + line + ": " + "Function" + ' ' + name + " is already defined!");
	}

    // keep the expression of the return statement
    @Override
    public void inAReturnStatement(AReturnStatement node){
        currentFunc.setReturnExpression(node.getExpression());
        currentFunc.setReturnExpression(node.getExpression());
    }

    @Override
    public void inAArgument(AArgument node){
        // LinkedList<> lls =  node.getId2();
        // out.println("eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee" + node.getId1().toString());
        // out.println("eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee" + node.getId2().toString());
    }

    public Types getValueType(PValue value)
    {
        if(value instanceof AStringValue){
            return Types.STRING;
        }
        else if (value instanceof ANoneValue){
            return Types.NULL;
        }
        else if(value instanceof AMethodValue){
            String retFunc = ((AMethodValue) value).getIdentifier().toString();
            return variables.get(retFunc);
        }
        else if(value instanceof ANumberValue){
            return Types.NUMERIC;
        }
        else{
            return Types.NULL;
        }
    }

    // temp - del functions
    public void printFunctionsData(){
        for (String funcName : functions.keySet()) {
            LinkedList<FunctionData> funcList = functions.get(funcName);
            for (FunctionData funcData : funcList) {
                out.println("Function name: " + funcData.getName());
                out.println("Arguments:");
                for (Map.Entry<String, Types> entry : funcData.arguments.entrySet()) {
                    out.println("    " + entry.getKey() + ": " + entry.getValue());
                }
                out.println("Return type: " + funcData.getType());
                out.println("Return expression: " + funcData.getReturnExpression());
            }
            out.println();
        }
    }

}
