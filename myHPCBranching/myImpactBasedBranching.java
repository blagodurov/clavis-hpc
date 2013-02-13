/**
 *  Copyright (c) 1999-2010, Ecole des Mines de Nantes
 *  All rights reserved.
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *
 *      * Redistributions of source code must retain the above copyright
 *        notice, this list of conditions and the following disclaimer.
 *      * Redistributions in binary form must reproduce the above copyright
 *        notice, this list of conditions and the following disclaimer in the
 *        documentation and/or other materials provided with the distribution.
 *      * Neither the name of the Ecole des Mines de Nantes nor the
 *        names of its contributors may be used to endorse or promote products
 *        derived from this software without specific prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND ANY
 *  EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL THE REGENTS AND CONTRIBUTORS BE LIABLE FOR ANY
 *  DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 *  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 *  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package myHPCBranching;

import choco.cp.solver.CPSolver;
import choco.kernel.common.TimeCacheThread;
import choco.kernel.common.util.iterators.DisposableIntIterator;
import choco.kernel.solver.ContradictionException;
import choco.kernel.solver.Solver;
import choco.kernel.solver.branch.AbstractLargeIntBranchingStrategy;
import choco.kernel.solver.search.IntBranchingDecision;
import choco.kernel.solver.variables.AbstractVar;
import choco.kernel.solver.variables.integer.IntDomainVar;

import java.util.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.StringTokenizer;
import java.util.Random;
import java.lang.Math.*;

/**
 * Impact based branchging based on the code from Hadrien
 * <p/>
 * Written by Guillaumme on 17 may 2008
 */
public class myImpactBasedBranching extends AbstractLargeIntBranchingStrategy {
	Solver _solver;
	IntDomainVar[] _vars;

	int[] container_classes;
	int[] communicating_containers;
	int[][] communication_matrix;
	int[] container_job_ids;
	int number_of_containers;
	int number_of_devils;
	int number_of_nodes;
	int number_of_jobs;

	int USE_FUND_DISTINCT;
	int USE_FUND_PRIO;
	int USE_CONTAINER_REORDERING;

	int USE_NAIVE_F;
	int USE_POTENTIAL_F;
	int DO_FATHOMING;
	int PRUNE_TOP_PERC;

	int TIME_LIMIT_IN_MS;
	long begin_epoch_in_ms;
	
	int[][] fund_distinct_values;
	int[] fund_distinct_values_taken_index;
	int[] next_value_to_take_index;

	int total_devil_pairs_index;
	int total_powered_up_nodes_index;
	int total_colloc_comm_pairs_index;

	int temp_powered_up_nodes;
	int temp_devil_pairs;
	int temp_colloc_comm_pairs;

	int temp_idle_slots_with_turtle;
	int temp_idle_slots_with_devil;
	int temp_unassigned_devils;
	int temp_unassigned_turtles;
	int temp_idle_nodes;
	int temp_potential_powered_up_nodes;
	int temp_potential_devil_pairs;
	int temp_potential_colloc_comm_pairs;

	int[] assigned_slots;
	int[] fathom_assigned_slots;
	int[] fathom_assigned_slots2;
	
	int[] assigned_containers;
	int[] fathom_assigned_containers;
	int[] fathom_assigned_containers2;
	
	int cur_phatom_empty_slot;
	int cur_phatom_empty_slot2;
	
	ArrayList[] not_in_pair_turtles;
	ArrayList[] not_in_pair_devils;

	ArrayList[] copy_not_in_pair_turtles;
	ArrayList[] copy_not_in_pair_devils;
	
	ArrayList[] copy_not_in_pair_turtles2;
	ArrayList[] copy_not_in_pair_devils2;

	ArrayList[][][] potential_communicating_processes;
	ArrayList[] copy_potential_communicating_processes;
	ArrayList[][][] potential_processes_comm_with;
	ArrayList[] copy_potential_processes_comm_with;
	
	
	
	ArrayList[] communicating_processes = new ArrayList[number_of_jobs];

	
	
	int d;
	int p;
	int c;
	int weight_index = -1;

	float bestF = Float.MAX_VALUE;
	float potentialF = Float.MAX_VALUE;
	float pruneFactor = 0;

	IntDomainVar[] reordered_containers;
	int branching_level = 0;
	int smallest_level_with_finished_branching = -1;
	int tree_nodes_evaluated = 0;
	int tree_nodes_pruned = 0;

	int latest_assigned_container = -1;
	int latest_assigned_pair_container = -1;

	
	int[][][] categories_slots;
	int[][] categories_slots_first_avail;

	
	float lastCalculatedPotentialF = Float.MAX_VALUE;
	int lastCalculatedBranchingLevel = -1;
	
	float lastFathomedPotentialF = Float.MAX_VALUE;
	
	int rightmost_assigned_slot = -1;
	
	int best_devil_pairs = -1;
	int best_powered_up_nodes = -1;
	int best_colloc_comm_pairs = -1;
	
	
	


	private static final int ABSTRACTVAR_EXTENSION =
			AbstractVar.getAbstractVarExtensionNumber("choco.cp.cpsolver.search.integer.myImpactBasedBranching");

	static IntDomainVar[] varsFromSolver(Solver s) {
		IntDomainVar[] vars = new IntDomainVar[s.getNbIntVars()];
		for (int i = 0; i < vars.length; i++) {
			vars[i] = s.getIntVar(i);
		}
		return vars;
	}

