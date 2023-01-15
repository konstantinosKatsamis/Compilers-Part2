import java.util.Hashtable;

import utils.*;
import minipython.node.*;
import static java.lang.System.out;
import java.util.*;

import minipython.analysis.DepthFirstAdapter;
import utils.FunctionData;
import utils.Types;

public class Visitor2 extends DepthFirstAdapter{
    private Hashtable<String, LinkedList<FunctionData>> functions;
    private Hashtable<String, FunctionCalls> functionCalls;
    private Hashtable<String, Types> variables;

    private FunctionCalls curFunc = null;
    private boolean inReturn = false;
    private int curLine = 0;

    public Visitor2(Hashtable<String, LinkedList<FunctionData>> functions, Hashtable<String, Types> vars, Hashtable<String, FunctionCalls> functionCalls)
    {
        this.functions = functions;
        this.variables = vars;
        this.functionCalls = functionCalls;
    }

    // we just found a use of a identifier so we check if it has been decleared
    @Override
    public void inAIdentifierExpression(AIdentifierExpression node){
        checkVariableDefinition(node.getIdentifier(), true);
    }

    // declear a new variable. we need to keep track of what value type ti is
    @Override
    public void inAAssignStatement(AAssignStatement node){
        String vName = node.getIdentifier().toString();
        variables.put(vName, getExpressionType(node.getExpression()));
    }

    // we declear a new list so we need to keep track of what value type it is
    @Override
    public void inAAssignListStatement(AAssignListStatement node){
        String vName = node.getIdentifier().toString();
        variables.put(vName, getExpressionType(node.getEx1()));
    }

    // save function's argument model.Types temprarly
    @Override
    public void inAArglistArglist(AArglistArglist node){
        if(curFunc != null)
        {
            curFunc.args.add(getExpressionType(node.getExpression()));
        }
    }

    @Override
    public void inATypeExpression(ATypeExpression node) {
    }

    // we found a new FunctionCall so we create a new functionData
    @Override
    public void inAFunctionCall(AFunctionCall node){
        curFunc = new FunctionCalls(node.getIdentifier().toString());
    }

    // we leaving the function call so we need to check if the arguments are correct and get its return type
    @Override
    public void outAFunctionCall(AFunctionCall node){
        // get functionData
        FunctionData f = getFunctionData(node, true);
        if(f != null){
            TIdentifier tIdentline =  node.getIdentifier();
            int line = tIdentline.getLine();
            Types returnType = f.getType();
            
            // print error if the rumber of the args is wrong
            int argsOfFuncCall = node.getExpression().size();
            int argsOfOriginalFunc = f.getArgumentsSize();
            if(argsOfFuncCall > argsOfOriginalFunc){
                functionCallErrorArgsNumber(0, Integer.toString(line), f.getName(), argsOfOriginalFunc, argsOfFuncCall);
            }
            if(argsOfFuncCall < f.getNonDefaultArguments()){
                functionCallErrorArgsNumber(1, Integer.toString(line), f.getName(), f.getNonDefaultArguments(), argsOfFuncCall);
            }

            // print error if the given arg has wrong type
            // for each argument of a function call
            Object [] strArr = node.getExpression().toArray();
            for(Object s: strArr){
                String he = s.toString();
                he = he.trim();
                if(variables.containsKey(s.toString())){
                    Types valuehe = variables.get(s.toString());
                    if(returnType != valuehe){
                        functionCallErrorType(Integer.toString(line), f.getName(), returnType.toString(), valuehe.toString());
                    }
                }
                // check a number argument
                if(he.matches("[-+]?[0-9]*\\.?[0-9]+") && returnType == Types.STRING){
                    functionCallErrorType(Integer.toString(line), f.getName(), returnType.toString(), "NUMERIC");
                }
            }
        
            // make String array of all parameter names of found function that we are calling
            String [] fTypes = f.arguments.keySet().toArray(new String[f.arguments.size()]);
            // For each parameter see its type equal to the functioncall parameter types
            for(int i=0; i<curFunc.args.size(); i++){
                variables.put(fTypes[i], curFunc.args.get(i));
            }
            // get its type
            f.setType(getExpressionType(f.getReturnExpression()));
        }
    }

