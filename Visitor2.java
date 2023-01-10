import java.util.Hashtable;

import org.sablecc.sablecc.node.TId;

import utils.*;
import minipython.node.*;

import java.util.*;

import minipython.analysis.DepthFirstAdapter;
import minipython.node.AFunction;
import utils.FunctionData;
import utils.Types;

public class Visitor2 extends DepthFirstAdapter{
    private Hashtable<String, LinkedList<FunctionData>> functions;
    private Hashtable<String, FunctionCalls> functionCalls;
    private Hashtable<String, Types> variables;

    private FunctionCalls curFunc = null;

    private boolean inPrint = false;
    private boolean inReturn = false;

    private int curLine = 0;

    public Visitor2(Hashtable<String, LinkedList<FunctionData>> functions, Hashtable<String, Types> variables, Hashtable<String, FunctionCalls> functionCalls)
    {
        this.functions = functions;
        this.variables = variables;
        this.functionCalls = functionCalls;
    }

    // we found a use of an array, so we check if it has been decleared @TODO
    /*@Override
    public void inAListDefExpression(AListDefExpression node){
        String vName = node.toString();
        int line = node.getLine();
        checkVariableDefinition(node.get, true);
        // node.toString();
        // node.getLine();
    }*/

    // we just found a use of a identifier so we check if it has been decleared
    @Override
    public void inAIdentifierExpression(AIdentifierExpression node){
        checkVariableDefinition(node.getIdentifier(), true);
    }

    // declear a new variable. we need to keep track of what value type ti is
    @Override
    public void inAEqStatement(AEqStatement node){
        String vName = node.getIdentifier().toString();
        variables.put(vName, getExpressionType(node.getExpression()));
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
            System.err.println("Line " + node.getIdentifier().getLine() + ": Arguments for function " + curFunc.name + " do not match any overload");
        }
        return null;
    }

    private Types getValueType(PValue value)
    {
        if(value instanceof AStrngLtrlValue)
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

    private void notDefined(int line, String type, String name)
    {
        System.err.println("Line " + line + ": " + type + ' ' + name + "is not defined");
    }



}