	public myImpactBasedBranching(Solver solver, IntDomainVar[] vars, int[] in_options, int in_number_of_nodes, int[] in_container_classes, int[] in_container_job_ids, int in_number_of_containers, int in_number_of_devils, int[][] in_communication_matrix) {
		super();
		int i = 0, j = 0, k;
		_solver = solver;
		_vars = vars;

		TIME_LIMIT_IN_MS = in_options[0]-1;
		begin_epoch_in_ms = System.currentTimeMillis();
		d = in_options[3];
		p = in_options[4];
		c = in_options[5];
		USE_FUND_DISTINCT = in_options[6];
		USE_FUND_PRIO = in_options[7];
		USE_CONTAINER_REORDERING = in_options[8];
		USE_NAIVE_F = in_options[9];
		USE_POTENTIAL_F = in_options[10];
		DO_FATHOMING = in_options[11];
		PRUNE_TOP_PERC = in_options[12];

		temp_powered_up_nodes = 0;
		temp_devil_pairs = 0;
		temp_colloc_comm_pairs = 0;

		if (d >= p && p >= c)
		{
			weight_index = 2;
		} else
			if (d >= c && c >= p)
			{
				weight_index = 3;
			} else
				if (p >= d && d >= c)
				{
					weight_index = 0;
				} else
					if (p >= c && c >= d)
					{
						weight_index = 1;
					} else
						if (c >= p && p >= d)
						{
							weight_index = 4;
						} else
							if (c >= d && d >= p)
							{
								weight_index = 5;
							}

		number_of_nodes = in_number_of_nodes;
		container_classes = in_container_classes;
		container_job_ids = in_container_job_ids;
		
		// count the number of jobs
		number_of_jobs = -1;
		for (i = 0; i < container_job_ids.length; i++) {
			if (container_job_ids[i] > number_of_jobs) number_of_jobs = container_job_ids[i];
		}
		number_of_jobs++;
		
		
		number_of_containers = in_number_of_containers;
		number_of_devils = in_number_of_devils;
		communication_matrix = in_communication_matrix;
		communicating_containers = new int[number_of_containers];
		for (i = 0; i < number_of_containers; i++) {
			communicating_containers[i] = 0;
		}
		for (i = 0; i < number_of_containers; i++) {
			if (communicating_containers[i] == 1) continue;
			
			for (j = 0; j < number_of_containers; j++) {
				if (i == j) continue;
				if (communication_matrix[i][j] == 1 || communication_matrix[j][i] == 1)
				{
					communicating_containers[i] = 1;
					communicating_containers[j] = 1;
					break;
				}
			}
		}
		
		i = 0;
		for (IntDomainVar var : _vars) {
			var.addExtension(ABSTRACTVAR_EXTENSION);

			Object temp0bj = var;
			String tempstr = temp0bj + "...";
			if (tempstr.startsWith("td"))
			{
				total_devil_pairs_index = i;
			}
			else if (tempstr.startsWith("tp"))
			{
				total_powered_up_nodes_index = i;
			}
			else if (tempstr.startsWith("tc"))
			{
				total_colloc_comm_pairs_index = i;
			}

			i++;
		}
		
		
		
		if (PRUNE_TOP_PERC > 0) pruneFactor = (float)(0.01)*(float)PRUNE_TOP_PERC;
		
		
		not_in_pair_turtles = new ArrayList[number_of_jobs];
		copy_not_in_pair_turtles = new ArrayList[number_of_jobs];
		copy_not_in_pair_turtles2 = new ArrayList[number_of_jobs];
		for(i = 0; i < number_of_jobs; i++){
			not_in_pair_turtles[i] = new ArrayList<Integer>();
			copy_not_in_pair_turtles[i] = new ArrayList<Integer>();
			copy_not_in_pair_turtles2[i] = new ArrayList<Integer>();
		}

		not_in_pair_devils = new ArrayList[number_of_jobs];
		copy_not_in_pair_devils = new ArrayList[number_of_jobs];
		copy_not_in_pair_devils2 = new ArrayList[number_of_jobs];
		for(i = 0; i < number_of_jobs; i++){
			not_in_pair_devils[i] = new ArrayList<Integer>();
			copy_not_in_pair_devils[i] = new ArrayList<Integer>();
			copy_not_in_pair_devils2[i] = new ArrayList<Integer>();
		}
		
		for (i = 0; i < number_of_containers; i++) {
			if (container_classes[i] == 0) not_in_pair_turtles[   container_job_ids[i]   ].add(i);
			else not_in_pair_devils[   container_job_ids[i]   ].add(i);
		}
		
		
		next_value_to_take_index = new int[number_of_containers];
		fund_distinct_values = new int[number_of_containers][number_of_nodes];
		fund_distinct_values_taken_index = new int[number_of_containers];

		assigned_slots = new int[number_of_nodes*2];
		fathom_assigned_slots = new int[assigned_slots.length];
		fathom_assigned_slots2 = new int[assigned_slots.length];
		for (i = 0; i < assigned_slots.length; i++) {
			assigned_slots[i] = -1;
		}
		
		assigned_containers = new int[number_of_containers];
		fathom_assigned_containers = new int[number_of_containers];
		fathom_assigned_containers2 = new int[number_of_containers];
		for (i = 0; i < assigned_containers.length; i++) {
			assigned_containers[i] = -1;
		}
		
		
		
		
		
		int[] container_order = new int[number_of_containers];
		if (USE_CONTAINER_REORDERING == 0)
		{
			for (i = 0; i < number_of_containers; i++) {
				container_order[i] = i;
			}
		}
		else
		{
			boolean class_switch = false;

			int[] temp_indexes = new int[number_of_containers];
			for (i = 0; i < number_of_containers; i++) {
				temp_indexes[i] = i;
			}
			
			for (i = 0; i < number_of_containers; i++) {
				container_order[i] = -1;
				for (j = 0; j < number_of_containers; j++) {
					if (temp_indexes[j] < 0) continue;
					
					if (c > d && isContainerCommunicating(j)) continue;
					
					if (!class_switch && !isContainerDevil(j))
					{
						container_order[i] = temp_indexes[j];
						temp_indexes[j] = -1;
						class_switch = true;
						break;
					}
					else if (class_switch && isContainerDevil(j))
					{
						container_order[i] = temp_indexes[j];
						temp_indexes[j] = -1;
						class_switch = false;
						break;
					}
				}
			}

			for (i = 0; i < number_of_containers; i++) {
				if (container_order[i] >= 0) continue;
				for (j = 0; j < number_of_containers; j++) {
					if (temp_indexes[j] < 0) continue;
					container_order[i] = temp_indexes[j];
					temp_indexes[j] = -1;
					break;
				}
			}
		}
		
		
		reordered_containers = new IntDomainVar[number_of_containers];
		int container_index = -1;
		for (i = 0; i < number_of_containers; i++) {
			for (IntDomainVar var : _vars) {
				container_index = getContainerIndex(var);
				if (container_order[i] == container_index)
				{
					reordered_containers[i] = var;
			    	//System.out.println("reordered_containers["+i+"]= " + getContainerIndex(reordered_containers[i]) + "; ");
					break;
				}
			}
		}
		
		
		
		
		
		if (USE_POTENTIAL_F == 1)
		{
			potential_communicating_processes = new ArrayList[number_of_containers][3][number_of_jobs];
			for(i = 0; i < number_of_containers; i++){
				for(j = 0; j < 3; j++){
					for(k = 0; k < number_of_jobs; k++){
						potential_communicating_processes[i][j][k] = new ArrayList<Integer>();
					}
				}
			}
			
			potential_processes_comm_with = new ArrayList[number_of_containers][3][number_of_containers];
			for(i = 0; i < number_of_containers; i++){
				for(j = 0; j < 3; j++){
					for(k = 0; k < number_of_containers; k++){
						potential_processes_comm_with[i][j][k] = new ArrayList<Integer>();
					}
				}
			}

			generatePotentialCommPairs(not_in_pair_turtles, not_in_pair_turtles, 0);
			generatePotentialCommPairs(not_in_pair_devils, not_in_pair_devils, 1);
			generatePotentialCommPairs(not_in_pair_turtles, not_in_pair_devils, 2);
			
			
			copy_potential_communicating_processes = new ArrayList[number_of_jobs];
			for(k = 0; k < number_of_jobs; k++){
				copy_potential_communicating_processes[k] = new ArrayList<Integer>();
			}

			copy_potential_processes_comm_with = new ArrayList[number_of_containers];
			for(k = 0; k < number_of_containers; k++){
				copy_potential_processes_comm_with[k] = new ArrayList<Integer>();
			}
		}
		
		
		
		

		
		
		// categories:
		// 0 - isolated
		// 1 - non-comm turtle
		// 2 - comm turtle
		// 3 - comm with that one turtle
		// 4 - non-comm devil
		// 5 - comm devil
		// 6 - comm with that one devil
		categories_slots = new int[number_of_containers][7][number_of_nodes];
		for (j = 0; j < 7; j++) {
			for (k = 0; k < number_of_nodes; k++) {
				categories_slots[0][j][k] = -1;
			}
		}
		categories_slots[0][0][0] = 0;
				
		categories_slots_first_avail = new int[number_of_containers][7];
		categories_slots_first_avail[0][0] = 1;
		for (i = 1; i < 7; i++) {
			categories_slots_first_avail[0][i] = 0;
		}

		
		
		
		
		
		temp_idle_slots_with_turtle = 0;
		temp_idle_slots_with_devil = 0;
		temp_unassigned_devils = number_of_devils;
		temp_unassigned_turtles = number_of_containers - number_of_devils;
		temp_idle_nodes = number_of_nodes;
	}

