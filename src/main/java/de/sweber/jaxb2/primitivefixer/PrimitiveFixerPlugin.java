/*
    Licensed to the Apache Software Foundation (ASF) under one or more
    contributor license agreements. See the NOTICE file distributed with
    this work for additional information regarding copyright ownership.
    The ASF licenses this file to You under the Apache License, Version 2.0
    (the "License"); you may not use this file except in compliance with
    the License. You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/
    
package de.sweber.jaxb2.primitivefixer;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;

import com.sun.codemodel.JMethod;
import com.sun.codemodel.JType;
import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.Plugin;
import com.sun.tools.xjc.outline.Outline;
import com.sun.tools.xjc.outline.ClassOutline;

/**
 * @author swe
 *
 */
public class PrimitiveFixerPlugin extends Plugin
{

    private static final String OPTION_NAME = "Xfix-primitives"; //$NON-NLS-1$

    @Override
    public String getOptionName()
        {
        return OPTION_NAME;
        }

    @Override
    public String getUsage()
        {
        return "-" + OPTION_NAME  //$NON-NLS-1$
            + "    :   Fixes a bug with primitive getter/setters with default values\n"; //$NON-NLS-1$
        }

    @Override
    public boolean run(Outline outline, Options opt, ErrorHandler errorHandler) throws SAXException
        {
        for (ClassOutline co : outline.getClasses())
            {
            /**
             * Map containing the getter as key and the setter as value
             */
            Map<JMethod, JMethod> getterSetterMap = new HashMap<JMethod, JMethod>();
            Collection<JMethod> methods = co.implClass.methods();
            // add all primitive getter to the map
            for (JMethod m : methods)
                {
                if (m.type().isPrimitive() && (m.name().startsWith("is") || m.name().startsWith("get"))) //$NON-NLS-1$ //$NON-NLS-2$
                    {
                    getterSetterMap.put(m, null);
                    }
                }
            // we only search for buggy methods
            // assign setter for each getter
            for(JMethod getter : getterSetterMap.keySet())
                {
                String varName = getter.name().substring(2);
                if(getter.name().startsWith("get")) //$NON-NLS-1$
                    {
                    varName = getter.name().substring(3);
                    }
                for (JMethod m : methods)
                    {
                    // ok found a method with the right name. 
                    // check the argument type and if the method is a 
                    // true setter thus has a void return type
                    JType newType = null;
                    if (m.name().equals("set" + varName)  //$NON-NLS-1$
                            && m.type().name().equals("void") //$NON-NLS-1$
                            && (newType = isMatching(getter.type(), m.listParamTypes())) != null) 
                        {
                        // for Boolean getter we have to change
                        // isXX to getXX due to returning objects instead of primitives
                        if(newType.name().equals("Boolean")) //$NON-NLS-1$
                            {
                            getter.name("get" + varName); //$NON-NLS-1$
                            }
                        getter.type(newType);
                        }
                    }
                
                }
            }
        return true;
        }

    /**
     * Checks if the given return type is the primitive type of the
     * given list of types. The list must contain exactly one item
     * @param returnType the retuny type
     * @param paramTypes the parameter types
     * @return the object type if the returnType is the primitive for of the only paramType, in all other cases null.
     */
    private static JType isMatching(JType returnType, JType[] paramTypes)
        {
        if(paramTypes.length != 1)
            {
            return null;
            }
        String retType = returnType.name();
        if ((retType == "byte" && paramTypes[0].name().equals("Byte")) //$NON-NLS-1$ //$NON-NLS-2$
            || (retType == "short" && paramTypes[0].name().equals("Short")) //$NON-NLS-1$ //$NON-NLS-2$
            || (retType == "char" && paramTypes[0].name().equals("Character")) //$NON-NLS-1$ //$NON-NLS-2$
            || (retType == "int" && paramTypes[0].name().equals("Integer")) //$NON-NLS-1$ //$NON-NLS-2$
            || (retType == "long" && paramTypes[0].name().equals("Long")) //$NON-NLS-1$ //$NON-NLS-2$
            || (retType == "float" && paramTypes[0].name().equals("Float")) //$NON-NLS-1$ //$NON-NLS-2$
            || (retType == "double" && paramTypes[0].name().equals("Double")) //$NON-NLS-1$ //$NON-NLS-2$
            || (retType == "boolean" && paramTypes[0].name().equals("Boolean"))) //$NON-NLS-1$ //$NON-NLS-2$
            {
            return paramTypes[0]; 
            }
        return null;
        }
}
