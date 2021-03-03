package org.sirix.access.json;

import org.sirix.access.AbstractResourceStore;
import org.sirix.access.DatabasesInternals;
import org.sirix.access.PathBasedPool;
import org.sirix.access.ResourceConfiguration;
import org.sirix.access.User;
import org.sirix.access.WriteLocksRegistry;
import org.sirix.access.trx.node.json.JsonResourceManagerImpl;
import org.sirix.access.trx.page.PageTrxFactory;
import org.sirix.api.ResourceManager;
import org.sirix.api.json.JsonResourceManager;
import org.sirix.cache.BufferManager;
import org.sirix.io.IOStorage;
import org.sirix.io.StorageType;
import org.sirix.page.UberPage;

import javax.annotation.Nonnull;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Manages all resource stuff.
 *
 * @author Johannes Lichtenberger
 */
public final class JsonResourceStore extends AbstractResourceStore<JsonResourceManager> {

  /**
   * This field should be use to fetch the locks for resource managers.
   */
  private final WriteLocksRegistry writeLocksRegistry;

  /**
   * Constructor.
   */
  public JsonResourceStore(final User user,
                           final WriteLocksRegistry writeLocksRegistry,
                           final PathBasedPool<ResourceManager<?, ?>> allResourceManagers,
                           final String databaseName,
                           final PageTrxFactory pageTrxFactory) {
    super(new ConcurrentHashMap<>(), allResourceManagers, user, databaseName, pageTrxFactory);

    this.writeLocksRegistry = writeLocksRegistry;
  }

  @Override
  public JsonResourceManager openResource(final @Nonnull ResourceConfiguration resourceConfig,
                                          final @Nonnull BufferManager bufferManager,
                                          final @Nonnull Path resourceFile) {
    checkNotNull(resourceConfig);
    checkNotNull(bufferManager);
    checkNotNull(resourceFile);

    return resourceManagers.computeIfAbsent(resourceFile, k -> {
      final IOStorage storage = StorageType.getStorage(resourceConfig);
      final UberPage uberPage = getUberPage(storage);

      final Lock writeLock = this.writeLocksRegistry.getWriteLock(resourceConfig.getResource());

      // Create the resource manager instance.
      final JsonResourceManager resourceManager = new JsonResourceManagerImpl(
              this,
              resourceConfig,
              bufferManager,
              StorageType.getStorage(resourceConfig),
              uberPage,
              writeLock,
              user,
              this.databaseName,
              pageTrxFactory
      );

      // Put it in the databases cache.
      this.allResourceManagers.putObject(resourceFile, resourceManager);

      // And return it.
      return resourceManager;
    });
  }
}
