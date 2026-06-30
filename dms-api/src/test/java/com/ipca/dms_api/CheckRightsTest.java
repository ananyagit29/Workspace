package com.ipca.dms_api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import com.ipca.dms_api.repository.UserRightsRepository;
import com.ipca.dms_api.entity.UserRights;
import java.util.List;

@SpringBootTest
public class CheckRightsTest {

    @Autowired
    private UserRightsRepository repo;

    @Test
    public void test() {
        System.out.println("==================================================");
        List<UserRights> rights = repo.findByUserId("ananya.parabat");
        System.out.println("FOUND RIGHTS: " + rights.size());
        for(UserRights r : rights) {
            if(r.getId() != null)
                System.out.println("RIGHT: " + r.getId().getCompanyId() + " | " + r.getId().getDivisionName() + " | " + r.getId().getLocationId() + " | " + r.getId().getApplicationName() + " | " + r.getId().getSubApplicationName() + " | " + r.getId().getAccessType());
            else
                System.out.println("RIGHT ID IS NULL");
        }
        System.out.println("==================================================");
    }
}
