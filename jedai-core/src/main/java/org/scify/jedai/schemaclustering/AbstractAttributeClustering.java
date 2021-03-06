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
package org.scify.jedai.schemaclustering;

import gnu.trove.iterator.TIntIterator;
import gnu.trove.iterator.TObjectIntIterator;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.jena.atlas.json.JsonArray;
import org.apache.jena.atlas.json.JsonObject;
import org.scify.jedai.configuration.gridsearch.IntGridSearchConfiguration;
import org.scify.jedai.datamodel.Attribute;
import org.scify.jedai.datamodel.EntityProfile;
import org.scify.jedai.datamodel.RepModelSimMetricCombo;
import org.scify.jedai.configuration.randomsearch.IntRandomSearchConfiguration;
import org.scify.jedai.textmodels.ITextModel;
import org.scify.jedai.utilities.enumerations.RepresentationModel;
import org.scify.jedai.utilities.enumerations.SimilarityMetric;
import org.scify.jedai.utilities.graph.ConnectedComponents;
import org.scify.jedai.utilities.graph.UndirectedGraph;

/**
 *
 * @author G.A.P. II
 */
public abstract class AbstractAttributeClustering implements ISchemaClustering {

    protected int attributesDelimiter;
    protected int noOfAttributes;

    protected int[] globalMostSimilarIds;
    protected double[] globalMaxSimilarities;

    protected final IntGridSearchConfiguration gridCombo;
    protected final IntRandomSearchConfiguration randomCombo;
    protected ITextModel[][] attributeModels;
    protected final List<RepModelSimMetricCombo> modelMetricCombinations;
    protected Map<String, TIntList> invertedIndex;
    protected RepresentationModel repModel;
    protected SimilarityMetric simMetric;
    protected TObjectIntMap<String> attrNameIndex;

    public AbstractAttributeClustering(RepresentationModel model, SimilarityMetric metric) {
        repModel = model;
        simMetric = metric;
        attributeModels = new ITextModel[2][];

        modelMetricCombinations = RepModelSimMetricCombo.getAllValidCombos();
        gridCombo = new IntGridSearchConfiguration(modelMetricCombinations.size() - 1, 0, 1);
        randomCombo = new IntRandomSearchConfiguration(modelMetricCombinations.size(), 0);
    }

    private void buildAttributeModels(int datasetId, List<EntityProfile> profiles) {
        attrNameIndex = new TObjectIntHashMap<>();
        for (EntityProfile profile : profiles) {
            for (Attribute attribute : profile.getAttributes()) {
                attrNameIndex.putIfAbsent(attribute.getName(), attrNameIndex.size() + 1);
            }
        }

        int currentAttributes = attrNameIndex.size();
        attributeModels[datasetId] = new ITextModel[currentAttributes];
        final TObjectIntIterator<String> it = attrNameIndex.iterator();
        while (it.hasNext()) {
            it.advance();
            attributeModels[datasetId][it.value() - 1] = RepresentationModel.getModel(datasetId, repModel, simMetric, it.key());
        }

        for (EntityProfile profile : profiles) {
            for (Attribute attribute : profile.getAttributes()) {
                updateModel(datasetId, attribute);
            }
        }

        for (int i = 0; i < currentAttributes; i++) {
            attributeModels[datasetId][i].finalizeModel();
        }
    }

    private void buildInvertedIndex() {
        invertedIndex = new HashMap<>();
        int indexedDataset = 0 < attributesDelimiter ? DATASET_2 : DATASET_1;
        for (int i = 0; i < attributeModels[indexedDataset].length; i++) {
            final Set<String> signatures = attributeModels[indexedDataset][i].getSignatures();

            for (String signature : signatures) {
                TIntList attributeIds = invertedIndex.get(signature);
                if (attributeIds == null) {
                    attributeIds = new TIntArrayList();
                    invertedIndex.put(signature, attributeIds);
                }
                attributeIds.add(i);
            }
        }
    }

    protected TObjectIntMap<String>[] clusterAttributes() {
        final UndirectedGraph similarityGraph = new UndirectedGraph(noOfAttributes);
        for (int i = 0; i < noOfAttributes; i++) {
            if (0 < globalMaxSimilarities[i]) {
                similarityGraph.addEdge(i, globalMostSimilarIds[i]);
            }
        }

        final ConnectedComponents cc = new ConnectedComponents(similarityGraph);
        if (attributesDelimiter < 0) { // Dirty ER
            return clusterDirtyAttributes(cc);
        }

        return clusterCleanCleanAttributes(cc); // Clean-Clean ER
    }

