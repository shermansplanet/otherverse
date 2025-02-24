package com.shermansplanet.otherverse.demesnes;

public interface ISectionSetter {
    void setSections(int minY, int maxY);
    void clearSections();
    int getMin();
    int getMax();
    boolean isSet();
}
