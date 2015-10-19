/**
 * Copyright (C) 2015 Werner Lamprecht
 * Copyright (C) 2015 Reinhold Gschweicher
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation in version 3.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * This driver is part of the OpenJEVis project, further project information are
 * published at <http://www.OpenJEVis.org/>.
 */

package org.jevis.structurecreator;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jevis.api.JEVisAttribute;
import org.jevis.api.JEVisClass;
import org.jevis.api.JEVisDataSource;
import org.jevis.api.JEVisException;
import org.jevis.api.JEVisFile;
import org.jevis.api.JEVisObject;
import org.jevis.api.JEVisSample;
import org.jevis.api.sql.JEVisDataSourceSQL;
import org.jevis.commons.JEVisFileImp;
import org.jevis.commons.driver.DataCollectorTypes;
import org.jevis.commons.json.JsonAttribute;
import org.jevis.commons.json.JsonObject;


public class JSON2JEVisStructureCreator {

    
    private interface ID_COMMANDS {
        final long IGNORE = -1;
        final long DELETE_EXISTING = -2;
        // UPDATE_EXISTING
    }
    private static final String REFERENCE_MARKER = "$(REF)";
    private static final String FILE_MARKER = "$(FILE)";
    /**
     * The JEVisDataSource is the central class handling the connection to the
     * JEVis Server
     */
    private static JEVisDataSource _jevis_ds;
    
    private HashMap<Long,Long> _mappedIDs;
    private String _jsonFile;
    
