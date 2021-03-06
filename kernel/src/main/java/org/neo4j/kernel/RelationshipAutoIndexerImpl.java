/**
 * Copyright (c) 2002-2011 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.kernel;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.event.PropertyEntry;
import org.neo4j.graphdb.event.TransactionData;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.graphdb.index.ReadableRelationshipIndex;
import org.neo4j.graphdb.index.RelationshipAutoIndexer;
import org.neo4j.graphdb.index.RelationshipIndex;

class RelationshipAutoIndexerImpl extends AbstractAutoIndexerImpl<Relationship>
        implements RelationshipAutoIndexer
{
    static final String RELATIONSHIP_AUTO_INDEX = "relationship_auto_index";

    public RelationshipAutoIndexerImpl( EmbeddedGraphDbImpl gdb )
    {
        super( gdb );
    }

    @Override
    protected Iterable<PropertyEntry<Relationship>> getAssignedPropertiesOnCommit(
            TransactionData data )
    {
        return data.assignedRelationshipProperties();
    }

    @Override
    protected String getAutoIndexConfigListName()
    {
        return Config.RELATIONSHIP_KEYS_INDEXABLE;
    }

    @Override
    protected String getAutoIndexName()
    {
        return RELATIONSHIP_AUTO_INDEX;
    }

    @Override
    protected String getEnableConfigName()
    {
        return Config.RELATIONSHIP_AUTO_INDEXING;
    }

    @Override
    protected RelationshipIndex getIndexInternal()
    {
        return ( (IndexManagerImpl) getGraphDbImpl().index() ).getOrCreateRelationshipIndex(
                RELATIONSHIP_AUTO_INDEX, null );
    }

    @Override
    public ReadableRelationshipIndex getAutoIndex()
    {
        return getIndexInternal();
    }

    @Override
    protected Iterable<PropertyEntry<Relationship>> getRemovedPropertiesOnCommit(
            TransactionData data )
    {
        return data.removedRelationshipProperties();
    }

    static class RelationshipReadOnlyIndexToIndexAdapter extends
            ReadOnlyIndexToIndexAdapter<Relationship> implements
            RelationshipIndex
    {
        private final ReadableRelationshipIndex delegate;

        public RelationshipReadOnlyIndexToIndexAdapter(
                ReadableRelationshipIndex delegate )
        {
            super( delegate );
            this.delegate = delegate;
        }

        @Override
        public IndexHits<Relationship> get( String key, Object valueOrNull,
                Node startNodeOrNull, Node endNodeOrNull )
        {
            return delegate.get( key,
                    valueOrNull, startNodeOrNull,
                    endNodeOrNull );
        }

        @Override
        public IndexHits<Relationship> query( String key,
                Object queryOrQueryObjectOrNull, Node startNodeOrNull,
                Node endNodeOrNull )
        {
            return delegate.query( key,
                    queryOrQueryObjectOrNull,
                    startNodeOrNull, endNodeOrNull );
        }

        @Override
        public IndexHits<Relationship> query( Object queryOrQueryObjectOrNull,
                Node startNodeOrNull, Node endNodeOrNull )
        {
            return delegate.query(
                    queryOrQueryObjectOrNull, startNodeOrNull,
                    endNodeOrNull );
        }
    }
}