	public myImpactBasedBranching(Solver solver, int[] in_options, int in_number_of_nodes, int[] in_container_classes, int[] in_container_job_ids, int in_number_of_containers, int in_number_of_devils, int[][] in_communication_matrix) {
		this(solver, varsFromSolver(solver), in_options, in_number_of_nodes, in_container_classes, in_container_job_ids, in_number_of_containers, in_number_of_devils, in_communication_matrix);
	}

	
	
	
	public float calculatePotentialFnaive() {
		temp_potential_powered_up_nodes = (int)Math.ceil(0.5*(temp_unassigned_devils + temp_unassigned_turtles - temp_idle_slots_with_turtle - temp_idle_slots_with_devil));
		if (temp_potential_powered_up_nodes < 0) temp_potential_powered_up_nodes = 0;
		
		temp_potential_devil_pairs = temp_unassigned_devils - temp_idle_nodes - temp_idle_slots_with_turtle;
		if (temp_potential_devil_pairs < 0) temp_potential_devil_pairs = 0;
		
		temp_potential_colloc_comm_pairs = temp_idle_slots_with_turtle + temp_idle_slots_with_devil + (int)Math.floor(0.5*(temp_unassigned_devils + temp_unassigned_turtles - temp_idle_slots_with_turtle - temp_idle_slots_with_devil));
		if (temp_potential_colloc_comm_pairs < 0) temp_potential_colloc_comm_pairs = 0;
		
		float potentialF = (temp_potential_powered_up_nodes+temp_powered_up_nodes)*p + (temp_potential_devil_pairs+temp_devil_pairs)*d - (temp_potential_colloc_comm_pairs+temp_colloc_comm_pairs)*c;
		
		if (   potentialF < (bestF-Math.abs(bestF)*pruneFactor)   ) return 0;
		else return 1;
	}

	
	
	
	public float calculatePotentialFbounding() {
		int max_potential_colloc_comm_turtle_pairs = 0, max_potential_colloc_comm_devil_turtle_pairs = 0, max_potential_colloc_comm_devil_pairs = 0;
		max_potential_colloc_comm_turtle_pairs = updatePotentialCommPair(0);
		max_potential_colloc_comm_devil_pairs = updatePotentialCommPair(1);
		max_potential_colloc_comm_devil_turtle_pairs = updatePotentialCommPair(2);
		
		
		// no need to calculate eloborate bounding if it won't prune the branch for sure:
		if (lastCalculatedBranchingLevel >= 0 && branching_level > lastCalculatedBranchingLevel)
		{
			if (   (lastCalculatedPotentialF + (d + p + c)*(branching_level-lastCalculatedBranchingLevel)) < (bestF-Math.abs(bestF)*pruneFactor)   )
				return 0;
		}

		
		float potentialF = Float.MAX_VALUE, minPotentialF = Float.MAX_VALUE;
		// 1. calculate the possible range for collocated devil pairs:
		int min_potential_devil_pairs = temp_unassigned_devils - temp_idle_nodes - temp_idle_slots_with_turtle;
		if (min_potential_devil_pairs < 0) min_potential_devil_pairs = 0;
		
		int max_potential_devil_pairs = temp_unassigned_devils; // if (temp_idle_slots_with_devil >= temp_unassigned_devils)
		if (temp_idle_slots_with_devil < temp_unassigned_devils) 
			max_potential_devil_pairs = temp_idle_slots_with_devil + (int)Math.floor(0.5*(temp_unassigned_devils-temp_idle_slots_with_devil));
		
		int potential_devil_pairs, potential_powered_up_nodes, potential_colloc_comm_pairs;
		int temp_not_in_pair_turtles, temp_not_in_pair_turtles2, temp_not_in_pair_devils, temp_not_in_pair_devils2, temp_not_in_pair_devils3, temp_idle_slots_with_x;
		int min_potential_devil_turtle_pairs, max_potential_devil_turtle_pairs, potential_devil_turtle_pairs, potential_turtle_pairs;
		int fathom_potential_devil_pairs = -1, fathom_potential_devil_turtle_pairs = -1, fathom_potential_turtle_pairs = -1, 
		   fathom_not_in_pair_turtles = -1, fathom_not_in_pair_devils = -1, fathom_potential_powered_up_nodes = -1;

		temp_not_in_pair_turtles = temp_unassigned_turtles + temp_idle_slots_with_turtle;
		temp_not_in_pair_devils = temp_unassigned_devils + temp_idle_slots_with_devil;
		temp_idle_slots_with_x = temp_idle_slots_with_turtle + temp_idle_slots_with_devil;
		
		
		int jobid, i, j;
		
		int potential_devil_pairs_start, potential_devil_pairs_end, potential_devil_pairs_step;
		int potential_devil_turtle_pairs_start, potential_devil_turtle_pairs_end, potential_devil_turtle_pairs_step;

		
		
		if (d > p && d > c)
		{
			potential_devil_pairs_start = min_potential_devil_pairs;
			potential_devil_pairs_end = max_potential_devil_pairs;
			potential_devil_pairs_step = 1;
		}
		else
		{
			potential_devil_pairs_start = max_potential_devil_pairs;
			potential_devil_pairs_end = min_potential_devil_pairs;
			potential_devil_pairs_step = -1;
		}

		
		
		for (potential_devil_pairs = potential_devil_pairs_start; ((d > p && d > c) && potential_devil_pairs <= potential_devil_pairs_end) || (!(d > p && d > c) && potential_devil_pairs >= potential_devil_pairs_end); potential_devil_pairs+=potential_devil_pairs_step) {
			// here we consider all temp_idle_slots_with_x as potential for simplicity, then just subtract them
			
			temp_not_in_pair_devils2 = temp_not_in_pair_devils - 2*potential_devil_pairs;
			if (temp_not_in_pair_devils2 < 0) temp_not_in_pair_devils2 = 0;
			
			if (temp_not_in_pair_turtles % 2 == 1 && temp_not_in_pair_devils2 > 0) min_potential_devil_turtle_pairs = 1; // because if one turtle is idle, it may as well go with a devil
			else min_potential_devil_turtle_pairs = 0; // when all the slots with one devil-neighbour are empty
			
			if (temp_not_in_pair_devils2 <= temp_not_in_pair_turtles) max_potential_devil_turtle_pairs = temp_not_in_pair_devils2;
			else max_potential_devil_turtle_pairs = temp_not_in_pair_turtles;
			
			
			
			if (c > p)
			{
				potential_devil_turtle_pairs_start = min_potential_devil_turtle_pairs;
				potential_devil_turtle_pairs_end = max_potential_devil_turtle_pairs;
				potential_devil_turtle_pairs_step = 1;
			}
			else
			{
				potential_devil_turtle_pairs_start = max_potential_devil_turtle_pairs;
				potential_devil_turtle_pairs_end = min_potential_devil_turtle_pairs;
				potential_devil_turtle_pairs_step = -1;
			}

			potential_devil_turtle_pairs = potential_devil_turtle_pairs_start;
			while (    (c > p && potential_devil_turtle_pairs <= potential_devil_turtle_pairs_end)  ||  (!(c > p) && potential_devil_turtle_pairs >= potential_devil_turtle_pairs_end))
			{
				temp_not_in_pair_turtles2 = temp_not_in_pair_turtles - potential_devil_turtle_pairs;
				potential_turtle_pairs = (int)Math.floor(0.5*temp_not_in_pair_turtles2);
				if (temp_not_in_pair_turtles2 % 2 == 1) temp_not_in_pair_turtles2 = 1;
				else temp_not_in_pair_turtles2 = 0;
				
				temp_not_in_pair_devils3 = temp_not_in_pair_devils2 - potential_devil_turtle_pairs;
				if (temp_not_in_pair_devils3 < 0) temp_not_in_pair_devils3 = 0;
				
				
				
				
				// 2. calculate the potential value for powered_up_nodes with a given potential_devil_pairs and potential_devil_turtle_pairs
				potential_powered_up_nodes = potential_devil_pairs + potential_devil_turtle_pairs + potential_turtle_pairs + temp_not_in_pair_turtles2 + temp_not_in_pair_devils3;
				// considered all temp_idle_slots_with_x as potential for simplicity, now just subtract them:
				potential_powered_up_nodes -=  temp_idle_slots_with_x;
				if (potential_powered_up_nodes < 0) potential_powered_up_nodes = 0;

				
				
				
				// 3. calculate the potential value for colloc_comm_pairs with a given potential_devil_pairs and potential_devil_turtle_pairs
				potential_colloc_comm_pairs = 0;

				if (potential_turtle_pairs >= max_potential_colloc_comm_turtle_pairs) potential_colloc_comm_pairs += max_potential_colloc_comm_turtle_pairs;
				else potential_colloc_comm_pairs += potential_turtle_pairs;
				
				if (potential_devil_pairs >= max_potential_colloc_comm_devil_pairs) potential_colloc_comm_pairs += max_potential_colloc_comm_devil_pairs;
				else potential_colloc_comm_pairs += potential_devil_pairs;
				
				if (potential_devil_turtle_pairs >= max_potential_colloc_comm_devil_turtle_pairs) potential_colloc_comm_pairs += max_potential_colloc_comm_devil_turtle_pairs;
				else potential_colloc_comm_pairs += potential_devil_turtle_pairs;
				
				
				
				
				
						
						

				
				potentialF = (potential_powered_up_nodes+temp_powered_up_nodes)*p + (potential_devil_pairs+temp_devil_pairs)*d - (potential_colloc_comm_pairs+temp_colloc_comm_pairs)*c;
				if (   potentialF < (bestF-Math.abs(bestF)*pruneFactor)   )
				{
					/*System.out.println("potentialF: " + potentialF + " bestF: " + bestF + " branching_level: " + branching_level
					+ " potential_powered_up_nodes: " + potential_powered_up_nodes
					 + " temp_powered_up_nodes: " + temp_powered_up_nodes
					  + " potential_devil_pairs: " + potential_devil_pairs
					   + " temp_devil_pairs: " + temp_devil_pairs 
					    + " potential_colloc_comm_pairs: " + potential_colloc_comm_pairs 
					     + " temp_colloc_comm_pairs: " + temp_colloc_comm_pairs);*/
					if (DO_FATHOMING == 0)
					{
						lastCalculatedPotentialF = potentialF;
						lastCalculatedBranchingLevel = branching_level;
						return 0;
					}
					else if (potentialF < minPotentialF)
					{
						minPotentialF = potentialF;
						fathom_potential_devil_pairs = potential_devil_pairs;
						fathom_potential_devil_turtle_pairs = potential_devil_turtle_pairs;
						fathom_potential_turtle_pairs = potential_turtle_pairs;
						fathom_not_in_pair_turtles = temp_not_in_pair_turtles;
						fathom_not_in_pair_devils = temp_not_in_pair_devils;
						fathom_potential_powered_up_nodes = potential_powered_up_nodes;
						
						lastCalculatedPotentialF = potentialF;
						lastCalculatedBranchingLevel = branching_level;

						break;
					}
				}
					
				
				
				// iterate by 2 because, again, no reason to left one turtle alone on a node, except for if there is no more idle devils
				potential_devil_turtle_pairs += potential_devil_turtle_pairs_step;
				if (potential_devil_turtle_pairs != potential_devil_turtle_pairs_end) potential_devil_turtle_pairs += potential_devil_turtle_pairs_step;
			}
			
			if (minPotentialF != Float.MAX_VALUE) break;
		}
		
		
		
		// consider possible fathoming
		if (minPotentialF != Float.MAX_VALUE )
		{
			if (minPotentialF < lastFathomedPotentialF) /*fathom only if it is more promising than the previous successfull fathoming*/
			{
				int fathom_add_colloc_comm_pairs = 0;
				int fathom_add_devil_pairs = 0;
				int fathom_add_powered_up_nodes = 0 - temp_idle_slots_with_x;

				int pairs_found1tt = 0;
				int pairs_found1dd = 0;
				int pairs_found1td = 0;
				
				int pairs_found2tt = 0;
				int pairs_found2dd = 0;
				int pairs_found2td = 0;
				
				System.arraycopy(assigned_slots, 0, fathom_assigned_slots, 0, assigned_slots.length);
				System.arraycopy(assigned_containers, 0, fathom_assigned_containers, 0, assigned_containers.length);
				
				cur_phatom_empty_slot = 0;
				
				for(jobid = 0; jobid < number_of_jobs; jobid++){
					copy_not_in_pair_turtles[jobid].clear();
					copy_not_in_pair_turtles[jobid] = new ArrayList<Integer>(not_in_pair_turtles[jobid]);
					
					copy_not_in_pair_devils[jobid].clear();
					copy_not_in_pair_devils[jobid] = new ArrayList<Integer>(not_in_pair_devils[jobid]);
				}
				
				
				
				int pairs_found;

				
				
				// try two ways of packing the comm pairs:
				pairs_found2tt = findPhatomPair(copy_not_in_pair_turtles, copy_not_in_pair_turtles, 0, fathom_potential_turtle_pairs, 1, 1);
				pairs_found2dd = findPhatomPair(copy_not_in_pair_devils, copy_not_in_pair_devils, 1, fathom_potential_devil_pairs, 1, 1);
				pairs_found2td = findPhatomPair(copy_not_in_pair_turtles, copy_not_in_pair_devils, 2, fathom_potential_devil_turtle_pairs, 1, 1);
				
				
				// save the state
				System.arraycopy(fathom_assigned_slots, 0, fathom_assigned_slots2, 0, assigned_slots.length);
				System.arraycopy(fathom_assigned_containers, 0, fathom_assigned_containers2, 0, assigned_containers.length);

				cur_phatom_empty_slot2 = cur_phatom_empty_slot;
				
				for(jobid = 0; jobid < number_of_jobs; jobid++){
					copy_not_in_pair_turtles2[jobid].clear();
					copy_not_in_pair_turtles2[jobid] = new ArrayList<Integer>(copy_not_in_pair_turtles[jobid]);
					
					copy_not_in_pair_devils2[jobid].clear();
					copy_not_in_pair_devils2[jobid] = new ArrayList<Integer>(copy_not_in_pair_devils[jobid]);
				}
				
				
				// reset the state:
				System.arraycopy(assigned_slots, 0, fathom_assigned_slots, 0, assigned_slots.length);
				System.arraycopy(assigned_containers, 0, fathom_assigned_containers, 0, assigned_containers.length);
				
				cur_phatom_empty_slot = 0;
				
				for(jobid = 0; jobid < number_of_jobs; jobid++){
					copy_not_in_pair_turtles[jobid].clear();
					copy_not_in_pair_turtles[jobid] = new ArrayList<Integer>(not_in_pair_turtles[jobid]);
					
					copy_not_in_pair_devils[jobid].clear();
					copy_not_in_pair_devils[jobid] = new ArrayList<Integer>(not_in_pair_devils[jobid]);
				}

				
				pairs_found1tt = findPhatomPair2(copy_not_in_pair_turtles, copy_not_in_pair_turtles, 0, fathom_potential_turtle_pairs, 1, 1);
				pairs_found1dd = findPhatomPair2(copy_not_in_pair_devils, copy_not_in_pair_devils, 1, fathom_potential_devil_pairs, 1, 1);
				pairs_found1td = findPhatomPair2(copy_not_in_pair_turtles, copy_not_in_pair_devils, 2, fathom_potential_devil_turtle_pairs, 1, 1);
				
				
				if (   (pairs_found1tt + pairs_found1dd + pairs_found1td)   <   (pairs_found2tt + pairs_found2dd + pairs_found2td))
				{
					// restore the state
					System.arraycopy(fathom_assigned_slots2, 0, fathom_assigned_slots, 0, assigned_slots.length);
					System.arraycopy(fathom_assigned_containers2, 0, fathom_assigned_containers, 0, assigned_containers.length);

					cur_phatom_empty_slot = cur_phatom_empty_slot2;
					
					for(jobid = 0; jobid < number_of_jobs; jobid++){
						copy_not_in_pair_turtles[jobid].clear();
						copy_not_in_pair_turtles[jobid] = new ArrayList<Integer>(copy_not_in_pair_turtles2[jobid]);
						
						copy_not_in_pair_devils[jobid].clear();
						copy_not_in_pair_devils[jobid] = new ArrayList<Integer>(copy_not_in_pair_devils2[jobid]);
					}

					
					fathom_potential_turtle_pairs -= pairs_found2tt;
					fathom_add_colloc_comm_pairs += pairs_found2tt;
					fathom_add_powered_up_nodes += pairs_found2tt;

					fathom_potential_devil_pairs -= pairs_found2dd;
					fathom_add_colloc_comm_pairs += pairs_found2dd;
					fathom_add_devil_pairs += pairs_found2dd;
					fathom_add_powered_up_nodes += pairs_found2dd;

					fathom_potential_devil_turtle_pairs -= pairs_found2td;
					fathom_add_colloc_comm_pairs += pairs_found2td;
					fathom_add_powered_up_nodes += pairs_found2td;
				}
				else
				{
					fathom_potential_turtle_pairs -= pairs_found1tt;
					fathom_add_colloc_comm_pairs += pairs_found1tt;
					fathom_add_powered_up_nodes += pairs_found1tt;

					fathom_potential_devil_pairs -= pairs_found1dd;
					fathom_add_colloc_comm_pairs += pairs_found1dd;
					fathom_add_devil_pairs += pairs_found1dd;
					fathom_add_powered_up_nodes += pairs_found1dd;

					fathom_potential_devil_turtle_pairs -= pairs_found1td;
					fathom_add_colloc_comm_pairs += pairs_found1td;
					fathom_add_powered_up_nodes += pairs_found1td;
				}
				

				
				pairs_found = findPhatomPair2(copy_not_in_pair_turtles, copy_not_in_pair_turtles, 0, fathom_potential_turtle_pairs, 0, 1);
				fathom_potential_turtle_pairs -= pairs_found;
				fathom_add_powered_up_nodes += pairs_found;
				
				pairs_found = findPhatomPair2(copy_not_in_pair_devils, copy_not_in_pair_devils, 1, fathom_potential_devil_pairs, 0, 1);
				fathom_potential_devil_pairs -= pairs_found;
				fathom_add_devil_pairs += pairs_found;
				fathom_add_powered_up_nodes += pairs_found;
				
				pairs_found = findPhatomPair2(copy_not_in_pair_turtles, copy_not_in_pair_devils, 2, fathom_potential_devil_turtle_pairs, 0, 1);
				fathom_potential_devil_turtle_pairs -= pairs_found;
				fathom_add_powered_up_nodes += pairs_found;
			
				
				pairs_found = findPhatomPair(copy_not_in_pair_turtles, copy_not_in_pair_turtles, 0, fathom_not_in_pair_turtles, 0, 0);
				fathom_not_in_pair_turtles -= pairs_found;
				fathom_add_powered_up_nodes += pairs_found;
				
				pairs_found = findPhatomPair(copy_not_in_pair_devils, copy_not_in_pair_devils, 1, fathom_not_in_pair_devils, 0, 0);
				fathom_not_in_pair_devils -= pairs_found;
				fathom_add_powered_up_nodes += pairs_found;
				
				float tempF = (fathom_add_powered_up_nodes+temp_powered_up_nodes)*p + (fathom_add_devil_pairs+temp_devil_pairs)*d - (fathom_add_colloc_comm_pairs+temp_colloc_comm_pairs)*c;
				if (tempF < bestF)
				{
					System.out.println("Found through fathoming at branching_level " + branching_level + ": " + tempF + " prev: " + bestF + " (d p c: " + (fathom_add_devil_pairs+temp_devil_pairs) + " " + (fathom_add_powered_up_nodes+temp_powered_up_nodes) + " " + (fathom_add_colloc_comm_pairs+temp_colloc_comm_pairs) + ")");
					bestF = tempF;
					
					best_devil_pairs = fathom_add_devil_pairs+temp_devil_pairs;
					best_powered_up_nodes = fathom_add_powered_up_nodes+temp_powered_up_nodes;
					best_colloc_comm_pairs = fathom_add_colloc_comm_pairs+temp_colloc_comm_pairs;
					
					lastFathomedPotentialF = minPotentialF;
				}
			}
			
			if (   minPotentialF < (bestF-Math.abs(bestF)*pruneFactor)   ) return 0;
		}
		
		return 1;
	}

	
	
	
	public boolean isVarStartsWith(IntDomainVar var, String beginstr) {
		Object temp0bj = var;
		String tempstr = temp0bj + ".";
		if (tempstr.startsWith(beginstr))
		{
			return true;
		}
		return false;
	}

