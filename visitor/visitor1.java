package visitor;

import java.util.*;

import org.sablecc.sablecc.node.TId;

import minipython.analysis.DepthFirstAdapter;
import minipython.node.AFunction;
import utils.FunctionData;
import utils.Types;
import minipython.node.*;

public class visitor1 extends DepthFirstAdapter{
    private Hashtable<String, LinkedList<FunctionData>> functions;
    private Hashtable<String, Types> variables;
    private FunctionData currentFunc;

    public visitor1(Hashtable<String, LinkedList<FunctionData>> functions, Hashtable<String, Types> variables){
        this.functions = functions;
        this.variables = variables;
    }
    
    // we got in a new function, so we create a new FunctionData object
    /* (non-Javadoc)
     * @see minipython.analysis.DepthFirstAdapter#inAFunction(minipython.node.AFunction)
     */
    @Override
    public void inAFunction(AFunction node){
        currentFunc = new FunctionData(node.getIdentifier().toString()); //new FunctionData(node.getId().toString());
    }

    

}
