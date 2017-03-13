package org.uberfire.backend.server.cdi;

import java.lang.reflect.InvocationTargetException;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.implementation.FieldAccessor;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uberfire.backend.server.cdi.workspace.WorkspaceDefinition;

import static org.junit.Assert.*;

public class WorkspaceScopedBeanTest {

    public <T> Class<? extends T> createClass( Class<T> clazz,
                                               ClassLoader classLoader ) {

        return new ByteBuddy().subclass( clazz, ConstructorStrategy.Default.IMITATE_SUPER_CLASS ).implement( WorkspaceDefinition.class )
                .intercept( FieldAccessor.ofField( "workspace" ) )
                .defineField( "workspace", String.class, Visibility.PRIVATE ).make().load( classLoader ).getLoaded();
    }

    @Test
    public void testAddGetUserMethodToBean() throws InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException {

        final String hendrix = "hendrix";
        final String bbking = "bbking";

        final Class<? extends WorkspaceBuilderService> clazz = this.createClass( WorkspaceBuilderServiceImpl.class, ClassLoader.getSystemClassLoader() );

        WorkspaceDefinition service1 = (WorkspaceDefinition) clazz.getConstructor( Logger.class ).newInstance( LoggerFactory.getLogger( "A" ) );
        WorkspaceDefinition service2 = (WorkspaceDefinition) clazz.getConstructor( Logger.class ).newInstance( LoggerFactory.getLogger( "B" ) );

        service1.setWorkspace( hendrix );
        service2.setWorkspace( bbking );

        assertEquals( hendrix, service1.getWorkspace() );
        assertEquals( bbking, service2.getWorkspace() );

    }

}
