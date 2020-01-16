package org.acme;

import org.acme.mapper.PersonMapper;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ExampleResource {

    @Inject
    SqlSessionFactory sqlSessionFactory;

    @GET
    @Path("hello")
    public String hello() {
        try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
            PersonMapper personMapper = sqlSession.getMapper(PersonMapper.class);
            return personMapper.select1();
        }
    }

    @GET
    @Path("persons")
    public Response persons() {
        try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
            PersonMapper personMapper = sqlSession.getMapper(PersonMapper.class);
            return Response.ok(personMapper.findAll())
                    .build();
        }
    }

    @GET
    @Path("persons/{id}")
    public Response personById(@PathParam(value = "id") Long id) {
        try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
            PersonMapper personMapper = sqlSession.getMapper(PersonMapper.class);
            return personMapper.selectByTop1(id)
                    .map(
                            p -> Response.ok(p).build()
                    ).orElse(Response.status(Response.Status.NOT_FOUND).build());
        }
    }
}