	public int getContainerIndex(Object var) {
		Object temp0bj = var;
		String tempstr = temp0bj + ".";
		if (tempstr.startsWith("v"))
		{
			return Integer.parseInt(tempstr.substring(2, tempstr.indexOf(':')));
		}
		else
		{
			return -1; 
		}
	}


	
	
	public boolean isContainerDevil(int index) {
		return container_classes[index] == 1;
	}


	public boolean isContainerCommunicating(int index) {
		return communicating_containers[index] == 1;
	}


	public boolean areContainersCommunicating(int index1, int index2) {
		return communication_matrix[index1][index2] == 1 || communication_matrix[index2][index1] == 1;
	}

	public boolean canContainersBeOnSameNode(int index1, int index2) {
		return assigned_containers[index1] < 0 || assigned_containers[index2] < 0;
	}
	
	
	
	
	

	// categories:
	// 0 - isolated
	// 1 - non-comm turtle
	// 2 - comm turtle
	// 3 - comm with that one turtle
	// 4 - non-comm devil
	// 5 - comm devil
	// 6 - comm with that one devil
	public int getNeighbouringContainerCategory(int neighbouring_container_index, int container_index) {
		if (neighbouring_container_index < 0 || container_index < 0)
			return 0;

		if (isContainerDevil(neighbouring_container_index))
		{
			if (isContainerCommunicating(neighbouring_container_index))
			{
				if (areContainersCommunicating(neighbouring_container_index, container_index))
				{
					return 6;
				}
				return 5;
			}
			return 4;
		}
		else
		{
			if (isContainerCommunicating(neighbouring_container_index))
			{
				if (areContainersCommunicating(neighbouring_container_index, container_index))
				{
					return 3;
				}
				return 2;
			}
			return 1;
		}
	}


