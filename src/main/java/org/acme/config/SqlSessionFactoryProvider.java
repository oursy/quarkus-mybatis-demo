package org.acme.config;

import io.agroal.api.AgroalDataSource;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.executor.ErrorContext;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.NestedIOException;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.ClassMetadata;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static org.springframework.util.StringUtils.tokenizeToStringArray;

@ApplicationScoped
public class SqlSessionFactoryProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(SqlSessionFactoryProvider.class);

    private static final ResourcePatternResolver RESOURCE_PATTERN_RESOLVER = new PathMatchingResourcePatternResolver();

    private static final MetadataReaderFactory METADATA_READER_FACTORY = new CachingMetadataReaderFactory();


    @Inject
    AgroalDataSource defaultDataSource;

    @ConfigProperty(name = "mybatis.mappers.package")
    String mappers;

    @ConfigProperty(name = "mybatis.mapperLocations", defaultValue = "")
    String mapperLocations;

    @ConfigProperty(name = "mybatis.typeAliasesPackage", defaultValue = "")
    String typeAliasesPackage;

    @Produces
    @ApplicationScoped
    public SqlSessionFactory produceFactory() throws IOException {
        TransactionFactory transactionFactory = new JdbcTransactionFactory();
        Environment environment = new Environment("development", transactionFactory, defaultDataSource);
        Configuration configuration = new Configuration(environment);
        configuration.setVfsImpl(SpringBootVFS.class);
        if (StringUtils.hasText(typeAliasesPackage)) {
            scanClasses(typeAliasesPackage, null)
                    .stream()
                    .filter(clazz -> !clazz.isAnonymousClass()).filter(clazz -> !clazz.isInterface())
                    .filter(clazz -> !clazz.isMemberClass()).forEach(configuration.getTypeAliasRegistry()::registerAlias);
        }
        configuration.addMappers(mappers);
        resolveMapperLocations(configuration);
        return new SqlSessionFactoryBuilder()
                .build(configuration);
    }

    private void resolveMapperLocations(Configuration targetConfiguration) throws NestedIOException {
        if (StringUtils.hasText(mapperLocations)) {
            Resource[] resolveMapperLocations = resolveMapperLocations();
            if (resolveMapperLocations.length == 0) {
                LOGGER.warn("Property 'mapperLocations' was specified but matching resources are not found.");
            } else {
                for (Resource mapperLocation : resolveMapperLocations) {
                    if (mapperLocation == null) {
                        continue;
                    }
                    try {
                        XMLMapperBuilder xmlMapperBuilder = new XMLMapperBuilder(mapperLocation.getInputStream(),
                                targetConfiguration, mapperLocation.toString(), targetConfiguration.getSqlFragments());
                        xmlMapperBuilder.parse();
                    } catch (Exception e) {
                        throw new NestedIOException("Failed to parse mapping resource: '" + mapperLocation + "'", e);
                    } finally {
                        ErrorContext.instance().reset();
                    }
                    LOGGER.debug("Parsed mapper file: '" + mapperLocation + "'");
                }
            }
        }

    }

    private static final String CONFIG_LOCATION_DELIMITERS = ",; \t\n";


    private Set<Class<?>> scanClasses(String packagePatterns, Class<?> assignableType) throws IOException {
        Set<Class<?>> classes = new HashSet<>();
        String[] packagePatternArray = tokenizeToStringArray(packagePatterns,
                CONFIG_LOCATION_DELIMITERS);
        for (String packagePattern : packagePatternArray) {
            Resource[] resources = RESOURCE_PATTERN_RESOLVER.getResources(ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX
                    + ClassUtils.convertClassNameToResourcePath(packagePattern) + "/**/*.class");
            for (Resource resource : resources) {
                try {
                    ClassMetadata classMetadata = METADATA_READER_FACTORY.getMetadataReader(resource).getClassMetadata();
                    Class<?> clazz = Resources.classForName(classMetadata.getClassName());
                    if (assignableType == null || assignableType.isAssignableFrom(clazz)) {
                        classes.add(clazz);
                    }
                } catch (Throwable e) {
                    LOGGER.warn("Cannot load the '" + resource + "'. Cause by " + e.toString());
                }
            }
        }
        return classes;
    }

    public Resource[] resolveMapperLocations() {
        return Stream.of(Optional.ofNullable(this.mapperLocations).orElse(Arrays.toString(new String[0])))
                .flatMap(location -> Stream.of(getResources(location))).toArray(Resource[]::new);
    }


    private Resource[] getResources(String location) {
        try {
            return RESOURCE_PATTERN_RESOLVER.getResources(location);
        } catch (IOException e) {
            return new Resource[0];
        }
    }

}
