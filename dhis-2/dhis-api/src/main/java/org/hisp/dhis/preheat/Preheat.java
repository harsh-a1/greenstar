package org.hisp.dhis.preheat;

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

import org.hisp.dhis.common.IdentifiableObject;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class Preheat
{
    private Map<PreheatIdentifier, Map<Class<? extends IdentifiableObject>, Map<String, IdentifiableObject>>> map = new HashMap<>();

    public Preheat()
    {
    }

    @SuppressWarnings( "unchecked" )
    public <T extends IdentifiableObject> T get( PreheatIdentifier identifier, Class<? extends IdentifiableObject> klass, String key )
    {
        if ( !containsKey( identifier, klass, key ) )
        {
            return null;
        }

        return (T) map.get( identifier ).get( klass ).get( key );
    }

    public <T extends IdentifiableObject> T get( PreheatIdentifier identifier, T object )
    {
        if ( object == null )
        {
            return null;
        }

        T reference = null;

        if ( PreheatIdentifier.UID == identifier || PreheatIdentifier.AUTO == identifier )
        {
            reference = get( PreheatIdentifier.UID, object.getClass(), object.getUid() );
        }

        if ( PreheatIdentifier.CODE == identifier || (reference == null && PreheatIdentifier.AUTO == identifier) )
        {
            reference = get( PreheatIdentifier.CODE, object.getClass(), object.getCode() );
        }

        return reference;
    }

    public boolean containsKey( PreheatIdentifier identifier, Class<? extends IdentifiableObject> klass, String key )
    {
        return !(isEmpty() || isEmpty( identifier ) || isEmpty( identifier, klass )) && map.get( identifier ).get( klass ).containsKey( key );
    }

    public boolean isEmpty()
    {
        return map.isEmpty();
    }

    public boolean isEmpty( PreheatIdentifier identifier )
    {
        return !map.containsKey( identifier ) || map.get( identifier ).isEmpty();
    }

    public boolean isEmpty( PreheatIdentifier identifier, Class<? extends IdentifiableObject> klass )
    {
        return isEmpty( identifier ) || !map.get( identifier ).containsKey( klass ) || map.get( identifier ).get( klass ).isEmpty();
    }

    public <T extends IdentifiableObject> Preheat put( PreheatIdentifier identifier, T object )
    {
        if ( object == null ) return this;
        if ( !map.containsKey( identifier ) ) map.put( identifier, new HashMap<>() );
        if ( !map.get( identifier ).containsKey( object.getClass() ) ) map.get( identifier ).put( object.getClass(), new HashMap<>() );

        Map<String, IdentifiableObject> identifierMap = map.get( identifier ).get( object.getClass() );
        String key = identifier.getIdentifier( object );
        identifierMap.put( key, object );

        return this;
    }

    public <T extends IdentifiableObject> Preheat put( PreheatIdentifier identifier, Collection<T> objects )
    {
        for ( T object : objects )
        {
            put( identifier, object );
        }

        return this;
    }

    public Preheat remove( PreheatIdentifier identifier, Class<? extends IdentifiableObject> klass, String key )
    {
        if ( containsKey( identifier, klass, key ) )
        {
            map.get( identifier ).get( klass ).remove( key );
        }

        return this;
    }

    public Preheat remove( PreheatIdentifier identifier, Class<? extends IdentifiableObject> klass, Collection<String> keys )
    {
        for ( String key : keys )
        {
            remove( identifier, klass, key );
        }

        return this;
    }
}