	public void initBranching()
	{
	}

	/**
	 * Select the variable to be constrained
	 *
	 * @return the branching object
	 */
	public Object selectBranchingObject() throws ContradictionException {
		int container_index = -1;
		int biggest_class = -1;
		IntDomainVar chosenVar = null;
		
		if (branching_level < number_of_containers)
		{
			chosenVar = reordered_containers[branching_level];
			if (chosenVar.isInstantiated())
			{
		    	System.out.println("Error: chosenVar.isInstantiated()");
			}
		}
		else
		{
			if (!_vars[total_powered_up_nodes_index].isInstantiated())
				chosenVar = _vars[total_powered_up_nodes_index];
			else if (!_vars[total_devil_pairs_index].isInstantiated())
				chosenVar = _vars[total_devil_pairs_index];
			else if (!_vars[total_colloc_comm_pairs_index].isInstantiated())
				chosenVar = _vars[total_colloc_comm_pairs_index];
		}

		return chosenVar;
	}

		
		
		
	/**
	 * Select the first value to assign, and set it in the decision object in parameter
	 *
	 * @param decision the first decision to apply
	 */
	public final void setFirstBranch(final IntBranchingDecision ctx) {
		IntDomainVar var = ctx.getBranchingIntVar();
		if (isVarStartsWith(var, "v"))
		{
			generateFundDistinctValues(var);
		}
		ctx.setBranchingValue(getUniqueVal(var));
	}

	/**
	 * Select the next value to assign, and set it in the decision object in parameter
	 *
	 * @param decision the next decision to apply
	 */
	public void setNextBranch(final IntBranchingDecision ctx) {
		IntDomainVar var = ctx.getBranchingIntVar();
		ctx.setBranchingValue(getUniqueVal(var));
	}



	// get the neighbouring slot
	public int getPair(int slot) {
		if (slot % 2 == 0) return slot + 1;
		else return slot - 1;
	}

	
	
	
	// fill the node with processes/containers (for bounding function calculation)
	// process == -2 if it is non-existent
	public int fillTheNode(int p1, int p2) {
		int slot = -1, slot2 = -1, pair_slot = -1;
		
		if (p1 >= 0) slot = fathom_assigned_containers[p1]; 
		if (p2 >= 0) slot2 = fathom_assigned_containers[p2]; 
		// cannot be assigned at the same time:
		if (slot >= 0 && slot2 >= 0) System.out.println("error on branching_level " + branching_level + ": two assigned containers to be paired again: " + p1 + " " + p2 + "   " + slot + " " + slot2);

		if (slot >= 0) // partially filled
		{
			pair_slot = getPair(slot);
			if (p2 >= 0)
			{
				fathom_assigned_slots[pair_slot] = p2;
				fathom_assigned_containers[p2] = pair_slot;
			}
		}
		else if (slot2 >= 0) // partially filled
		{
			pair_slot = getPair(slot2);
			if (p1 >= 0)
			{
				fathom_assigned_slots[pair_slot] = p1;
				fathom_assigned_containers[p1] = pair_slot;
			}
		}
		else // both processes are unassigned yet
		{
			// in search for the next idle node:
			for (slot = cur_phatom_empty_slot; slot < fathom_assigned_slots.length; slot+=2) {
				pair_slot = getPair(slot);
				if (fathom_assigned_slots[slot] == -1 && fathom_assigned_slots[pair_slot] == -1)
				{
					if (p1 >= 0){
						fathom_assigned_slots[slot] = p1;
						fathom_assigned_containers[p1] = slot;
					}
					if (p2 >= 0){
						fathom_assigned_slots[pair_slot] = p2;
						fathom_assigned_containers[p2] = pair_slot;
					}
					cur_phatom_empty_slot = slot + 2;
					break;
				}
			}
		}
		
		return 0;
	}

	
	
	
	
	
	
	
	
	
	
