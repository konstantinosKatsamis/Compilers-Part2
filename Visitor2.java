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
    // private boolean inPrint = false; del
    private boolean inReturn = false;
    private int curLine = 0;

    public Visitor2(Hashtable<String, LinkedList<FunctionData>> functions, Hashtable<String, Types> vars, Hashtable<String, FunctionCalls> functionCalls)
    {
        this.functions = functions;
        this.variables = vars;
        this.functionCalls = functionCalls;
        // printFunctionsData();
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

    // save function's argument model.Types
    @Override
    public void outAArglistArglist(AArglistArglist node){
        // if current function != null we are ok to put the arguments in the correct fynction
        if (curFunc != null){
            String fName = curFunc.name;
        }
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
            int line = tIdentline.getLine();  // out.println(f.getName()); // kserw oti ekalesen th sosth methodo
            Types returnType = f.getType();   // out.println(f.getType()); // kserw oti eshei sosto tipo epistrofis
            // for each argument of a function call
            Object [] strArr = node.getExpression().toArray();
            for(Object s: strArr){
                String he = s.toString();
                he = he.trim();
                if(variables.containsKey(s.toString())){
                    Types valuehe = variables.get(s.toString());
                    if(returnType != valuehe){
                        functionCallError( Integer.toString(line), f.getName(), returnType.toString(), valuehe.toString());
                    }
                }
                // check a number argument
                if(he.matches("[-+]?[0-9]*\\.?[0-9]+") && returnType == Types.STRING){
                    functionCallError( Integer.toString(line), f.getName(), returnType.toString(), "NUMERIC");
                }
                // check a String argument
                // ??
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

    // save functions's argument utils.Types
    @Override
    public void inAArgument(AArgument node){
        if(curFunc != null){
            //curFunc.args.add(getExpressionType(node.getId2().toString()));
        }
    }

    // for every arithmetic like +, -, *, /, **
    @Override
    public void outAArithmeticExpression(AArithmeticExpression node){
        PExpression a = node.getE1();
        PExpression b = node.getE2();
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
        } /*else if (node instanceof AIdentifierExpression)
        {
            TIdentifier id = ((AIdentifierExpression) node.getIdentifier();
            if (!checkVariableDefinition(id, false))
            {
                return Types.NAN;
            }
            return variables.get(id.toString());
        } else if (node instanceof AAssignListStatement)
        {
            TId id = ((AAssignListStatement) node).getId();
            if (!checkVariableDefinition(id, false))
            {
                return Types.NAN;
            }
            return variables.get(id.toString());
        }*/
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
            System.err.println("EEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEE1rror: Line " + node.getIdentifier().getLine() + ": Arguments for function " + curFunc.name + " do not match any overload");
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

    private void functionCallError(String line, String funcName, String correctType, String wrongType){
        System.err.println("Error: Line " + line + ": Function " + funcName + "takes " + correctType + " arguments. No " + wrongType);
    }

    private void notDefined(int line, String type, String name)
    {
        System.err.println("EEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEE2rror: Line " + line + ": " + type + ' ' + name + "is not defined");
    }

    // Temp del functions
    public void printFunctionsData(){
        out.println("\nFunctions decleared in the program:");
        for (String funcName : functions.keySet()) {
            LinkedList<FunctionData> funcList = functions.get(funcName);
            for (FunctionData funcData : funcList) {
                out.println("Function name: " + funcData.getName());
                out.println("Arguments - " + funcData.arguments.size() + " | Default args - " + funcData.getDefaultArguments() + " | Non Default args - " + funcData.getNonDefaultArguments());
                for (Map.Entry<String, Types> entry : funcData.arguments.entrySet()) {
                    out.println("    " + entry.getKey() + ": " + entry.getValue());
                }
                out.println("Return type: " + funcData.getType());
                out.println("Return expression: " + funcData.getReturnExpression());
            }
            out.println();
        }
        
    }

    public void printFunctionCalls(){
        out.println("\nCalls of functions in the program:");
        for (String funcName : functionCalls.keySet()) {
            FunctionCalls func = functionCalls.get(funcName);
            out.println("Function name: " + funcName);
            out.println("Number of calls: " + func.getCalls());
            out.println("Call locations:");
            func.printArguments();
            /*for (String callLocation : func.getCallLocations()) {
                System.out.println("    " + callLocation);
            }*/
        }
    }

    public void printAllVariables(){
        out.println("\nPrinting all Variables:");
        for (Map.Entry<String, Types> entry : variables.entrySet()) {
            out.println("Key:" + entry.getKey() + ",Value:" + entry.getValue());
        }
        
    }

}
