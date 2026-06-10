package com.ipca.dms_api.dto;

import lombok.Data;

@Data
public class AuthRequest {
    private String userId;
    private String password;
}