	// 777
	public int findPhatomPair(ArrayList[] not_in_pair_classes1, ArrayList[] not_in_pair_classes2, int type, int pairs_limit, int with_comm, int in_pair) {
		int i, j, p1, p2, pairs_found = 0, jobid;
		
		if (pairs_limit == 0) return 0;
		
		jobid = 0;
		while (jobid < number_of_jobs){
			i = 0;
			while (i < not_in_pair_classes1[jobid].size()){
				if (in_pair == 1){
					j = 0;
					while (j < not_in_pair_classes2[jobid].size()){
						if (not_in_pair_classes1 != not_in_pair_classes2 || i != j){
							p1 = (Integer)not_in_pair_classes1[jobid].get(i);
							p2 = (Integer)not_in_pair_classes2[jobid].get(j);
							if (   canContainersBeOnSameNode(p1, p2)   &&   (areContainersCommunicating(p1, p2) || with_comm == 0)   ){
								pairs_found++;
								pairs_limit--;
								fillTheNode(p1, p2);
								not_in_pair_classes1[jobid].remove((Object)(new Integer(p1)));
								not_in_pair_classes2[jobid].remove((Object)(new Integer(p2)));
								
								if (pairs_limit == 0) return pairs_found;
								if (i >= not_in_pair_classes1[jobid].size()) break;
								else { j = 0; continue; }
							}
						}
						j++;
					}
				}
				else {
					p1 = (Integer)not_in_pair_classes1[jobid].get(i);
					p2 = -2;
					pairs_found++;
					pairs_limit--;
					fillTheNode(p1, p2);
					not_in_pair_classes1[jobid].remove((Object)(new Integer(p1)));

					if (pairs_limit == 0) return pairs_found;
					if (i == not_in_pair_classes1[jobid].size()) break;
					else continue;
				}
				i++;
			}
				
			jobid++;
		}
		
		return pairs_found;
	}

	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	public int findPhatomPair2(ArrayList[] not_in_pair_classes1, ArrayList[] not_in_pair_classes2, int type, int pairs_limit, int with_comm, int in_pair) {
		int i, j, k, p1, p2, pairs_found = 0, jobid, smallest_pairs, smallest_paired;
		
		if (pairs_limit == 0) return 0;

		if (with_comm == 1)
		{
			// copy
			for(k = 0; k < number_of_jobs; k++){
				copy_potential_communicating_processes[k].clear();
				copy_potential_communicating_processes[k] = new ArrayList<Integer>(potential_communicating_processes[branching_level][type][k]);
			}

			for(k = 0; k < number_of_jobs; k++){
				for(i = 0; i < copy_potential_communicating_processes[k].size(); i++){
					p1 = (Integer)copy_potential_communicating_processes[k].get(i);
					copy_potential_processes_comm_with[p1].clear();
					copy_potential_processes_comm_with[p1] = new ArrayList<Integer>(potential_processes_comm_with[branching_level][type][p1]);
				}
			}
			
			// clean up those processes that were already assigned when type was 0 and 1:
			if (type == 2)
			{
				Object pobj;
				jobid = 0;
				while (jobid < number_of_jobs){
					i = 0;
					while (i < copy_potential_communicating_processes[jobid].size()){
						pobj = copy_potential_communicating_processes[jobid].get(i);
						
						if (!not_in_pair_classes1[jobid].contains(pobj) && !not_in_pair_classes2[jobid].contains(pobj))
						{
							removeCopyLinks(type, (Integer)pobj, jobid);
							i = 0;
						}
						else i++;
					}
					jobid++;
				}
			}

			

			jobid = 0;
			while (jobid < number_of_jobs){
				// find the process with the smallest pairs - and then pair it first
				smallest_pairs = 10000000;
				smallest_paired = -1;
				for(i = 0; i < copy_potential_communicating_processes[jobid].size(); i++){
					p1 = (Integer)copy_potential_communicating_processes[jobid].get(i);
					j = copy_potential_processes_comm_with[p1].size();
					if (j < smallest_pairs)
					{
						smallest_pairs = j;
						smallest_paired = p1;
						if (j == 1)
							break;
					}
				}
				
				if (smallest_paired >= 0)
				{
					p1 = smallest_paired;
					p2 = (Integer)copy_potential_processes_comm_with[p1].get(0);
					
					removeCopyLinks(type, p1, jobid);
					removeCopyLinks(type, p2, jobid);
					
					pairs_found++;
					pairs_limit--;
					fillTheNode(p1, p2);
					
					not_in_pair_classes1[jobid].remove((Object)(new Integer(p1)));
					not_in_pair_classes2[jobid].remove((Object)(new Integer(p2)));
					if (type == 2)
					{
						// not sure which one here
						not_in_pair_classes1[jobid].remove((Object)(new Integer(p2)));
						not_in_pair_classes2[jobid].remove((Object)(new Integer(p1)));
					}
					
					if (pairs_limit == 0) return pairs_found;
					
					continue;
				}
				
				jobid++; // if got to here then no more pairs can be found within this job 
			}
		}
		else
		{
			jobid = 0;
			while (jobid < number_of_jobs){
				i = 0;
				while (i < not_in_pair_classes1[jobid].size()){
					if (in_pair == 1){
						j = 0;
						while (j < not_in_pair_classes2[jobid].size()){
							if (not_in_pair_classes1 != not_in_pair_classes2 || i != j){
								p1 = (Integer)not_in_pair_classes1[jobid].get(i);
								p2 = (Integer)not_in_pair_classes2[jobid].get(j);
								if (canContainersBeOnSameNode(p1, p2)){
									pairs_found++;
									pairs_limit--;
									fillTheNode(p1, p2);
									not_in_pair_classes1[jobid].remove((Object)(new Integer(p1)));
									not_in_pair_classes2[jobid].remove((Object)(new Integer(p2)));
									
									if (pairs_limit == 0) return pairs_found;
									if (i >= not_in_pair_classes1[jobid].size()) break;
									else { j = 0; continue; }
								}
							}
							j++;
						}
					}
					else {
						p1 = (Integer)not_in_pair_classes1[jobid].get(i);
						p2 = -2;
						pairs_found++;
						pairs_limit--;
						fillTheNode(p1, p2);
						not_in_pair_classes1[jobid].remove((Object)(new Integer(p1)));

						if (pairs_limit == 0) return pairs_found;
						if (i == not_in_pair_classes1[jobid].size()) break;
						else continue;
					}
					i++;
				}
					
				jobid++;
			}
		}
		
		return pairs_found;
	}
	
	
	

	public void removeCopyLinks(int type, int p1, int job_p1) {
		int p2;
		
		while (copy_potential_processes_comm_with[p1].size() > 0){
			p2 = (Integer)copy_potential_processes_comm_with[p1].get(0);
			
			copy_potential_processes_comm_with[p1].remove((Object)(new Integer(p2)));
			copy_potential_processes_comm_with[p2].remove((Object)(new Integer(p1)));
			
			// check if we need to remove it from copy_potential_communicating_processes too:
			if (copy_potential_processes_comm_with[p1].size() == 0)
				copy_potential_communicating_processes[job_p1].remove((Object)(new Integer(p1)));

			if (copy_potential_processes_comm_with[p2].size() == 0)
				copy_potential_communicating_processes[job_p1].remove((Object)(new Integer(p2)));
		}
	}

	
	
	
	
	

	// generate fully once at the very beginning
	public void generatePotentialCommPairs(ArrayList[] not_in_pair_classes1, ArrayList[] not_in_pair_classes2, int type) {
		int i, j, p1, p2, jobid;
		
		jobid = 0;
		while (jobid < number_of_jobs){
			i = 0;
			while (i < not_in_pair_classes1[jobid].size()){
				if (not_in_pair_classes1 != not_in_pair_classes2) j = 0;
				else j = i;

				p1 = (Integer)not_in_pair_classes1[jobid].get(i);

				while (j < not_in_pair_classes2[jobid].size()){
					if (not_in_pair_classes1 != not_in_pair_classes2 || i != j){
						p2 = (Integer)not_in_pair_classes2[jobid].get(j);
						if (   areContainersCommunicating(p1, p2)   ){
							potential_communicating_processes[0][type][jobid].add(p1);
							potential_communicating_processes[0][type][jobid].add(p2);
							
							potential_processes_comm_with[0][type][p1].add(p2);
							potential_processes_comm_with[0][type][p2].add(p1);
						}
					}
					j++;
				}
				i++;
			}
			
			jobid++;
		}
		
		
		jobid = 0;
		while (jobid < number_of_jobs){
			// remove duplicates:
			HashSet hs = new HashSet<Integer>();
			hs.addAll(potential_communicating_processes[0][type][jobid]);
			potential_communicating_processes[0][type][jobid].clear();
			potential_communicating_processes[0][type][jobid].addAll(hs);
			jobid++;
		}
	}

	
	
	public int updatePotentialCommPair(int type) {
		int i, j, k, p1, p2, pairs_found, jobid, job_p1, job_p2, prev_branching_level = branching_level-1;
		
		// copy to the new level from the previous one first
		for(k = 0; k < number_of_jobs; k++){
			potential_communicating_processes[branching_level][type][k].clear();
			potential_communicating_processes[branching_level][type][k] = new ArrayList<Integer>(potential_communicating_processes[prev_branching_level][type][k]);
		}
		
		for(k = 0; k < number_of_jobs; k++){
			for(i = 0; i < potential_communicating_processes[branching_level][type][k].size(); i++){
				p1 = (Integer)potential_communicating_processes[branching_level][type][k].get(i);
				potential_processes_comm_with[branching_level][type][p1].clear();
				potential_processes_comm_with[branching_level][type][p1] = new ArrayList<Integer>(potential_processes_comm_with[prev_branching_level][type][p1]);
			}
		}
		
		
		// incrementally update:
		if (latest_assigned_pair_container < 0) // the branched process landed on a previously idle node
		{
			removeLinks(type, latest_assigned_container, container_job_ids[latest_assigned_container], 0);
		}
		else // the branched process landed on a partially filled node
		{
			removeLinks(type, latest_assigned_container, container_job_ids[latest_assigned_container], 1);
			removeLinks(type, latest_assigned_pair_container, container_job_ids[latest_assigned_pair_container], 1);
		}
		
		
		
		pairs_found = 0; jobid = 0;
		while (jobid < number_of_jobs){
			pairs_found += potential_communicating_processes[branching_level][type][jobid].size() / 2;
			jobid++;
		}
		
		
		

		return pairs_found;
	}

	
	
	
	
	
	// complete == 0: remove only the links that connect the given provess with other processes that are assigned alone on their nodes
	// complete == 1: remove all the mentioning of the two processes
	public void removeLinks(int type, int p1, int job_p1, int complete) {
		int i, p2;
		
		i = 0;
		while (i < potential_processes_comm_with[branching_level][type][p1].size()){
			p2 = (Integer)potential_processes_comm_with[branching_level][type][p1].get(i);
			
			if (canContainersBeOnSameNode(p1, p2) == false || complete == 1){
				potential_processes_comm_with[branching_level][type][p1].remove((Object)(new Integer(p2)));
				potential_processes_comm_with[branching_level][type][p2].remove((Object)(new Integer(p1)));
				
				// check if we need to remove it from potential_communicating_processes too:
				if (potential_processes_comm_with[branching_level][type][p1].size() == 0)
					potential_communicating_processes[branching_level][type][job_p1].remove((Object)(new Integer(p1)));

				if (potential_processes_comm_with[branching_level][type][p2].size() == 0)
					potential_communicating_processes[branching_level][type][job_p1].remove((Object)(new Integer(p2)));
				
				continue;
			}
			i++;
		}
	}

	
	
	
	
	
	
	
	
	
	
