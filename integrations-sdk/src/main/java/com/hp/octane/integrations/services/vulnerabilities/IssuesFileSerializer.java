package com.hp.octane.integrations.services.vulnerabilities;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hp.octane.integrations.dto.securityscans.OctaneIssue;
import com.hp.octane.integrations.exceptions.PermanentException;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IssuesFileSerializer {
    private String targetDir;
    private List<OctaneIssue> octaneIssues;
    public IssuesFileSerializer(String targetDir, List<OctaneIssue> issues){
        this.targetDir = targetDir;
        this.octaneIssues = issues;
    }

    public InputStream doSerializeAndCache() {
        try{

            validateFolderExists();
            Map dataFormat = new HashMap<>();
            dataFormat.put("data",octaneIssues);
            ObjectMapper mapper = new ObjectMapper();
            mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(mapper);
            oos.flush();
            oos.close();
            InputStream is = new ByteArrayInputStream(baos.toByteArray());

            //send to cache
            if(targetDir!=null) {
                String vulnerabilitiesScanFilePath = targetDir + File.separator + SSCHandler.SCAN_RESULT_FILE;
                PrintWriter fw = new PrintWriter(vulnerabilitiesScanFilePath, "UTF-8");
                mapper.writeValue(fw, dataFormat);
            fw.flush();
            fw.close();
            }
            return is;
        }catch(Exception e){
            throw new PermanentException(e);
        }

    }

    private void validateFolderExists() {
        File file = new File(this.targetDir);
        if(!file.exists()){
            file.mkdirs();
        }
    }
}
