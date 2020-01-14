package org.acme.config;

import io.agroal.api.AgroalDataSource;
import org.apache.ibatis.io.JBoss6VFS;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

@ApplicationScoped
public class SqlSessionFactoryProvider {

    @Inject
    AgroalDataSource defaultDataSource;


    @ConfigProperty(name = "mybatis.mappers.package")
    String mappers;

    @Produces
    @ApplicationScoped
    public SqlSessionFactory produceFactory() {
        TransactionFactory transactionFactory = new JdbcTransactionFactory();
        Environment environment = new Environment("development", transactionFactory, defaultDataSource);
        Configuration configuration = new Configuration(environment);
        configuration.setVfsImpl(JBoss6VFS.class);
        configuration.addMappers(mappers);
        return new SqlSessionFactoryBuilder()
                .build(configuration);
    }
}