	// get the neighbouring container index
	public int getPairContainerIndex(int slot) {
		return assigned_slots[getPair(slot)];
		
		/*
		int pair_slot = getPair(slot);

		int pair_container_index = -1;
		int container_index = -1;
		for (IntDomainVar var : _vars) {
			container_index = getContainerIndex(var);
			if (container_index >=0 && var.isInstantiated())
			{
				if (var.getVal() == pair_slot)
				{
					pair_container_index = container_index;
					break;
				}
			}
		}

		return pair_container_index;
		*/
	}


	
	
	
	// categories:
	// 0 - isolated
	// 1 - non-comm turtle
	// 2 - comm turtle
	// 3 - comm with that one turtle
	// 4 - non-comm devil
	// 5 - comm devil
	// 6 - comm with that one devil

	// for turtles:
	int[][] turtle_fund_priorities = {
			{6, 4, 5, 3, 1, 2, 0}, // pdc 0
			{6, 3, 4, 1, 5, 2, 0}, // pcd 1
			{6, 4, 5, 3, 1, 2, 0}, // dpc 2
			{6, 4, 5, 3, 1, 2, 0}, // dcp 3
			{6, 3, 4, 1, 0, 5, 2}, // cpd 4
			{6, 3, 4, 1, 0, 5, 2} // cdp 5
	};

	// for devils:
	int[][] devil_fund_priorities = {
			{3, 1, 2, 6, 4, 5, 0}, // pdc 0
			{3, 6, 1, 4, 2, 5, 0}, // pcd 1
			{3, 1, 2, 0, 6, 4, 5}, // dpc 2
			{3, 1, 2, 0, 6, 4, 5}, // dcp 3
			{3, 6, 1, 4, 0, 2, 5}, // cpd 4
			{3, 6, 1, 4, 0, 2, 5} // cdp 5
	};

	public void generateFundDistinctValues(IntDomainVar var) {
		int container_index = getContainerIndex(var);

		if (USE_FUND_DISTINCT == 0)
		{
			int i;
			next_value_to_take_index[container_index] = assigned_slots.length;
			for (i = 0; i < assigned_slots.length; i++) {
				if (assigned_slots[i] < 0/* && var.canBeInstantiatedTo(i)*/)
				{
					next_value_to_take_index[container_index] = i;
					break;
				}
			}
			return;
		}
		else
		{
			int i, i2, j, k;
			int pair_category;

			int[] temp_values = new int[fund_distinct_values[container_index].length];
			for (i = 0; i < fund_distinct_values[container_index].length; i++) {
				fund_distinct_values[container_index][i] = -1;
				temp_values[i] = -1;
			}
			
			// categories:
			// 0 - isolated
			// 1 - non-comm turtle
			// 2 - comm turtle
			// 3 - comm with that one turtle
			// 4 - non-comm devil
			// 5 - comm devil
			// 6 - comm with that one devil
			int[][] categories_slots = new int[7][number_of_nodes];
			for (i = 0; i < 7; i++) {
				for (j = 0; j < number_of_nodes; j++) {
					categories_slots[i][j] = -1;
				}
			}
			int[] categories_slots_first_avail = new int[7];
			for (i = 0; i < 7; i++) {
				categories_slots_first_avail[i] = 0;
			}
			
			
			int rightmost_assigned_slot_and_one_idle;
			if (rightmost_assigned_slot >= (assigned_slots.length-3)/*space of one idle node*/) rightmost_assigned_slot_and_one_idle = assigned_slots.length-1;
			else rightmost_assigned_slot_and_one_idle = rightmost_assigned_slot+2;

			for (i = 0; i <= rightmost_assigned_slot_and_one_idle; i++) {
			//for (i = 0; i < assigned_slots.length; i++) {
				if (assigned_slots[i] < 0/* && var.canBeInstantiatedTo(i)*/)
				{
					// what is collocated on the neighbouring slot and what is the category?
					pair_category = getNeighbouringContainerCategory(getPairContainerIndex(i), container_index);
					
					if (pair_category == 0 || pair_category == 1 || pair_category == 4)
					{
						if (categories_slots_first_avail[pair_category] != 0) 
						{
							if (pair_category == 0) i++;
							continue;
						}
					}

					categories_slots[pair_category][   categories_slots_first_avail[pair_category]++   ] = i;
					if (pair_category == 0) i++;
				}
			}
			
			
			if (USE_FUND_PRIO == 0) // just dump all the fund dist slots into fund_distinct_values and then shuffle it
			{
				k = 0;
				for (i = 0; i < 7; i++) {
					for (j = 0; j < categories_slots_first_avail[i]; j++) {
						temp_values[k++] = categories_slots[i][j];
					}
				}
				
				for(i = 0; i < fund_distinct_values[container_index].length; i++)
			    {
					fund_distinct_values[container_index][i] = temp_values[i];
				}
			}
			else
			{
				k = 0;
				if (isContainerDevil(container_index))
				{
					for (i = 0; i < 7; i++) {
						i2 = devil_fund_priorities[weight_index][i];
						
						for (j = 0; j < categories_slots_first_avail[i2]; j++) {
							fund_distinct_values[container_index][k++] = categories_slots[i2][j];
						}
					}
				}
				else
				{
					for (i = 0; i < 7; i++) {
						i2 = turtle_fund_priorities[weight_index][i];
						
						for (j = 0; j < categories_slots_first_avail[i2]; j++) {
							fund_distinct_values[container_index][k++] = categories_slots[i2][j];
						}
					}
				}
			}
			

			next_value_to_take_index[container_index] = fund_distinct_values[container_index].length;
			for (i = 0; i < fund_distinct_values[container_index].length; i++) {
				if (fund_distinct_values[container_index][i] >= 0)
				{
					next_value_to_take_index[container_index] = i;
					break;
				}
			}
			
			
			//long cur_timestamp2 = System.currentTimeMillis();
	    	//System.out.println("duration in ms: " + (cur_timestamp2-cur_timestamp));
		}
	}


	
	
	
	
	
	public int getUniqueVal(IntDomainVar var) {
		if (isVarStartsWith(var, "tp"))
		{
			return temp_powered_up_nodes;
		}
		else if (isVarStartsWith(var, "td"))
		{
			return temp_devil_pairs;
		}
		else if (isVarStartsWith(var, "tc"))
		{
			return temp_colloc_comm_pairs;
		}
		else if (isVarStartsWith(var, "v"))
		{
			int container_index = getContainerIndex(var);
			int old_next = next_value_to_take_index[container_index];

			if (USE_FUND_DISTINCT == 0)
			{
				int i;
				next_value_to_take_index[container_index] = assigned_slots.length;
				for (i = (old_next+1); i < assigned_slots.length; i++) {
					if (assigned_slots[i] < 0/* && var.canBeInstantiatedTo(i)*/)
					{
						next_value_to_take_index[container_index] = i;
						break;
					}
				}
				return old_next;
			}
			else
			{
				if (old_next >= fund_distinct_values[container_index].length)
					return -1;

				next_value_to_take_index[container_index]++;
				while (next_value_to_take_index[container_index] < fund_distinct_values[container_index].length)
				{
					if (fund_distinct_values[container_index][next_value_to_take_index[container_index]] >= 0)
					{
						break;
					}
					next_value_to_take_index[container_index]++;
				}

				return fund_distinct_values[container_index][old_next];
			}
		}
		return -1;
	}

	
	

	
	
	
	
	public void finalOutput() {
		float tree_levels_explored = (float)(1.0);
		tree_levels_explored -= (float)((float)smallest_level_with_finished_branching/(float)number_of_containers);
		System.out.println("tree levels explored: " + tree_levels_explored + " tree_nodes_evaluated: " + tree_nodes_evaluated + " tree_nodes_pruned: " + tree_nodes_pruned + " smallest_level_with_finished_branching: " + smallest_level_with_finished_branching);

		myHPCScheduling.logString(best_devil_pairs + " " + best_powered_up_nodes + " " + best_colloc_comm_pairs + " " + bestF + " " + tree_nodes_evaluated + " " + tree_nodes_pruned + " " + tree_levels_explored);
		
		begin_epoch_in_ms = 0;
	}	
	
	

	
	
	
	
	
	
	public boolean finishedBranching(final IntBranchingDecision ctx) {
		IntDomainVar var = ctx.getBranchingIntVar();
		boolean outcome = false;
		
		if (isVarStartsWith(var, "tc"))
		{
			float tempF = temp_devil_pairs*d + temp_powered_up_nodes*p - temp_colloc_comm_pairs*c;
			if (tempF < bestF)
			{
				System.out.println("Found at leaf: " + tempF + " prev: " + bestF + " (d p c: " + temp_devil_pairs + " " + temp_powered_up_nodes + " " + temp_colloc_comm_pairs + ")");
				bestF = tempF;

				best_devil_pairs = temp_devil_pairs;
				best_powered_up_nodes = temp_powered_up_nodes;
				best_colloc_comm_pairs = temp_colloc_comm_pairs;
			}

			outcome = true;
		}
		else if (isVarStartsWith(var, "t"))
		{
			outcome = true;
		}
		else if (isVarStartsWith(var, "v"))
		{
			int container_index = getContainerIndex(var);
			if (USE_FUND_DISTINCT == 0)
			{
				outcome = next_value_to_take_index[container_index] >= assigned_slots.length;
			}
			else
			{
				outcome = next_value_to_take_index[container_index] >= fund_distinct_values[container_index].length;
			}
			
			
			if (outcome)
			{
				if (branching_level < smallest_level_with_finished_branching  || smallest_level_with_finished_branching < 0)
				{
					smallest_level_with_finished_branching = branching_level;
				}

				if (branching_level == 0)
				{
					finalOutput();
				}
			}
		}
		
		//if (outcome) System.out.println("branching finished (" + ctx.getBranchingIntVar() + ")");

		return outcome;
	}


