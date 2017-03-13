package org.uberfire.backend.server.cdi;

import java.io.Serializable;

/**
 * Created by aparedes on 3/9/17.
 */
public interface WorkspaceBuilderService extends Serializable {

    void build( String gav );
}
