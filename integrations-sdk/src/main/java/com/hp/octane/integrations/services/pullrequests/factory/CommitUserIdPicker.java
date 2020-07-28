package com.hp.octane.integrations.services.pullrequests.factory;

public interface CommitUserIdPicker {

    String getUserIdForCommit(String email, String name);
}
