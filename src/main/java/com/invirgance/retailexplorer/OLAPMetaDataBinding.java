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

import com.invirgance.convirgance.json.JSONArray;
import com.invirgance.convirgance.json.JSONObject;
import com.invirgance.convirgance.olap.Dimension;
import com.invirgance.convirgance.olap.Measure;
import com.invirgance.convirgance.olap.Star;
import com.invirgance.convirgance.web.binding.Binding;
import org.springframework.context.support.GenericXmlApplicationContext;
import org.springframework.core.io.ClassPathResource;

/**
 *
 * @author jbanes
 */
public class OLAPMetaDataBinding implements Binding
{
    private String schema;
    private Star star;

    public String getSchema()
    {
        return schema;
    }

    public void setSchema(String schema)
    {
        this.schema = schema;
    }
    
    private void loadStar()
    {
        this.star = new GenericXmlApplicationContext(new ClassPathResource(schema)).getBean(Star.class);
    }
    
    @Override
    public Iterable<JSONObject> getBinding(JSONObject parameters)
    {
        JSONArray<JSONObject> results = new JSONArray<>();
        JSONObject record;
        
        if(this.star == null) loadStar();
        
        for(Dimension dimension : star.getDimensions())
        {
            record = new JSONObject();
            
            record.put("type", "dimension");
            record.put("name", dimension.getName());
            
            results.add(record);
        }
        
        for(Measure measure : star.getMeasures())
        {
            record = new JSONObject();
            
            record.put("type", "measure");
            record.put("name", measure.getName());
            record.put("function", measure.getFunction());
            
            results.add(record);
        }
        
        return results;
    }
    
}