	/**
	 * Apply the computed decision building the i^th branch.
	 * --> assignment: the variable is instantiated to the value
	 *
	 *
	 * @param decision the decision to apply.
	 * @throws ContradictionException if the decision leads to an incoherence
	 */
	@Override
	public void goDownBranch(final IntBranchingDecision ctx) throws ContradictionException {
		IntDomainVar var = ctx.getBranchingIntVar();
		int val = ctx.getBranchingValue();
		int i;
		try {
			if (isVarStartsWith(var, "v"))
			{
				branching_level++;
				tree_nodes_evaluated++;

				long cur_epoch_in_ms = System.currentTimeMillis();
				if (   (cur_epoch_in_ms-begin_epoch_in_ms) > TIME_LIMIT_IN_MS  &&  begin_epoch_in_ms > 0   )
				{
					finalOutput();
				}
			
				//System.out.println("branching_level after down (" + ctx.getBranchingIntVar() + "): " + branching_level);

				
				
				
				
				int container_index = getContainerIndex(var);
				int pair_container_index = getPairContainerIndex(val);

				latest_assigned_container = container_index;
				latest_assigned_pair_container = pair_container_index;
						
				if (isContainerDevil(container_index)) temp_unassigned_devils--;
				else temp_unassigned_turtles--;
				
				if (pair_container_index < 0)
				{
					temp_powered_up_nodes++;
					temp_idle_nodes--;

					if (isContainerDevil(container_index))
					{
						temp_idle_slots_with_devil++;
					}
					else
					{
						temp_idle_slots_with_turtle++;
					}
				}
				else
				{
					if (!isContainerDevil(container_index)) not_in_pair_turtles[   container_job_ids[container_index]   ].remove((Object)(new Integer(container_index)));
					else not_in_pair_devils[   container_job_ids[container_index]   ].remove((Object)(new Integer(container_index)));

					if (!isContainerDevil(pair_container_index)) not_in_pair_turtles[   container_job_ids[pair_container_index]   ].remove((Object)(new Integer(pair_container_index)));
					else not_in_pair_devils[   container_job_ids[pair_container_index]   ].remove((Object)(new Integer(pair_container_index)));

					int pair_category = getNeighbouringContainerCategory(pair_container_index, container_index);

					if (isContainerDevil(container_index) && pair_category >= 4) temp_devil_pairs++;

					if (pair_category == 3 || pair_category == 6)
						temp_colloc_comm_pairs++;

					if (isContainerDevil(pair_container_index)) temp_idle_slots_with_devil--;
					else temp_idle_slots_with_turtle--;
				}

				assigned_slots[val] = container_index;
				assigned_containers[container_index] = val; // must be here, not after calculatePotentialFbounding!!!
				
				if (val > rightmost_assigned_slot) rightmost_assigned_slot = val;

				
				
				if (   branching_level < number_of_containers && (USE_NAIVE_F == 1 || USE_POTENTIAL_F == 1)    )
				{
					if (   (USE_NAIVE_F == 1 && calculatePotentialFnaive() == 1) || (USE_POTENTIAL_F == 1 && calculatePotentialFbounding() == 1)   )
					{
						tree_nodes_pruned++;
						var.setVal(Integer.MAX_VALUE);
					}
					else
					{
						var.setVal(val);
					}
				}
				else
				{
					var.setVal(val);
				}
			}
			else
			{
				var.setVal(val);
			}

			//_solver.propagate();
		} catch (ContradictionException e) {
			//System.out.println("branching contradiction (" + ctx.getBranchingIntVar() + ") value: "+val);
			throw e;
		}
	}

	/**
	 * Reconsider the computed decision, destroying the i^th branch
	 *
	 * @param decision the decision that has been set at the father choice point
	 * @throws ContradictionException if the non-decision leads to an incoherence
	 */
	@Override
	public void goUpBranch(final IntBranchingDecision ctx) throws ContradictionException {
		IntDomainVar var = ctx.getBranchingIntVar();

		if (isVarStartsWith(var, "v"))
		{
			int container_index = getContainerIndex(var);
			int val = ctx.getBranchingValue();
			int pair_container_index = getPairContainerIndex(val);
			
			if (isContainerDevil(container_index)) temp_unassigned_devils++;
			else temp_unassigned_turtles++;
			
			if (pair_container_index < 0)
			{
				temp_powered_up_nodes--;
				temp_idle_nodes++;
				
				if (isContainerDevil(container_index))
				{
					temp_idle_slots_with_devil--;
				}
				else
				{
					temp_idle_slots_with_turtle--;
				}
			}
			else
			{
				if (!isContainerDevil(container_index)) not_in_pair_turtles[   container_job_ids[container_index]   ].add(container_index);
				else not_in_pair_devils[   container_job_ids[container_index]   ].add(container_index);

				if (!isContainerDevil(pair_container_index)) not_in_pair_turtles[   container_job_ids[pair_container_index]   ].add(pair_container_index);
				else not_in_pair_devils[   container_job_ids[pair_container_index]   ].add(pair_container_index);
				
				int pair_category = getNeighbouringContainerCategory(pair_container_index, container_index);

				if (isContainerDevil(container_index) && pair_category >= 4)
					temp_devil_pairs--;

				if (pair_category == 3 || pair_category == 6)
					temp_colloc_comm_pairs--;

				if (isContainerDevil(pair_container_index)) temp_idle_slots_with_devil++;
				else temp_idle_slots_with_turtle++;
			}
			
			if (val >=0 && val < assigned_slots.length)
			{
				assigned_slots[val] = -1;
				assigned_containers[container_index] = -1;

				if (val == rightmost_assigned_slot)
				{
					rightmost_assigned_slot = -1;
					for (int i = (val-1); i >= 0; i--) {
						if (assigned_slots[i] >= 0)
						{
							rightmost_assigned_slot = i;
							break;
						}
					}
				}
			}
			
			branching_level--;

			//System.out.println("branching_level after up (" + ctx.getBranchingIntVar() + "): " + branching_level);
		}

		//ctx.remIntVal();
	}

	/**
	 * Create and return the message to print, in case of strong verbosity
	 * @param decision current decision
	 * @return pretty print of the current decision
	 */
	@Override
	public String getDecisionLogMessage(IntBranchingDecision decision) {
		String outputstr = "";
		
		outputstr += decision.getBranchingObject() + "==" + decision.getBranchingValue() + "   ";
		int container_index = getContainerIndex(decision.getBranchingObject());
		if (container_index >= 0)
		{
			outputstr += "temp_idle_nodes: " + temp_idle_nodes + " bestF: " + bestF + "; potentialF: " + potentialF + " (" + "p " + temp_powered_up_nodes + ", d " + temp_devil_pairs + ", c " + temp_colloc_comm_pairs + "; pp " + temp_potential_powered_up_nodes + ", pd " + temp_potential_devil_pairs + ", pc " + temp_potential_colloc_comm_pairs + "). ";
		}

		//outputstr += "temp_powered_up_nodes " + temp_powered_up_nodes + "   ";
		//outputstr += getContainerIndex(_vars[total_powered_up_nodes_index]) + "   ";
		//outputstr += getDefaultAssignMsg(decision) + "   ";


		//int container_index = getContainerIndex(decision.getBranchingObject());
		/*if (container_index >= 0)
		{
			int i;
			outputstr += "container_index:   " + container_index;

			outputstr += "fund_distinct_values: ";
			for (i = 0; i < fund_distinct_values[container_index].length; i++) {
				outputstr += fund_distinct_values[container_index][i] + ",";
			}

			//outputstr += "assigned_slots: ";
			//for (i = 0; i < assigned_slots.length; i++) {
			//	outputstr += assigned_slots[i] + ",";
			//}

			//outputstr += "next_value_to_take_index[container_index]:   " + next_value_to_take_index[container_index] + "   ";

			//outputstr += " and " + fund_distinct_values_taken_index[container_index];

			//outputstr += "||" + fraction_d_index + "|" + fraction_p_index + "|" + fraction_c_index  + "||" + fraction_d + "|" + fraction_p + "|" + fraction_c;

			//outputstr += "   number_of_containers " + number_of_containers;
		}*/

		outputstr += outputstr + "\r\n\r\n";
		


		return outputstr;
	}
}
