/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.commons.rdf.jena.impl;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.rdf.api.BlankNodeOrIRI;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.QuadLike;
import org.apache.commons.rdf.api.RDFTerm;
import org.apache.commons.rdf.jena.JenaQuad;
import org.apache.commons.rdf.jena.JenaQuadLike;
import org.apache.commons.rdf.jena.JenaRDF;
import org.apache.commons.rdf.jena.JenaTriple;
import org.apache.jena.graph.Factory;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.core.Quad;

/**
 * A generalized {@link QuadLike}, backed by a Jena {@link Quad} or
 * {@link Triple}.
 * <p>
 * This class does not implement any particular {@link #equals(Object)} or
 * {@link #hashCode()} but can otherwise be used as a base class for both a
 * {@link JenaTriple} and a {@link JenaQuad}.
 * 
 * @see JenaTripleImpl
 * @see JenaQuadImpl
 * @see internalJenaFactory#createGeneralizedTriple(RDFTerm, RDFTerm, RDFTerm)
 * @see internalJenaFactory#createGeneralizedQuad(RDFTerm, RDFTerm, RDFTerm,
 *      RDFTerm)
 *
 */
abstract class AbstractQuadLike<S extends RDFTerm, P extends RDFTerm, O extends RDFTerm, G extends RDFTerm>
        implements JenaQuadLike<G> {

    private static InternalJenaFactory internalJenaFactory = new InternalJenaFactory() {
    };

    /**
     * COMMONSRDF-55 - special handling of urn:x-arq:DefaultGraph and friends
     */
    private static class DefaultGraphChecker {
        // Fixed UUID for comparison of defaultGraphNodeGenerated
        private UUID salt = UUID.fromString("aaa6bf96-ea58-4a55-9485-3733403a1f24");     
        private RDFTerm defaultGraph = internalJenaFactory.createRDFTerm(Quad.defaultGraphIRI, salt);
        private RDFTerm defaultGraphNodeGenerated = internalJenaFactory.createRDFTerm(Quad.defaultGraphNodeGenerated, salt);
        
        public boolean isNotDefaultGraph(RDFTerm graphName) {
            return !(graphName.equals(defaultGraph) || graphName.equals(defaultGraphNodeGenerated)); 
        }
    }
    
    private static DefaultGraphChecker defaultGraphChecker = new DefaultGraphChecker();

    final Optional<G> graphName;
    final S subject;
    final P predicate;
    final O object;
    org.apache.jena.sparql.core.Quad quad = null;
    org.apache.jena.graph.Triple triple = null;

    
    AbstractQuadLike(final S subject, final P predicate, final O object, final Optional<G> graphName) {
        this.subject = Objects.requireNonNull(subject);
        this.predicate = Objects.requireNonNull(predicate);
        this.object = Objects.requireNonNull(object);
        this.graphName = Objects.requireNonNull(graphName).filter(defaultGraphChecker::isNotDefaultGraph);
    }

    AbstractQuadLike(final S subject, final P predicate, final O object) {
        this(subject, predicate, object, Optional.empty());
    }

    @SuppressWarnings("unchecked")
    AbstractQuadLike(final org.apache.jena.sparql.core.Quad quad, final UUID salt) {
        this.quad = Objects.requireNonNull(quad);
        this.subject = (S) internalJenaFactory.createRDFTerm(quad.getSubject(), salt);
        this.predicate = (P) internalJenaFactory.createRDFTerm(quad.getPredicate(), salt);
        this.object = (O) internalJenaFactory.createRDFTerm(quad.getObject(), salt);
        if (quad.isDefaultGraph()) {
            this.graphName = Optional.empty();
        } else { 
            this.graphName = Optional.of((G) internalJenaFactory.createRDFTerm(quad.getGraph(), salt));
        }
    }

    @SuppressWarnings("unchecked")
    AbstractQuadLike(final org.apache.jena.graph.Triple triple, final UUID salt) {
        this.triple = Objects.requireNonNull(triple);
        this.subject = (S) internalJenaFactory.createRDFTerm(triple.getSubject(), salt);
        this.predicate = (P) internalJenaFactory.createRDFTerm(triple.getPredicate(), salt);
        this.object = (O) internalJenaFactory.createRDFTerm(triple.getObject(), salt);
        this.graphName = Optional.empty();
    }

    @Override
    public org.apache.jena.sparql.core.Quad asJenaQuad() {
        final JenaRDF factory = new JenaRDF();
        if (quad == null) {
            quad = org.apache.jena.sparql.core.Quad.create(
                    graphName.map(factory::asJenaNode).orElse(Quad.defaultGraphIRI),
                    factory.asJenaNode(subject), 
                    factory.asJenaNode(predicate), 
                    factory.asJenaNode(object));
        }
        return quad;
    }

    @Override
    public org.apache.jena.graph.Triple asJenaTriple() {
        final JenaRDF factory = new JenaRDF();
        if (triple == null) {
            triple = org.apache.jena.graph.Triple.create(
                    factory.asJenaNode(subject), 
                    factory.asJenaNode(predicate),
                    factory.asJenaNode(object));
        }
        return triple;
    }

    @Override
    public S getSubject() {
        return subject;
    }

    @Override
    public P getPredicate() {
        return predicate;
    }

    @Override
    public O getObject() {
        return object;
    }

    @Override
    public Optional<G> getGraphName() {
        return graphName;
    }

    @Override
    public String toString() {
        // kind of nquad syntax
        return getSubject().ntriplesString() + " " + getPredicate().ntriplesString() + " "
                + getObject().ntriplesString() + " " + getGraphName().map(RDFTerm::ntriplesString).orElse("") + ".";
    }

}
