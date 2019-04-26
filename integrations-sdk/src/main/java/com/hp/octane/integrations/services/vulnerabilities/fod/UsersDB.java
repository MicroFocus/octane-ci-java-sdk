/*
 *     Copyright 2017 EntIT Software LLC, a Micro Focus company, L.P.
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */
package com.hp.octane.integrations.services.vulnerabilities.fod;

import com.hp.octane.integrations.services.vulnerabilities.fod.dto.pojos.FODUser;
import com.hp.octane.integrations.services.vulnerabilities.fod.dto.services.UsersService;

import java.util.List;

public class UsersDB {
    private List<FODUser> allUsers;

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
        for(FODUser user : allUsers){
            if(assignedUser.equals(user.userName)){
                return user.email;
            }
        }
        return null;
    }

    private String searchByLastFirstComb(String assignedUser) {
        for(FODUser user : allUsers){
            if(assignedUser.equals(getFullName(user))){
                return user.email;
            }
        }
        return null;
    }

    private String getFullName(FODUser user) {
        //"assignedUser": "Hijazi, Yamin",
        return String.format("%s, %s", user.lastName, user.firstName);
    }
}
