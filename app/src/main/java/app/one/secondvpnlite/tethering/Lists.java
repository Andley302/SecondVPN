package app.one.secondvpnlite.tethering;

import java.util.ArrayList;
import java.util.Collections;
/**
 * Provides static methods for creating {@code List} instances easily, and other
 * utility methods for working with lists.
 */
public class Lists {
    /**
     * Creates an empty {@code ArrayList} instance.
     *
     * <p><b>Note:</b> if you only need an <i>immutable</i> empty List, use
     * {@link Collections#emptyList} instead.
     *
     * @return a newly-created, initially-empty {@code ArrayList}
     */
    @UnsupportedAppUsage
    public static <E> ArrayList<E> newArrayList() {
        return new ArrayList<>();
    }
}