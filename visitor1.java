/*
* Konstantinos Andrinopoulos 3190009
* Giorgos Kouros 3190095
* Konstantinos Katsamis 3190237
*/

import java.util.*;
import utils.*;
import java.lang.String;
import static java.lang.System.out;
import minipython.analysis.DepthFirstAdapter;
import utils.FunctionData;
import utils.Types;
import minipython.node.*;

/**
 * Visitor1, saves function's and variables's data
 * Prints errors about wrong function declaretions
 * Every inXXX function is used when we got in a situation(declaretion of function, call of function)
 * Every outXXX function is used when we got out from a situation
 */

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
    
    // new function(in a function) => save its data temporarly
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

    // exit from the new function(out a function) => save its and checks for problems(e.g. if is already decleared)
    @Override
    public void outAFunction(AFunction node){
        if(functions.containsKey(currentFunc.getName())){
            for(FunctionData f: functions.get(currentFunc.getName())){
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
            out.println("EXCEPTION in outAFunction");
        }
    }

    // new function call(in a FunctionCall) => save its data temporarly
    @Override
    public void inAFunctionCall(AFunctionCall node){
        curFunc = new FunctionCalls(node.getIdentifier().toString());
    }

    // exit from the function call(out a FunctionCall) => save its and checks for problems
    @Override
    public void outAFunctionCall(AFunctionCall node){
        FunctionData f = getFunctionData(node, false);
        if(f != null){
            Types type = getExpressionType(f.getReturnExpression());
            f.setType(getExpressionType(f.getReturnExpression()));
            Hashtable<String, Types> tempArgs = f.getArgumHashtable();

            for (Map.Entry<String, Types> entry : tempArgs.entrySet()) {
                String key = entry.getKey();
                Types value = entry.getValue();
                variables.replace(key, type);
            }
            updateFunctionsVariables();
        }
    }

    // function that returns the type of expression
    private Types getExpressionType(PExpression node)
    {
        if (node instanceof AValueExpression){
            PValue value = ((AValueExpression) node).getValue();
            return getValueType(value);
        } else if (node instanceof AFuncCallExpression){
            FunctionData f = getFunctionData(((AFunctionCall) ((AFuncCallExpression) node).getFunctionCall()), false);
            return f == null ? Types.NUMERIC : f.getType();
        }
        else{
            return Types.NUMERIC;
        }
    }

    // function for error
    private void notDefined(int line, String type, String name){
        System.err.println("Error: Line " + line + ": " + type + ' ' + name + "is not defined");
    }

    // function that's returns the data of a given function node
    private FunctionData getFunctionData(AFunctionCall node, boolean print){
        if(!functions.containsKey(node.getIdentifier().toString())){
            if(print){
                notDefined(node.getIdentifier().getLine(), "Function", node.getIdentifier().toString());
            }
        }
        else{
            for(FunctionData f : functions.get(node.getIdentifier().toString())){
                if(f.arguments.size() >= curFunc.args.size() && f.getArguments() <= curFunc.args.size()){
                    f.setType(getExpressionType(f.getReturnExpression()));
                    return f;
                }
            }
            System.err.println("Error: Line " + node.getIdentifier().getLine() + ": Arguments for function " + curFunc.name + " do not match any overload");
        }
        return null;
    }

    // function for error
    private void alreadyDefinedFunction(int line, String name){
		name = name.substring(0, name.lastIndexOf(' '));
		System.err.println("Error: Line " + line + ": " + "Function" + ' ' + name + " is already defined!");
	}

    // new return statement(in a Return Statement) => save its data temporarly
    @Override
    public void inAReturnStatement(AReturnStatement node){
        currentFunc.setReturnExpression(node.getExpression());        
    }

    // function for get the type of a value
    public Types getValueType(PValue value){
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

    // function which updates the variables of a function
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

}
