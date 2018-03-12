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
package org.scify.jedai.demoworkflows;

import java.io.File;
import org.scify.jedai.blockbuilding.IBlockBuilding;
import org.scify.jedai.blockbuilding.StandardBlocking;
import org.scify.jedai.blockprocessing.blockcleaning.BlockFiltering;
import org.scify.jedai.blockprocessing.comparisoncleaning.CardinalityNodePruning;
import org.scify.jedai.blockprocessing.IBlockProcessing;
import org.scify.jedai.datamodel.AbstractBlock;
import org.scify.jedai.datamodel.EntityProfile;
import org.scify.jedai.datamodel.EquivalenceCluster;
import org.scify.jedai.datamodel.SimilarityPairs;
import org.scify.jedai.datareader.entityreader.EntityCSVReader;
import org.scify.jedai.datareader.groundtruthreader.GtCSVReader;
import org.scify.jedai.entityclustering.ConnectedComponentsClustering;
import org.scify.jedai.entityclustering.IEntityClustering;
import org.scify.jedai.entitymatching.IEntityMatching;
import org.scify.jedai.entitymatching.ProfileMatcher;
import org.scify.jedai.utilities.BlocksPerformance;
import org.scify.jedai.utilities.ClustersPerformance;
import org.scify.jedai.utilities.datastructures.AbstractDuplicatePropagation;
import org.scify.jedai.utilities.datastructures.BilateralDuplicatePropagation;
import org.scify.jedai.utilities.PrintToFile;
import java.io.FileNotFoundException;
import java.util.List;
import org.apache.log4j.BasicConfigurator;

/**
 *
 * @author gap2
 */
public class CsvDblpAcm {

    public static void main(String[] args) throws FileNotFoundException {
        BasicConfigurator.configure();
        
        String mainDirectory = "data" + File.separator + "cleanCleanErDatasets" + File.separator + "DBLP-ACM" + File.separator;

        EntityCSVReader csvEntityReader = new EntityCSVReader(mainDirectory + "DBLP2.csv");
        csvEntityReader.setAttributeNamesInFirstRow(true);
        csvEntityReader.setIdIndex(0);
        csvEntityReader.setSeparator(',');
        List<EntityProfile> csvDBLP = csvEntityReader.getEntityProfiles();
        System.out.println("CSV DBLP Entity Profiles\t:\t" + csvDBLP.size());
        
        csvEntityReader = new EntityCSVReader(mainDirectory + "ACM.csv");
        csvEntityReader.setAttributeNamesInFirstRow(true);
        csvEntityReader.setIdIndex(0);
        csvEntityReader.setSeparator(',');
        List<EntityProfile> csvACM = csvEntityReader.getEntityProfiles();
        System.out.println("CSV ACM Entity Profiles\t:\t" + csvACM.size());

        GtCSVReader csvGtReader = new GtCSVReader(mainDirectory + "DBLP-ACM_perfectMapping.csv");
        csvGtReader.setIgnoreFirstRow(true);
        csvGtReader.setSeparator(',');
        csvGtReader.getDuplicatePairs(csvDBLP, csvACM);
        final AbstractDuplicatePropagation duplicatePropagation = new BilateralDuplicatePropagation(csvGtReader.getDuplicatePairs(csvDBLP, csvACM));
        System.out.println("Existing Duplicates\t:\t" + duplicatePropagation.getDuplicates().size());

        StringBuilder workflowConf = new StringBuilder();
        StringBuilder workflowName = new StringBuilder();

        double time1 = System.currentTimeMillis();

        IBlockBuilding tokenBlocking = new StandardBlocking();
        List<AbstractBlock> blocks = tokenBlocking.getBlocks(csvDBLP, csvACM);
        workflowConf.append(tokenBlocking.getMethodConfiguration());
        workflowName.append(tokenBlocking.getMethodName());

        IBlockProcessing blockCleaning = new BlockFiltering();
        List<AbstractBlock> bcBlocks = blockCleaning.refineBlocks(blocks);
        workflowConf.append("\n").append(blockCleaning.getMethodConfiguration());
        workflowName.append("->").append(blockCleaning.getMethodName());
        
        IBlockProcessing comparisonCleaning = new CardinalityNodePruning();
        List<AbstractBlock> ccBlocks = comparisonCleaning.refineBlocks(bcBlocks);
        workflowConf.append("\n").append(comparisonCleaning.getMethodConfiguration());
        workflowName.append("->").append(comparisonCleaning.getMethodName());
                
        double time2 = System.currentTimeMillis();

        BlocksPerformance blStats = new BlocksPerformance(ccBlocks, duplicatePropagation);
        blStats.setStatistics();
        blStats.printStatistics(time2 - time1, workflowConf.toString(), workflowName.toString());
        
        double time3 = System.currentTimeMillis();
        
        IEntityMatching entityMatching = new ProfileMatcher();
        SimilarityPairs simPairs = entityMatching.executeComparisons(blocks, csvDBLP, csvACM);
        workflowConf.append("\n").append(entityMatching.getMethodConfiguration());
        workflowName.append("->").append(entityMatching.getMethodName());

        IEntityClustering entityClusttering = new ConnectedComponentsClustering();
        List<EquivalenceCluster> entityClusters = entityClusttering.getDuplicates(simPairs);
        workflowConf.append("\n").append(entityClusttering.getMethodConfiguration());
        workflowName.append("->").append(entityClusttering.getMethodName());

        double time4 = System.currentTimeMillis();

        ClustersPerformance clp = new ClustersPerformance(entityClusters, duplicatePropagation);
        clp.setStatistics();
        clp.printStatistics(time4 - time3, workflowName.toString(), workflowConf.toString());
        
        PrintToFile.toCSV(entityClusters, mainDirectory + "foundMatches.csv");
    }

}
