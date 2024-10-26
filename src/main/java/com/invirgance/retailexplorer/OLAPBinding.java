/*
 * Copyright 2024 INVIRGANCE LLC

Permission is hereby granted, free of charge, to any person obtaining a copy 
of this software and associated documentation files (the “Software”), to deal 
in the Software without restriction, including without limitation the rights to 
use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies 
of the Software, and to permit persons to whom the Software is furnished to do 
so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all 
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE 
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER 
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, 
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE 
SOFTWARE.
 */
package com.invirgance.retailexplorer;

import com.invirgance.convirgance.ConvirganceException;
import com.invirgance.convirgance.dbms.DBMS;
import com.invirgance.convirgance.dbms.Query;
import com.invirgance.convirgance.json.JSONArray;
import com.invirgance.convirgance.json.JSONObject;
import com.invirgance.convirgance.olap.Dimension;
import com.invirgance.convirgance.olap.Measure;
import com.invirgance.convirgance.olap.ReportGenerator;
import com.invirgance.convirgance.olap.Star;
import com.invirgance.convirgance.web.binding.Binding;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import org.springframework.context.support.GenericXmlApplicationContext;
import org.springframework.core.io.ClassPathResource;

/**
 *
 * @author jbanes
 */
public class OLAPBinding implements Binding
{
    private String jndiName;
    private String schema;
    private boolean caseSensitive;
    private boolean logQuery;
    
    private Star star;

    public String getJndiName()
    {
        return jndiName;
    }

    public void setJndiName(String jndiName)
    {
        this.jndiName = jndiName;
    }

    public String getSchema()
    {
        return schema;
    }

    public void setSchema(String schema)
    {
        this.schema = schema;
    }

    public boolean isCaseSensitive()
    {
        return caseSensitive;
    }

    public void setCaseSensitive(boolean caseSensitive)
    {
        this.caseSensitive = caseSensitive;
    }

    public boolean isLogQuery()
    {
        return logQuery;
    }

    public void setLogQuery(boolean logQuery)
    {
        this.logQuery = logQuery;
    }
    
    private void loadStar()
    {
        this.star = new GenericXmlApplicationContext(new ClassPathResource(schema)).getBean(Star.class);
    }
    
    private DataSource getDataSource()
    {
        Context context;
        DataSource source;
        
        try
        {
            context = new InitialContext();
            source = (DataSource)context.lookup(this.jndiName);
            
            if(source != null) return source;
            
            // Tomcat prefixes java:/comp/env/ to database registrations
            source = (DataSource)context.lookup("java:/comp/env/" + this.jndiName);
            
            if(source != null) return source;
            
            throw new ConvirganceException("No DataSource configured at JNDI location " + jndiName);
        }
        catch(NamingException e)
        {
            throw new ConvirganceException(e);
        }
    }
    
    @Override
    public Iterable<JSONObject> getBinding(JSONObject parameters)
    {
        ReportGenerator generator;
        DBMS dbms;
        
        Measure measure;
        Dimension dimension;
        
        JSONArray<String> dimensions = (JSONArray<String>)parameters.getJSONArray("dimensions");
        JSONArray<String> measures = (JSONArray<String>)parameters.getJSONArray("measures");
        
        if(star == null) loadStar();
        if(parameters.getJSONArray("dimensions").isEmpty() && parameters.getJSONArray("measures").isEmpty()) return new JSONArray<>();
        
        generator = new ReportGenerator(star);
        dbms = new DBMS(getDataSource());
        
        for(String name : dimensions)
        {
            dimension = star.getDimension(name);
            
            if(dimension == null) throw new ConvirganceException("Dimension [" + name + "] not found!");
            
            generator.addDimension(dimension);
        }
        
        for(String name : measures)
        {
            measure = star.getMeasure(name);
            
            if(measure == null) throw new ConvirganceException("Measure [" + name + "] not found!");
            
            generator.addMeasure(measure);
        }
        
        generator.setCaseSensitive(caseSensitive);
        
        if(logQuery) System.out.println(generator.getSQL());
        
        return dbms.query(new Query(generator.getSQL()));
    }
    
}
