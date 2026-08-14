package com.drakmyth.minecraft.blockwisemcp.mcp;

import io.modelcontextprotocol.json.schema.JsonSchemaValidator;
import io.modelcontextprotocol.json.schema.jackson3.DefaultJsonSchemaValidator;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import java.util.function.Supplier;

/** Makes networknt's root schema resources visible through NeoForge's module-aware loader. */
final class ModuleCompatibleJsonSchemaValidator implements JsonSchemaValidator {
    private static final String ROOT_PREFIX = "draft/";
    private static final String PACKAGED_PREFIX = "schema/";

    private final ClassLoader resourceLoader = new SchemaResourceClassLoader();
    private final JsonSchemaValidator delegate = withResourceLoader(DefaultJsonSchemaValidator::new);

    @Override
    public ValidationResponse validate(Map<String, Object> schema, Object structuredContent) {
        return withResourceLoader(() -> delegate.validate(schema, structuredContent));
    }

    @Override
    public ValidationResponse validateSchema(Map<String, Object> schema) {
        return withResourceLoader(() -> delegate.validateSchema(schema));
    }

    private <T> T withResourceLoader(Supplier<T> operation) {
        var thread = Thread.currentThread();
        var previous = thread.getContextClassLoader();
        thread.setContextClassLoader(resourceLoader);
        try {
            return operation.get();
        } finally {
            thread.setContextClassLoader(previous);
        }
    }

    private static final class SchemaResourceClassLoader extends ClassLoader {
        private SchemaResourceClassLoader() {
            super(ModuleCompatibleJsonSchemaValidator.class.getClassLoader());
        }

        @Override
        public URL getResource(String name) {
            if (name.startsWith(ROOT_PREFIX)) {
                return ModuleCompatibleJsonSchemaValidator.class.getResource(PACKAGED_PREFIX + name);
            }
            return super.getResource(name);
        }

        @Override
        public InputStream getResourceAsStream(String name) {
            if (name.startsWith(ROOT_PREFIX)) {
                return ModuleCompatibleJsonSchemaValidator.class.getResourceAsStream(PACKAGED_PREFIX + name);
            }
            return super.getResourceAsStream(name);
        }
    }
}
