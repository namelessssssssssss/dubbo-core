package org.apache.dubbo.common.deploy;

import org.apache.dubbo.common.extension.SPI;

import java.util.List;

/**
 * PackageLifeManager.
 * <br>
 * Used in an application Lifecycle managing procedure, and dubbo packages can implement this interface to define what to do when Application start or destroy.
 * <br>
 * On the other word , when {@link DefaultApplicationDeployer#initialize()} , {@link DefaultApplicationDeployer#preDestroy()} or {@link DefaultApplicationDeployer#postDestroy()} called, all implementations of this interface will also be called.
 */
@SPI
public interface PackageLifeManager {

    /**
     * Returns the name of this initializer.
     * @return the name of this initializer. Can not be null.
     */
    String name();

    /**
     * Specifies which PackageLifeManager should be called before this one when {@link DefaultApplicationDeployer#initialize()} called. They are guaranteed to be called before this one.
     * <br>
     * Note that if there are cyclic dependencies between PackageLifeManagers, an {@link IllegalStateException} will be thrown.
     * @return PackageLifeManager names. Can be null or empty list.
     */
    default List<String> dependOnInit(){
        return null;
    }

    /**
     * Initialize. This method will be called when {@link DefaultApplicationDeployer#initialize()} called.
     * @param  applicationDeployer The ApplicationDeployer that called this PackageLifeManager.
     */
    default void initialize(DefaultApplicationDeployer applicationDeployer){};


    /**
     * Specifies which PackageLifeManager should be called before this one when {@link DefaultApplicationDeployer#preDestroy()} called.
     * <br>
     * Works just like {@link PackageLifeManager#dependOnInit()}.
     * @return PackageLifeManager names. Can be null or empty list.
     */
    default List<String> dependOnPreDestroy(){
        return null;
    };

    /**
     * preDestroy. This method will be called when {@link DefaultApplicationDeployer#preDestroy()} called.
     * @param  applicationDeployer The ApplicationDeployer that called this PackageLifeManager.
     */
    default void preDestroy(DefaultApplicationDeployer applicationDeployer){};

    /**
     * Specifies which PackageLifeManager should be called before this one when {@link DefaultApplicationDeployer#postDestroy()} called.
     * <br>
     * Works just like {@link PackageLifeManager#dependOnInit()}.
     * @return PackageLifeManager names. Can be null or empty list.
     */
    default List<String> dependOnPostDestroy(){
        return null;
    };

    /**
     * postDestroy. This method will be called when {@link DefaultApplicationDeployer#postDestroy()} called.
     * @param  applicationDeployer The ApplicationDeployer that called this PackageLifeManager.
     */
    default void postDestroy(DefaultApplicationDeployer applicationDeployer){
        return;
    };

}
