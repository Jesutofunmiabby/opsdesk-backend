package com.opsdesk.opsdeskbackend.user;

import org.springframework.stereotype.Service;

import com.opsdesk.opsdeskbackend.common.PageResponse;

@Service
public class UserService {

    private final AppUserRepository appUserRepository;

    public UserService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    public PageResponse<AppUser> findPage(int page, int size) {
        return PageResponse.fetch(page, size, appUserRepository::findAll);
    }
}
