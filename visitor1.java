

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
        Hashtable<String, Types> temp_args = new Hashtable<>();
        LinkedList<AIdentifierValue> idefs = node.getIdentifierValue();
        for(AIdentifierValue i: idefs){
            temp_args.put(i.getIdentifier().toString(), getValueType(i.getValue()));
        }

        /*for (Map.Entry<String, Types> entry : temp_args.entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue());
        }*/
        
        currentFunc = new FunctionData(node.getIdentifier().toString()); //new FunctionData(node.getId().toString());
        currentFunc.arguments = temp_args;                
    }

    // exit the function declaretion. collect all required info and check for fuplicates
    @Override
    public void outAFunction(AFunction node){
        // if other function exist with the same name
        if(functions.containsKey(currentFunc.getName())){
            // get all functions with same name
            for(FunctionData f: functions.get(currentFunc.getName())){
                // and check if there is a function with same arguments
                if((f.getArguments() == currentFunc.getArguments()) || (f.getArgumentsSize() == currentFunc.getArgumentsSize())){
                    alreadyDefinedFunction(((TIdentifier) node.getIdentifier()).getLine(), currentFunc.getName());
                    return;
                }
            }
            functions.get(currentFunc.getName()).add(currentFunc);
        }
        LinkedList<FunctionData> temp = new LinkedList<>();
        temp.add(currentFunc);
        functions.put(currentFunc.getName(), temp);
        printFunctionsData();
    }

    private void alreadyDefinedFunction(int line, String name)
	{
		name = name.substring(0, name.lastIndexOf(' '));
		System.err.println("Line " + line + ": " + "Function" + ' ' + name + " is already defined");
	}

    // keep the expression of the return statement
    @Override
    public void inAReturnStatement(AReturnStatement node){
        currentFunc.setReturnExpression(node.getExpression());
    }


    // parameter of the current function with defult value
    /* @Override
    public void inAArgument(AArgument node){
        out.println(node.getId2().toString());
        currentFunc.arguments.put(node.getId1().toString(), Types.NUMERIC);
        variables.put(node.getId2().toString(), Types.NUMERIC);
        // currentFunc.printArguments();
        
    }*/

    /*@Override
    public void outAArgument(AArgument node){
        //System.out.println(node.getCommaIdAssignValue());
        out.println(node.getId2().toString());
        
    }*/
    
    /*
    @Override
    public void inADefaultArgument(AArgument node){
        // 
        currentFunc.arguments.put(node.getId1().toString(), Types.NUMERIC);
        variables.put(node.getId1().toString(), Types.NUMERIC);
    }
     */

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
                // out.println("Function name: " + funcData.getName());
                // out.println("Arguments:");
                for (Map.Entry<String, Types> entry : funcData.arguments.entrySet()) {
                    // out.println("    " + entry.getKey() + ": " + entry.getValue());
                }
                // out.println("Return type: " + funcData.getType());
                // out.println("Return expression: " + funcData.getReturnExpression());
            }
            // out.println();
        }
    }

}