     /**
     * Example how to use WiotechStructureCreator
     *
     * @param args not used
     */
    public static void main(String[] args){
        JSON2JEVisStructureCreator wsc = new JSON2JEVisStructureCreator();
        wsc.connectToJEVis("localhost", "3306", "jevis", "jevis", "jevistest", "Sys Admin", "jevis");
        try {
            //wsc.processJSONFile("DesigioStructure.json");
            wsc.processJSONFile("../JEDrivers/MySQL-Driver/MySQLDriverObjects.json");
        } catch (JEVisException ex) {
            Logger.getLogger(JSON2JEVisStructureCreator.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(JSON2JEVisStructureCreator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public JSON2JEVisStructureCreator() {
        this._mappedIDs = new HashMap<Long,Long>();
    }
    
    /**
     * 
     * Creates the needed JEVis structure
     * 
     */
    public void processJSONFile(String jsonFile) throws JEVisException, IOException {
        _jsonFile = jsonFile;
        
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String input = new String(Files.readAllBytes(Paths.get(jsonFile)), StandardCharsets.UTF_8);
        
        JsonObject root = gson.fromJson(input, JsonObject.class);
        System.out.println(root.getId() + ":" + root.getName() + ":" + root.getJevisClass());
        
        JEVisClass rootClass = _jevis_ds.getJEVisClass(root.getJevisClass());
        if (rootClass == null) {
            System.out.println(String.format(
                    "Error: JEVisClass with the name '%s' not found",
                    root.getJevisClass()));
            return;
        }
        
        // Get root-object to create nodes under
        // Leave ID of root blank if name/jevisClass combination is unique.
        // Otherwise additionally specify the JEVis-ID
        JEVisObject rootObj = null;
        if (root.getId() > 0) {
            rootObj = _jevis_ds.getObject(root.getId());
            if (rootObj.getName().equals(root.getName()) &&
                rootObj.getJEVisClass().getName().equals(root.getJevisClass())) {
                // Found by ID
            } else {
                // Found ID, but name/jevisClass does not match
                System.out.println(String.format(
                    "Error: Found ID (%d), but name/jevisClass does not match\n"+
                    "\t searched for: %s/%s"+
                    "\t found: %s/%s",
                    root.getId(), root.getName(), root.getJevisClass(),
                    rootObj.getName(), rootObj.getJEVisClass().getName()));
                return;
            }
        }
        for (JEVisObject obj : _jevis_ds.getObjects(rootClass, false)) {
            if (obj.getName().equals(root.getName())) {
                
                rootObj = obj;
                break;
            }
        }
        if (rootObj == null) {
            System.out.println(String.format(
                    "Error: Object with specified name/jevisClass '%s/%s' not found",
                    root.getName(), root.getJevisClass()));
            return;
        }
        
        // Pretty-print current JEVis build
        //System.out.println(gson.toJson(JsonFactory.buildObject(rootObj, true, true, false)));
        
        // Create all children under given root-node
        for (JsonObject child : root.getChildren()) {
            createObjectFromJSON(child, rootObj);
        }
    }
    
    private JEVisObject createObjectFromJSON(JsonObject jsonObject, JEVisObject parent) throws JEVisException {
        if (parent == null) {
            //TODO: more verbose
            System.out.println("Error: Need a parent to create object under");
            return null;
        }
        if (jsonObject == null) {
            //TODO: more verbose
            System.out.println("Error: jsonObject should not be null");
            return null;
        }
        long id = jsonObject.getId();
        String name = jsonObject.getName();
        String jevisClass = jsonObject.getJevisClass();
        
        System.out.println(String.format(
        "JSON-Processing: id/name/jevisClass: '%d/%s/%s'",
                id, name, jevisClass));
        
        
        // Check if object exists
        JEVisObject newObject = null;
        for (JEVisObject child : parent.getChildren()) {
            System.out.println(String.format(
                "\tComparing to: id/name/jevisClass: '%d/%s/%s'",
                        child.getID(), child.getName(), child.getJEVisClass().getName()));
            if (child.getName().equals(name) && child.getJEVisClass().getName().equals(jevisClass)) {
                System.out.println("\t Found match");
                newObject = child;
                break;
            }
        }
        
        // Object already exists
        if (newObject != null) {
            if (id == ID_COMMANDS.DELETE_EXISTING) {
                System.out.println("\tDelete JEVis-Object");
                // delete object
                newObject.delete();
                newObject.commit();
                newObject = null;
            } else {
                System.out.println("\tUpdate JEVis-Object");
            }
        }
        
        // New object or object was deleted, create new
        if (newObject == null) {
            System.out.println("\tCreate JEVis-Object");
            
            newObject = createObject(parent.getID(), jevisClass, name);
            System.out.println(newObject.getName());
        }
        
        // IDs > 0 are treated as reference IDs and can be used by '$(REF)<ID>'
        if (id > 0) {
            //TODO: what to do if ref-id already set?
            _mappedIDs.put(id, newObject.getID());
        }
        
        // Set/Update attributes
        for (JsonAttribute att : jsonObject.getAttributes()) {
            String key = att.getName();
            String value = att.getLastvalue();
            Object uploadValue = value;
            System.out.println(String.format(
                "\tProcess Attribute: key/value: '%s/%s'",
                        key, value));
            System.out.flush();
            if (value == null || value.isEmpty())
                continue;
            // replace reference-ID with created JEVis-ID
            if (value.startsWith(REFERENCE_MARKER)) {
                String refIDStr = value.substring(REFERENCE_MARKER.length());
                long refID = Long.valueOf(refIDStr);
                //TODO: check if reference ID was set
                uploadValue = "" + _mappedIDs.get(refID);
            } else if (value.startsWith(FILE_MARKER)) {
                try {
                    String fileName = value.substring(FILE_MARKER.length());
                    
                    // Get relative path from json-file
                    int indexSeperator = _jsonFile.lastIndexOf(File.separator);
                    String filePath;
                    if (indexSeperator >= 0) {
                        filePath = _jsonFile.
                            substring(0,_jsonFile.lastIndexOf(File.separator));
                    } else {
                        filePath = "";
                    }
                    
                    if (!filePath.isEmpty()) {
                        fileName = filePath + File.separator + fileName;
                    }
                    
                    // Read file to upload
                    File file = new File(fileName);
                    JEVisFile jfile = new JEVisFileImp(file.getName(), file);
                    
                    uploadValue = jfile;
                } catch (IOException ex) {
                    Logger.getLogger(JSON2JEVisStructureCreator.class.getName()).log(Level.SEVERE, null, ex);
                    break;
                }
                
            }
            if (uploadValue == null) {
                System.out.println("\t No value specified, not writing new Attribute");
                continue;
            }
            writeToJEVis(newObject.getID(), key, uploadValue);
        }
        
        // Create children from JSON
        for (JsonObject child : jsonObject.getChildren()) {
            createObjectFromJSON(child, newObject);
        }
        return newObject;
    }
    
     /**
     * Create an new JEVisObject on the JEVis Server.
     *
     * @param parentObjectID unique ID of the parent object where the new object
     * will be created under
     * @param newObjectClass The JEVisClass of the new JEVisObject
     * @param newObjectName The name of the new JEVisObject
     */
    private static JEVisObject createObject(long parentObjectID, String newObjectClass, String newObjectName) {
        JEVisObject newObject = null;
        try {
            //Check if the connection is still alive. An JEVisException will be
            //thrown if you use one of the functions and the connection is lost
            if (_jevis_ds.isConnectionAlive()) {

                //Get the ParentObject from the JEVis system
                if (_jevis_ds.getObject(parentObjectID) != null) {

                    JEVisObject parentObject = _jevis_ds.getObject(parentObjectID);
                    JEVisClass parentClass = parentObject.getJEVisClass();

                    //Get the JEVisClass we want our new JEVisObject to have
                    if (_jevis_ds.getJEVisClass(newObjectClass) != null) {
                        JEVisClass newClass = _jevis_ds.getJEVisClass(newObjectClass);

                        //Check if the JEVisObject with this class is allowed under a parent of the other Class
                        //it will also check if the JEVisClass is unique and if another object of the Class exist.
                        if (newClass.isAllowedUnder(parentClass)) {
                            newObject = parentObject.buildObject(newObjectName, newClass);
                            newObject.commit();
                            Logger.getLogger(JSON2JEVisStructureCreator.class.getName()).log(Level.INFO, "New ID: " + newObject.getID());
                        } else {
                            Logger.getLogger(JSON2JEVisStructureCreator.class.getName()).log(Level.SEVERE, "Cannot create Object because the parent JEVisClass does not allow the child");
                        }
                    }

                } else {
                    Logger.getLogger(JSON2JEVisStructureCreator.class.getName()).log(Level.SEVERE, "Cannot create Object because the parent is not accessible");
                }

            } else {
                Logger.getLogger(JSON2JEVisStructureCreator.class.getName()).log(Level.SEVERE, "Connection to the JEVisServer is not alive");
            }

        } catch (JEVisException ex) {
            Logger.getLogger(JSON2JEVisStructureCreator.class.getName()).log(Level.SEVERE, null, ex);
        }
        return newObject;
    }
    
    /**
     * 
     * Connect to JEVis
     *
     * @param sqlServer Address of the MySQL Server
     * @param port Port of the MySQL Server, Default is 3306
     * @param sqlSchema Database schema of the JEVis database
     * @param sqlUser MySQl user for the connection
     * @param sqlPW MySQL password for the connection
     * @param jevisUser Username of the JEVis user
     * @param jevisPW Password of the JEVis user
     */
    public void connectToJEVis(String sqlServer, String port, String sqlSchema, String sqlUser, String sqlPW, String jevisUser, String jevisPW) {

        try {
            //Create an new JEVisDataSource from the MySQL implementation 
            //JEAPI-SQl. This connection needs an vaild user on the MySQl Server.
            //Later it will also be possible to use the JEAPI-WS and by this 
            //using the JEVis webservice (REST) as an endpoint which is much
            //saver than using a public SQL-port.
            _jevis_ds = new JEVisDataSourceSQL(sqlServer, port, sqlSchema, sqlUser, sqlPW);

            //authentificate the JEVis user.
            if (_jevis_ds.connect(jevisUser, jevisPW)) {
                Logger.getLogger(JSON2JEVisStructureCreator.class.getName()).log(Level.INFO, "Connection was successful");
            } else {
                Logger.getLogger(JSON2JEVisStructureCreator.class.getName()).log(Level.INFO, "Connection was not successful, exiting app");
                System.exit(1);
            }

        } catch (JEVisException ex) {
            Logger.getLogger(JSON2JEVisStructureCreator.class.getName()).log(Level.SEVERE, "There was an error while connecting to the JEVis Server");
            Logger.getLogger(JSON2JEVisStructureCreator.class.getName()).log(Level.SEVERE, null, ex);
            System.exit(1);
        }

    }
    
     /**
     *
     * set a node attribute 
     *
     * @param objectID unique ID of the JEVisObject on the Server.
     * @param attributeName unique name of the Attribute under this Object
     * @param value and its value
     *
     */
    public static void writeToJEVis(long objectID, String attributeName, Object value ) {
        try {
            //Check if the connection is still alive. An JEVisException will be
            //thrown if you use one of the functions and the connection is lost
            if (_jevis_ds.isConnectionAlive()) {

                //Get the JEVisObject with the given ID. You can get the uniqe
                //ID with the help of JEConfig.
                if (_jevis_ds.getObject(objectID) != null) {
                    JEVisObject myObject = _jevis_ds.getObject(objectID);
                    Logger.getLogger(JSON2JEVisStructureCreator.class.getName()).log(Level.INFO, "JEVisObject: " + myObject);

                    //Get the JEVisAttribute by its unique identifier.
                    if (myObject.getAttribute(attributeName) != null) {
                        JEVisAttribute attribute = myObject.getAttribute(attributeName);
                        Logger.getLogger(JSON2JEVisStructureCreator.class.getName()).log(Level.INFO, "JEVisAttribute: " + attribute);

                        //Now we let the Attribute creates an JEVisSample,an JEVisSample allways need an Timestamp and an value.
                        JEVisSample newSample = attribute.buildSample(null, value);
                        //Until now we created the sample only localy and we have to commit it to the JEVis Server.
                        newSample.commit();

                        //TODO: we need an example for attribute.addSamples(listOfSamples); function. This function allows to commit a bunch of sample at once
                    } else {
                        Logger.getLogger(JSON2JEVisStructureCreator.class.getName()).log(Level.SEVERE, "Could not found the Attribute with the name:" + attributeName);
                    }
                } else {
                    Logger.getLogger(JSON2JEVisStructureCreator.class.getName()).log(Level.SEVERE, "Could not found the Object with the id:" + objectID);
                }
            } else {
                Logger.getLogger(JSON2JEVisStructureCreator.class.getName()).log(Level.SEVERE, "Connection to the JEVisServer is not alive");
                //TODO: the programm could now retry to connect,
                //We dont have to do the isConnectionAlive() but use the JEVisException to handle this problem.
            }
        } catch (JEVisException ex) {
            Logger.getLogger(JSON2JEVisStructureCreator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
