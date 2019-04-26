package com.hp.octane.integrations.services.vulnerabilities.fod.dto.services;

import com.hp.octane.integrations.services.vulnerabilities.fod.dto.FodConnectionFactory;
import com.hp.octane.integrations.services.vulnerabilities.fod.dto.pojos.User;


import java.util.List;

public class UsersService {
    static final String urlUsers = "%s/users";

    public static List<User> getAllUsers(){
        String url = String.format(urlUsers, FodConnectionFactory.instance().getEntitiesURL());
        User.Users users = FodConnectionFactory.instance().getAllFODEntities(url,User.Users.class, null );
        return users.items;
    }
}
