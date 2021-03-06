/*
* Copyright [2016-2018] [George Papadakis (gpapadis@yahoo.gr)]
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
 */
package org.scify.jedai.datamodel;

import com.esotericsoftware.minlog.Log;
import java.io.Serializable;

import java.util.List;

/**
 *
 * @author G.A.P. II
 */
public class SimilarityPairs implements Serializable {

    private final static int MAX_COMPARISONS = Integer.MAX_VALUE - 2;

    private final boolean isCleanCleanER;
    private int currentIndex;
    private final double[] similarities;
    private final int[] entityIds1;
    private final int[] entityIds2;

    public SimilarityPairs(boolean ccer, List<AbstractBlock> blocks) {
        isCleanCleanER = ccer;
        double totalComparisons = countComparisons(blocks);
        entityIds1 = new int[(int) totalComparisons];
        entityIds2 = new int[(int) totalComparisons];
        similarities = new double[(int) totalComparisons];
    }

    public void addComparison(Comparison comparison) {
        entityIds1[currentIndex] = comparison.getEntityId1();
        entityIds2[currentIndex] = comparison.getEntityId2();
        similarities[currentIndex++] = comparison.getUtilityMeasure();
    }

    private double countComparisons(List<AbstractBlock> blocks) {
        double comparisons = 0;
        for (AbstractBlock block : blocks) {
            comparisons += block.getNoOfComparisons();
        }

        if (MAX_COMPARISONS < comparisons) {
            Log.error("Very high number of comparisons to be executed! "
                    + "Maximum allowed number is : " + MAX_COMPARISONS);
            System.exit(-1);
        }
        return comparisons;
    }

    public int[] getEntityIds1() {
        return entityIds1;
    }

    public int[] getEntityIds2() {
        return entityIds2;
    }

    public int getNoOfComparisons() {
        return currentIndex;
    }

    public PairIterator getPairIterator() {
        return new PairIterator(this);
    }

    public double[] getSimilarities() {
        return similarities;
    }

    public boolean isCleanCleanER() {
        return isCleanCleanER;
    }
}