    protected TObjectIntMap<String>[] clusterCleanCleanAttributes(ConnectedComponents cc) {
        int glueClusterId = cc.count() + 1;
        final TObjectIntMap<String>[] clusters = new TObjectIntHashMap[2];
        clusters[DATASET_1] = new TObjectIntHashMap<>();
        for (int i = 0; i < attributesDelimiter; i++) {
            int ccId = cc.id(i);
            if (cc.size(i) == 1) { // singleton attribute
                ccId = glueClusterId;
            }
            clusters[DATASET_1].put(attributeModels[DATASET_1][i].getInstanceName(), ccId);
        }
        clusters[DATASET_2] = new TObjectIntHashMap<>();
        for (int i = attributesDelimiter; i < noOfAttributes; i++) {
            int ccId = cc.id(i);
            if (cc.size(i) == 1) { // singleton attribute
                ccId = glueClusterId;
            }
            clusters[DATASET_2].put(attributeModels[DATASET_2][i - attributesDelimiter].getInstanceName(), ccId);
        }
        return clusters;
    }

    protected TObjectIntMap<String>[] clusterDirtyAttributes(ConnectedComponents cc) {
        int glueClusterId = cc.count() + 1;
        final TObjectIntMap<String>[] clusters = new TObjectIntHashMap[1];
        clusters[DATASET_1] = new TObjectIntHashMap<>();
        for (int i = 0; i < noOfAttributes; i++) {
            int ccId = cc.id(i);
            if (cc.size(i) == 1) { // singleton attribute
                ccId = glueClusterId;
            }
            clusters[DATASET_1].put(attributeModels[DATASET_1][i].getInstanceName(), ccId);
        }
        return clusters;
    }

    protected void compareAttributes() {
        globalMostSimilarIds = new int[noOfAttributes];
        globalMaxSimilarities = new double[noOfAttributes];
        final TIntSet coOccurringAttrs = new TIntHashSet();
        int lastId = 0 < attributesDelimiter ? attributesDelimiter : noOfAttributes;
        for (int i = 0; i < lastId; i++) {
            coOccurringAttrs.clear();

            final Set<String> signatures = attributeModels[DATASET_1][i].getSignatures();
            for (String signature : signatures) {
                final TIntList attrIds = invertedIndex.get(signature);
                if (attrIds == null) {
                    continue;
                }
                coOccurringAttrs.addAll(attrIds);
            }

            if (0 < attributesDelimiter) { // Clean-Clean ER
                executeCleanCleanErComparisons(i, coOccurringAttrs);
            } else { // Dirty ER
                executeDirtyErComparisons(i, coOccurringAttrs);
            }
        }
    }

    private void executeCleanCleanErComparisons(int attributeId, TIntSet coOccurringAttrs) {
        final TIntIterator sigIterator = coOccurringAttrs.iterator();
        while (sigIterator.hasNext()) {
            int neighborId = sigIterator.next();

            int normalizedNeighborId = neighborId + attributesDelimiter;
            double similarity = attributeModels[DATASET_1][attributeId].getSimilarity(attributeModels[DATASET_2][neighborId]);
            if (globalMaxSimilarities[attributeId] < similarity) {
                globalMaxSimilarities[attributeId] = similarity;
                globalMostSimilarIds[attributeId] = normalizedNeighborId;
            } else if (globalMaxSimilarities[attributeId] == similarity) {
                globalMostSimilarIds[attributeId] = (int) Math.min(normalizedNeighborId, globalMostSimilarIds[attributeId]);
            }

            if (globalMaxSimilarities[normalizedNeighborId] < similarity) {
                globalMaxSimilarities[normalizedNeighborId] = similarity;
                globalMostSimilarIds[normalizedNeighborId] = attributeId;
            } else if (globalMaxSimilarities[normalizedNeighborId] == attributeId) {
                globalMostSimilarIds[normalizedNeighborId] = (int) Math.min(attributeId, globalMostSimilarIds[normalizedNeighborId]);
            }
        }
    }

    private void executeDirtyErComparisons(int attributeId, TIntSet coOccurringAttrs) {
        final TIntIterator sigIterator = coOccurringAttrs.iterator();
        while (sigIterator.hasNext()) {
            int neighborId = sigIterator.next();
            if (neighborId <= attributeId) { // avoid repeated comparisons & comparison with attributeId
                continue;
            }

            double similarity = attributeModels[DATASET_1][attributeId].getSimilarity(attributeModels[DATASET_1][neighborId]);
            if (globalMaxSimilarities[attributeId] < similarity) {
                globalMaxSimilarities[attributeId] = similarity;
                globalMostSimilarIds[attributeId] = neighborId;
            } else if (globalMaxSimilarities[attributeId] == similarity) {
                globalMostSimilarIds[attributeId] = (int) Math.min(neighborId, globalMostSimilarIds[attributeId]);
            }

            if (globalMaxSimilarities[neighborId] < similarity) {
                globalMaxSimilarities[neighborId] = similarity;
                globalMostSimilarIds[neighborId] = attributeId;
            } else if (globalMaxSimilarities[neighborId] == attributeId) {
                globalMostSimilarIds[neighborId] = (int) Math.min(attributeId, globalMostSimilarIds[neighborId]);
            }
        }
    }

