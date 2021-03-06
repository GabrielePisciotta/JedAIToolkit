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

import gnu.trove.map.TIntDoubleMap;

/**
 *
 * @author G.A.P. II
 */
public class VertexWeight {

    private final int pos;
    private final double weight;
    private final int noOfAdj;
    private final TIntDoubleMap Connections;

    public VertexWeight(int pos, double weight, int noOfAdj, TIntDoubleMap Connections) {
        this.pos = pos;
        this.weight = weight;
        this.noOfAdj = noOfAdj;
        this.Connections = Connections;
    }

    public int getPos() {
        return this.pos;
    }

    public double getWeight() {
        return this.weight;
    }

    public int getNoOfAdj() {
        return this.noOfAdj;
    }

    public TIntDoubleMap Connections() {
        return this.Connections;
    }

    @Override
    public String toString() {
        return "pos : " + pos + ", weight : " + weight + ", noOfAdj : " + noOfAdj;
    }
}
