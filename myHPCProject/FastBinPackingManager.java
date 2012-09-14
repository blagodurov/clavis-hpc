package myHPCProject;

import choco.cp.model.managers.IntConstraintManager;
import choco.cp.solver.CPSolver;
import choco.kernel.common.util.tools.VariableUtils;
import choco.kernel.model.variables.integer.IntegerVariable;
import choco.kernel.solver.Solver;
import choco.kernel.solver.constraints.SConstraint;
import choco.kernel.solver.variables.integer.IntDomainVar;
import myHPCProject.FastBinPacking;

import java.util.Arrays;
import java.util.List;

/**
 * The manager for the {@link FastBinPacking} constraint.
 * @author Fabien Hermenier
 */
public class FastBinPackingManager extends IntConstraintManager {

    @Override
    public SConstraint makeConstraint(Solver solver, IntegerVariable[] vars, Object o, List<String> strings) {
        if (solver instanceof CPSolver) {
            int []params = (int []) o;
            int nbBins = params[0];
            int nbItems = params[1];
            IntDomainVar [] loads = solver.getVar(Arrays.copyOfRange(vars, 0, nbBins));
            IntDomainVar [] sizes = solver.getVar(Arrays.copyOfRange(vars, nbBins, nbBins + nbItems));
            IntDomainVar [] bins = solver.getVar(Arrays.copyOfRange(vars, nbBins + nbItems, nbBins + nbItems + nbItems));
            return new FastBinPacking(solver.getEnvironment(), loads, sizes, bins);
        }
        return null;
    }
}
