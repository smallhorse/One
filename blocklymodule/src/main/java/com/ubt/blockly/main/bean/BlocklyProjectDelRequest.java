package com.ubt.blockly.main.bean;

import java.util.List;

/**
 * @author admin
 * @className
 * @description
 * @date
 * @update
 */


public class BlocklyProjectDelRequest extends BaseRequest {

    private List<String> programIds;

    public List<String> getProgramIds() {
        return programIds;
    }

    public void setProgramIds(List<String> programIds) {
        this.programIds = programIds;
    }

    @Override
    public String toString() {
        return "BlocklyProjectDelRequest{" +
                "programIds=" + programIds +
                '}';
    }
}
