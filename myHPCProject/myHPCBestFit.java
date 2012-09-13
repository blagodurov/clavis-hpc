package myHPCProject;

import choco.cp.solver.constraints.global.pack.PackSConstraint;
import choco.kernel.common.util.iterators.DisposableIntIterator;
import choco.kernel.solver.search.ValSelector;
import choco.kernel.solver.variables.integer.IntDomainVar;

public class myHPCBestFit implements ValSelector<IntDomainVar> {

	public final PackSConstraint pack;

	public final int d;
	public final int p;
	public final int number_of_nodes;
	public final int number_of_containers;
	public final int number_of_devils;
	public final int[] container_devils;
	
	public int unassigned_devils;
	public int unassigned_turtles;
	public int idle_slots_with_devil;
	public int idle_slots_with_turtle;
	
	public int start_from;

	
	
	public myHPCBestFit(PackSConstraint cstr, int in_d, int in_p, int in_number_of_nodes, int in_number_of_containers, int in_number_of_devils, int[] in_container_devils) {
		super();
		this.pack = cstr;
		
		this.d = in_d;
		this.p = in_p;
		this.number_of_nodes = in_number_of_nodes;
		this.number_of_containers = in_number_of_containers;
		this.number_of_devils = in_number_of_devils;
		this.container_devils = in_container_devils;
	}

	
	
	public int getContainerIndex(Object var) {
		Object temp0bj = var;
		String tempstr = temp0bj + ".";
		if (tempstr.startsWith("bins"))
		{
			return Integer.parseInt(tempstr.substring(5, tempstr.indexOf(':')));
		}
		else
		{
			return -1; 
		}
	}

	
	
	public boolean containerIsDevil(int index, boolean use_start_from) {
		int i0 = 0;
		if (use_start_from) i0 = start_from;
		for (int i = i0; i < container_devils.length; i++) {
			if (container_devils[i] == index)
			{
				start_from = i+1;
				return true;
			}
			if (container_devils[i] > index)
			{
				start_from = i;
				break;
			}
		}
		return false;
	}

	
	
	public void countUnassignedClasses() {
		unassigned_devils = 0;
		unassigned_turtles = 0;
		IntDomainVar[] bins = pack.getBins();
		start_from = 0;
		for (int i = 0; i < bins.length; i++) {
			if (!bins[i].isInstantiated())
			{
				final int container_index = getContainerIndex(bins[i]);
				if (containerIsDevil(container_index, true)) unassigned_devils++;
				else unassigned_turtles++;
			}
		}
	}

	
	
	public int getPairContainerIndex(int bin) {
		int index = -1;
		final DisposableIntIterator iter= pack.svars[bin].getDomain().getKernelIterator();
		if (iter.hasNext()) index = iter.next();
		iter.dispose();
		return index;
	}
	
	
	
	
	
	

	
	

	
	
	
	@Override
	public int getBestVal(IntDomainVar x) {
		final int container_index = getContainerIndex(x);
		final boolean container_is_devil = containerIsDevil(container_index, false);
		
		countUnassignedClasses();
		
		//System.out.println(System.currentTimeMillis());
		
		// 0: with none
		// 1: with devil
		// 2: with turtle
		int[] category_bins = new int[3];
		for (int i = 0; i < category_bins.length; i++) category_bins[i] = -1;

		int backup_b = -1; // in case no empty and no partially filled bins were passed
		idle_slots_with_devil = 0;
		idle_slots_with_turtle = 0;
		
		final DisposableIntIterator iter=x.getDomain().getIterator();
		while(iter.hasNext()) {
			final int b = iter.next();
			if (backup_b < 0) backup_b = b;
			
			// empty
			if (pack.isEmpty(b))
			{
				if (category_bins[0] < 0)
				{
					category_bins[0] = b;
				}
				continue;
			}
			
			// partially filled
			if(!pack.svars[b].isInstantiated() && pack.svars[b].getKernelDomainSize()>0) {
				final int pair_container_index = getPairContainerIndex(b);
				final boolean pair_container_is_devil = containerIsDevil(pair_container_index, false);
				
				if (pair_container_is_devil)
				{
					idle_slots_with_devil++;
					if (category_bins[1] < 0) category_bins[1] = b;
					continue;
				}
				else
				{
					idle_slots_with_turtle++;
					if (category_bins[2] < 0) category_bins[2] = b;
					continue;
				}
			}
		}
        iter.dispose();
        
        
        
		int best_found_bin = -1;
		if (container_is_devil)
		{
			if (category_bins[2] >= 0) best_found_bin = category_bins[2];
			else
			{
				if (p > d)
				{
					if (   (unassigned_turtles - idle_slots_with_devil) > 0 && category_bins[0] >= 0   ) best_found_bin = category_bins[0];
					else if (category_bins[1] >= 0) best_found_bin = category_bins[1];
					else best_found_bin = category_bins[0];
				}
				else
				{
					if (category_bins[0] >= 0) best_found_bin = category_bins[0];
					else best_found_bin = category_bins[1];
				}
			}
		}
		else
		{
			if (category_bins[1] >= 0) best_found_bin = category_bins[1];
			else
			{
				if (   (unassigned_turtles - idle_slots_with_devil - unassigned_devils) > 0 && category_bins[2] >= 0) best_found_bin = category_bins[2];
				else if (category_bins[0] >= 0) best_found_bin = category_bins[0];
				else best_found_bin = category_bins[2];
			}
		}

        if (best_found_bin < 0)
        {
        	//System.out.println("best_found_bin; total_categories_found " + total_categories_found + "; ");
        	best_found_bin = backup_b;
        }
        
        

		return best_found_bin;
	}
}
