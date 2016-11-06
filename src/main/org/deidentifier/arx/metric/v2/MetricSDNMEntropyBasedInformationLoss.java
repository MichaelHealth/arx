/*
 * ARX: Powerful Data Anonymization
 * Copyright 2012 - 2016 Fabian Prasser, Florian Kohlmayer and contributors
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.deidentifier.arx.metric.v2;

import org.deidentifier.arx.ARXConfiguration;
import org.deidentifier.arx.DataDefinition;
import org.deidentifier.arx.framework.check.groupify.HashGroupify;
import org.deidentifier.arx.framework.check.groupify.HashGroupifyEntry;
import org.deidentifier.arx.framework.data.Data;
import org.deidentifier.arx.framework.data.DataManager;
import org.deidentifier.arx.framework.data.GeneralizationHierarchy;
import org.deidentifier.arx.framework.lattice.Transformation;
import org.deidentifier.arx.metric.InformationLossWithBound;
import org.deidentifier.arx.metric.MetricConfiguration;

/**
 * This class implements a the IL model proposed in:<br>
 * A Game Theoretic Framework for Analyzing Re-Identification Risk.
 * Zhiyu Wan, Yevgeniy Vorobeychik, Weiyi Xia, Ellen Wright Clayton,
 * Murat Kantarcioglu, Ranjit Ganta, Raymond Heatherly, Bradley A. Malin
 * PLOS|ONE. 2015.
 * 
 * 
 * @author Fabian Prasser
 */
public class MetricSDNMEntropyBasedInformationLoss extends AbstractMetricSingleDimensional {

    /** SVUID*/
    private static final long serialVersionUID = -2443537745262162075L;

    /** Domain shares for each dimension. */
    private DomainShare[]                     shares;

    /** MaxIL */
    private double                            maxIL;

    /**
     * Creates a new instance. Default constructor which treats all transformation methods equally.
     */
    public MetricSDNMEntropyBasedInformationLoss() {
        this(0.5d);
    }

    /**
     * Creates a new instance.
     * 
     * @param gsFactor A factor [0,1] weighting generalization and suppression.
     *            The default value is 0.5, which means that generalization
     *            and suppression will be treated equally. A factor of 0
     *            will favor suppression, and a factor of 1 will favor
     *            generalization. The values in between can be used for
     *            balancing both methods.
     */
    public MetricSDNMEntropyBasedInformationLoss(double gsFactor) {
        super(true, false, false, gsFactor);
    }

    @Override
    public ILSingleDimensional createMaxInformationLoss() {
        Double rows = getNumTuples();
        if (rows == null) {
            throw new IllegalStateException("Metric must be initialized first");
        } else {
            return new ILSingleDimensional(rows);
        }
    }

    @Override
    public ILSingleDimensional createMinInformationLoss() {
        return new ILSingleDimensional(0d);
    }

    @Override
    public boolean isGSFactorSupported() {
        return true;
    }

    /**
     * Returns the configuration of this metric.
     * 
     * @return
     */
    public MetricConfiguration getConfiguration() {
        return new MetricConfiguration(false, 
                                       super.getGeneralizationSuppressionFactor(), 
                                       false, 
                                       0.0d, 
                                       this.getAggregateFunction());
    }

    @Override
    public String getName() {
        return "Entropy-based information loss";
    }

    @Override
    public String toString() {
        return "EntropyBasedInformationLoss";
    }

    /**
     * Implements the entropy-based IL model. Ignores record suppression. Returns the loss for exactly one record.
     * @param transformation
     * @param entry
     * @return
     */
    private double getEntropyBasedInformationLoss(Transformation transformation, HashGroupifyEntry entry) {

        // We transform the formula, to make evaluating it more efficient. We have:
        // 
        // [-log( 1 / (share_1 * size_1) ) - log ( 1 / (share_2 * size_2) ) ... - log( 1 / (share_n * size_n) ) ] / maxIL
        //
        // Step 1:
        //
        // [log(share_1 * size_1 ) + log (share_2 * size_2 ) ... + log( share_n * size_n) ] / maxIL
        //
        // Step 2:
        //
        // [log(share_1 * share_2 * ... * share_n) + log(size_1 * size_2 * size_n) ] / maxIL
        //
        // Step 3:
        // 
        // [log(share_1 * share_2 * ... * share_n) + maxIL ] / maxIL
        //
        // Step 4:
        // 
        // log(share_1 * share_2 * ... * share_n) / maxIL + 1

        int[] generalization = transformation.getGeneralization();
        double infoLoss = 1d;
        for (int dimension = 0; dimension < shares.length; dimension++) {
            int value = entry.key[dimension];
            int level = generalization[dimension];
            infoLoss *= shares[dimension].getShare(value, level);
        }
        
        // Finalize
        return Math.log10(infoLoss) / maxIL + 1d;
    }

    @Override
    protected ILSingleDimensionalWithBound getInformationLossInternal(Transformation transformation, HashGroupify g) {
        
        // Compute
        double real = 0;
        double bound = 0;
        double gFactor = super.getGeneralizationFactor();
        double sFactor = super.getSuppressionFactor();
        HashGroupifyEntry entry = g.getFirstEquivalenceClass();
        while (entry != null) {
            if (entry.count > 0) {
                double loss = entry.count * getEntropyBasedInformationLoss(transformation, entry);
                real += entry.isNotOutlier ? gFactor * loss : sFactor * entry.count;
                bound += gFactor * loss;
            }
            entry = entry.nextOrdered;
        }
        
        // Return
        return super.createInformationLoss(real, bound);
    }

    @Override
    protected InformationLossWithBound<ILSingleDimensional> getInformationLossInternal(Transformation transformation,
                                                                                       HashGroupifyEntry entry) {
        
        double gFactor = super.getGeneralizationFactor();
        double sFactor = super.getSuppressionFactor();
        double bound = entry.count * getEntropyBasedInformationLoss(transformation, entry);
        double loss = entry.isNotOutlier ? gFactor * bound : sFactor * entry.count;
        return super.createInformationLoss(loss, gFactor * bound);
    }

    @Override
    protected ILSingleDimensional getLowerBoundInternal(Transformation transformation) {
        return null;
    }

    @Override
    protected ILSingleDimensional getLowerBoundInternal(Transformation transformation,
                                                        HashGroupify groupify) {

        // Compute
        double bound = 0;
        double gFactor = super.getGeneralizationFactor();
        HashGroupifyEntry entry = groupify.getFirstEquivalenceClass();
        while (entry != null) {
            
            bound += entry.count == 0 ? 0d : gFactor * entry.count * getEntropyBasedInformationLoss(transformation, entry);
            entry = entry.nextOrdered;
        }
        
        // Return
        return new ILSingleDimensional(bound);
    }

    /**
     * For subclasses.
     * 
     * @return
     */
    protected DomainShare[] getShares() {
        return this.shares;
    }
    
    @Override
    protected void initializeInternal(final DataManager manager,
                                      final DataDefinition definition,
                                      final Data input,
                                      final GeneralizationHierarchy[] hierarchies,
                                      final ARXConfiguration config) {

        // Prepare weights
        super.initializeInternal(manager, definition, input, hierarchies, config);

        // Compute domain shares
        this.shares =  manager.getDomainShares();
                
        // Calculate MaxIL
        this.maxIL = 1d;
        for (DomainShare share : shares) {
            maxIL *= share.getDomainSize();
        }
        maxIL = Math.log10(maxIL);
    }
}