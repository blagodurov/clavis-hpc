// javac -cp /cs/kvm/choco/samples-2.1.3.jar myHPCScheduling.java
// java -cp $CLASSPATH:/cs/kvm/choco/samples-2.1.3.jar:/opt/sun-jdk-1.6.0.29/lib/tools.jar:. myHPCScheduling > myHPCScheduling.txt

package myHPCBranching;

import choco.Choco;
import choco.Options;
import choco.cp.model.CPModel;
import choco.cp.solver.CPSolver;
import choco.cp.solver.constraints.global.geost.Constants;
import choco.cp.solver.search.BranchingFactory;
import choco.cp.solver.search.integer.branching.AssignVar;
import choco.cp.solver.search.integer.valiterator.*;
import choco.cp.solver.search.integer.varselector.*;
import choco.cp.solver.search.integer.valselector.*;
import choco.kernel.model.constraints.geost.externalConstraints.IExternalConstraint;
import choco.kernel.model.constraints.geost.externalConstraints.NonOverlappingModel;
import choco.kernel.model.variables.geost.GeostObject;
import choco.kernel.model.variables.geost.ShiftedBox;
import choco.kernel.model.variables.integer.IntegerVariable;
import samples.tutorials.PatternExample;
import choco.kernel.solver.Solver;
import choco.kernel.solver.branch.VarSelector;
import choco.kernel.solver.variables.integer.IntDomainVar;
import choco.cp.solver.search.integer.branching.domwdeg.DomOverWDegBinBranchingNew;
import choco.cp.solver.search.integer.branching.ImpactBasedBranching;
import choco.kernel.common.logging.ChocoLogging;
import choco.kernel.common.logging.Verbosity;
//import trace.*;

import java.util.*;
import java.io.*;

public class myHPCScheduling extends PatternExample {

	IntegerVariable[] containers;

	//IntegerVariable[] devil_pairs;
	IntegerVariable total_devil_pairs;
	//IntegerVariable[] devil_containers;

	//IntegerVariable[] powered_up_nodes;
	IntegerVariable total_powered_up_nodes;

	//IntegerVariable[] colloc_comm_pairs;
	IntegerVariable total_colloc_comm_pairs;

	IntegerVariable weighted_sum;

	int d; // collocated devils
	int p; // powered up nodes
	int c; // collocated comm pairs
	
	static int[] options;

	
	int TIME_LIMIT_IN_MS;
	int SET_LOGGING;
	int COMPUTE_RANDOM;
	int COMPUTE_RANDOM_TIME_LIMIT_IN_MS;

	int number_of_containers;
	int number_of_nodes;
	int number_of_devils;

	int[] container_classes;
	int[][] communication_matrix;
	int[] container_job_ids;

	String classes_filename = "/cs/systems/home/sba70/octopus_garden/clavis-src/classes-for-choco-" + options[13] + ".csv";
	String comm_filename = "/cs/systems/home/sba70/octopus_garden/clavis-src/comm-matrix-for-choco-" + options[13] + ".csv";
	String schedule_filename = "/cs/systems/home/sba70/octopus_garden/clavis-src/schedule-" + options[13] + ".csv";
	int processes_per_job = options[13];

	@Override
	public void printDescription() {
		super.printDescription();
	}