    // for every arithmetic like +, -, *, /, **
    @Override
    public void outAArithmeticExpression(AArithmeticExpression node){
        PExpression a = node.getExpr1();
        PExpression b = node.getExpr2();
        getOperationType(a, b);
    }

    private void getOperationType(PExpression a, PExpression b)
    {
        //If we are in a return statement ignore the check since we don't know what each variable might be
        if (!inReturn)
        {
            Types at, bt;
            //Get a Type
            if (variables.containsKey(a.toString()))
            {
                at = variables.get(a.toString());
            } else
            {
                at = getExpressionType(a);
            }

            //Get b Type
            if (variables.containsKey(b.toString()))
            {
                bt = variables.get(b.toString());
            } else
            {
                bt = getExpressionType(b);
            }

            //If model.Types are the both Numeric or NAN it's fine
            if ((at == Types.NUMERIC && bt == Types.NUMERIC) || (at == Types.NAN && bt == Types.NAN))
            {
                variables.put(a.parent().toString(), at);
            }
            //Else print error and set it's type as Numeric to avoid further errors
            else
            {
                System.err.println(String.format("Line %d: Numeric operations cannot be performed between %s type and %s type", curLine, at.toString(), bt.toString()));
                variables.put(a.parent().toString(), Types.NUMERIC);
            }
        }
    }

    // when we are in return statement
    @Override
    public void inAReturnStatement(AReturnStatement node){
        inReturn = true;
    }

    @Override
    public void outAReturnStatement(AReturnStatement node)
    {
        inReturn = false;
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

    public Types getValueType(PValue value)
    {
        if(value instanceof AStringValue)
        {
            return Types.STRING;
        }
        else if (value instanceof ANoneValue)
        {
            return Types.NULL;
        }
        else if(value instanceof AMethodValue)
        {
            String retFunc = ((AMethodValue) value).getIdentifier().toString();
            return variables.get(retFunc);
        }
        else
        {
            return Types.NUMERIC;
        }
    }

    private boolean checkVariableDefinition(TIdentifier node, boolean print){
        String vName = node.toString();
        for (String funcName : functions.keySet()) {
            LinkedList<FunctionData> funcList = functions.get(funcName);
            for (FunctionData funcData : funcList) {
                Hashtable<String, Types> funcArgs = funcData.getArgumHashtable();
                for (Map.Entry<String, Types> entry : funcArgs.entrySet()) {
                    String key = entry.getKey();
                    Types value = entry.getValue();
                    if(vName.equals(key)){
                        return false;
                    }
                }
            }
        }

        if (!variables.containsKey(vName))
        {
            if(print)
            {
                int line = node.getLine();
                
                notDefined(line, "Variable", vName);
            }
            return false;
        }
        
        return true;
    }

    private void functionCallErrorType(String line, String funcName, String correctType, String wrongType){
        System.err.println("Error: Line " + line + ": Function " + funcName + "takes " + correctType + " arguments. No " + wrongType);
    }
    
    private void functionCallErrorArgsNumber(int bit, String line, String funcName, int originalArgs, int givenArgs){
        if(bit == 0){
            System.err.println("Error: Line " + line + ": Function " + funcName + "takes " + originalArgs + " arguments. No " + givenArgs);
        } else{
            System.err.println("Error: Line " + line + ": Function " + funcName + "takes at least " + originalArgs + " arguments. No " + givenArgs);
        }
    }

    private void notDefined(int line, String type, String name)
    {
        System.err.println("Error: Line " + line + ": " + type + ' ' + name + "is not defined");
    }

}
