package com.exactpro.th2.gradle

import org.gradle.api.provider.Property

abstract class GrpcTh2Extension {
    /**
     * Version of protoc to be used
     */
    abstract val protocVersion: Property<String>

    /**
     * Defines whether the protobuf definition contain services.
     * If it has the `service generator` dependency will be added
     */
    abstract val includeServices: Property<Boolean>

    /**
     * Version of gRPC generator to be used
     */
    abstract val grpcVersion: Property<String>

    /**
     * Version of th2 service generator to be used
     */
    abstract val serviceGeneratorVersion: Property<String>
}