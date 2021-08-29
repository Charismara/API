package com.envyful.api.config.type;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@ConfigSerializable
public  class SQLDatabaseDetails {

    private String poolName;
    private String ip;
    private int port;
    private String username;
    private String password;
    private String database;

    public SQLDatabaseDetails() {
    }

    public SQLDatabaseDetails(String poolName, String ip, int port, String username, String password, String database) {
        this.poolName = poolName;
        this.ip = ip;
        this.port = port;
        this.username = username;
        this.password = password;
        this.database = database;
    }

    public String getPoolName() {
        return this.poolName;
    }

    public String getIp() {
        return this.ip;
    }

    public int getPort() {
        return this.port;
    }

    public String getUsername() {
        return this.username;
    }

    public String getPassword() {
        return this.password;
    }

    public String getDatabase() {
        return this.database;
    }
}