package com.hp.octane.integrations.uft.items;

/**
 * @author Itay Karo on 02/08/2021
 */
public enum UftParameterDirection {
    INPUT(0),
    OUTPUT(1);

    int direction;

    UftParameterDirection(int direction) {
        this.direction = direction;
    }

    public static UftParameterDirection get(int direction) {
        if(direction == 0) {
            return INPUT;
        } else if(direction == 1) {
            return OUTPUT;
        } else {
            return null;
        }
    }
}
