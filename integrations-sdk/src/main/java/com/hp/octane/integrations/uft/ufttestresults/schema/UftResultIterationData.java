package com.hp.octane.integrations.uft.ufttestresults.schema;

import java.io.Serializable;
import java.util.List;
import java.util.StringJoiner;

public class UftResultIterationData implements Serializable {
    private List<UftResultStepData> steps;
    private long duration;

    public UftResultIterationData(List<UftResultStepData> steps, long duration) {
        this.steps = steps;
        this.duration = duration;
    }

    public List<UftResultStepData> getSteps() {
        return steps;
    }

    public long getDuration() {
        return duration;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", UftResultIterationData.class.getSimpleName() + "[", "]")
                .add("steps=" + steps)
                .add("duration=" + duration)
                .toString();
    }
}
