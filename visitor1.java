

import java.util.*;

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
        currentFunc = new FunctionData(node.getIdentifier().toString()); //new FunctionData(node.getId().toString());
                
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

// TODO - prepei na mpoun 2 inXXArgument(default value kai without deafult value)
// isos na allaksoume to grammar file, k na mpoun rita entoles gia default timi kai
// gia kanoniki timi
    // parameter of the current function with defult value
    @Override
    public void inAArgument(AArgument node){
        // 
        currentFunc.arguments.put(node.getId1().toString(), Types.NUMERIC);
        variables.put(node.getId1().toString(), Types.NUMERIC);
    }

    /*
    @Override
    public void inADefaultArgument(AArgument node){
        // 
        currentFunc.arguments.put(node.getId1().toString(), Types.NUMERIC);
        variables.put(node.getId1().toString(), Types.NUMERIC);
    }
     */
    

}
