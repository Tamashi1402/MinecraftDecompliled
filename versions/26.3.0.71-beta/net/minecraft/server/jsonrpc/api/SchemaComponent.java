package net.minecraft.server.jsonrpc.api;

import java.net.URI;
import java.util.List;

/// Neo: Use the constructor taking an [Identifier][net.minecraft.resources.Identifier] for internal references,
/// as it will automatically create a local reference for you.
public record SchemaComponent<T>(String name, URI ref, Schema<T> schema) {
    public SchemaComponent(net.minecraft.resources.Identifier name, Schema<T> schema) {
        this(name.toString(), ReferenceUtil.createLocalReference(name.toString()), schema);
    }

    public Schema<T> asRef() {
        return Schema.ofRef(this.ref, this.schema.codec());
    }

    public Schema<List<T>> asArray() {
        return Schema.arrayOf(this.asRef(), this.schema.codec());
    }
}
