// javac -cp /cs/kvm/choco/samples-2.1.3.jar myHPCScheduling.java
// java -cp $CLASSPATH:/cs/kvm/choco/samples-2.1.3.jar:/opt/sun-jdk-1.6.0.29/lib/tools.jar:. myHPCScheduling > myHPCScheduling.txt

package myHPCProject;

import choco.Choco;
import choco.Options;
import choco.cp.model.CPModel;
import choco.cp.solver.CPSolver;
import choco.cp.solver.search.integer.branching.*;
import choco.cp.solver.search.integer.varselector.*;
import choco.kernel.common.util.tools.ArrayUtils;
import choco.kernel.model.constraints.pack.PackModel;
import choco.kernel.solver.search.ValSelector;
import choco.kernel.model.variables.integer.IntegerVariable;
import choco.kernel.model.variables.integer.IntegerConstantVariable;
import choco.kernel.model.variables.set.SetVariable;
import choco.kernel.model.variables.set.SetConstantVariable;
import samples.tutorials.PatternExample;
import choco.kernel.solver.variables.integer.IntDomainVar;
import choco.kernel.common.logging.ChocoLogging;
import choco.kernel.common.logging.Verbosity;
import choco.cp.solver.constraints.global.pack.*;
import choco.kernel.model.constraints.*;

import choco.cp.solver.search.integer.valselector.BestFit;

import java.util.ArrayList;

import choco.kernel.model.constraints.Constraint;


//import trace.*;

import java.util.*;

public class myHPCScheduling extends PatternExample {

	SetVariable[] itemSets;
	SetVariable[] devilSets;
	IntegerVariable[] loads;
	IntegerVariable[] bins;
	IntegerConstantVariable[] sizes;
	IntegerVariable nbNonEmpty;
	
	Constraint packC;

	IntegerVariable total_idle_nodes;

	IntegerConstantVariable[] classes;
	SetConstantVariable classes2;
	
	
	IntegerVariable[] node_devils;
	IntegerVariable total_devil_pairs;

	IntegerVariable weighted_sum;

	int number_of_containers;
	int number_of_nodes;
	int number_of_devils;

	int[] container_devils;

	int d = 100; // collocated devils
	int p = 1; // powered up nodes
	int c = 1; // collocated comm pairs

	@Override
	public void printDescription() {
		super.printDescription();
	}

