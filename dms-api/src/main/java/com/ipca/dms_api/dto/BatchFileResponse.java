package com.ipca.dms_api.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatchFileResponse {
    private String subApplicationName; // IMAGE | COA | INVOICE
    private String fileName;
    private String filePath;
    private boolean hasFile; // true = show button, false = show upload input
}