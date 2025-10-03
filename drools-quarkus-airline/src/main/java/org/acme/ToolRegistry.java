package org.acme;

import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolArg;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import org.jboss.logging.Logger;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;

@ApplicationScoped
public class ToolRegistry {
    
    private static final Logger LOG = Logger.getLogger(ToolRegistry.class);
    private final Map<String, ToolDefinition> tools = new HashMap<>();
    
    public static class ToolDefinition {
        public Method method;
        public Object instance;
        public MaasRequest.Tool maasToolDef;
        
        public ToolDefinition(Method method, Object instance, MaasRequest.Tool maasToolDef) {
            this.method = method;
            this.instance = instance;
            this.maasToolDef = maasToolDef;
        }
    }
    
    public void registerTool(Object bean) {
        Class<?> clazz = bean.getClass();
        
        for (Method method : clazz.getDeclaredMethods()) {
            Tool toolAnnotation = method.getAnnotation(Tool.class);
            if (toolAnnotation != null) {
                String toolName = method.getName();
                LOG.info("Registering tool: " + toolName);
                
                // Build MaasRequest.Tool from @Tool annotation
                Map<String, MaasRequest.Property> properties = new HashMap<>();
                List<String> required = new ArrayList<>();
                
                for (Parameter param : method.getParameters()) {
                    ToolArg argAnnotation = param.getAnnotation(ToolArg.class);
                    if (argAnnotation != null) {
                        String paramName = param.getName();
                        String paramType = mapJavaTypeToJsonType(param.getType());
                        String description = argAnnotation.description();
                        
                        properties.put(paramName, new MaasRequest.Property(paramType, description));
                        required.add(paramName);
                    }
                }
                
                MaasRequest.Parameters parameters = new MaasRequest.Parameters(properties, required);
                MaasRequest.Function function = new MaasRequest.Function(
                    toolName,
                    toolAnnotation.description(),
                    parameters
                );
                MaasRequest.Tool maasToolDef = new MaasRequest.Tool(function);
                
                ToolDefinition toolDef = new ToolDefinition(method, bean, maasToolDef);
                tools.put(toolName, toolDef);
                
                LOG.info("✓ Registered tool: " + toolName + " with " + properties.size() + " parameters");
            }
        }
    }
    
    private String mapJavaTypeToJsonType(Class<?> javaType) {
        if (javaType == String.class) return "string";
        if (javaType == int.class || javaType == Integer.class) return "integer";
        if (javaType == long.class || javaType == Long.class) return "integer";
        if (javaType == double.class || javaType == Double.class) return "number";
        if (javaType == float.class || javaType == Float.class) return "number";
        if (javaType == boolean.class || javaType == Boolean.class) return "boolean";
        return "string"; // default
    }
    
    public List<MaasRequest.Tool> getAllToolDefinitions() {
        List<MaasRequest.Tool> toolDefs = new ArrayList<>();
        for (ToolDefinition def : tools.values()) {
            toolDefs.add(def.maasToolDef);
        }
        return toolDefs;
    }
    
    public String invokeTool(String toolName, Map<String, Object> arguments) throws Exception {
        ToolDefinition toolDef = tools.get(toolName);
        if (toolDef == null) {
            throw new IllegalArgumentException("Tool not found: " + toolName);
        }
        
        Method method = toolDef.method;
        Parameter[] params = method.getParameters();
        Object[] args = new Object[params.length];
        
        for (int i = 0; i < params.length; i++) {
            String paramName = params[i].getName();
            Object value = arguments.get(paramName);
            
            // Type conversion
            Class<?> paramType = params[i].getType();
            if (value != null) {
                if (paramType == int.class || paramType == Integer.class) {
                    args[i] = ((Number) value).intValue();
                } else if (paramType == double.class || paramType == Double.class) {
                    args[i] = ((Number) value).doubleValue();
                } else if (paramType == String.class) {
                    args[i] = value.toString();
                } else {
                    args[i] = value;
                }
            }
        }
        
        Object result = method.invoke(toolDef.instance, args);
        return result != null ? result.toString() : "Success";
    }
    
    public boolean hasTool(String toolName) {
        return tools.containsKey(toolName);
    }
}