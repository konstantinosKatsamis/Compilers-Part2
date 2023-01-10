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
    private Hashtable<String, Types> operations;

    private FunctionCalls curFunc = null;

    private boolean inPrint = false;
    private boolean inReturn = false;

    private int curLine = 0;

    public Visitor2(Hashtable<String, LinkedList<FunctionData>> functions, Hashtable<String, Types> variables, Hashtable<String, FunctionCalls> functionCalls)
    {
        this.functions = functions;
        this.operations = variables;
        this.functionCalls = functionCalls;
    }

    // we found a use of an array, so we check if it has been decleared
    @Override
    public void inAListDefExpression(AListDefExpression node){
        checkVariableDefinition(()node.getE1(), true);
        TIdentifier temp = node.getTIdentifier();
    }

    private boolean checkVariableDefinition(TIdentifier node, boolean print)
    {
        String vName = node.toString();
        if (!operations.containsKey(vName))
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
