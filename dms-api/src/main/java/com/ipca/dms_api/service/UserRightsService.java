package com.ipca.dms_api.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.ipca.dms_api.dto.UserRightsDTO;
import com.ipca.dms_api.repository.UserRightsRepository;

@Service
public class UserRightsService {

    private final UserRightsRepository repository;

    public UserRightsService(UserRightsRepository repository) {
        this.repository = repository;
    }

    public Page<UserRightsDTO> searchUserRights(String search, Pageable pageable) {

        return repository.searchRightsDTO(
                search != null ? search.trim().toLowerCase() : "",
                pageable
        );
    }
}