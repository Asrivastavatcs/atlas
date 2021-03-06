package com.accordance.atlas.repository;

import com.accordance.atlas.model.DeployRecord;
import com.orientechnologies.orient.core.sql.OCommandSQL;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientGraphNoTx;
import com.tinkerpop.blueprints.impls.orient.OrientVertex;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Repository
public class ApplicationsRepositoryImpl implements ApplicationsRepository {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationsRepositoryImpl.class);

    private final OrientDbFactory orientDb;

    @Autowired
    public ApplicationsRepositoryImpl(OrientDbFactory orientDb) {
        this.orientDb = Objects.requireNonNull(orientDb, "orientDb");
    }

    @Override
    public List<Vertex> findApplications() {
        return findApplications(new ApplicationQueryBuilder());
    }

    @Override
    public List<Vertex> findApplications(ApplicationQueryBuilder query) {

//        Iterable<Vertex> vertices = orientDb.startNoTransaction().getVerticesOfClass("Application");

//        select *, out('Uses').id as "uses" from (traverse out('Uses') from #11:0)
//        select *, out('Uses').id as "uses" from (traverse out('Uses') from (select from Application where id = 'My Application') where $depth <= 1)
//        String query = "select *, out('Uses').id as \"uses\" from Application";

        String startNodeQuery = "Application";
        if (query.startNode != null) {
            startNodeQuery = String.format("(traverse %s('Uses') from (select from Application where id = \"%s\") %s)", query.getDirection(), query.startNode, query.getHopsQuery());
        }

        String q = String.format("select %s from %s", query.filterFields(), startNodeQuery);

        logger.debug("Executing App search query: " + q);

        OSQLSynchQuery<OrientVertex> qr = new OSQLSynchQuery<>(q);
        return orientDb.withGraphNoTx((OrientGraphNoTx db) -> {
            List<Vertex> result = new ArrayList<>();
            Iterable<OrientVertex> vertices = db.command(qr).execute(Collections.emptyMap());
            vertices.forEach(result::add);
            return result;
        });
    }

    @Override
    public Vertex getApplicationById(String id) {
        String q = "select *, first(In(\"Owns\").id) as owner_team_id from Application where id= :id";
        Map<String, String> params = new HashMap<>();
        params.put("id", id);

        OSQLSynchQuery<OrientVertex> qr = new OSQLSynchQuery<>(q);
        return orientDb.withGraphNoTx((OrientGraphNoTx db) -> {
            Iterable<Vertex> vertices = db.command(qr).execute(params);
            Iterator<Vertex> verticesItr = vertices.iterator();

            return verticesItr.hasNext() ? verticesItr.next() : null;
        });
    }
	
	@Override
    public boolean getAppDeploymentStatus(String id) {
        String q = "select deploy from Application where id= :id";
        Map<String, String> params = new HashMap<>();
        params.put("id", id);

        OSQLSynchQuery<OrientVertex> qr = new OSQLSynchQuery<>(q);
        return orientDb.withGraphNoTx((OrientGraphNoTx db) -> {
            Iterable<Vertex> vertices = db.command(qr).execute(params);
    		
            if (vertices.iterator().hasNext())
                return (boolean)vertices.iterator().next().getProperty("deploy");
    		else 
    			return false;
        });
    }
	
	@Override
    public boolean activateDeploymentLock(String id) {
        String q = "update Application set deploy = true where id= :id";
        Map<String, String> params = new HashMap<>();
        params.put("id", id);
        
        return orientDb.withGraphNoTx((OrientGraphNoTx db) -> {
            int updated = db.command(new OCommandSQL(q)).execute(params);
    	    if(updated > 0) return true;
    	    else return false;
        });
    }
	
	@Override
    public boolean releaseDeploymentLock(DeployRecord app) {
    
	    String q = "update Application set deploy = false, version = :version where id= :id";
	    
	    Map<String, String> params = new HashMap<>();
	    params.put("id", app.getId());
	    params.put("version", app.getVersion());
	
        return orientDb.withGraphNoTx((OrientGraphNoTx db) -> {
    	    int updated = db.command(new OCommandSQL(q)).execute(params);
    	    if(updated > 0) return true;
    	    else return false;
        }); 
	}
}
