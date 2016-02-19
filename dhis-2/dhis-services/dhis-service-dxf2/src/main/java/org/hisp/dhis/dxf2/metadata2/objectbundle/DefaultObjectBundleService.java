package org.hisp.dhis.dxf2.metadata2.objectbundle;

/*
 * Copyright (c) 2004-2016, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IdentifiableObjectUtils;
import org.hisp.dhis.preheat.InvalidObject;
import org.hisp.dhis.preheat.Preheat;
import org.hisp.dhis.preheat.PreheatIdentifier;
import org.hisp.dhis.preheat.PreheatMode;
import org.hisp.dhis.preheat.PreheatParams;
import org.hisp.dhis.preheat.PreheatService;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.schema.validation.SchemaValidator;
import org.hisp.dhis.schema.validation.ValidationViolation;
import org.hisp.dhis.user.CurrentUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Component
public class DefaultObjectBundleService implements ObjectBundleService
{
    private static final Log log = LogFactory.getLog( DefaultObjectBundleService.class );

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private PreheatService preheatService;

    @Autowired
    private SchemaValidator schemaValidator;

    @Autowired
    private SchemaService schemaService;

    @Autowired
    private SessionFactory sessionFactory;

    @Autowired
    private IdentifiableObjectManager manager;

    @Override
    public ObjectBundle create( ObjectBundleParams params )
    {
        ObjectBundle bundle = new ObjectBundle( params );
        bundle.putObjects( params.getObjects() );

        if ( params.getUser() == null )
        {
            params.setUser( currentUserService.getCurrentUser() );
        }

        PreheatParams preheatParams = params.getPreheatParams();

        if ( PreheatMode.REFERENCE == preheatParams.getPreheatMode() )
        {
            preheatParams.setReferences( preheatService.collectReferences( params.getObjects() ) );
        }

        bundle.setPreheat( preheatService.preheat( preheatParams ) );

        if ( !(bundle.getImportMode().isCreate() || bundle.getImportMode().isCreateAndUpdate()) )
        {
            return bundle;
        }

        // add preheat placeholders for objects that will be created
        for ( Class<? extends IdentifiableObject> klass : bundle.getObjects().keySet() )
        {
            Map<PreheatIdentifier, Map<Class<? extends IdentifiableObject>, Map<String, IdentifiableObject>>> map = bundle.getPreheat().getMap();

            if ( !map.containsKey( PreheatIdentifier.UID ) )
            {
                map.put( PreheatIdentifier.UID, new HashMap<>() );
            }

            if ( !map.get( PreheatIdentifier.UID ).containsKey( klass ) )
            {
                map.get( PreheatIdentifier.UID ).put( klass, new HashMap<>() );
            }

            if ( !map.containsKey( PreheatIdentifier.CODE ) )
            {
                map.put( PreheatIdentifier.CODE, new HashMap<>() );
            }

            if ( !map.get( PreheatIdentifier.CODE ).containsKey( klass ) )
            {
                map.get( PreheatIdentifier.CODE ).put( klass, new HashMap<>() );
            }

            for ( IdentifiableObject identifiableObject : bundle.getObjects().get( klass ) )
            {
                if ( Preheat.isDefault( identifiableObject ) ) continue;

                if ( !StringUtils.isEmpty( identifiableObject.getUid() ) )
                {
                    map.get( PreheatIdentifier.UID ).get( klass ).put( identifiableObject.getUid(), identifiableObject );
                }

                if ( !StringUtils.isEmpty( identifiableObject.getCode() ) )
                {
                    map.get( PreheatIdentifier.CODE ).get( klass ).put( identifiableObject.getCode(), identifiableObject );
                }
            }
        }

        return bundle;
    }

    @Override
    public ObjectBundleValidation validate( ObjectBundle bundle )
    {
        ObjectBundleValidation objectBundleValidation = new ObjectBundleValidation();

        List<Class<? extends IdentifiableObject>> klasses = getSortedClasses( bundle );

        for ( Class<? extends IdentifiableObject> klass : klasses )
        {
            if ( bundle.getImportMode().isUpdate() || bundle.getImportMode().isDelete() )
            {
                Iterator<IdentifiableObject> iterator = bundle.getObjects().get( klass ).iterator();

                while ( iterator.hasNext() )
                {
                    IdentifiableObject identifiableObject = iterator.next();
                    IdentifiableObject object = bundle.getPreheat().get( bundle.getPreheatIdentifier(), identifiableObject );

                    if ( object == null )
                    {
                        objectBundleValidation.addInvalidObject( klass, new InvalidObject( identifiableObject, bundle.getPreheatIdentifier() ) );
                        iterator.remove();
                    }
                }
            }

            objectBundleValidation.addPreheatValidations( klass, preheatService.checkReferences(
                bundle.getObjects().get( klass ), bundle.getPreheat(), bundle.getPreheatIdentifier() ) );

            List<List<ValidationViolation>> validationViolations = new ArrayList<>();

            for ( IdentifiableObject object : bundle.getObjects().get( klass ) )
            {
                List<ValidationViolation> validate = schemaValidator.validate( object );

                if ( !validate.isEmpty() )
                {
                    validationViolations.add( validate );
                }
            }

            objectBundleValidation.addValidationViolation( klass, validationViolations );
        }

        bundle.setObjectBundleStatus( ObjectBundleStatus.VALIDATED );

        return objectBundleValidation;
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public void commit( ObjectBundle bundle )
    {
        if ( ObjectBundleMode.VALIDATE == bundle.getObjectBundleMode() )
        {
            return; // skip if validate only
        }

        Session session = sessionFactory.getCurrentSession();

        List<Class<? extends IdentifiableObject>> klasses = getSortedClasses( bundle );

        for ( Class<? extends IdentifiableObject> klass : klasses )
        {
            List<IdentifiableObject> objects = bundle.getObjects().get( klass );

            if ( objects.isEmpty() )
            {
                continue;
            }

            switch ( bundle.getImportMode() )
            {
                case CREATE_AND_UPDATE:
                case NEW_AND_UPDATES:
                {
                    handleCreatesAndUpdates( session, objects, bundle );
                    break;
                }
                case CREATE:
                case NEW:
                {
                    handleCreates( session, objects, bundle );
                    break;
                }
                case UPDATE:
                case UPDATES:
                {
                    handleUpdates( session, objects, bundle );
                    break;
                }
                case DELETE:
                case DELETES:
                {
                    handleDeletes( session, objects, bundle );
                    break;
                }
            }
        }

        bundle.setObjectBundleStatus( ObjectBundleStatus.COMMITTED );
    }

    private void handleCreatesAndUpdates( Session session, List<IdentifiableObject> objects, ObjectBundle bundle )
    {

    }

    private void handleCreates( Session session, List<IdentifiableObject> objects, ObjectBundle bundle )
    {
        for ( IdentifiableObject object : objects )
        {
            if ( Preheat.isDefault( object ) ) continue;

            preheatService.connectReferences( object, bundle.getPreheat(), bundle.getPreheatIdentifier() );
            manager.save( object, bundle.getUser() );

            bundle.getPreheat().put( bundle.getPreheatIdentifier(), object );

            if ( log.isDebugEnabled() )
            {
                String msg = "Created object '" + IdentifiableObjectUtils.getDisplayName( object ) + "'";
                log.debug( msg );
            }
        }
    }

    private void handleUpdates( Session session, List<IdentifiableObject> objects, ObjectBundle bundle )
    {

    }

    private void handleDeletes( Session session, List<IdentifiableObject> objects, ObjectBundle bundle )
    {
        List<IdentifiableObject> persistedObjects = bundle.getPreheat().getAll( bundle.getPreheatIdentifier(), objects );

        for ( IdentifiableObject object : persistedObjects )
        {
            manager.delete( object, bundle.getUser() );

            bundle.getPreheat().remove( bundle.getPreheatIdentifier(), object );

            if ( log.isDebugEnabled() )
            {
                String msg = "Deleted object '" + IdentifiableObjectUtils.getDisplayName( object ) + "'";
                log.debug( msg );
            }
        }
    }

    @SuppressWarnings( "unchecked" )
    private List<Class<? extends IdentifiableObject>> getSortedClasses( ObjectBundle bundle )
    {
        List<Class<? extends IdentifiableObject>> klasses = new ArrayList<>();

        schemaService.getMetadataSchemas().forEach( schema -> {
            Class<? extends IdentifiableObject> klass = (Class<? extends IdentifiableObject>) schema.getKlass();

            if ( bundle.getObjects().containsKey( klass ) )
            {
                klasses.add( klass );
            }
        } );

        return klasses;
    }
}
