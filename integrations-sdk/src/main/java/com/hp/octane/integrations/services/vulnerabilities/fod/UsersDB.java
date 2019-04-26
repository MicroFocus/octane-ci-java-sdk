package com.hp.octane.integrations.services.vulnerabilities.fod;

import com.hp.octane.integrations.services.vulnerabilities.fod.dto.pojos.User;
import com.hp.octane.integrations.services.vulnerabilities.fod.dto.services.UsersService;

import java.util.List;

public class UsersDB {
    List<User> allUsers;

    public void init() {
        allUsers = UsersService.getAllUsers();
    }

    public String getEmailFromAssignedUser(String assignedUser) {
        if(assignedUser == null || assignedUser.isEmpty()){
            return null;
        }
        String retVal = searchByLastFirstComb(assignedUser);
        if(retVal != null){
            return retVal;
        }
        retVal = searchByUserName(assignedUser);
        if(retVal != null){
            return retVal;
        }
        return null;
    }

    private String searchByUserName(String assignedUser) {
        for(User user : allUsers){
            if(assignedUser.equals(user.userName)){
                return user.email;
            }
        }
        return null;
    }

    private String searchByLastFirstComb(String assignedUser) {
        for(User user : allUsers){
            if(assignedUser.equals(getFullName(user))){
                return user.email;
            }
        }
        return null;
    }

    private String getFullName(User user) {
        //"assignedUser": "Hijazi, Yamin",
        return String.format("%s, %s", user.lastName, user.firstName);
    }
}