	@Override
	public void buildModel() {
		model = new CPModel();
		
		int i;

		/*String classes_filename = "/cs/systems/home/sba70/octopus_garden/clavis-src/classes-for-choco-12.csv";
		LOGGER.info("classes_filename: "+classes_filename);

		number_of_containers = 0;
		number_of_devils = 0;
		//reading container classes
		try {
			File file = new File(classes_filename);
			BufferedReader bufRdr  = new BufferedReader(new FileReader(file));
			String line = null;
			int col, devil_id, class_id;
			//read each line of text file
			while((line = bufRdr.readLine()) != null)
			{
				StringTokenizer st = new StringTokenizer(line,",");
				while (st.hasMoreTokens())
				{
					class_id = Integer.parseInt(st.nextToken());
					if (class_id == 1)
					{
						number_of_devils++;
					}
					number_of_containers++;
				}
				container_devils = new int[number_of_devils];

				col = 0; devil_id = 0;
				st = new StringTokenizer(line,",");
				while (st.hasMoreTokens())
				{
					class_id = Integer.parseInt(st.nextToken());
					if (class_id == 1)
					{
						container_devils[devil_id] = col;
						devil_id++;
					}
					col++;
				}

				break;
			}
			bufRdr.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		*/

		number_of_containers = 115;
		number_of_devils = 80;
		number_of_nodes = number_of_containers;
		container_devils = new int[number_of_devils];
		
		// generating devils randomly
		ArrayList<Integer> numbers = new ArrayList<Integer>();
		for(i = 0; i < number_of_containers; i++){
			numbers.add(i);
		}
		Collections.shuffle(numbers);
		for(i = 0; i < number_of_devils; i++)
	    {
			container_devils[i] = numbers.get(i);
		}
		Arrays.sort(container_devils);
		
		LOGGER.info("container_devils:");
		String temp_str = "{";
		for (i = 0; i < container_devils.length; i++) {
			if (i == 0) temp_str += container_devils[0];
			else temp_str += ", " + container_devils[i];
		}
		temp_str += "}";
		
		LOGGER.info(temp_str);
		LOGGER.info("number_of_containers: "+number_of_containers);
		LOGGER.info("number_of_devils: "+number_of_devils);

		number_of_nodes = number_of_containers;
		LOGGER.info("number_of_nodes: "+number_of_nodes);
		
		itemSets = Choco.makeSetVarArray("itemSets", number_of_nodes, 0, number_of_containers, Options.V_ENUM, Options.V_NO_DECISION);
		model.addVariables(itemSets);
		
		loads = Choco.makeIntVarArray("loads", number_of_nodes, 0, 2, Options.V_NO_DECISION, Options.V_BOUND);
		model.addVariables(loads);

		bins = Choco.makeIntVarArray("bins", number_of_containers, 0, number_of_nodes - 1);
		model.addVariables(bins);

		sizes = new IntegerConstantVariable[number_of_containers];
		for (i=0;i<number_of_containers;i++) sizes[i] = Choco.constant(1);

        makeFastPack();
		PackModel m1 = new PackModel(bins, sizes, loads, itemSets);
		packC = Choco.pack(m1, Options.C_PACK_AR);
		model.addConstraint(packC);

		total_idle_nodes = Choco.makeIntVar("total_idle_nodes", 0, number_of_nodes, Options.V_NO_DECISION);
		model.addVariable(total_idle_nodes);
		model.addConstraint(Choco.occurrence(total_idle_nodes, loads, 0));

		classes2 = Choco.constant(container_devils);
		devilSets = Choco.makeSetVarArray("devilSets", number_of_nodes, 0, number_of_containers, Options.V_ENUM, Options.V_NO_DECISION);
		model.addVariables(devilSets);
		node_devils = Choco.makeIntVarArray("node_devils", number_of_nodes, 0, 2, Options.V_NO_DECISION);
		model.addVariables(node_devils);
		for (i=0;i<number_of_nodes;i++)
		{
			model.addConstraint(Choco.setInter(itemSets[i], classes2, devilSets[i]));
			model.addConstraint(Choco.eqCard(devilSets[i], node_devils[i]));
		}
		//for (i=0;i<number_of_nodes;i++)
		//{
		//	model.addConstraint(Choco.among(node_devils[i], classes, itemSets[i]));
		//}

		total_devil_pairs = Choco.makeIntVar("total_devil_pairs", 0, (int)Math.ceil(0.5*number_of_devils), Options.V_NO_DECISION);
		model.addVariable(total_devil_pairs);
		model.addConstraint(Choco.occurrence(total_devil_pairs, node_devils, 2));

		weighted_sum = Choco.makeIntVar("ws", -1*p*(int)Math.ceil(0.5*number_of_containers), d*(int)Math.ceil(0.5*number_of_devils), Options.V_NO_DECISION);
		model.addVariable(weighted_sum);
		model.addConstraint(Choco.eq(weighted_sum,Choco.sum(   Choco.mult(total_devil_pairs, d),   Choco.neg(Choco.mult(total_idle_nodes, p))   )));
	}

    private void makeFastPack() {
        model.addConstraint(new ComponentConstraint(FastBinPackingManager.class, new int []{loads.length, bins.length},ArrayUtils.append(loads, sizes, bins)));
    }

