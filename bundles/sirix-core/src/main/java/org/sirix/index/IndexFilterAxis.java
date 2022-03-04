package org.sirix.index;

import static com.google.common.base.Preconditions.checkNotNull;
import java.util.Iterator;
import java.util.Set;
import org.sirix.index.redblacktree.RBNode;
import org.sirix.index.redblacktree.keyvalue.NodeReferences;
import com.google.common.collect.AbstractIterator;

public final class IndexFilterAxis<K extends Comparable<? super K>>
    extends AbstractIterator<NodeReferences> {

  private final Iterator<RBNode<K, NodeReferences>> iter;

  private final Set<? extends Filter> filter;

  public IndexFilterAxis(final Iterator<RBNode<K, NodeReferences>> iter,
      final Set<? extends Filter> filter) {
    this.iter = checkNotNull(iter);
    this.filter = checkNotNull(filter);
  }

  @Override
  protected NodeReferences computeNext() {
    while (iter.hasNext()) {
      final RBNode<K, NodeReferences> node = iter.next();
      boolean filterResult = true;
      for (final Filter filter : filter) {
        filterResult = filterResult && filter.filter(node);
        if (!filterResult) {
          break;
        }
      }
      if (filterResult) {
        return node.getValue();
      }
    }
    return endOfData();
  }
}
