package xf.xfvrp.opt.construct.savings;

import xf.xfvrp.base.Quality;
import xf.xfvrp.base.exception.XFVRPException;
import xf.xfvrp.opt.Solution;


/**
 * Copyright (c) 2012-2022 Holger Schneider
 * All rights reserved.
 * <p>
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 * <p>
 * <p>
 * Contains the Savings optimization procedure, where
 * the acceptance criteria is parameterized by the lamda
 * value. Six variants with different lamda values (0.6, 1, 1.4, 1.6, 2, 3)
 * are calculated and best result is taken.
 *
 * @author hschneid
 */
public class XFVRPSavingsLamda extends XFVRPSavings {

    private final float[] lambdaParameters = new float[]{0.6f, 1, 1.4f, 1.6f, 2, 3};

    /*
     * (non-Javadoc)
     * @see de.fhg.iml.vlog.xftour.xfvrp.opt.construct.XFVRPSavings#execute(de.fhg.iml.vlog.xftour.model.XFNode[])
     */
    @Override
    public Solution execute(Solution solution) throws XFVRPException {
        Solution best = super.execute(solution.copy());
        Solution sol;
        Quality quality, bestQuality;

        bestQuality = check(best);

        for (float l : lambdaParameters) {
            this.lambda = l;
            sol = super.execute(solution.copy());
            quality = check(sol);
            if (quality.getFitness() < bestQuality.getFitness()) {
                best = sol;
                bestQuality = quality;
            }
        }

        return best;
    }

}
