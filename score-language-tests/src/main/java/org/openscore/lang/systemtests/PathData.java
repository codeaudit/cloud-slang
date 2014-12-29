/******************************************************************************
 * (c) Copyright 2014 Hewlett-Packard Development Company, L.P.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License v2.0 which accompany this distribution.
 *
 * The Apache License is available at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * *****************************************************************************/

 package org.openscore.lang.systemtests;

import java.io.Serializable;
import java.util.Map;

public class PathData {

    private final String path;
    private final String name;
    private final Map<String, Serializable> inputs;
    private final Map<String, Serializable> outputs;
    private final String result;

    public PathData(String path, String name, Map<String, Serializable> inputs, Map<String, Serializable> outputs, String result) {
        this.path = path;
        this.name = name;
        this.inputs = inputs;
        this.outputs = outputs;
        this.result = result;
    }

    public String getPath() {
        return path;
    }

    public String getName() {
        return name;
    }

    public Map<String, Serializable> getInputs() {
        return inputs;
    }

    public Map<String, Serializable> getOutputs() {
        return outputs;
    }

    public String getResult() {
        return result;
    }

}
