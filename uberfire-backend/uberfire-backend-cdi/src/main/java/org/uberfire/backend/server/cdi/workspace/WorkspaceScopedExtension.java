/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.uberfire.backend.server.cdi.workspace;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.WithAnnotations;
import javax.enterprise.util.AnnotationLiteral;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.implementation.FieldAccessor;
import net.bytebuddy.implementation.bytecode.member.MethodInvocation;
import org.apache.deltaspike.core.util.metadata.builder.AnnotatedTypeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Workspace Scoped CDI Extension to add WorkspaceScoped behavior into Uberfire
 */
public class WorkspaceScopedExtension implements Extension {

    private Logger logger = LoggerFactory.getLogger( WorkspaceScopedExtension.class );

    private Set<Class<?>> classesToBeReplaced = new HashSet<>();

    public <T> void vetoEntities( @Observes @WithAnnotations(WorkspaceScoped.class) ProcessAnnotatedType<T> pat ) {
        final AnnotatedType<T> target = pat.getAnnotatedType();
        if ( logger.isDebugEnabled() ) {
            logger.debug( "Vetoing class {} to be replaced with workspace subclass", target.getJavaClass().getCanonicalName() );
        }

        final Class<T> originalClass = target.getJavaClass();

        Class<? extends T> newClazz = new ByteBuddy()
                .subclass( originalClass, ConstructorStrategy.Default.IMITATE_SUPER_CLASS.withInheritedAnnotations() )
                .name( originalClass.getSimpleName() + "RuntimeGeneratedImpl" )
                .implement( WorkspaceDefinition.class )
                .intercept( FieldAccessor.ofField( "workspace" ) )
                .defineField( "workspace", String.class, Visibility.PRIVATE )
                .annotateType( originalClass.getAnnotations() )
                .make()
                .load( ClassLoader.getSystemClassLoader() )
                .getLoaded();

        pat.setAnnotatedType( new AnnotatedTypeBuilder<T>()
                                      .readFromType( pat.getAnnotatedType() ).setJavaClass( (Class<T>) newClazz ).create() );

//        classesToBeReplaced.add( target.getJavaClass() );
//        pat.veto();
    }

    public void beforeBeanDiscovery( @Observes BeforeBeanDiscovery bbd ) {
        if ( logger.isDebugEnabled() ) {
            logger.debug( "Before bean discovery, adding WosrkspaceScoped" );
        }

        bbd.addScope( WorkspaceScoped.class, true, false );
    }

    public void afterBeanDiscovery( @Observes AfterBeanDiscovery abd,
                                    BeanManager beanManager ) {
        if ( logger.isDebugEnabled() ) {
            logger.debug( "After bean discovery, adding WorkspaceScopeContext" );
        }

//        this.classesToBeReplaced.forEach( clazz -> abd.addBean( this.createBean( clazz, beanManager ) ) );
        abd.addContext( new WorkspaceScopeContext( beanManager ) );
    }

    protected <T> Bean<T> createBean( Class<T> originalClass,
                                      BeanManager bm ) {

        if ( logger.isDebugEnabled() ) {
            logger.debug( "Generating workspace class for {}", originalClass.getSimpleName() );
        }

        Class<T> newClazz = (Class<T>) new ByteBuddy()
                .subclass( originalClass, ConstructorStrategy.Default.IMITATE_SUPER_CLASS.withInheritedAnnotations() )
                .name( originalClass.getSimpleName() + "RuntimeGeneratedImpl" )
                .implement( WorkspaceDefinition.class )
                .intercept( FieldAccessor.ofField( "workspace" ) )
                .defineField( "workspace", String.class, Visibility.PRIVATE )
                .annotateType( originalClass.getAnnotations() )
                .make()
                .load( ClassLoader.getSystemClassLoader() )
                .getLoaded();

        return new Bean<T>() {

            final AnnotatedType<T> annotatedType = bm.createAnnotatedType( this.getBeanClass() );
            final InjectionTarget<T> injectionTarget = bm.createInjectionTarget( annotatedType );

            @Override
            public Class<T> getBeanClass() {
                return newClazz;
            }

            @Override
            public Set<InjectionPoint> getInjectionPoints() {
                return injectionTarget.getInjectionPoints();
            }

            @Override
            public boolean isNullable() {
                return false;
            }

            @Override
            public T create( final CreationalContext<T> creationalContext ) {
                final T instance = injectionTarget.produce( creationalContext );
                injectionTarget.inject( instance, creationalContext );
                injectionTarget.postConstruct( instance );
                return instance;
            }

            @Override
            public void destroy( final T t,
                                 final CreationalContext<T> creationalContext ) {
                creationalContext.release();
            }

            @Override
            public Set<Type> getTypes() {

                Set<Class<?>> types = new HashSet<>();

                Class c = this.getBeanClass();
                Arrays.stream( c.getInterfaces() ).forEach( i -> types.add( i ) );
                while ( c != null ) {
                    types.add( c );
                    Arrays.stream( c.getInterfaces() ).forEach( i -> types.add( i ) );
                    c = c.getSuperclass();
                }

                return new HashSet<Type>() {{
                    types.forEach( type -> add( type ) );
                    add( Object.class );
                }};
            }

            @Override
            public Set<Annotation> getQualifiers() {
                return new HashSet<Annotation>() {{
                    add( new AnnotationLiteral<Default>() {
                    } );
                    add( new AnnotationLiteral<Any>() {
                    } );
                }};
            }

            @Override
            public Class<? extends Annotation> getScope() {
                return WorkspaceScoped.class;
            }

            @Override
            public String getName() {
                return this.getBeanClass().getSimpleName();
            }

            @Override
            public Set<Class<? extends Annotation>> getStereotypes() {
                return Collections.emptySet();
            }

            @Override
            public boolean isAlternative() {
                return false;
            }
        };

    }
}
