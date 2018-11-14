package com.hp.octane.integrations.dto.scm.impl;

public class LineRange {
    private int gte;
    private int lte;


    public LineRange(int gte, int lte) {
        this.gte = gte;
        this.lte = lte;
    }

    public LineRange(Long singleNum) {
        this.gte = singleNum.intValue();
        this.lte = singleNum.intValue();
    }

    public int getLte() {
        return lte;
    }

    public int getGte() {
        return gte;
    }

    public void setGte(int gte) {
        this.gte = gte;
    }

    public void setLte(int lte) {
        this.lte = lte;
    }

}