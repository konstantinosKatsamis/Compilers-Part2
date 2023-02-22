/*
* Konstantinos Andrinopoulos 3190009
* Giorgos Kouros 3190095
* Konstantinos Katsamis 3190237
*/

import java.util.Hashtable;

import utils.*;
import minipython.node.*;
import static java.lang.System.out;
import java.util.*;

import minipython.analysis.DepthFirstAdapter;
import utils.FunctionData;
import utils.Types;

/*
 * Visitor1, takes saved data from Visitor1 and:
 * Saves data and checks function calls, declaretions variables, arithmetic operations
 */

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

    // new use of identifier(in a Identifier) => check for correct definition
    @Override
    public void inAIdentifierExpression(AIdentifierExpression node){
        checkVariableDefinition(node.getIdentifier(), true);
    }

    // new use of assign statement(in a Assign Statement) => save its data temporarly
    @Override
    public void inAAssignStatement(AAssignStatement node){
        String vName = node.getIdentifier().toString();
        variables.put(vName, getExpressionType(node.getExpression()));
    }

    // new use of function call statement(in a FunctionCall) => save its data temporarly
    @Override
    public void inAFunctionCall(AFunctionCall node){
        curFunc = new FunctionCalls(node.getIdentifier().toString());
    }

    // exit from the new function call(out a FunctionCall) => save its and checks for problems(e.g. if is already decleared,
    // number and type of arguments)
    @Override
    public void outAFunctionCall(AFunctionCall node){
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

            // print error if the given arg has wrong type for each argument of a function call
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
        
            String [] fTypes = f.arguments.keySet().toArray(new String[f.arguments.size()]);
            for(int i=0; i<curFunc.args.size(); i++){
                variables.put(fTypes[i], curFunc.args.get(i));
            }
            f.setType(getExpressionType(f.getReturnExpression()));
        }
    }

    // exit from arithmetic expression(out a ArithmeticExpression) => check if the expression is correct
    @Override
    public void outAArithmeticExpression(AArithmeticExpression node){
        PExpression a = node.getExpr1();
        PExpression b = node.getExpr2();
        getOperationType(a, b);
    }

    // check and update variables' type by
    private void getOperationType(PExpression a, PExpression b){
        if (!inReturn){
            Types at, bt;
            if (variables.containsKey(a.toString())){
                at = variables.get(a.toString());
            } else{
                at = getExpressionType(a);
            }

            if (variables.containsKey(b.toString())){
                bt = variables.get(b.toString());
            } else{
                bt = getExpressionType(b);
            }

            if ((at == Types.NUMERIC && bt == Types.NUMERIC) || (at == Types.NAN && bt == Types.NAN)){
                variables.put(a.parent().toString(), at);
            } else{
                System.err.println(String.format("Line %d: Numeric operations cannot be performed between %s type and %s type", curLine, at.toString(), bt.toString()));
                variables.put(a.parent().toString(), Types.NUMERIC);
            }
        }
    }

    // new use of return statement(in a ReturnStatement) => set true the temporary variable
    @Override
    public void inAReturnStatement(AReturnStatement node){
        inReturn = true;
    }

    // exit from return statement(out a ReturnStatement) => set false the temporary variable
    @Override
    public void outAReturnStatement(AReturnStatement node){
        inReturn = false;
    }

    // returns the expression type of a PExpression node
    private Types getExpressionType(PExpression node){
        if (node instanceof AValueExpression){
            PValue value = ((AValueExpression) node).getValue();
            return getValueType(value);
        } else if (node instanceof AFuncCallExpression){
            FunctionData f = getFunctionData(((AFunctionCall) ((AFuncCallExpression) node).getFunctionCall()), false);
            return f == null ? Types.NUMERIC : f.getType();
        }
        else {
            return Types.NUMERIC;
        }
    }

    // exit from the function call(out a FunctionCall) => save its and checks for problems
    private FunctionData getFunctionData(AFunctionCall node, boolean print){
        if(!functions.containsKey(node.getIdentifier().toString())){
            if(print){
                notDefined(node.getIdentifier().getLine(), "Function", node.getIdentifier().toString());
            }
        }
        else {
            for(FunctionData f : functions.get(node.getIdentifier().toString())){
                //If they have the same parameters then we are calling this one so return it
                if(f.arguments.size() >= curFunc.args.size() && f.getArguments() <= curFunc.args.size()){
                    f.setType(getExpressionType(f.getReturnExpression()));
                    return f;
                }
            }
            System.err.println("Error: Line " + node.getIdentifier().getLine() + ": Arguments for function " + curFunc.name + " do not match any overload");
        }
        return null;
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
        else{
            return Types.NUMERIC;
        }
    }

    // check of variables's definition
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

        if (!variables.containsKey(vName)){
            if(print){
                int line = node.getLine();
                notDefined(line, "Variable", vName);
            }
            return false;
        }
        
        return true;
    }

    // function for error
    private void functionCallErrorType(String line, String funcName, String correctType, String wrongType){
        System.err.println("Error: Line " + line + ": Function " + funcName + "takes " + correctType + " arguments. No " + wrongType);
    }
    
    // function for error
    private void functionCallErrorArgsNumber(int bit, String line, String funcName, int originalArgs, int givenArgs){
        if(bit == 0){
            System.err.println("Error: Line " + line + ": Function " + funcName + "takes " + originalArgs + " arguments. No " + givenArgs);
        } else{
            System.err.println("Error: Line " + line + ": Function " + funcName + "takes at least " + originalArgs + " arguments. No " + givenArgs);
        }
    }

    // function for error
    private void notDefined(int line, String type, String name){
        System.err.println("Error: Line " + line + ": " + type + ' ' + name + "is not defined");
    }

}
