package org.uberfire.backend.server.workspace;

public class WorkspaceContext {

    private static ThreadLocal<String> workspace = new ThreadLocal<>();

    public static void set( String w ) {
        workspace.set( w );
    }

    public static String get() {
        return workspace.get();
    }

}