	@Override
	public void buildSolver() {
		solver = new CPSolver();
		solver.read(model);
		
		// A: same as no goal (default):
		//solver.addGoal(new DomOverWDegBranchingNew(solver, solver.getVar(bins), new IncreasingDomain(), null));
    	//System.out.println("solver.addGoal(new DomOverWDegBranchingNew(solver, solver.getVar(bins), new IncreasingDomain(), null));");
		// B (better than A):
		//solver.addGoal(new DomOverWDegBranchingNew(solver, solver.getVar(bins), new IncreasingDomain(), 1));
    	//System.out.println("solver.addGoal(new DomOverWDegBranchingNew(solver, solver.getVar(bins), new IncreasingDomain(), 1));");
		
		// not good:
		//solver.addGoal(new DomOverWDegBinBranchingNew(solver, solver.getVar(bins), new BestFit((PackSConstraint)solver.getCstr(packC)), null));
		//solver.addGoal(new DomOverWDegBinBranchingNew(solver, solver.getVar(bins), new BestFit((PackSConstraint)solver.getCstr(packC)), 1));
    	//System.out.println("solver.addGoal(new DomOverWDegBinBranchingNew(solver, solver.getVar(bins), new BestFit((PackSConstraint)solver.getCstr(packC)), 1));");
		
		// C (better than B): 
		//solver.addGoal(BranchingFactory.completeDecreasingBestFit(solver, (PackSConstraint)solver.getCstr(packC)));
    	//System.out.println("solver.addGoal(BranchingFactory.completeDecreasingBestFit(solver, (PackSConstraint)solver.getCstr(packC)));");
		

		PackSConstraint ct = (PackSConstraint)solver.getCstr(packC);
		final StaticVarOrder varSel = new StaticVarOrder(solver, ct.getBins());
		final ValSelector<IntDomainVar> valSel = new BestFit(ct);
		final ValSelector<IntDomainVar> valSel2 = new myHPCBestFit(ct, d, p, number_of_nodes, number_of_containers, number_of_devils, container_devils);
		
		final MaxRegret varSel2 = new MaxRegret(solver, ct.getBins());
		final RandomIntVarSelector varSel3 = new RandomIntVarSelector(solver, ct.getBins(), 1);
		
		// D (same as C):
		//solver.addGoal(new PackDynRemovals(varSel, valSel, ct));
    	//System.out.println("solver.addGoal(new PackDynRemovals(varSel, valSel, ct));");
		// worse than D:
		//solver.addGoal(new AssignVar(varSel, valSel));
    	//System.out.println("solver.addGoal(new AssignVar(varSel, valSel));");
		
		// same as D:
		//solver.addGoal(new PackDynRemovals(varSel2, valSel, ct));
    	//System.out.println("solver.addGoal(new PackDynRemovals(varSel2, valSel, ct));");
		// same as D:
		//solver.addGoal(new PackDynRemovals(varSel3, valSel, ct));
    	//System.out.println("solver.addGoal(new PackDynRemovals(varSel3, valSel, ct));");

		//solver.addGoal(new PackDynRemovals(varSel, valSel2, ct));
    	//System.out.println("solver.addGoal(new PackDynRemovals(varSel, valSel2, ct));");

    	solver.addGoal(new AssignVar(varSel, valSel2));
    	System.out.println("solver.addGoal(new AssignVar(varSel, valSel2));");
}

	@Override
	public void solve() {
		//ChocoLogging.setVerbosity(Verbosity.FINEST);
		//ChocoLogging.setVerbosity(Verbosity.SOLUTION);
		//LOGGER.info(solver.pretty());
		//ChocoLogging.setEveryXNodes(1000);
		//ChocoLogging.setLoggingMaxDepth(100000);
		solver.setTimeLimit(30000); // in milliseconds
		solver.minimize(solver.getVar(weighted_sum), false);
		//solver.solveAll();
		//ChocoLogging.flushLogs();
	}

	@Override
	public void prettyOut() {

    	System.out.println("p=" + p + ", d=" + d);

    	StringBuilder st2 = new StringBuilder("total_devil_pairs ").append(solver.getVar(total_devil_pairs).getVal());
		LOGGER.info(st2.toString());
		StringBuilder st3 = new StringBuilder("total_idle_nodes ").append(solver.getVar(total_idle_nodes).getVal());
		LOGGER.info(st3.toString());
		StringBuilder st5 = new StringBuilder("weighted_sum ").append(solver.getVar(weighted_sum).getVal());
		LOGGER.info(st5.toString());
}

	public static void main(String[] args) {
        ChocoLogging.setVerbosity(Verbosity.SEARCH);
		new myHPCScheduling().execute();
	}
}
