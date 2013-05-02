/* $This file is distributed under the terms of the license in /doc/license.txt$ */
package edu.cornell.mannlib.vitro.webapp.controller.ajax;

import java.io.IOException;
import java.lang.Integer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONException;

import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.RDFNode;

import edu.cornell.mannlib.vitro.webapp.controller.VitroRequest;
import edu.cornell.mannlib.vitro.webapp.controller.ajax.VitroAjaxController;
import edu.cornell.mannlib.vitro.webapp.controller.freemarker.UrlBuilder;
import edu.cornell.mannlib.vitro.webapp.dao.jena.QueryUtils;

public class GeoFocusMapLocations extends AbstractAjaxResponder {

    private static final Log log = LogFactory.getLog(GeoFocusMapLocations.class.getName());
    private List<Map<String,String>>  geoLocations;
    private static String GEO_FOCUS_QUERY = ""
        + "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> \n"
        + "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n"
        + "PREFIX core: <http://vivoweb.org/ontology/core#> \n"
        + "PREFIX foaf: <http://xmlns.com/foaf/0.1/> \n"
        + "PREFIX vivoc: <http://vivo.library.cornell.edu/ns/0.1#> \n"
        + "SELECT DISTINCT ?label ?location (COUNT(?person) AS ?count) \n"
        + "WHERE { {"
        + "    ?location rdf:type core:GeographicRegion . \n"
        + "    ?location rdfs:label ?label . " 
        + "    ?location core:geographicFocusOf ?person . \n"
        + "    ?person rdf:type foaf:Person \n"
        + "    FILTER (! regex(str(?location), \"dbpedia\")) \n"
        + "} UNION { \n "
        + "    ?sublocation rdf:type vivoc:DomesticGeographicalRegion  . \n"
        + "    ?sublocation core:geographicFocusOf ?person . \n"
        + "    ?person rdf:type foaf:Person \n"
        + "    bind((\"United States of America\"^^<http://www.w3.org/2001/XMLSchema#string>) AS ?label) \n"
        + "    bind(<http://aims.fao.org/aos/geopolitical.owl#United_States_of_America> AS ?location) \n"
        + "} } \n"
        + "GROUP BY ?label ?location \n"
        + "ORDER BY ?label ?location \n";
    
	public GeoFocusMapLocations(HttpServlet parent, VitroRequest vreq,
			HttpServletResponse resp) {
		super(parent, vreq, resp);
    }

	@Override
	public String prepareResponse() throws IOException, JSONException {
		try {
            geoLocations = getGeoLocations(vreq);
            
            String response = "[";
            String geometry = "{\"geometry\": {\"type\": \"Point\",\"coordinates\": \"\"},";
            String typeProps = "\"type\": \"Feature\",\"properties\": {\"mapType\": \"\",";
            String previousLabel = "";
            
            for (Map<String, String> map: geoLocations) {
                String label = map.get("label");
                String html  = map.get("count");
                String uri = map.get("location");
                if ( uri != null ) {
                    uri = UrlBuilder.urlEncode(uri);
                }
                Integer count    = Integer.parseInt(map.get("count"));
                String radius   = String.valueOf(calculateRadius(count));
                
                if ( label != null && !label.equals(previousLabel) ) {
                    String tempStr = geometry; //+label
                    tempStr += typeProps //+ label 
                                        + "\"popupContent\": \""
                                        + label 
                                        + "\",\"html\":"
                                        + html 
                                        + ",\"radius\":"
                                        + radius
                                        + ",\"uri\": \""
                                        + uri
                                        + "\"}},";                 
                    response +=  tempStr;
                    previousLabel = label;
                }
            }
			if ( response.lastIndexOf(",") > 0 ) {
			    response = response.substring(0, response.lastIndexOf(","));
			}
			response += " ]";
			log.debug(response);
			return response;
		} catch (Exception e) {
			log.error("Failed geographic focus locations", e);
			return EMPTY_RESPONSE;
		}
	}
           
    private List<Map<String,String>>  getGeoLocations(VitroRequest vreq) {
          
        String queryStr = GEO_FOCUS_QUERY;
        log.debug("queryStr = " + queryStr);
        List<Map<String,String>>  locations = new ArrayList<Map<String,String>>();
        try {
            ResultSet results = QueryUtils.getQueryResults(queryStr, vreq);
            while (results.hasNext()) {
                QuerySolution soln = results.nextSolution();
                locations.add(QueryUtils.querySolutionToStringValueMap(soln));
            }
        } catch (Exception e) {
            log.error(e, e);
        }    
       
        return locations;
    }
    private Integer calculateRadius(Integer count) {
          
        int radius = 8;
        if ( count != null ) {
            if ( count < 4 ) {
                radius = 8;
            }
            else if ( count < 7 ) {
                radius = 10;
            }
            else if ( count < 10 ) {
                radius = 12;
            }
            else if ( count < 16 ) {
                radius = 14;
            }
            else if ( count < 21 ) {
                radius = 16;
            }
            else if ( count < 26 ) {
                radius = 18;
            }
            else {
                radius = 20;
            }
        }

        return radius;
    }
}
