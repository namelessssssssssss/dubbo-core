package org.apache.dubbo.common.deploy;

import org.apache.dubbo.common.extension.SPI;

import java.util.List;

/**
 * Initializer.
 * Used in an application initialization procedure, and dubbo modules can implement this interface to complete its deployment procedure.
 * <br>
 * When DefaultApplicationDeployer.initialize() is called, all implementations of this interface will also be called.
 */
@SPI
public interface Initializer {

    /**
     * Returns the name of this initializer.
     * @return the name of this initializer
     */
    String name();

    /**
     * Specifies the initializers this one depends on. They are guaranteed to be called before this one.
     * <br>
     * Note that if there are cyclic dependencies between initializers, an {@link IllegalStateException} will be thrown.
     * @return Initializer name
     */
    List<String> dependOn();


    /**
     * Initializes the application deployer.
     * @param  defaultApplicationDeployer the application deployer
     */
    void init(Object defaultApplicationDeployer);


}
