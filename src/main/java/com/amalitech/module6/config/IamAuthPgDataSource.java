package com.amalitech.module6.config;

import org.postgresql.ds.PGSimpleDataSource;
import software.amazon.awssdk.services.rds.RdsUtilities;

import java.sql.Connection;
import java.sql.SQLException;

/** PostgreSQL DataSource that uses a freshly generated RDS IAM auth token as the password. */
public class IamAuthPgDataSource extends PGSimpleDataSource {

    private final transient RdsUtilities rdsUtilities;

    public IamAuthPgDataSource(RdsUtilities rdsUtilities) {
        this.rdsUtilities = rdsUtilities;
    }

    @Override
    public Connection getConnection() throws SQLException {
        return super.getConnection(getUser(), generateToken());
    }

    private String generateToken() {
        return rdsUtilities.generateAuthenticationToken(builder -> builder
                .hostname(getServerNames()[0])
                .port(getPortNumbers()[0])
                .username(getUser()));
    }
}