    @Override
    public TObjectIntMap<String>[] getClusters(List<EntityProfile> profiles) {
        return this.getClusters(profiles, null);
    }

    @Override
    public TObjectIntMap<String>[] getClusters(List<EntityProfile> profilesD1, List<EntityProfile> profilesD2) {
        buildAttributeModels(DATASET_1, profilesD1);
        attributesDelimiter = -1;
        noOfAttributes = attrNameIndex.size();

        if (profilesD2 != null) {
            buildAttributeModels(DATASET_2, profilesD2);
            attributesDelimiter = noOfAttributes;
            noOfAttributes += attrNameIndex.size();
        }
        attrNameIndex = null;

        buildInvertedIndex();
        compareAttributes();

        return clusterAttributes();
    }
    

    @Override
    public String getMethodConfiguration() {
        return getParameterName(0) + "=" + repModel + "\t"
                + getParameterName(1) + "=" + simMetric;
    }

    @Override
    public String getMethodParameters() {
        return getMethodName() + " involves two parameters:\n"
                + "1)" + getParameterDescription(0) + ".\n"
                + "2)" + getParameterDescription(1) + ".";
    }

    @Override
    public int getNumberOfGridConfigurations() {
        return gridCombo.getNumberOfConfigurations();
    }

    @Override
    public JsonArray getParameterConfiguration() {
        final JsonObject obj1 = new JsonObject();
        obj1.put("class", "org.scify.jedai.utilities.enumerations.RepresentationModel");
        obj1.put("name", getParameterName(0));
        obj1.put("defaultValue", "org.scify.jedai.utilities.enumerations.RepresentationModel.TOKEN_UNIGRAM_GRAPHS");
        obj1.put("minValue", "-");
        obj1.put("maxValue", "-");
        obj1.put("stepValue", "-");
        obj1.put("description", getParameterDescription(0));

        final JsonObject obj2 = new JsonObject();
        obj2.put("class", "org.scify.jedai.utilities.enumerations.SimilarityMetric");
        obj2.put("name", getParameterName(1));
        obj2.put("defaultValue", "org.scify.jedai.utilities.enumerations.SimilarityMetric.GRAPH_VALUE_SIMILARITY");
        obj2.put("minValue", "-");
        obj2.put("maxValue", "-");
        obj2.put("stepValue", "-");
        obj2.put("description", getParameterDescription(1));

        final JsonArray array = new JsonArray();
        array.add(obj1);
        array.add(obj2);
        return array;
    }

    @Override
    public String getParameterDescription(int parameterId) {
        switch (parameterId) {
            case 0:
                return "The " + getParameterName(0) + " aggregates the textual items that correspond to every attribute.";
            case 1:
                return "The " + getParameterName(1) + " compares the models of two attributes, returning a value between 0 (completely dissimlar) and 1 (identical).";
            default:
                return "invalid parameter id";
        }
    }

    @Override
    public String getParameterName(int parameterId) {
        switch (parameterId) {
            case 0:
                return "Representation Model";
            case 1:
                return "Similarity Measure";
            default:
                return "invalid parameter id";
        }
    }
    
    @Override
    public void setNextRandomConfiguration() {
        int comboId = (Integer) randomCombo.getNextRandomValue();
        final RepModelSimMetricCombo selectedCombo = modelMetricCombinations.get(comboId);
        repModel = selectedCombo.getRepModel();
        simMetric = selectedCombo.getSimMetric();
    }

    @Override
    public void setNumberedGridConfiguration(int iterationNumber) {
        int comboId = (Integer) gridCombo.getNumberedValue(iterationNumber);
        final RepModelSimMetricCombo selectedCombo = modelMetricCombinations.get(comboId);
        repModel = selectedCombo.getRepModel();
        simMetric = selectedCombo.getSimMetric();
    }

    @Override
    public void setNumberedRandomConfiguration(int iterationNumber) {
        int comboId = (Integer) randomCombo.getNumberedRandom(iterationNumber);
        final RepModelSimMetricCombo selectedCombo = modelMetricCombinations.get(comboId);
        repModel = selectedCombo.getRepModel();
        simMetric = selectedCombo.getSimMetric();
    }

    protected abstract void updateModel(int datasetId, Attribute attribute);
}