	@Override
	public void buildModel() {
		model = new CPModel();

		int i, j, k;
		
		
		TIME_LIMIT_IN_MS = options[0];
		COMPUTE_RANDOM_TIME_LIMIT_IN_MS = options[0];
		SET_LOGGING = options[1];
		COMPUTE_RANDOM = options[2];
		d = options[3];
		p = options[4];
		c = options[5];
		
		
		
		
		
		
		
		


		/*number_of_containers = 12;
		number_of_nodes = number_of_containers;
		number_of_devils = 0;
		
		container_classes = new int[number_of_containers];
        for (i = 0; i < 2; i++) {
            container_classes[i] = 0;
        }
        for (i = 2; i < number_of_containers; i++) {
            container_classes[i] = 1;
            number_of_devils++;
        }
        
        communication_matrix = new int[number_of_containers][number_of_containers];
		for (i = 0; i < number_of_containers; i++) {
			for (j = 0; j < number_of_containers; j++) {
				communication_matrix[i][j] = 0;
			}
		}
		*/
		
		
		LOGGER.info("classes_filename: "+classes_filename);
		LOGGER.info("comm_filename: "+comm_filename);

		number_of_containers = 0;
		number_of_devils = 0;
		//reading container classes
		try {
			File file = new File(classes_filename);
			BufferedReader bufRdr  = new BufferedReader(new FileReader(file));
			String line = null;
			int col, class_id;
			//read each line of text file
			while((line = bufRdr.readLine()) != null)
			{
				StringTokenizer st = new StringTokenizer(line,",");
				while (st.hasMoreTokens())
				{
					Integer.parseInt(st.nextToken());
					number_of_containers++;
				}
				container_classes = new int[number_of_containers];
				container_job_ids = new int[number_of_containers];

				col = 0;
				st = new StringTokenizer(line,",");
				while (st.hasMoreTokens())
				{
					class_id = Integer.parseInt(st.nextToken());
					if (class_id == 1)
					{
						number_of_devils++;
					}
					container_classes[col] = class_id;
					container_job_ids[col] = col / processes_per_job;
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

		/*LOGGER.info("container_classes:");
		String temp_str = "{";
		for (i = 0; i < container_classes.length; i++) {
			if (i == 0) temp_str += container_classes[0];
			else temp_str += ", " + container_classes[i];
		}
		temp_str += "}";
		LOGGER.info(temp_str);

		LOGGER.info("container_job_ids:");
		temp_str = "{";
		for (i = 0; i < container_job_ids.length; i++) {
			if (i == 0) temp_str += container_job_ids[0];
			else temp_str += ", " + container_job_ids[i];
		}
		temp_str += "}";
		LOGGER.info(temp_str);
		*/

		number_of_nodes = number_of_containers;

        communication_matrix = new int[number_of_containers][number_of_containers];
		for (i = 0; i < number_of_containers; i++) {
			for (j = 0; j < number_of_containers; j++) {
				communication_matrix[i][j] = 0;
			}
		}

		
		LOGGER.info("number_of_containers: "+number_of_containers);
		LOGGER.info("number_of_devils: "+number_of_devils);
		LOGGER.info("number_of_nodes: "+number_of_nodes);


		//reading communication matrix
		try {
			File file = new File(comm_filename);
			BufferedReader bufRdr  = new BufferedReader(new FileReader(file));
			String line = null;
			int col, rank_to, rank_from;
			//read each line of text file
			while((line = bufRdr.readLine()) != null)
			{
				col = 0;
				rank_from = -1;
				StringTokenizer st = new StringTokenizer(line,",");
				while (st.hasMoreTokens())
				{
					if (col == 0)
					{
						rank_from = Integer.parseInt(st.nextToken());
					}
					else
					{
						rank_to = Integer.parseInt(st.nextToken());
						if (rank_from >= 0)
							communication_matrix[rank_from][rank_to] = 1;
					}
					col++;
				}
			}
			bufRdr.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}


		
		
		
		/*LOGGER.info("communication_matrix:");
	    LOGGER.info("{");
	    for (i = 0; i < communication_matrix.length; i++) {
	        temp_str = "   "+i+": {";
	        for (j = 0; j < communication_matrix[i].length; j++) {
	            if (j == 0) temp_str += communication_matrix[i][0];
	            else temp_str += ", " + communication_matrix[i][j];
	        }
	        temp_str += "}";
	    		LOGGER.info(temp_str);
	    }
	    LOGGER.info("}");*/
		
		
		
		
		
		
		
		
		
		
		if (COMPUTE_RANDOM == 1)
		{
			int numberOfThreads = Runtime.getRuntime().availableProcessors();
		    int total_solutions_tried = 0;
		    
		    int best_num_of_powered_nodes, best_num_of_collocated_devils, best_num_of_collocated_comms;
			
	        SolutionThread[] threads = new SolutionThread[numberOfThreads];
	        for (i = 0; i < numberOfThreads; i++) {
	            threads[i] = new SolutionThread(container_classes, communication_matrix, number_of_nodes, d, p, c);
	            threads[i].start();
	        }
	        
	        try{
		        Thread.sleep(COMPUTE_RANDOM_TIME_LIMIT_IN_MS);
		        for (i = 0; i < numberOfThreads; i++) {
		            threads[i].timeOut();
		            threads[i].join();
		        }
	        } catch(Exception e){
	        }

	        ArrayList<Pair> bestOfBest = threads[0].bestSolution;
	        float bestCost = threads[0].bestCost;
	        total_solutions_tried = threads[0].solutions_tried;
            best_num_of_powered_nodes = threads[0].best_num_of_powered_nodes;
            best_num_of_collocated_devils = threads[0].best_num_of_collocated_devils;
            best_num_of_collocated_comms = threads[0].best_num_of_collocated_comms;

            for(i = 1; i<numberOfThreads; i++){
	            if(threads[i].bestCost < bestCost){
	                bestCost = threads[i].bestCost;
	                bestOfBest = threads[i].bestSolution;
	                best_num_of_powered_nodes = threads[i].best_num_of_powered_nodes;
	                best_num_of_collocated_devils = threads[i].best_num_of_collocated_devils;
	                best_num_of_collocated_comms = threads[i].best_num_of_collocated_comms;
	            }
                total_solutions_tried += threads[i].solutions_tried;
	        }

			System.out.println("numberOfThreads " + numberOfThreads + " best random: " + bestCost + " (d p c: " + best_num_of_collocated_devils + " " + best_num_of_powered_nodes + " " + best_num_of_collocated_comms + ") total_solutions_tried: " + total_solutions_tried);
			//System.out.println("bestOfBest: " + bestOfBest);

			logString(best_num_of_collocated_devils + " " + best_num_of_powered_nodes + " " + best_num_of_collocated_comms + " " + bestCost + " " + total_solutions_tried);
			
			TIME_LIMIT_IN_MS = 0;
		}
		
		
		
		



		//int[] valuesA = new int[]{d, 0};
		//int[] valuesB = new int[]{0, p};

		// can be reduced since not all of the possible permutations are useful - some will for sure give not so good solutions
		containers = Choco.makeIntVarArray("v", number_of_containers, 0, number_of_nodes*2-1);
		model.addVariables(containers);
		//model.addConstraint(Choco.allDifferent(containers));

		//int number_of_pairs = number_of_containers;
		//int number_of_pairs = number_of_nodes;
		//devil_pairs = Choco.makeIntVarArray("devil_pairs", number_of_pairs, valuesA, Options.V_NO_DECISION);
		//model.addVariables(devil_pairs);
		total_devil_pairs = Choco.makeIntVar("td", 0, number_of_nodes, Options.V_NO_DECISION);
		model.addVariables(total_devil_pairs);

		//powered_up_nodes = Choco.makeIntVarArray("powered_up_nodes", number_of_nodes, valuesB, Options.V_NO_DECISION);
		//model.addVariables(powered_up_nodes);
		total_powered_up_nodes = Choco.makeIntVar("tp", 0, number_of_nodes, Options.V_NO_DECISION);
		model.addVariables(total_powered_up_nodes);

		total_colloc_comm_pairs = Choco.makeIntVar("tc", 0, number_of_nodes, Options.V_NO_DECISION);
		model.addVariables(total_colloc_comm_pairs);

		weighted_sum = Choco.makeIntVar("ws", -1000000, 1000000, Options.V_NO_DECISION);
		//model.addVariable(weighted_sum);
		//model.addConstraint(Choco.eq(weighted_sum,Choco.plus(   Choco.sum(devil_pairs),   Choco.sum(powered_up_nodes)   )));
		//        model.addConstraint(Choco.eq(weighted_sum,Choco.plus(   total_devil_pairs,   total_powered_up_nodes   )));
		model.addConstraint(Choco.eq(weighted_sum,Choco.sum(   Choco.mult(total_devil_pairs, d),   Choco.mult(total_powered_up_nodes, p),   Choco.neg(Choco.mult(total_colloc_comm_pairs, c))   )));
	}

	@Override
	public void buildSolver() {
		solver = new CPSolver();
		solver.read(model);

		solver.addGoal(new myImpactBasedBranching(solver, options, number_of_nodes, container_classes, container_job_ids, number_of_containers, number_of_devils, communication_matrix));

		//solver.addGoal(BranchingFactory.randomIntSearch(solver, 2));
	}

	@Override
	public void solve() {
		//-------> Visualization declaration starts here <-------//
		// create a new instance of Visualization

		//Visualization visu = new Visualization("HPCScheduling", solver, "myHPCProject/visuals");
		//visu.createTree("layout", "compact", "final", 10000, 10000); // declare tree search visualization

		//visu.createViz(); // declare visualizers container
		// create a new Vector visualizer
		//trace.visualizers.Vector visualizer = new trace.visualizers.Vector(solver.getVar(total_devil_pairs, total_powered_up_nodes, total_colloc_comm_pairs), "expanded", 0, 0, 8, 10, "hpc", 0, 9);
		//trace.visualizers.Vector visualizer = new trace.visualizers.Vector(solver.getVar(containers), "expanded", 13, 13);
		// add the vector to the visualizers container
		//visu.addVisualizer(visualizer);
		
		
		if (SET_LOGGING == 1)
		{
			ChocoLogging.setVerbosity(Verbosity.FINEST);
			ChocoLogging.setEveryXNodes(1000);
			ChocoLogging.setLoggingMaxDepth(100000);
		}
		//LOGGER.info(solver.pretty());

		solver.setTimeLimit(TIME_LIMIT_IN_MS); // in milliseconds
		//solver.solve(false);
		// the following might result in: Illegal state: the best objective X is not equal to the best solution objective Y
		// this is due to false as the last argument
		//solver.minimize(solver.getVar(weighted_sum), false);
		//((CPSolver)solver).setGeometricRestart(1, 1d, 10000);
		//solver.solveAll();
		solver.minimize(solver.getVar(weighted_sum), false);

		if (SET_LOGGING == 1)
		{
			ChocoLogging.flushLogs();
		}


		// close the visualization
		//visu.close();
		//-------> Visualization declaration ends here <-------//
	}

	@Override
	public void prettyOut() {
		int i,j,slot;
		boolean slot_was_found;

		/*
		LOGGER.info("\n");
		for (i = 0; i < containers.length; i++) {
			StringBuilder st = new StringBuilder("containers").append("[").append(i).append("]: ");
			st.append(solver.getVar(containers[i]).getVal());
			st.append(" ");
			LOGGER.info(st.toString());
		}
		String output_schedule = "";
		for (i = 0; i < (number_of_nodes*2-1); i++) {
			slot_was_found = false;
			for (j = 0; j < containers.length; j++) {
				slot = solver.getVar(containers[j]).getVal();
				if (slot == i)
				{
					output_schedule += "," + j;
					slot_was_found = true;
					break;
				}
			}
			if (!slot_was_found) output_schedule += ",-1";
		}
		if (output_schedule.length() > 0) output_schedule = "[" + output_schedule.substring(1);
		else output_schedule = "[";
		output_schedule += "]";
        LOGGER.info(output_schedule);
		
        
        try {
            File file = new File(schedule_filename);
            file.delete();
            file.createNewFile();
            FileWriter fstream = new FileWriter(schedule_filename);
            BufferedWriter out = new BufferedWriter(fstream);
            out.write(output_schedule);
            out.close();
        } catch (IOException e) {
        	// do whatever
        }
		*/

		/*        for (i = 0; i < devil_pairs.length; i++) {
            StringBuilder st = new StringBuilder("devil_pairs").append("[").append(i).append("]: ");
            st.append(solver.getVar(devil_pairs[i]).getVal());
            st.append(" ");
            LOGGER.info(st.toString());
        }

        for (i = 0; i < powered_up_nodes.length; i++) {
            StringBuilder st = new StringBuilder("powered_up_nodes").append("[").append(i).append("]: ");
            st.append(solver.getVar(powered_up_nodes[i]).getVal());
            st.append(" ");
            LOGGER.info(st.toString());
        }
		 */
		/*StringBuilder st2 = new StringBuilder("total_devil_pairs ").append(solver.getVar(total_devil_pairs).getVal());
		LOGGER.info(st2.toString());
		StringBuilder st3 = new StringBuilder("total_powered_up_nodes ").append(solver.getVar(total_powered_up_nodes).getVal());
		LOGGER.info(st3.toString());
		StringBuilder st4 = new StringBuilder("total_colloc_comm_pairs ").append(solver.getVar(total_colloc_comm_pairs).getVal());
		LOGGER.info(st4.toString());*/
		/*for (i = 0; i < colloc_comm_pairs.length; i++) {
            StringBuilder st = new StringBuilder("colloc_comm_pairs").append("[").append(i).append("]: ");
            st.append(solver.getVar(colloc_comm_pairs[i]).getVal());
            st.append(" ");
            LOGGER.info(st.toString());
        }
		 */

		/*for (i = 0; i < devil_containers.length; i++) {
            StringBuilder st = new StringBuilder("devil_containers").append("[").append(i).append("]: ");
            st.append(solver.getVar(devil_containers[i]).getVal());
            st.append(" ");
            LOGGER.info(st.toString());
        }
		 */

		//StringBuilder st = new StringBuilder("total_devil_pairs ").append(solver.getVar(total_devil_pairs).getVal());
		//LOGGER.info(st.toString());
		//StringBuilder st2 = new StringBuilder("total_powered_up_nodes ").append(solver.getVar(total_powered_up_nodes).getVal());
		//LOGGER.info(st2.toString());
		//StringBuilder st3 = new StringBuilder("total_colloc_comm_pairs ").append(solver.getVar(total_colloc_comm_pairs).getVal());
		//LOGGER.info(st3.toString());
		//StringBuilder st5 = new StringBuilder("weighted_sum ").append(solver.getVar(weighted_sum).getVal());
		//LOGGER.info(st5.toString());
	}

	public static void main(String[] args) {
		options = new int[16];
		String optstr;
		int curArg, i = 0;
		for (String s: args) {
            try {
            	switch (i) {
                case 0:  optstr = "TIME_LIMIT_IN_MS"; break;
                case 1:  optstr = "SET_LOGGING"; break;
                case 2:  optstr = "COMPUTE_RANDOM"; break;
                case 3:  optstr = "d"; break;
                case 4:  optstr = "p"; break;
                case 5:  optstr = "c"; break;
                case 6:  optstr = "USE_FUND_DISTINCT"; break;
                case 7:  optstr = "USE_FUND_PRIO"; break;
                case 8:  optstr = "USE_CONTAINER_REORDERING"; break;
                case 9:  optstr = "USE_NAIVE_F"; break;
                case 10:  optstr = "USE_POTENTIAL_F"; break;
                case 11:  optstr = "DO_FATHOMING"; break;
                case 12:  optstr = "PRUNE_TOP_PERC"; break;
                case 13:  optstr = "RANKS_PER_JOB"; break;
                default: optstr = "Invalid option"; break;
            	}
            	System.out.println(optstr + ": " + s);
            	curArg = Integer.parseInt(s);
            	options[i++] = curArg;
		    } catch (NumberFormatException e) {
		        System.err.println("Argument '" + s + "' must be an integer");
		        System.exit(1);
		    }
        }

		new myHPCScheduling().execute();
	}
	
	
	
	public static void logString(String line) {
		try{
			FileWriter fstream = new FileWriter("/cs/systems/home/sba70/octopus_garden/clusters/choco/myHPCBranching/results.txt", true);
			BufferedWriter out = new BufferedWriter(fstream);

			String logstr = "";
            for(int i = 0; i < 14; i++){
            	logstr += options[i] + " ";
            }
            logstr += ": ";
            		
			out.write(logstr+line+"\n");
			out.close();
		}
		catch (Exception e){//Catch exception if any
			System.err.println("Error: " + e.getMessage());
		}
	}
}
