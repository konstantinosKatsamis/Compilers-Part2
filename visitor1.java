

import java.util.*;


import utils.*;
import java.lang.String;
import static java.lang.System.out;

import minipython.analysis.DepthFirstAdapter;
import utils.FunctionData;
import utils.Types;
import minipython.node.*;

public class Visitor1 extends DepthFirstAdapter{
    private Hashtable<String, LinkedList<FunctionData>> functions;
    private Hashtable<String, Types> variables;
    private FunctionData currentFunc;
    private Hashtable<String, Types> tempVars = new Hashtable<>();
    private FunctionCalls curFunc = null;

    public Visitor1(Hashtable<String, LinkedList<FunctionData>> functions, Hashtable<String, Types> variables){
        this.functions = functions;
        this.variables = variables;
    }
    
    // we got in a new function, so we create a new FunctionData object
    @Override
    public void inAFunction(AFunction node){
        
        Hashtable<String, Types> temp_args = new Hashtable<>();
        LinkedList<AIdentifierValue> idefs = node.getIdentifierValue();
        int def_args = 0, non_def_args = 0;
        for(AIdentifierValue i: idefs){
            try{                
                tempVars.put(i.getIdentifier().toString(), getValueType(i.getValue()));
            }catch(Exception e) {
                out.println("EXCEPTION inAFunction");
            }
            if(getValueType(i.getValue()) == Types.NULL){
                non_def_args++;
            } else{
                def_args++;
            }
            temp_args.put(i.getIdentifier().toString(), getValueType(i.getValue()));
        }        
        currentFunc = new FunctionData(node.getIdentifier().toString());
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
                if((f.getArgumentsSize() == currentFunc.getArgumentsSize())){
                    alreadyDefinedFunction(((TIdentifier) node.getIdentifier()).getLine(), currentFunc.getName());
                    return;
                }
                if(f.getNonDefaultArguments() == currentFunc.getNonDefaultArguments()){
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
        try{
            variables.putAll(tempVars);
            tempVars.clear();
        }catch(Exception e){
            out.println("EXCEPTION outAFunction");
        }
    }

    // we found a new FunctionCall so we create a new functionData
    @Override
    public void inAFunctionCall(AFunctionCall node){
        curFunc = new FunctionCalls(node.getIdentifier().toString());
    }

    @Override
    public void outAFunctionCall(AFunctionCall node){
        FunctionData f = getFunctionData(node, false);
        if(f != null){
            // get its type
            Types type = getExpressionType(f.getReturnExpression());
            // out.println("Type of function: " + type);
            f.setType(getExpressionType(f.getReturnExpression()));
            Hashtable<String, Types> tempArgs = f.getArgumHashtable();

            // For each parameter see its type equal to the functioncall parameter types
            for (Map.Entry<String, Types> entry : tempArgs.entrySet()) {
                String key = entry.getKey();
                Types value = entry.getValue();
                // out.println("Key: " + key + ", Value: " + value);
                variables.replace(key, type);
            }
            updateFunctionsVariables();
        }
    }

    private Types getExpressionType(PExpression node)
    {
        //Cast node to the appropriate PExpression and then return its type
        if (node instanceof AValueExpression)
        {
            PValue value = ((AValueExpression) node).getValue();
            return getValueType(value);
        } else if (node instanceof AFuncCallExpression)
        {
            FunctionData f = getFunctionData(((AFunctionCall) ((AFuncCallExpression) node).getFunctionCall()), false);
            return f == null ? Types.NUMERIC : f.getType();
        }
        //If its nothing of the above then it must be NUMERIC
        else
        {
            return Types.NUMERIC;
        }
    }

    private void notDefined(int line, String type, String name)
    {
        System.err.println("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA1Error: Line " + line + ": " + type + ' ' + name + "is not defined");
    }

    private FunctionData getFunctionData(AFunctionCall node, boolean print)
    {
        //if we cannot find it then print error
        if(!functions.containsKey(node.getIdentifier().toString()))
        {
            if(print)
            {
                notDefined(node.getIdentifier().getLine(), "Function", node.getIdentifier().toString());
            }
        }
        else
        {
            //Check all function with same name
            for(FunctionData f : functions.get(node.getIdentifier().toString()))
            {
                //If they have the same parameters then we are calling this one so return it
                if(f.arguments.size() >= curFunc.args.size() && f.getArguments() <= curFunc.args.size())
                {
                    f.setType(getExpressionType(f.getReturnExpression()));
                    return f;
                }
            }
            //If nothing found print an error
            System.err.println("Error: Line " + node.getIdentifier().getLine() + ": Arguments for function " + curFunc.name + " do not match any overload");
        }
        return null;
    }

    private void alreadyDefinedFunction(int line, String name)
	{
		name = name.substring(0, name.lastIndexOf(' '));
		System.err.println("Error: Line " + line + ": " + "Function" + ' ' + name + " is already defined!");
	}

    // keep the expression of the return statement
    @Override
    public void inAReturnStatement(AReturnStatement node){
        currentFunc.setReturnExpression(node.getExpression());        
    }

    @Override
    public void outAArgument(AArgument node) {
        out.println("outAArgument");
    }

    @Override
    public void inAArgument(AArgument node){
        out.println("INAArgument");
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

    private void updateFunctionsVariables(){
        for (String funcName : functions.keySet()) {
            LinkedList<FunctionData> funcList = functions.get(funcName);
            for (FunctionData funcData : funcList) {
                for (Map.Entry<String, Types> entry : funcData.arguments.entrySet()) {
                    if(variables.containsKey(entry.getKey())){
                        Types value = variables.get(entry.getKey());
                        funcData.arguments.replace(entry.getKey(), value);
                    }
                }
            }
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

    public void printAllVariables(){
        out.println("\nPrinting all Variables:");
        for (Map.Entry<String, Types> entry : variables.entrySet()) {
            out.println("Key:" + entry.getKey() + ",Value:" + entry.getValue());
        }
    }

}
