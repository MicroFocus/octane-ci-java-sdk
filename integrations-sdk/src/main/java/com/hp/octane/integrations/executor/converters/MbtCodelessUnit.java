package com.hp.octane.integrations.executor.converters;

import com.hp.octane.integrations.dto.general.MbtUnitParameter;

import java.io.Serializable;
import java.util.List;

/**
 * @author Itay Karo on 21/11/2021
 */
public class MbtCodelessUnit implements Serializable {

    private String name;

    private long unitId;

    private String script;

    private String path;

    private int order;

    private List<MbtUnitParameter> parameters;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getUnitId() {
        return unitId;
    }

    public void setUnitId(long unitId) {
        this.unitId = unitId;
    }

    public String getScript() {
        return script;
    }

    public void setScript(String script) {
        this.script = script;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public List<MbtUnitParameter> getParameters() {
        return parameters;
    }

    public void setParameters(List<MbtUnitParameter> parameters) {
        this.parameters = parameters;
    }

}
