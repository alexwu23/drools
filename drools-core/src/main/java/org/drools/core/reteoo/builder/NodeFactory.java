/*
 * Copyright 2010 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package org.drools.core.reteoo.builder;


import org.drools.core.common.BaseNode;
import org.drools.core.common.BetaConstraints;
import org.drools.core.reteoo.AlphaNode;
import org.drools.core.reteoo.EntryPointNode;
import org.drools.core.reteoo.JoinNode;
import org.drools.core.reteoo.LeftInputAdapterNode;
import org.drools.core.reteoo.LeftTupleSource;
import org.drools.core.reteoo.ObjectSource;
import org.drools.core.reteoo.ObjectTypeNode;
import org.drools.core.reteoo.QueryElementNode;
import org.drools.core.reteoo.TerminalNode;
import org.drools.core.rule.Declaration;
import org.drools.core.rule.From;
import org.drools.core.rule.GroupElement;
import org.drools.core.rule.QueryElement;
import org.drools.core.rule.Rule;
import org.drools.core.spi.AlphaNodeFieldConstraint;
import org.drools.core.spi.DataProvider;
import org.drools.core.spi.ObjectType;
import org.drools.core.time.impl.Timer;

public interface NodeFactory {


    public AlphaNode buildAlphaNode( final int id,
                                     final AlphaNodeFieldConstraint constraint,
                                     final ObjectSource objectSource,
                                     final BuildContext context );

    public TerminalNode buildTerminalNode( int id,
                                           LeftTupleSource source,
                                           Rule rule,
                                           GroupElement subrule,
                                           int subruleIndex,
                                           BuildContext context );

    public ObjectTypeNode buildObjectTypeNode( int id,
                                               EntryPointNode objectSource,
                                               ObjectType objectType,
                                               BuildContext context );
    
    public JoinNode buildJoinNode( final int id,
                                   final LeftTupleSource leftInput,
                                   final ObjectSource rightInput,
                                   final BetaConstraints binder,
                                   final BuildContext context );

    public LeftInputAdapterNode buildLeftInputAdapterNode( int nextId,
                                                           ObjectSource objectSource,
                                                           BuildContext context );

    public TerminalNode buildQueryTerminalNode( int id,
                                                LeftTupleSource source,
                                                Rule rule,
                                                GroupElement subrule,
                                                int subruleIndex,
                                                BuildContext context );

    public QueryElementNode buildQueryElementNode( int nextId,
                                                   LeftTupleSource tupleSource,
                                                   QueryElement qe,
                                                   boolean tupleMemoryEnabled,
                                                   boolean openQuery,
                                                   BuildContext context );

    public BaseNode buildFromNode( int id,
                                   DataProvider dataProvider,
                                   LeftTupleSource tupleSource,
                                   AlphaNodeFieldConstraint[] alphaNodeFieldConstraints,
                                   BetaConstraints betaConstraints,
                                   boolean tupleMemoryEnabled,
                                   BuildContext context,
                                   From from );

    public BaseNode buildTimerNode( int id,
                                    Timer timer,
                                    final String[] calendarNames,
                                    final Declaration[][]   declarations,
                                    LeftTupleSource tupleSource,
                                    BuildContext context  );
}
