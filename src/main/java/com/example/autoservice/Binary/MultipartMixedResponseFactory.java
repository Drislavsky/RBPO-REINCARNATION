package com.example.autoservice.binary;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@Component
public class MultipartMixedResponseFactory {

    public MultiValueMap<String, Object> create(byte[] manifestBytes, byte[] dataBytes) {
        LinkedMultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("manifest", createPart("manifest.bin", manifestBytes));
        body.add("data", createPart("data.bin", dataBytes));
        return body;
    }

    private HttpEntity<ByteArrayResource> createPart(String filename, byte[] bytes) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
        headers.setContentLength(bytes.length);

        ByteArrayResource resource = new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return filename;
            }
        };

        return new HttpEntity<>(resource, headers);
    }
}